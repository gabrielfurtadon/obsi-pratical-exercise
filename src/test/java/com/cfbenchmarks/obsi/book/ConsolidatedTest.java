package com.cfbenchmarks.obsi.book;

import com.cfbenchmarks.obsi.book.ConsolidationResult.Consolidated;
import com.cfbenchmarks.obsi.input.Level;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static com.cfbenchmarks.obsi.TestBooks.lv;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** What {@link Consolidated} promises about its levels (§1: B_T descending, A_T ascending; §3: uncrossed). */
class ConsolidatedTest {

    private static void rejected(List<Level> bids, List<Level> asks) {
        assertThatThrownBy(() -> new Consolidated(bids, asks)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void aWellFormedBookIsAccepted() {
        Consolidated book = new Consolidated(
                List.of(lv("99", "1"), lv("98.5", "2")), List.of(lv("100", "1"), lv("100.5", "0.1")));

        assertThat(book.bids()).hasSize(2);
        assertThat(book.asks()).hasSize(2);
    }

    @Test
    void bidsInAscendingOrderAreRejected() {
        rejected(List.of(lv("98", "1"), lv("99", "1")), List.of(lv("100", "1")));
    }

    @Test
    void asksInDescendingOrderAreRejected() {
        rejected(List.of(lv("99", "1")), List.of(lv("101", "1"), lv("100", "1")));
    }

    /** Strict order: a repeated price is rejected, also when the scales differ (compareTo, not equals). */
    @Test
    void aRepeatedBidPriceIsRejected() {
        rejected(List.of(lv("99", "1"), lv("99.0", "1")), List.of(lv("100", "1")));
    }

    @Test
    void aRepeatedAskPriceIsRejected() {
        rejected(List.of(lv("99", "1")), List.of(lv("100", "1"), lv("100.00", "1")));
    }

    @Test
    void aZeroBidSizeIsRejected() {
        rejected(List.of(lv("99", "0.0")), List.of(lv("100", "1")));
    }

    @Test
    void aNegativeBidSizeIsRejected() {
        rejected(List.of(lv("99", "1"), lv("98", "-1")), List.of(lv("100", "1")));
    }

    @Test
    void aZeroAskSizeIsRejected() {
        rejected(List.of(lv("99", "1")), List.of(lv("100", "0")));
    }

    @Test
    void aNegativeAskSizeIsRejected() {
        rejected(List.of(lv("99", "1")), List.of(lv("100", "1"), lv("101", "-0.5")));
    }

    /** §3: uncrossing leaves best bid strictly below best ask, so equal is rejected as well as above. */
    @Test
    void aBestBidEqualToTheBestAskIsRejected() {
        rejected(List.of(lv("100.0", "1")), List.of(lv("100", "1")));
    }

    @Test
    void aBestBidAboveTheBestAskIsRejected() {
        rejected(List.of(lv("101", "1")), List.of(lv("100", "1")));
    }

    /** Best bid and ask are the first levels, so a crossing deeper in the book does not matter to this check. */
    @Test
    void onlyTheBestLevelsDecideWhetherTheBookIsCrossed() {
        assertThatCode(() -> new Consolidated(List.of(lv("99", "1"), lv("98", "1")), List.of(lv("100", "1"))))
                .doesNotThrowAnyException();
    }

    /** An empty side has no best price; that case is the §6 exhaustion failure, not a book. */
    @Test
    void anEmptySideIsRejected() {
        rejected(List.of(), List.of(lv("100", "1")));
        rejected(List.of(lv("99", "1")), List.of());
    }

    @Test
    void theLevelsAreCopiedSoLaterChangesToTheInputDoNotShowThrough() {
        List<Level> bids = new ArrayList<>(List.of(lv("99", "1")));
        Consolidated book = new Consolidated(bids, List.of(lv("100", "1")));

        bids.add(lv("98", "1"));

        assertThat(book.bids()).hasSize(1);
    }
}
