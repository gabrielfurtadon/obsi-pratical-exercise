package com.cfbenchmarks.obsi.deviation;

import com.cfbenchmarks.obsi.book.ValidBook;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static com.cfbenchmarks.obsi.TestBooks.names;
import static com.cfbenchmarks.obsi.TestBooks.snapshot;
import static com.cfbenchmarks.obsi.TestBooks.validAtMid;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Spec §2: median of the structurally valid mids, PED = 100·|m − median| / median, exclude a book
 * only if PED &gt; 5%. Books here are already structurally valid; the structural screens are tested
 * with the screener.
 */
class StaticPedFilterTest {

    private final StaticPedFilter filter = new StaticPedFilter();

    /** Builds books A, B, C, ... at the given mids and returns the names the filter excludes. */
    private List<String> excludedAt(String... mids) {
        List<ValidBook> books = new ArrayList<>();
        for (int i = 0; i < mids.length; i++) {
            books.add(validAtMid(String.valueOf((char) ('A' + i)), mids[i]));
        }
        return names(filter.excluded(books, snapshot()));
    }

    // ---------------------------------------------------------------- median

    /** M2. §1/§2 (2.10): the median of an odd count is the middle value, whatever the order. */
    @ParameterizedTest
    @MethodSource("oddMedians")
    void medianOfAnOddCountIsTheMiddleMid(List<String> mids, String expected) {
        assertThat(StaticPedFilter.median(decimals(mids))).isEqualByComparingTo(new BigDecimal(expected));
    }

    static Stream<Arguments> oddMedians() {
        return Stream.of(
                Arguments.of(List.of("7"), "7"),
                Arguments.of(List.of("3", "1", "2"), "2"),
                Arguments.of(List.of("100", "105", "101"), "101"),
                Arguments.of(List.of("5", "1", "4", "2", "3"), "3"));
    }

    /** M3. §2 (2.10), D-14: the median of an even count is the exact mean of the two middle mids. */
    @ParameterizedTest
    @MethodSource("evenMedians")
    void medianOfAnEvenCountIsTheMeanOfTheTwoMiddleMids(List<String> mids, String expected) {
        assertThat(StaticPedFilter.median(decimals(mids))).isEqualByComparingTo(new BigDecimal(expected));
    }

    static Stream<Arguments> evenMedians() {
        return Stream.of(
                Arguments.of(List.of("100", "110"), "105"),
                Arguments.of(List.of("100.1", "100.2"), "100.15"),
                Arguments.of(List.of("1", "2", "3", "4"), "2.5"),
                Arguments.of(List.of("4", "1", "3", "2"), "2.5"),
                Arguments.of(List.of("100.00", "100.10"), "100.05"),
                Arguments.of(List.of("0.0000001", "0.0000002"), "0.00000015"),
                // The two middle values are the 2nd and 3rd of the sorted list, not the first and last.
                Arguments.of(List.of("1", "10", "20", "1000"), "15"));
    }

    private static List<BigDecimal> decimals(List<String> texts) {
        return texts.stream().map(BigDecimal::new).toList();
    }

    /**
     * M4. §2 (2.10): the even-count mean decides the outcome. Mids 100, 100, 110, 110 give median
     * 105 and every book is within 5%; taking the lower or the upper middle would exclude two.
     */
    @Test
    void evenCountMedianIsTheMeanSoAllFourSurvive() {
        assertThat(excludedAt("100", "100", "110", "110")).isEmpty();
    }

    // ---------------------------------------------------------------- the 5% boundary

    /** D1. §2 (2.13): a book at exactly +5% or -5% passes. 500 <= 500. */
    @Test
    void aDeviationOfExactlyFivePercentPasses() {
        assertThat(excludedAt("100", "100", "105")).isEmpty();
        assertThat(excludedAt("100", "100", "95")).isEmpty();
    }

    /** D2. §2 (2.11, 2.12): just over 5% is excluded, on either side of the median. */
    @Test
    void justOverFivePercentIsExcluded() {
        assertThat(excludedAt("100", "100", "105.01")).containsExactly("C");
        assertThat(excludedAt("100", "100", "94.99")).containsExactly("C");
        assertThat(excludedAt("100", "100", "105.0001")).containsExactly("C");
    }

    /** D3. Exact boundaries with odd decimals; trailing zeros in the scale don't matter. */
    @Test
    void exactBoundariesWithDecimalValuesPass() {
        assertThat(excludedAt("3", "3", "3.15")).isEmpty();
        assertThat(excludedAt("100", "100", "105.000")).isEmpty();
        assertThat(excludedAt("100.00", "100.0", "105.0000")).isEmpty();
    }

    /**
     * D3. These are exact +5% / -5% boundaries (370.40 · 1.05 = 388.92 and 918.60 · 0.95 = 872.67),
     * but IEEE double arithmetic lands on the wrong side: both {@code |m - med| / med * 100 <= 5}
     * and {@code |m - med| * 100 <= 5 * med} evaluate to false in double. Passing here shows the
     * comparison is done in BigDecimal (CLAUDE.md, "Numbers"). The premise is asserted below so the
     * comment stays true.
     */
    @Test
    void exactBoundariesThatDoubleArithmeticGetsWrongStillPass() {
        assertThat(doubleRuleSaysWithinLimit(388.92, 370.40)).as("premise: double, +5%").isFalse();
        assertThat(doubleRuleSaysWithinLimit(872.67, 918.60)).as("premise: double, -5%").isFalse();

        assertThat(excludedAt("370.40", "370.40", "388.92")).isEmpty();
        assertThat(excludedAt("918.60", "918.60", "872.67")).isEmpty();
    }

    /** Both double formulations of the 5% rule; "within the limit" only if both say so. */
    private static boolean doubleRuleSaysWithinLimit(double m, double median) {
        boolean divided = Math.abs(m - median) / median * 100 <= 5;
        boolean multiplied = Math.abs(m - median) * 100 <= 5 * median;
        return divided && multiplied;
    }

    /** D4. D-12: two books at 100 and 120 have median 110 and both deviate by 9.09%: both are excluded. */
    @Test
    void twoBooksAt100And120BothFail() {
        assertThat(excludedAt("100", "120")).containsExactly("A", "B");
    }

    /** D5. D-12: with an odd count the median book has PED 0 and survives; outliers go. */
    @Test
    void theMedianBookOfAnOddCountSurvives() {
        assertThat(excludedAt("100", "120", "150")).containsExactly("A", "C");
    }

    /** D6. A single book is its own median. */
    @Test
    void aSingleBookSurvives() {
        assertThat(excludedAt("100")).isEmpty();
    }

    /** No books, no exclusions (and no division by an empty median). */
    @Test
    void noBooksExcludesNothing() {
        assertThat(filter.excluded(List.of(), snapshot())).isEmpty();
    }

    /** D7. §2 (2.15): there is no half-limit test; 2.6% away passes. */
    @Test
    void thereIsNoHalfLimit() {
        assertThat(excludedAt("100", "100", "102.6")).isEmpty();
        assertThat(excludedAt("100", "100", "97.4")).isEmpty();
    }

    /** D8. §2 (2.14, 2.15): no state between calls; every snapshot is judged on its own books. */
    @Test
    void theFilterKeepsNoStateBetweenSnapshots() {
        List<ValidBook> outlierSet = List.of(validAtMid("A", "100"), validAtMid("B", "100"), validAtMid("C", "110"));
        List<ValidBook> calmSet = List.of(validAtMid("A", "100"), validAtMid("B", "100"), validAtMid("C", "101"));

        assertThat(names(filter.excluded(outlierSet, snapshot()))).containsExactly("C");
        assertThat(names(filter.excluded(calmSet, snapshot()))).isEmpty();   // C was out last time: no reentry state
        assertThat(names(filter.excluded(outlierSet, snapshot()))).containsExactly("C");
    }

    /** D9. §2 (2.11): PED is measured from the median, not the mean. 104 is 4% off 100 but 26% off the mean. */
    @Test
    void deviationIsMeasuredFromTheMedianNotTheMean() {
        assertThat(excludedAt("100", "100", "100", "104", "300")).containsExactly("E");
    }

    /** The filter returns books in input order and never modifies its input. */
    @Test
    void returnsExcludedBooksInInputOrderWithoutTouchingTheInput() {
        List<ValidBook> books = new ArrayList<>(List.of(validAtMid("X", "150"), validAtMid("A", "100"),
                validAtMid("D", "60"), validAtMid("E", "101"), validAtMid("F", "100.5")));
        List<ValidBook> copy = List.copyOf(books);

        List<ValidBook> excluded = filter.excluded(books, snapshot());

        assertThat(names(excluded)).containsExactly("X", "D");
        assertThat(books).isEqualTo(copy);
    }
}
