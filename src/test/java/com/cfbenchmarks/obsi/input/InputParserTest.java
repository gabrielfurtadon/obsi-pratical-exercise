package com.cfbenchmarks.obsi.input;

import com.cfbenchmarks.obsi.input.BookInput.ReadableBook;
import com.cfbenchmarks.obsi.input.BookInput.UnreadableBook;
import com.cfbenchmarks.obsi.input.InputResult.Accepted;
import com.cfbenchmarks.obsi.input.InputResult.Rejected;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Parsing handles shape and type only (D-01 to D-07, D-15 to D-17). The value rules (price and
 * size > 0, empty sides, crossed books) are §2 and belong to the screening phase, so several
 * tests below pin that those values survive parsing.
 */
class InputParserTest {

    private static final String TIME = "2026-10-27T12:00:00Z";
    private static final String TIME_FIELD = "\"calculationTime\":\"" + TIME + "\"";
    private static final String GOOD_BOOK = book("A", "[[100,1]]", "[[101,1]]");

    // ---------------------------------------------------------------- helpers

    private static String quote(String s) {
        return JsonNodeFactory.instance.textNode(s).toString();
    }

    private static String book(String name, String bids, String asks) {
        return "{\"exchange\":" + quote(name) + ",\"bids\":" + bids + ",\"asks\":" + asks + "}";
    }

    private static String doc(String... books) {
        return "{" + TIME_FIELD + ",\"books\":[" + String.join(",", books) + "]}";
    }

    private static InputResult parse(String json) {
        return InputParser.parse(json.getBytes(StandardCharsets.UTF_8));
    }

    private static Snapshot accepted(String json) {
        InputResult result = parse(json);
        if (!(result instanceof Accepted a)) {
            throw new AssertionError("expected Accepted but was " + result + " for " + json);
        }
        return a.snapshot();
    }

    private static String rejection(String json) {
        return rejection(parse(json));
    }

    private static String rejection(InputResult result) {
        if (!(result instanceof Rejected r)) {
            throw new AssertionError("expected Rejected but was " + result);
        }
        return r.reason();
    }

    private static Level lv(String price, String size) {
        return new Level(new BigDecimal(price), new BigDecimal(size));
    }

    private static ReadableBook readable(Snapshot s, int i) {
        assertThat(s.books().get(i)).isInstanceOf(ReadableBook.class);
        return (ReadableBook) s.books().get(i);
    }

    private static Path dataFile(String name) throws IOException {
        try (Stream<Path> files = Files.walk(Path.of("data"))) {
            return files.filter(p -> p.getFileName().toString().equals(name)).findFirst()
                    .orElseThrow(() -> new AssertionError(name + " not found under data/"));
        }
    }

    // ---------------------------------------------------------------- accepted input

    /** P1. D-07: calculationTime is echoed as given; books keep input order. */
    @Test
    void acceptsMinimalSnapshotAndKeepsOrder() {
        Snapshot s = accepted(doc(book("B", "[[100,1]]", "[[101,1]]"), book("A", "[[100,1]]", "[[101,1]]")));

        assertThat(s.calculationTime()).isEqualTo(TIME);
        assertThat(s.books()).extracting(BookInput::exchange).containsExactly("B", "A");
    }

    /** P1 (D-07). The validated string is kept verbatim, fractional seconds included. */
    @Test
    void echoesCalculationTimeVerbatim() {
        String json = "{\"calculationTime\":\"2026-10-27T12:00:00.100Z\",\"books\":[]}";

        assertThat(accepted(json).calculationTime()).isEqualTo("2026-10-27T12:00:00.100Z");
    }

    /** P2. The reference fixture. */
    @Test
    void parsesSampleInput() throws IOException {
        Snapshot s = ((Accepted) InputParser.parse(dataFile("sample-input.json"))).snapshot();

        assertThat(s.calculationTime()).isEqualTo("2026-10-27T12:00:00Z");
        assertThat(s.books()).extracting(BookInput::exchange).containsExactly("Alpha", "Beta", "Gamma", "Delta");
        assertThat(s.books()).allSatisfy(b -> assertThat(b).isInstanceOf(ReadableBook.class));
        assertThat(readable(s, 0).bids()).containsExactly(
                lv("100.0", "1.2"), lv("99.8", "1.3"), lv("99.0", "2.0"), lv("98.0", "2.0"));
        assertThat(readable(s, 0).asks()).containsExactly(
                lv("100.4", "1.1"), lv("100.6", "1.4"), lv("101.2", "2.0"), lv("102.0", "2.0"));
        // Gamma is crossed (bid 100.6 > ask 100.5): kept, §2 screening is a later phase.
        assertThat(readable(s, 2).bids()).containsExactly(lv("100.6", "1.0"));
        assertThat(readable(s, 2).asks()).containsExactly(lv("100.5", "1.0"));
    }

    /** P3. D-01 (string price dropped), D-11 (no sorting), and the phase-1/phase-2 line. */
    @Test
    void parsesEdgeCasesAndKeepsWhatPhaseTwoOwns() throws IOException {
        Snapshot s = ((Accepted) InputParser.parse(dataFile("edge-cases.json"))).snapshot();

        assertThat(s.books()).extracting(BookInput::exchange)
                .containsExactly("Alpha", "Bravo", "Charlie", "Delta", "Echo", "Foxtrot");
        // Alpha's bids are unsorted in the input and stay that way (D-11 is applied in screening).
        assertThat(readable(s, 0).bids()).containsExactly(lv("99.6", "3"), lv("99.9", "2"));
        // Echo is crossed: kept.
        assertThat(readable(s, 4).bids()).containsExactly(lv("100.2", "1"));
        assertThat(readable(s, 4).asks()).containsExactly(lv("100.1", "1"));
        // Foxtrot: the string-price entry is dropped (D-01); the -1 size is a value rule and survives.
        assertThat(readable(s, 5).bids()).containsExactly(lv("99.8", "-1"), lv("99.85", "1"));
    }

    /** P4. D-07: an empty books array is valid input (it fails later as §6). */
    @Test
    void acceptsEmptyBooksArray() {
        Snapshot s = accepted(doc());

        assertThat(s.books()).isEmpty();
    }

    /** P5. D-04: unknown fields are ignored at top level and book level. */
    @Test
    void ignoresUnknownFields() {
        String json = "{" + TIME_FIELD + ",\"extra\":{\"x\":[1,2]},\"books\":["
                + "{\"exchange\":\"A\",\"note\":\"hi\",\"bids\":[[100,1]],\"asks\":[[101,1]],\"depth\":7}]}";

        Snapshot s = accepted(json);

        assertThat(readable(s, 0).bids()).containsExactly(lv("100", "1"));
        assertThat(readable(s, 0).asks()).containsExactly(lv("101", "1"));
    }

    // ---------------------------------------------------------------- numbers

    /** P6. CLAUDE.md numbers: JSON numbers become exact BigDecimals (scale included), never doubles. */
    @ParameterizedTest
    @ValueSource(strings = {"100.2", "100.0", "0.1", "100.12345678901234567890123456789", "7", "12345678901234567890123"})
    void readsNumbersAsExactBigDecimals(String text) {
        Snapshot s = accepted(doc(book("A", "[[" + text + "," + text + "]]", "[]")));

        Level level = readable(s, 0).bids().get(0);
        assertThat(level.price()).isEqualTo(new BigDecimal(text));
        assertThat(level.size()).isEqualTo(new BigDecimal(text));
    }

    // ---------------------------------------------------------------- entry screen: shape and type only

    private static void assertEntryDiscarded(String entry) {
        String bids = "[[99,1]," + entry + ",[98,2]]";
        Snapshot s = accepted(doc(book("A", bids, "[[101,1]," + entry + "]")));

        assertThat(readable(s, 0).bids()).containsExactly(lv("99", "1"), lv("98", "2"));
        assertThat(readable(s, 0).asks()).containsExactly(lv("101", "1"));
    }

    /** P8. D-01: a price or size given as a JSON string is not numeric; only that entry is discarded. */
    @ParameterizedTest
    @ValueSource(strings = {"[\"100\",1]", "[100,\"1\"]", "[\"1e3\",1]", "[100,\"1e3\"]", "[\"\",1]"})
    void discardsEntriesWithStringNumbers(String entry) {
        assertEntryDiscarded(entry);
    }

    /** P9. D-05: an entry that is not an array of exactly two elements is discarded. */
    @ParameterizedTest
    @ValueSource(strings = {"[]", "[1]", "[1,2,3]", "5", "\"x\"", "{}", "null", "true"})
    void discardsEntriesThatAreNotTwoElementArrays(String entry) {
        assertEntryDiscarded(entry);
    }

    /** P10. D-05: null, boolean, nested array or object components make the entry discarded. */
    @ParameterizedTest
    @ValueSource(strings = {"[null,1]", "[1,null]", "[true,1]", "[1,false]", "[[1],1]", "[1,[2]]",
            "[{\"a\":1},1]", "[1,{}]"})
    void discardsEntriesWithNonNumericComponents(String entry) {
        assertEntryDiscarded(entry);
    }

    /** P11. Zero and negative values are §2 value rules: they survive parsing. */
    @Test
    void keepsNonPositiveValuesForPhaseTwo() {
        Snapshot s = accepted(doc(book("A", "[[0,1],[1,0],[-5,1],[1,-1]]", "[]")));

        assertThat(readable(s, 0).bids())
                .containsExactly(lv("0", "1"), lv("1", "0"), lv("-5", "1"), lv("1", "-1"));
    }

    /** P12. D-03/D-06: bad entries leave the book readable with an empty side, not unreadable. */
    @Test
    void booksWithOnlyMalformedEntriesStayReadableWithEmptySide() {
        Snapshot s = accepted(doc(book("A", "[\"x\",[1],null]", "[]")));

        assertThat(s.books()).hasSize(1);
        assertThat(readable(s, 0).bids()).isEmpty();
        assertThat(readable(s, 0).asks()).isEmpty();
    }

    // ---------------------------------------------------------------- book level

    static Stream<Arguments> unreadableSides() {
        return Stream.of("{}", "\"x\"", "5", "true", "null", "<missing>")
                .flatMap(bad -> Stream.of(Arguments.of("bids", bad), Arguments.of("asks", bad)));
    }

    /** P13. D-03, D-06: a named book with a missing or non-array side is kept as unreadable, in place. */
    @ParameterizedTest(name = "{0} = {1}")
    @MethodSource("unreadableSides")
    void keepsBookWithUnreadableSideAsUnreadable(String side, String bad) {
        String bids = side.equals("bids") ? bad : "[[100,1]]";
        String asks = side.equals("asks") ? bad : "[[101,1]]";
        String broken = "<missing>".equals(bad)
                ? "{\"exchange\":\"B\"," + (side.equals("bids") ? "\"asks\":" + asks : "\"bids\":" + bids) + "}"
                : book("B", bids, asks);

        Snapshot s = accepted(doc(book("A", "[[100,1]]", "[[101,1]]"), broken, book("C", "[[100,1]]", "[[101,1]]")));

        assertThat(s.books()).extracting(BookInput::exchange).containsExactly("A", "B", "C");
        assertThat(s.books().get(0)).isInstanceOf(ReadableBook.class);
        assertThat(s.books().get(1)).isEqualTo(new UnreadableBook("B"));
        assertThat(s.books().get(2)).isInstanceOf(ReadableBook.class);
    }

    /** P13. Both sides missing is also unreadable. */
    @Test
    void bookWithoutAnySideIsUnreadable() {
        Snapshot s = accepted(doc("{\"exchange\":\"B\"}"));

        assertThat(s.books()).containsExactly(new UnreadableBook("B"));
    }

    /** P14. D-16: names are kept exactly as given, on readable and unreadable books. */
    @ParameterizedTest
    @ValueSource(strings = {"Kraken", " Pad ", "kRaKeN", "Bitstamp é", "ÅSE 取引所"})
    void keepsExchangeNamesExactlyAsGiven(String name) {
        Snapshot s = accepted(doc(book(name, "[[100,1]]", "[[101,1]]")));
        assertThat(s.books().get(0)).isInstanceOf(ReadableBook.class);
        assertThat(s.books().get(0).exchange()).isEqualTo(name);

        Snapshot u = accepted(doc("{\"exchange\":" + quote(name) + "}"));
        assertThat(u.books().get(0)).isEqualTo(new UnreadableBook(name));
    }

    // ---------------------------------------------------------------- input errors

    private static final String INVALID_JSON = "input error: invalid JSON";

    /** P15. D-05, D-06: anything that is not one valid JSON document is an input error. */
    @ParameterizedTest
    @ValueSource(strings = {
            "",
            "   ",
            "{",
            "not json",
            "{\"calculationTime\":\"2026-10-27T12:00:00Z\",\"books\":[]} trailing",
            "{\"calculationTime\":\"2026-10-27T12:00:00Z\",\"books\":[]}{}",
            "{\"calculationTime\":\"2026-10-27T12:00:00Z\",\"books\":[],}",
            "/* c */ {\"calculationTime\":\"2026-10-27T12:00:00Z\",\"books\":[]}",
            "{\"calculationTime\":\"2026-10-27T12:00:00Z\",\"books\":[{\"exchange\":\"A\",\"bids\":[[NaN,1]],\"asks\":[]}]}",
            "{\"calculationTime\":\"2026-10-27T12:00:00Z\",\"books\":[{\"exchange\":\"A\",\"bids\":[[Infinity,1]],\"asks\":[]}]}",
            "{'calculationTime':'2026-10-27T12:00:00Z','books':[]}"})
    void rejectsInvalidJson(String json) {
        assertThat(rejection(json)).isEqualTo(INVALID_JSON);
    }

    /** P15. Invalid UTF-8 cannot be read as JSON text. */
    @Test
    void rejectsInvalidUtf8() {
        byte[] bytes = {'{', '"', 'a', '"', ':', '"', (byte) 0xFF, (byte) 0xFE, '"', '}'};

        assertThat(rejection(InputParser.parse(bytes))).isEqualTo(INVALID_JSON);
    }

    /** P16. D-17: a duplicate key in one JSON object is invalid JSON. */
    @ParameterizedTest
    @ValueSource(strings = {
            "{\"calculationTime\":\"2026-10-27T12:00:00Z\",\"calculationTime\":\"2026-10-27T13:00:00Z\",\"books\":[]}",
            "{\"calculationTime\":\"2026-10-27T12:00:00Z\",\"books\":[],\"books\":[]}",
            "{\"calculationTime\":\"2026-10-27T12:00:00Z\",\"books\":[{\"exchange\":\"A\",\"bids\":[],\"bids\":[],\"asks\":[]}]}",
            "{\"calculationTime\":\"2026-10-27T12:00:00Z\",\"books\":[{\"exchange\":\"A\",\"exchange\":\"B\",\"bids\":[],\"asks\":[]}]}"})
    void rejectsDuplicateKeys(String json) {
        assertThat(rejection(json)).isEqualTo(INVALID_JSON);
    }

    /** P17. D-06: the root must be an object. */
    @ParameterizedTest
    @ValueSource(strings = {"[]", "\"x\"", "5", "null", "true"})
    void rejectsNonObjectRoot(String json) {
        assertThat(rejection(json)).isEqualTo("input error: root must be an object");
    }

    /** P18. D-07: calculationTime is required. */
    @ParameterizedTest
    @ValueSource(strings = {"{\"books\":[]}", "{\"calculationTime\":null,\"books\":[]}"})
    void rejectsMissingCalculationTime(String json) {
        assertThat(rejection(json)).isEqualTo("input error: calculationTime is missing");
    }

    /** P19. D-07, D-15: calculationTime must be an ISO-8601 UTC instant ending in Z. */
    @ParameterizedTest
    @ValueSource(strings = {"\"\"", "\"yesterday\"", "\"2026-10-27\"", "\"2026-10-27T12:00:00\"",
            "\"2026-10-27T12:00:00+01:00\"", "\"2026-10-27T12:00:00+00:00\"", "\"2026-10-27T12:00:00z\"",
            "\"2026-10-27T12:00Z\"", "1730000000", "1.5", "true", "{}", "[]"})
    void rejectsInvalidCalculationTime(String value) {
        String json = "{\"calculationTime\":" + value + ",\"books\":[]}";

        assertThat(rejection(json))
                .isEqualTo("input error: calculationTime must be an ISO-8601 UTC timestamp ending in Z");
    }

    /** P20. D-06, D-07: books is required. */
    @ParameterizedTest
    @ValueSource(strings = {"{" + TIME_FIELD + "}", "{" + TIME_FIELD + ",\"books\":null}"})
    void rejectsMissingBooks(String json) {
        assertThat(rejection(json)).isEqualTo("input error: books is missing");
    }

    /** P21. D-06, D-08: books must be an array. */
    @ParameterizedTest
    @ValueSource(strings = {"{}", "\"x\"", "5", "true"})
    void rejectsBooksThatIsNotAnArray(String value) {
        assertThat(rejection("{" + TIME_FIELD + ",\"books\":" + value + "}"))
                .isEqualTo("input error: books must be an array");
    }

    /** P22. D-06: every books element must be an object. */
    @ParameterizedTest
    @ValueSource(strings = {"null", "5", "\"x\"", "[]", "true"})
    void rejectsBookThatIsNotAnObject(String element) {
        String json = "{" + TIME_FIELD + ",\"books\":[" + GOOD_BOOK + "," + GOOD_BOOK.replace("\"A\"", "\"B\"")
                + "," + element + "]}";

        assertThat(rejection(json)).isEqualTo("input error: books[2] must be an object");
    }

    /** P23. D-03, D-06, D-16: the exchange name must be a nonempty, non-blank string. */
    @ParameterizedTest
    @ValueSource(strings = {"", "\"exchange\":null,", "\"exchange\":5,", "\"exchange\":\"\",",
            "\"exchange\":\"   \",", "\"exchange\":\"\\t\\n\",", "\"exchange\":[],", "\"exchange\":{},"})
    void rejectsMissingOrBlankExchangeName(String exchangeField) {
        String bad = "{" + exchangeField + "\"bids\":[[100,1]],\"asks\":[[101,1]]}";

        assertThat(rejection(doc(GOOD_BOOK, bad)))
                .isEqualTo("input error: books[1].exchange must be a nonempty string");
    }

    static Stream<Arguments> duplicateNames() {
        return Stream.of(
                Arguments.of("Kraken", "Kraken"),
                Arguments.of("Kraken", "kraken"),
                Arguments.of("Kraken", "Kraken "),
                Arguments.of(" Kraken", "KRAKEN"),
                Arguments.of("kraken", "  Kraken\t"));
    }

    /** P24. D-02, D-16: duplicates are detected on trimmed, case-insensitive names. */
    @ParameterizedTest(name = "[{0}] vs [{1}]")
    @MethodSource("duplicateNames")
    void rejectsDuplicateExchangeNames(String first, String second) {
        String json = doc(book(first, "[[100,1]]", "[[101,1]]"), book(second, "[[100,1]]", "[[101,1]]"));

        assertThat(rejection(json)).isEqualTo("input error: duplicate exchange name \"" + second + "\"");
    }

    /**
     * D-16, D-18: the duplicate check strips Unicode whitespace, the same definition the blank check
     * uses, so "Kraken" and "Kraken" followed by U+2003 (em space) are duplicates. (U+00A0 is outside
     * that definition for both rules: accepted, not handled.)
     */
    @Test
    void namesDifferingOnlyByUnicodeWhitespaceAreDuplicates() {
        String padded = "Kraken ";
        String json = doc(book("Kraken", "[[100,1]]", "[[101,1]]"), book(padded, "[[100,1]]", "[[101,1]]"));

        assertThat(rejection(json)).isEqualTo("input error: duplicate exchange name \"" + padded + "\"");
        // Leading position too, in either order.
        assertThat(rejection(doc(book(" Kraken", "[[100,1]]", "[[101,1]]"),
                book("kraken", "[[100,1]]", "[[101,1]]"))))
                .isEqualTo("input error: duplicate exchange name \"kraken\"");
    }

    /** P24. Names that differ beyond trim and case are distinct. */
    @Test
    void acceptsNamesThatDifferBeyondTrimAndCase() {
        Snapshot s = accepted(doc(book("Kraken", "[[100,1]]", "[[101,1]]"), book("Kraken 2", "[[100,1]]", "[[101,1]]")));

        assertThat(s.books()).hasSize(2);
    }

    /** P25. D-02 applies to every named book, including one that would be unreadable. */
    @Test
    void duplicateWithAnUnreadableBookIsStillADuplicate() {
        assertThat(rejection(doc(book("A", "[[100,1]]", "[[101,1]]"), "{\"exchange\":\"a\"}")))
                .isEqualTo("input error: duplicate exchange name \"a\"");
        assertThat(rejection(doc("{\"exchange\":\"A\"}", book("a", "[[100,1]]", "[[101,1]]"))))
                .isEqualTo("input error: duplicate exchange name \"a\"");
    }

    /** P26. D-06 order: JSON, root, calculationTime, books, then per book in order, then duplicates. */
    @Test
    void reportsTheFirstProblemInDocumentedOrder() {
        // calculationTime is checked before books.
        assertThat(rejection("{\"calculationTime\":\"nope\",\"books\":{}}"))
                .isEqualTo("input error: calculationTime must be an ISO-8601 UTC timestamp ending in Z");
        // books is checked before any element.
        assertThat(rejection("{" + TIME_FIELD + ",\"books\":5}"))
                .isEqualTo("input error: books must be an array");
        // Per book, in order: a bad name at index 1 comes before a non-object at index 2.
        assertThat(rejection(doc(GOOD_BOOK, "{\"exchange\":\"\"}", "5")))
                .isEqualTo("input error: books[1].exchange must be a nonempty string");
        // A non-object at index 1 comes before a duplicate between 0 and 2.
        assertThat(rejection(doc(GOOD_BOOK, "5", GOOD_BOOK)))
                .isEqualTo("input error: books[1] must be an object");
        // Duplicates are reported after every book has been checked individually.
        assertThat(rejection(doc(GOOD_BOOK, GOOD_BOOK, "{\"exchange\":\" \"}")))
                .isEqualTo("input error: books[2].exchange must be a nonempty string");
    }

    // ---------------------------------------------------------------- files

    /** P27. A missing file, a directory and an unreadable file are input errors, not exceptions. */
    @Test
    void rejectsMissingFileAndDirectory(@TempDir Path dir) {
        String expected = "input error: cannot read input file";

        assertThat(rejection(InputParser.parse(dir.resolve("nope.json")))).isEqualTo(expected);
        assertThat(rejection(InputParser.parse(dir))).isEqualTo(expected);
    }

    /** P27. Permissions; skipped where they can't be enforced (non-POSIX file system, or running as root). */
    @Test
    void rejectsUnreadableFile(@TempDir Path dir) throws IOException {
        Path file = dir.resolve("locked.json");
        Files.writeString(file, doc());
        try {
            Files.setPosixFilePermissions(file, PosixFilePermissions.fromString("---------"));
        } catch (UnsupportedOperationException e) {
            assumeTrue(false, "POSIX permissions not supported");
        }
        assumeTrue(!Files.isReadable(file), "file is still readable (running as root?)");

        assertThat(rejection(InputParser.parse(file))).isEqualTo("input error: cannot read input file");
    }

    /** P27. An empty file is not valid JSON. */
    @Test
    void rejectsEmptyFileAsInvalidJson(@TempDir Path dir) throws IOException {
        Path file = Files.write(dir.resolve("empty.json"), new byte[0]);

        assertThat(rejection(InputParser.parse(file))).isEqualTo(INVALID_JSON);
    }

    /** P28. Files are read as UTF-8. */
    @Test
    void readsFilesAsUtf8(@TempDir Path dir) throws IOException {
        Path file = Files.writeString(dir.resolve("utf8.json"),
                doc(book("Bitstamp é", "[[100,1]]", "[[101,1]]")), StandardCharsets.UTF_8);

        Snapshot s = ((Accepted) InputParser.parse(file)).snapshot();

        assertThat(s.books()).extracting(BookInput::exchange).isEqualTo(List.of("Bitstamp é"));
    }
}
