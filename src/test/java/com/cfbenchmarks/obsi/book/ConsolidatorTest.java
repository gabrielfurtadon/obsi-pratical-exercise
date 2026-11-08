package com.cfbenchmarks.obsi.book;

import com.cfbenchmarks.obsi.TestBooks;
import com.cfbenchmarks.obsi.book.ConsolidationResult.Consolidated;
import com.cfbenchmarks.obsi.book.ConsolidationResult.Failed;
import com.cfbenchmarks.obsi.deviation.StaticPedFilter;
import com.cfbenchmarks.obsi.input.Level;
import com.cfbenchmarks.obsi.screening.Screener;
import com.cfbenchmarks.obsi.screening.ScreeningResult;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

import static com.cfbenchmarks.obsi.TestBooks.lv;
import static com.cfbenchmarks.obsi.TestBooks.validBook;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** §3: consolidation and uncrossing. */
class ConsolidatorTest {

    /** Levels as {@code price:size} with trailing zeros removed, so a test compares values and not scales. */
    static List<String> show(List<Level> levels) {
        return levels.stream()
                .map(l -> l.price().stripTrailingZeros().toPlainString() + ":" + l.size().stripTrailingZeros().toPlainString())
                .toList();
    }

    static Consolidated consolidated(ConsolidationResult result) {
        if (!(result instanceof Consolidated c)) {
            throw new AssertionError("expected Consolidated but was " + result);
        }
        return c;
    }

    static String failureReason(ConsolidationResult result) {
        if (!(result instanceof Failed f)) {
            throw new AssertionError("expected Failed but was " + result);
        }
        return f.reason();
    }

    private static List<ValidBook> survivorsOf(String fixture) throws IOException {
        ScreeningResult result = Screener.screen(TestBooks.fixture(fixture), new StaticPedFilter());
        return ((ScreeningResult.Screened) result).survivors();
    }

    /** A book with one bid level and one ask level, sizes as given. */
    private static ValidBook oneLevel(String name, String bid, String bidSize, String ask, String askSize) {
        return validBook(name, List.of(lv(bid, bidSize)), List.of(lv(ask, askSize)));
    }

    // ---------------------------------------------------------------- consolidation (§3 ¶1)

    /** C1. The sample's two survivors both have 99.8 bids (1.3 and 1.0) and 100.6 asks (1.4 and 1.0). */
    @Test
    void theSampleSumsTheBidsAt99_8To2_3AndTheAsksAt100_6To2_4() throws IOException {
        Consolidated book = consolidated(Consolidator.consolidate(survivorsOf("sample-input.json")));

        assertThat(show(book.bids())).containsExactly(
                "100.2:0.8", "100:1.2", "99.8:2.3", "99.1:2", "99:2", "98.1:2", "98:2");
        assertThat(show(book.asks())).containsExactly(
                "100.3:0.8", "100.4:1.1", "100.6:2.4", "101.1:2", "101.2:2", "102:2", "102.1:2");
    }

    /** C2. {@code 100.0} and {@code 100} are the same price (compareTo), whether in two books or in one (D-11). */
    @Test
    void pricesWithDifferentScalesMergeIntoOneLevel() {
        ValidBook a = oneLevel("A", "100.0", "1", "101", "1");
        ValidBook b = oneLevel("B", "100", "2", "101.00", "3");

        Consolidated ab = consolidated(Consolidator.consolidate(List.of(a, b)));
        Consolidated ba = consolidated(Consolidator.consolidate(List.of(b, a)));

        assertThat(show(ab.bids())).containsExactly("100:3");
        assertThat(show(ab.asks())).containsExactly("101:4");
        assertThat(show(ba.bids())).containsExactly("100:3");
        assertThat(show(ba.asks())).containsExactly("101:4");
    }

    @Test
    void levelsAtTheSamePriceWithinOneBookAreSummedToo() {
        ValidBook a = validBook("A", List.of(lv("100.0", "1"), lv("100", "2"), lv("99", "1")),
                List.of(lv("101", "1"), lv("101.0", "1")));

        Consolidated book = consolidated(Consolidator.consolidate(List.of(a)));

        assertThat(show(book.bids())).containsExactly("100:3", "99:1");
        assertThat(show(book.asks())).containsExactly("101:2");
    }

    /** C3. Sizes are summed as decimals: 0.1 + 0.2 is exactly 0.3. */
    @Test
    void sizesAreSummedExactly() {
        ValidBook a = oneLevel("A", "99", "0.1", "101", "0.1");
        ValidBook b = oneLevel("B", "99", "0.2", "101", "0.2");

        Consolidated book = consolidated(Consolidator.consolidate(List.of(a, b)));

        assertThat(book.bids().get(0).size()).isEqualByComparingTo("0.3");
        assertThat(book.asks().get(0).size()).isEqualByComparingTo("0.3");
    }

    /** C4. Bids come out highest first, asks lowest first, whatever order the books and levels came in. */
    @Test
    void bidsAreDescendingAndAsksAscendingWhateverTheInputOrder() {
        ValidBook a = validBook("A", List.of(lv("97", "1"), lv("99", "1"), lv("98", "1")),
                List.of(lv("103", "1"), lv("101", "1")));
        ValidBook b = validBook("B", List.of(lv("96", "1"), lv("99.5", "1")),
                List.of(lv("102", "1"), lv("100.5", "1")));

        Consolidated book = consolidated(Consolidator.consolidate(List.of(a, b)));

        assertThat(show(book.bids())).containsExactly("99.5:1", "99:1", "98:1", "97:1", "96:1");
        assertThat(show(book.asks())).containsExactly("100.5:1", "101:1", "102:1", "103:1");
    }

    /** C5. The screener never passes an empty survivor list on (it fails with §6 first), so this is a caller bug. */
    @Test
    void anEmptySurvivorListIsRejected() {
        assertThatThrownBy(() -> Consolidator.consolidate(List.of())).isInstanceOf(IllegalArgumentException.class);
    }

    // ---------------------------------------------------------------- uncrossing (§3 ¶2)

    /** U1. A bid equal to the best ask is crossed (>=, not >); equal sizes remove both levels. */
    @Test
    void aBidEqualToTheAskIsUncrossedAndEqualSizesRemoveBothLevels() {
        ValidBook a = oneLevel("A", "99", "1", "100", "1");
        ValidBook b = oneLevel("B", "100.0", "1", "101", "1");

        Consolidated book = consolidated(Consolidator.consolidate(List.of(a, b)));

        assertThat(show(book.bids())).containsExactly("99:1");
        assertThat(show(book.asks())).containsExactly("101:1");
    }

    /** U2. The smaller size comes off both levels; the larger one stays, at its own price, with the rest. */
    @Test
    void theSmallerSizeIsSubtractedFromBothAndTheLargerLevelKeepsItsResidual() {
        ValidBook a = oneLevel("A", "99", "1", "100", "1");
        ValidBook b = oneLevel("B", "100.5", "3", "101", "1");

        Consolidated book = consolidated(Consolidator.consolidate(List.of(a, b)));

        assertThat(show(book.bids())).containsExactly("100.5:2", "99:1");
        assertThat(show(book.asks())).containsExactly("101:1");
    }

    /** U2b. A bid and an ask at the same price are separate sides: the ask keeps its residual and is not merged. */
    @Test
    void aBidAndAnAskAtTheSamePriceAreNotMerged() {
        ValidBook a = oneLevel("A", "99", "1", "100", "2");
        ValidBook b = oneLevel("B", "100", "1", "101", "1");

        Consolidated book = consolidated(Consolidator.consolidate(List.of(a, b)));

        assertThat(show(book.bids())).containsExactly("99:1");
        assertThat(show(book.asks())).containsExactly("100:1", "101:1");
    }

    /** U3. Several rounds: a bid is partly eaten, a second bid level is eaten by two ask levels, residuals are exact. */
    @Test
    void severalRoundsLeaveTheExactResiduals() {
        ValidBook a = validBook("A", List.of(lv("103", "1"), lv("102", "2")), List.of(lv("104", "1")));
        ValidBook b = validBook("B", List.of(lv("98", "1")), List.of(lv("100", "1.5"), lv("101", "1.0")));

        // 103:1 eats 100:1.5 (ask left 0.5); 102:2 eats 100:0.5 (bid left 1.5) and then 101:1.0 (bid left 0.5)
        Consolidated book = consolidated(Consolidator.consolidate(List.of(a, b)));

        assertThat(show(book.bids())).containsExactly("102:0.5", "98:1");
        assertThat(show(book.asks())).containsExactly("104:1");
    }

    /** U4. A residual of zero is removed even when the scales differ: 1.0 - 0.3 - 0.7 leaves nothing. */
    @Test
    void aZeroResidualIsRemovedWhateverItsScale() {
        ValidBook a = validBook("A", List.of(lv("103", "1.0"), lv("99", "1")), List.of(lv("104", "1")));
        ValidBook b = validBook("B", List.of(lv("98", "1")),
                List.of(lv("100", "0.3"), lv("101", "0.7"), lv("102.5", "1")));

        Consolidated book = consolidated(Consolidator.consolidate(List.of(a, b)));

        assertThat(show(book.bids())).containsExactly("99:1", "98:1");
        assertThat(show(book.asks())).containsExactly("102.5:1", "104:1");
    }

    /** U6. Edge-cases fixture: Charlie's bid 104.9 crosses Bravo's ask 100.05; one unit comes off both. */
    @Test
    void theEdgeCasesFixtureRemovesOneUnitAt104_9And100_05() throws IOException {
        Consolidated book = consolidated(Consolidator.consolidate(survivorsOf("edge-cases.json")));

        assertThat(show(book.bids())).containsExactly("99.95:1", "99.9:2", "99.85:1", "99.6:3", "99.4:2");
        assertThat(show(book.asks())).containsExactly("100.1:2", "100.15:1", "100.5:3", "100.8:2", "105.1:1");
    }

    // ---------------------------------------------------------------- exhaustion (§3 ¶2, §6, D-20)
    // Books that passed §2 can never exhaust a side (D-20), so these call uncross() with sides built by hand.

    @Test
    void bothSidesExhaustedFails() {
        ConsolidationResult result = Consolidator.uncross(List.of(lv("105", "1")), List.of(lv("100", "1")));

        assertThat(failureReason(result)).isEqualTo("uncrossing exhausted both sides");
    }

    @Test
    void theAskSideExhaustedFails() {
        ConsolidationResult result = Consolidator.uncross(
                List.of(lv("105", "3"), lv("99", "1")), List.of(lv("100", "2")));

        assertThat(failureReason(result)).isEqualTo("uncrossing exhausted the ask side");
    }

    @Test
    void theBidSideExhaustedFails() {
        ConsolidationResult result = Consolidator.uncross(
                List.of(lv("105", "1")), List.of(lv("100", "3"), lv("106", "1")));
        assertThat(failureReason(result)).isEqualTo("uncrossing exhausted the bid side");
    }

    /** A bid level that is eaten while others remain on both sides is not an exhaustion. */
    @Test
    void aSideThatStillHasLevelsIsNotExhausted() {
        Consolidated book = consolidated(Consolidator.uncross(
                List.of(lv("105", "2")), List.of(lv("100", "1"), lv("106", "1"))));

        assertThat(show(book.bids())).containsExactly("105:1");
        assertThat(show(book.asks())).containsExactly("106:1");
    }
}
