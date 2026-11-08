package com.cfbenchmarks.obsi;

import com.cfbenchmarks.obsi.book.ValidBook;
import com.cfbenchmarks.obsi.input.BookInput;
import com.cfbenchmarks.obsi.input.BookInput.ReadableBook;
import com.cfbenchmarks.obsi.input.BookInput.UnreadableBook;
import com.cfbenchmarks.obsi.input.InputParser;
import com.cfbenchmarks.obsi.input.InputResult;
import com.cfbenchmarks.obsi.input.Level;
import com.cfbenchmarks.obsi.input.Snapshot;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

/** Builders shared by the screening and deviation tests. */
public final class TestBooks {

    public static final String TIME = "2026-10-27T12:00:00Z";
    private static final BigDecimal HALF_SPREAD = new BigDecimal("0.1");

    private TestBooks() {
    }

    public static Level lv(String price, String size) {
        return new Level(new BigDecimal(price), new BigDecimal(size));
    }

    /** A readable book with one level of size 1 on each side. */
    public static ReadableBook book(String name, String bid, String ask) {
        return new ReadableBook(name, List.of(lv(bid, "1")), List.of(lv(ask, "1")));
    }

    public static ReadableBook book(String name, List<Level> bids, List<Level> asks) {
        return new ReadableBook(name, bids, asks);
    }

    /** A readable book whose top-of-book mid is exactly {@code mid}, with a spread of 0.2. */
    public static ReadableBook bookAtMid(String name, String mid) {
        BigDecimal m = new BigDecimal(mid);
        return book(name, m.subtract(HALF_SPREAD).toPlainString(), m.add(HALF_SPREAD).toPlainString());
    }

    /** A book that is crossed (best bid above best ask) around {@code mid}. */
    public static ReadableBook crossedAtMid(String name, String mid) {
        BigDecimal m = new BigDecimal(mid);
        return book(name, m.add(HALF_SPREAD).toPlainString(), m.subtract(HALF_SPREAD).toPlainString());
    }

    public static UnreadableBook unreadable(String name) {
        return new UnreadableBook(name);
    }

    /** A structurally valid book with one level per side. */
    public static ValidBook valid(String name, String bid, String ask) {
        return new ValidBook(name, List.of(lv(bid, "1")), List.of(lv(ask, "1")),
                new BigDecimal(bid), new BigDecimal(ask));
    }

    /** A structurally valid book with the given levels; the best prices are taken from the levels (D-11). */
    public static ValidBook validBook(String name, List<Level> bids, List<Level> asks) {
        BigDecimal bestBid = bids.stream().map(Level::price).max(BigDecimal::compareTo).orElseThrow();
        BigDecimal bestAsk = asks.stream().map(Level::price).min(BigDecimal::compareTo).orElseThrow();
        return new ValidBook(name, bids, asks, bestBid, bestAsk);
    }

    /** A structurally valid book whose mid is exactly {@code mid}, with a spread of 0.2. */
    public static ValidBook validAtMid(String name, String mid) {
        BigDecimal m = new BigDecimal(mid);
        return valid(name, m.subtract(HALF_SPREAD).toPlainString(), m.add(HALF_SPREAD).toPlainString());
    }

    public static Snapshot snapshot(BookInput... books) {
        return new Snapshot(TIME, List.of(books));
    }

    public static Snapshot snapshot(List<? extends BookInput> books) {
        return new Snapshot(TIME, List.copyOf(books));
    }

    public static List<String> names(List<ValidBook> books) {
        return books.stream().map(ValidBook::exchange).toList();
    }

    /** Parses a fixture under {@code data/}, found by file name wherever it sits below that directory. */
    public static Snapshot fixture(String fileName) throws IOException {
        Path file;
        try (Stream<Path> files = Files.walk(Path.of("data"))) {
            file = files.filter(p -> p.getFileName().toString().equals(fileName)).findFirst()
                    .orElseThrow(() -> new AssertionError(fileName + " not found under data/"));
        }
        InputResult result = InputParser.parse(file);
        if (!(result instanceof InputResult.Accepted accepted)) {
            throw new AssertionError("fixture " + fileName + " was rejected: " + result);
        }
        return accepted.snapshot();
    }
}
