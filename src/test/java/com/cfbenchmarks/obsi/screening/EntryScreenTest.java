package com.cfbenchmarks.obsi.screening;

import com.cfbenchmarks.obsi.book.ValidBook;
import com.cfbenchmarks.obsi.input.Level;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;
import java.util.List;

import static com.cfbenchmarks.obsi.TestBooks.book;
import static com.cfbenchmarks.obsi.TestBooks.lv;
import static com.cfbenchmarks.obsi.TestBooks.snapshot;
import static com.cfbenchmarks.obsi.screening.ScreenerTestSupport.screenWithoutDeviation;
import static org.assertj.core.api.Assertions.assertThat;

/** Spec §2, first screen: discard an entry whose price or size is not strictly positive (2.2). */
class EntryScreenTest {

    /** E1. Non-positive price or size, in either component, on either side: the entry is dropped. */
    @ParameterizedTest(name = "[{0}, {1}] is dropped")
    @CsvSource({"0,1", "-1,1", "1,0", "1,-1", "0,0", "-1,-1", "0.0,1", "-0.0,1", "1,0.00", "-0.5,-0.5"})
    void dropsEntriesThatAreNotStrictlyPositive(String price, String size) {
        List<Level> bids = List.of(lv("99", "1"), lv(price, size), lv("98", "2"));
        List<Level> asks = List.of(lv("101", "1"), lv(price, size), lv("102", "2"));

        ValidBook kept = screenWithoutDeviation(snapshot(book("A", bids, asks))).survivors().get(0);

        assertThat(kept.bids()).containsExactly(lv("99", "1"), lv("98", "2"));
        assertThat(kept.asks()).containsExactly(lv("101", "1"), lv("102", "2"));
    }

    /** E2. Anything above zero stays, however small. */
    @Test
    void keepsTinyPositiveValues() {
        List<Level> bids = List.of(lv("0.0000001", "0.0000001"));
        List<Level> asks = List.of(lv("1", "1"));

        ValidBook kept = screenWithoutDeviation(snapshot(book("A", bids, asks))).survivors().get(0);

        assertThat(kept.bids()).containsExactly(lv("0.0000001", "0.0000001"));
    }

    /**
     * E3. The entry screen comes first: a zero-size bid at 101 is dropped before "best" is taken, so
     * the best bid is 100 and the mid is 101, and the book is not treated as crossed.
     */
    @Test
    void bestPricesAreTakenAfterTheEntryScreen() {
        List<Level> bids = List.of(lv("101", "0"), lv("100", "1"));
        List<Level> asks = List.of(lv("102", "1"));

        ValidBook kept = screenWithoutDeviation(snapshot(book("A", bids, asks))).survivors().get(0);

        assertThat(kept.bestBid()).isEqualByComparingTo("100");
        assertThat(kept.bestAsk()).isEqualByComparingTo("102");
        assertThat(kept.mid()).isEqualByComparingTo("101");
    }

    /** E4. CLAUDE.md "Numbers": the kept levels are the input BigDecimals, scale included. */
    @Test
    void keptLevelsAreTheInputValuesUnchanged() {
        Level bid = new Level(new BigDecimal("100.50"), new BigDecimal("1.200"));
        Level ask = new Level(new BigDecimal("101.0"), new BigDecimal("3"));

        ValidBook kept = screenWithoutDeviation(snapshot(book("A", List.of(bid), List.of(ask)))).survivors().get(0);

        assertThat(kept.bids().get(0).price().scale()).isEqualTo(2);
        assertThat(kept.bids().get(0).size().scale()).isEqualTo(3);
        assertThat(kept.bids()).containsExactly(bid);
        assertThat(kept.asks()).containsExactly(ask);
    }
}
