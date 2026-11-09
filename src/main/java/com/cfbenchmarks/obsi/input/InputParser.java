package com.cfbenchmarks.obsi.input;

import com.fasterxml.jackson.core.StreamReadFeature;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.cfg.JsonNodeFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Reads and validates the input snapshot: shape and type only.
 *
 * <p>Spec §2, first paragraph, last sentence: "Malformed top-level input and missing required
 * top-level fields are input errors." The value rules of the same paragraph (price and size
 * strictly positive, empty sides, best bid at or above best ask) are screening, not parsing, and
 * are not applied here.
 *
 * <p>Input errors are returned as {@link InputResult.Rejected} with a short reason (D-08); nothing
 * about the input makes this class throw.
 */
public final class InputParser {

    static final String INVALID_JSON = "input error: invalid JSON";
    static final String CANNOT_READ = "input error: cannot read input file";
    static final String ROOT_NOT_OBJECT = "input error: root must be an object";
    static final String TIME_MISSING = "input error: calculationTime is missing";
    static final String TIME_INVALID =
            "input error: calculationTime must be an ISO-8601 UTC timestamp ending in Z";
    static final String BOOKS_MISSING = "input error: books is missing";
    static final String BOOKS_NOT_ARRAY = "input error: books must be an array";
    static final String UNLOCK_NOT_ARRAY = "input error: unlockEvents must be an array";

    private static final ObjectMapper JSON = JsonMapper.builder()
            // CLAUDE.md "Numbers": 100.2 must never pass through a double.
            .enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS)
            // Keep the scale as written (100.0 stays 100.0) instead of Jackson's default stripping.
            .disable(JsonNodeFeature.STRIP_TRAILING_BIGDECIMAL_ZEROES)
            // D-06: "valid JSON" means the whole document, so trailing content is an error.
            .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
            // D-17: a duplicate key inside one object is invalid JSON.
            .enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION)
            // D-05: NaN and Infinity stay invalid (ALLOW_NON_NUMERIC_NUMBERS is off by default).
            .build();

    private InputParser() {
    }

    /** Reads a file as UTF-8 JSON. A missing or unreadable file is an input error (D-10). */
    public static InputResult parse(Path file) {
        byte[] bytes;
        try {
            bytes = Files.readAllBytes(file);
        } catch (IOException | SecurityException e) {
            return reject(CANNOT_READ);
        }
        return parse(bytes);
    }

    /**
     * As {@link #parse(Path)}, and with {@code readUnlockEvents} also reads the optional {@code unlockEvents} (what
     * {@code --new-ped} needs). Without it the field is not looked at (D-04); with it a malformed value is an input
     * error (D-23).
     */
    public static InputResult parse(Path file, boolean readUnlockEvents) {
        byte[] bytes;
        try {
            bytes = Files.readAllBytes(file);
        } catch (IOException | SecurityException e) {
            return reject(CANNOT_READ);
        }
        return parse(bytes, readUnlockEvents);
    }

    /** Parses the UTF-8 bytes of an input document. {@code unlockEvents} is ignored (D-04). */
    public static InputResult parse(byte[] json) {
        return parse(json, false);
    }

    /** As {@link #parse(byte[])}, and with {@code readUnlockEvents} also reads {@code unlockEvents} (D-23). */
    public static InputResult parse(byte[] json, boolean readUnlockEvents) {
        JsonNode root;
        try {
            root = JSON.readTree(json);
        } catch (IOException e) {
            return reject(INVALID_JSON);
        }
        // An empty or blank document yields a missing node: not valid JSON.
        if (root == null || root.isMissingNode()) {
            return reject(INVALID_JSON);
        }

        // D-06: the root must be an object.
        if (!root.isObject()) {
            return reject(ROOT_NOT_OBJECT);
        }

        // D-07: calculationTime is required; it is validated but echoed unchanged.
        JsonNode time = root.get("calculationTime");
        if (time == null || time.isNull()) {
            return reject(TIME_MISSING);
        }
        if (!isUtcInstant(time)) {
            return reject(TIME_INVALID);
        }

        // D-06, D-07: books is required and must be an array (it may be empty).
        JsonNode booksNode = root.get("books");
        if (booksNode == null || booksNode.isNull()) {
            return reject(BOOKS_MISSING);
        }
        if (!booksNode.isArray()) {
            return reject(BOOKS_NOT_ARRAY);
        }

        // D-06: each element must be an object with a valid exchange name. Checked book by book in
        // order; duplicate names are checked afterwards.
        List<BookInput> books = new ArrayList<>(booksNode.size());
        for (int i = 0; i < booksNode.size(); i++) {
            JsonNode element = booksNode.get(i);
            if (!element.isObject()) {
                return reject("input error: books[" + i + "] must be an object");
            }
            JsonNode name = element.get("exchange");
            // D-03, D-16: the name must be a string that is not empty or whitespace-only.
            if (name == null || !name.isTextual() || name.textValue().isBlank()) {
                return reject("input error: books[" + i + "].exchange must be a nonempty string");
            }
            books.add(readBook(name.textValue(), element));
        }

        // D-02, D-16, D-18: duplicate names are an input error. Compared stripped and
        // case-insensitively (Locale.ROOT); strip() uses the same whitespace definition as the
        // isBlank() check above. The names themselves are never altered.
        Set<String> seen = new HashSet<>();
        for (BookInput book : books) {
            if (!seen.add(book.exchange().strip().toLowerCase(Locale.ROOT))) {
                return reject("input error: duplicate exchange name \"" + book.exchange() + "\"");
            }
        }

        // D-23: checked last, so every other input error keeps its precedence. Without --new-ped the field is not
        // looked at (D-04).
        List<Instant> unlockStarts = new ArrayList<>();
        if (readUnlockEvents) {
            String problem = readUnlockEvents(root.get("unlockEvents"), unlockStarts);
            if (problem != null) {
                return reject(problem);
            }
        }

        return new InputResult.Accepted(new Snapshot(time.textValue(), books, unlockStarts));
    }

    /**
     * D-23: absent means no unlock; otherwise an array of objects, each with a {@code start} that is a UTC instant
     * (the D-15 rule). Extra keys in an event are ignored (D-04).
     *
     * @param starts receives the {@code start} of every event, in input order
     * @return the input error reason, or null if the field is well formed
     */
    private static String readUnlockEvents(JsonNode events, List<Instant> starts) {
        if (events == null) {
            return null;
        }
        if (!events.isArray()) {
            return UNLOCK_NOT_ARRAY;
        }
        for (int i = 0; i < events.size(); i++) {
            JsonNode event = events.get(i);
            if (!event.isObject()) {
                return "input error: unlockEvents[" + i + "] must be an object";
            }
            JsonNode start = event.get("start");
            if (start == null || !isUtcInstant(start)) {
                return "input error: unlockEvents[" + i + "].start must be an ISO-8601 UTC timestamp ending in Z";
            }
            starts.add(Instant.parse(start.textValue()));
        }
        return null;
    }

    /**
     * D-03, D-06: a book with a valid name whose {@code bids} or {@code asks} is missing or not an
     * array is kept as an {@link BookInput.UnreadableBook}, so it can be excluded and reported
     * (spec §2: "Exclude a book if its structure cannot be read").
     */
    private static BookInput readBook(String exchange, JsonNode book) {
        JsonNode bids = book.get("bids");
        JsonNode asks = book.get("asks");
        if (bids == null || !bids.isArray() || asks == null || !asks.isArray()) {
            return new BookInput.UnreadableBook(exchange);
        }
        return new BookInput.ReadableBook(exchange, readLevels(bids), readLevels(asks));
    }

    /**
     * Keeps the entries that are an array of exactly two JSON numbers (spec §2: "discard a
     * price/size entry if either component is not numeric"). D-01: only JSON numbers are numeric,
     * strings are not. D-05: wrong length, null, boolean, nested array or object: discarded.
     * Whether a number is strictly positive is a §2 value rule and is not checked here.
     */
    private static List<Level> readLevels(JsonNode side) {
        List<Level> levels = new ArrayList<>(side.size());
        for (JsonNode entry : side) {
            if (entry.isArray() && entry.size() == 2 && entry.get(0).isNumber() && entry.get(1).isNumber()) {
                levels.add(new Level(entry.get(0).decimalValue(), entry.get(1).decimalValue()));
            }
        }
        return levels;
    }

    /**
     * D-07, D-15: a JSON string that {@link Instant#parse} accepts and that ends in an uppercase
     * {@code Z}. {@code Instant.parse} alone also accepts offsets and a lowercase {@code z}, which
     * are not UTC timestamps in the sense of the brief.
     */
    private static boolean isUtcInstant(JsonNode time) {
        if (!time.isTextual() || !time.textValue().endsWith("Z")) {
            return false;
        }
        try {
            Instant.parse(time.textValue());
            return true;
        } catch (DateTimeParseException e) {
            return false;
        }
    }

    private static InputResult reject(String reason) {
        return new InputResult.Rejected(reason);
    }
}
