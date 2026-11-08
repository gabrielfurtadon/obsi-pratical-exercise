package com.cfbenchmarks.obsi.screening;

import com.cfbenchmarks.obsi.book.ValidBook;
import com.cfbenchmarks.obsi.input.Level;
import com.cfbenchmarks.obsi.screening.ScreeningResult.Screened;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.stream.Stream;

import static com.cfbenchmarks.obsi.TestBooks.book;
import static com.cfbenchmarks.obsi.TestBooks.lv;
import static com.cfbenchmarks.obsi.TestBooks.snapshot;
import static com.cfbenchmarks.obsi.TestBooks.unreadable;
import static com.cfbenchmarks.obsi.screening.ScreenerTestSupport.screenWithoutDeviation;
import static com.cfbenchmarks.obsi.screening.ScreenerTestSupport.survivorNames;
import static org.assertj.core.api.Assertions.assertThat;

/** Spec §2, structural screens: unreadable book, empty side, best bid at or above best ask (2.3 - 2.6). */
class BookScreenTest {

    private static final List<Level> GOOD_BIDS = List.of(lv("100", "1"));
    private static final List<Level> GOOD_ASKS = List.of(lv("101", "1"));

    /** B1. §2 (2.3), D-03: an unreadable book is excluded and listed; the others carry on. */
    @Test
    void anUnreadableBookIsExcludedAndListed() {
        Screened s = screenWithoutDeviation(snapshot(book("A", "100", "101"), unreadable("U"), book("B", "100", "101")));

        assertThat(survivorNames(s)).containsExactly("A", "B");
        assertThat(s.excludedExchanges()).containsExactly("U");
    }

    static Stream<Arguments> emptySides() {
        return Stream.of(
                Arguments.of("all bids non-positive", List.of(lv("0", "1"), lv("-5", "1")), GOOD_ASKS),
                Arguments.of("bids with zero size", List.of(lv("100", "0")), GOOD_ASKS),
                Arguments.of("empty bids", List.of(), GOOD_ASKS),
                Arguments.of("empty asks", GOOD_BIDS, List.of()),
                Arguments.of("all asks non-positive", GOOD_BIDS, List.of(lv("0", "1"), lv("101", "-1"))),
                Arguments.of("both sides empty", List.of(), List.of()));
    }

    /** B2. §2 (2.4): a side that is empty after the entry screen excludes the whole book. */
    @ParameterizedTest(name = "{0}")
    @MethodSource("emptySides")
    void aSideThatIsEmptyAfterTheEntryScreenExcludesTheBook(String label, List<Level> bids, List<Level> asks) {
        // "ok" survives, so the snapshot is not a §6 failure and the exclusion can be observed.
        Screened s = screenWithoutDeviation(snapshot(book("E", bids, asks), book("ok", "100", "101")));

        assertThat(survivorNames(s)).containsExactly("ok");
        assertThat(s.excludedExchanges()).containsExactly("E");
    }

    /** B3. §2 (2.5): best bid >= best ask excludes; just below passes. */
    @Test
    void aBookWhoseBestBidIsNotBelowItsBestAskIsExcluded() {
        Screened s = screenWithoutDeviation(snapshot(
                book("equal", "100", "100"),
                book("crossed", "101", "100"),
                book("justBelow", "99.99", "100"),
                book("equalScales", "100.0", "100.00")));

        assertThat(survivorNames(s)).containsExactly("justBelow");
        assertThat(s.excludedExchanges()).containsExactly("equal", "crossed", "equalScales");
    }

    /** B4. D-11: "best" is by price, not by position; unsorted input is read correctly and kept as given. */
    @Test
    void bestIsByPriceNotByPosition() {
        List<Level> bids = List.of(lv("99", "1"), lv("100", "1"));
        List<Level> asks = List.of(lv("101", "1"), lv("100.5", "1"));

        ValidBook kept = screenWithoutDeviation(snapshot(book("A", bids, asks))).survivors().get(0);

        assertThat(kept.bestBid()).isEqualByComparingTo("100");
        assertThat(kept.bestAsk()).isEqualByComparingTo("100.5");
        assertThat(kept.mid()).isEqualByComparingTo("100.25");
        assertThat(kept.bids()).containsExactly(lv("99", "1"), lv("100", "1"));
        assertThat(kept.asks()).containsExactly(lv("101", "1"), lv("100.5", "1"));
    }

    /** B5. D-11: a crossing is judged on the best prices, wherever they sit in the input. */
    @Test
    void aCrossingIsJudgedOnBestPricesNotFirstEntries() {
        // first bid 90 and first ask 105 look fine; best bid 100 >= best ask 99
        Level[] bids1 = {lv("100", "1"), lv("90", "1")};
        Level[] asks1 = {lv("105", "1"), lv("99", "1")};
        // first bid 90 is below the only ask 100, but the best bid 101 is not
        Level[] bids2 = {lv("90", "1"), lv("101", "1")};
        Level[] asks2 = {lv("100", "1")};

        Screened s = screenWithoutDeviation(snapshot(
                book("late-ask", List.of(bids1), List.of(asks1)),
                book("late-bid", List.of(bids2), List.of(asks2)),
                book("ok", "100", "101")));

        assertThat(survivorNames(s)).containsExactly("ok");
        assertThat(s.excludedExchanges()).containsExactly("late-ask", "late-bid");
    }

    /** B6. D-11: duplicate prices on a side don't change the checks; both levels are kept for §3. */
    @Test
    void duplicatePricesDoNotAffectTheChecks() {
        List<Level> bids = List.of(lv("100", "1"), lv("100", "2"));

        ValidBook kept = screenWithoutDeviation(snapshot(book("A", bids, GOOD_ASKS))).survivors().get(0);

        assertThat(kept.bestBid()).isEqualByComparingTo("100");
        assertThat(kept.bids()).containsExactly(lv("100", "1"), lv("100", "2"));
    }

    /** B7. §2 (2.6): the checks apply to each book on its own; a bad book changes nothing for its neighbours. */
    @Test
    void aBadBookDoesNotAffectItsNeighbours() {
        Screened s = screenWithoutDeviation(snapshot(
                book("ok1", "100", "101"),
                book("crossed", "105", "100"),
                book("ok2", "100.5", "101.5"),
                unreadable("unreadable"),
                book("empty", List.of(), GOOD_ASKS),
                book("ok3", "99", "100")));

        assertThat(survivorNames(s)).containsExactly("ok1", "ok2", "ok3");
        assertThat(s.excludedExchanges()).containsExactly("crossed", "unreadable", "empty");
        assertThat(s.survivors().get(1).bestBid()).isEqualByComparingTo("100.5");
    }
}
