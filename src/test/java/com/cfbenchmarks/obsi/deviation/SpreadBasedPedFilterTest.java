package com.cfbenchmarks.obsi.deviation;

import com.cfbenchmarks.obsi.book.ValidBook;
import com.cfbenchmarks.obsi.input.Snapshot;
import com.cfbenchmarks.obsi.result.CalculationResult;
import com.cfbenchmarks.obsi.result.ObsiCalculator;
import com.cfbenchmarks.obsi.screening.Screener;
import com.cfbenchmarks.obsi.screening.ScreeningResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static com.cfbenchmarks.obsi.TestBooks.fixture;
import static com.cfbenchmarks.obsi.TestBooks.names;
import static com.cfbenchmarks.obsi.TestBooks.snapshotAt;
import static com.cfbenchmarks.obsi.TestBooks.valid;
import static com.cfbenchmarks.obsi.TestBooks.validAtMid;
import static com.cfbenchmarks.obsi.TestBooks.validWithSpread;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * The spread-based PED (proposal.md, Approach 2; D-23, D-24): limit = clamp(k·S, floor, cap), where S is the median
 * relative spread of the structurally valid books; the unlock band applies in [start, start + 48h).
 *
 * <p>{@code validWithSpread} chooses S. The default {@code validAtMid} has an absolute spread of 0.2, which at mid 100
 * is S = 0.2% and k·S = 2%: not a floor case.
 */
class SpreadBasedPedFilterTest {

    private static final String T = "2026-10-27T12:00:00Z";
    private static final Snapshot NORMAL = snapshotAt(T);
    private final SpreadBasedPedFilter filter = new SpreadBasedPedFilter();

    private List<String> excluded(Snapshot snapshot, ValidBook... books) {
        return names(filter.excluded(List.of(books), snapshot));
    }

    /** Three books with a spread of 0.04 (S = 0.04%, below both floors); the median mid is 100. */
    private static ValidBook[] tight(String thirdMid) {
        return new ValidBook[] {
                validWithSpread("A", "100", "0.04"), validWithSpread("B", "100", "0.04"),
                validWithSpread("C", thirdMid, "0.04")};
    }

    /** Three books with a spread of 1 (S about 1%, so k·S is above both caps); the median mid is 100. */
    private static ValidBook[] wide(String thirdMid) {
        return new ValidBook[] {
                validWithSpread("A", "100", "1"), validWithSpread("B", "100", "1"),
                validWithSpread("C", thirdMid, "1")};
    }

    // ---------------------------------------------------------------- the spread

    /** 1. The relative spread is (ask - bid) / mid, divided at DECIMAL128 (D-24). */
    @Test
    void theSpreadIsAskMinusBidOverMidAtDecimal128() {
        // bid 100, ask 100.4: mid 100.2, spread 0.4 / 100.2 = 2 / 501
        ValidBook book = valid("A", "100", "100.4");

        assertThat(SpreadBasedPedFilter.spread(book))
                .isEqualByComparingTo(BigDecimal.valueOf(2).divide(BigDecimal.valueOf(501), MathContext.DECIMAL128));
        assertThat(SpreadBasedPedFilter.spread(book).toPlainString()).startsWith("0.00399201596806387225548902195608");
    }

    @Test
    void aNonTerminatingSpreadDoesNotThrow() {
        // bid 2, ask 4: mid 3, spread 2 / 3
        BigDecimal spread = SpreadBasedPedFilter.spread(valid("A", "2", "4"));

        assertThat(spread.precision()).isEqualTo(34);
        assertThat(spread).isBetween(new BigDecimal("0.6666666666666666666666666666666666"),
                new BigDecimal("0.6666666666666666666666666666666667"));
    }

    // ---------------------------------------------------------------- S, the median of the spreads

    /** 2. Odd count: the middle spread. Even count: the exact mean of the middle two. Input order is irrelevant. */
    @Test
    void sIsTheMedianOfTheSpreads() {
        ValidBook a = validWithSpread("A", "100", "0.2");
        ValidBook b = validWithSpread("B", "100", "0.4");
        ValidBook c = validWithSpread("C", "100", "0.1");
        ValidBook d = validWithSpread("D", "100", "1");

        assertThat(SpreadBasedPedFilter.medianSpread(List.of(a, b, c))).isEqualByComparingTo("0.002");
        assertThat(SpreadBasedPedFilter.medianSpread(List.of(c, b, a))).isEqualByComparingTo("0.002");
        assertThat(SpreadBasedPedFilter.medianSpread(List.of(a, b, c, d))).isEqualByComparingTo("0.003");
        assertThat(SpreadBasedPedFilter.medianSpread(List.of(d, c, b, a))).isEqualByComparingTo("0.003");
    }

    // ---------------------------------------------------------------- the sample (proposal.md)

    /** The structurally valid books of the sample, as the screener hands them to a deviation filter. */
    private static List<ValidBook> sampleValidBooks() throws IOException {
        List<ValidBook> seen = new ArrayList<>();
        Screener.screen(fixture("sample-input.json"), (books, snapshot) -> {
            seen.addAll(books);
            return List.of();
        });
        return seen;
    }

    /** 3. The sample: spreads 0.40%, 0.10%, 1.33% (Gamma is crossed, so not counted); S = 0.40%; limit 3.99%. */
    @Test
    void theSamplesLimitIs3Point99Percent() throws IOException {
        List<ValidBook> books = sampleValidBooks();

        assertThat(names(books)).containsExactly("Alpha", "Beta", "Delta");
        assertThat(books.stream().map(b -> SpreadBasedPedFilter.spread(b).movePointRight(2)
                .setScale(2, RoundingMode.HALF_UP))).containsExactly(
                new BigDecimal("0.40"), new BigDecimal("0.10"), new BigDecimal("1.33"));
        BigDecimal s = BigDecimal.valueOf(2).divide(BigDecimal.valueOf(501), MathContext.DECIMAL128);   // Alpha
        assertThat(SpreadBasedPedFilter.medianSpread(books)).isEqualByComparingTo(s);

        BigDecimal limit = SpreadBasedPedFilter.limit(books, false);
        assertThat(limit).as("k·S, unrounded (D-24)").isEqualByComparingTo(s.multiply(BigDecimal.TEN));
        assertThat(limit.setScale(4, RoundingMode.DOWN)).as("displays as 3.99%").isEqualByComparingTo("0.0399");
    }

    /** 4. Delta (49.6% from the median) is still excluded and the index stays 100.23. */
    @Test
    void theSampleStillExcludesDeltaAndPublishes100Point23() throws IOException {
        Snapshot sample = fixture("sample-input.json");

        ScreeningResult.Screened screened = (ScreeningResult.Screened) Screener.screen(sample, filter);
        assertThat(screened.excludedExchanges()).containsExactly("Gamma", "Delta");

        CalculationResult result = ObsiCalculator.calculate(sample, filter);
        assertThat(result).isInstanceOf(CalculationResult.Published.class);
        assertThat(((CalculationResult.Published) result).value()).isEqualByComparingTo("100.23");
    }

    // ---------------------------------------------------------------- the limit: band, floor, cap

    /**
     * 5, 6, 7, 14. All books have the same spread at mid 100, so S = spread / 100. Inside the band the limit is k·S; below
     * the floor it is the floor; above the cap it is the cap. A k·S exactly on a bound is that bound. The unlock band
     * replaces the normal one (large S gives 1% in the window, not 5%).
     */
    @ParameterizedTest(name = "spread {0}, unlock={1} -> limit {2}")
    @MethodSource("limits")
    void theLimitIsClampedToTheBandOfItsRegime(String spread, boolean inUnlock, String expected) {
        List<ValidBook> books = List.of(validWithSpread("A", "100", spread), validWithSpread("B", "100", spread),
                validWithSpread("C", "100", spread));

        assertThat(SpreadBasedPedFilter.limit(books, inUnlock)).isEqualByComparingTo(expected);
    }

    static Stream<Arguments> limits() {
        return Stream.of(
                // inside the band: 10·S
                Arguments.of("0.3", false, "0.03"),
                Arguments.of("0.07", true, "0.007"),
                // below the floor (S = 0.04%, k·S = 0.4%)
                Arguments.of("0.04", false, "0.01"),
                Arguments.of("0.04", true, "0.005"),
                // exactly on a floor: k·S = 0.5% (unlock floor) and 1% (normal floor)
                Arguments.of("0.05", true, "0.005"),
                Arguments.of("0.05", false, "0.01"),
                Arguments.of("0.1", false, "0.01"),
                // exactly on a cap: unlock cap 1% (k·S = 1%) and normal cap 5% (k·S = 5%)
                Arguments.of("0.1", true, "0.01"),
                Arguments.of("0.5", false, "0.05"),
                // above the cap, and the unlock band replaces the normal one
                Arguments.of("1", false, "0.05"),
                Arguments.of("1", true, "0.01"),
                Arguments.of("0.5", true, "0.01"));
    }

    // ---------------------------------------------------------------- the comparison at the book level

    /** 8. Floors: with tight spreads the limit is 1% (normal) or 0.5% (unlock). Exactly at the limit passes. */
    @ParameterizedTest(name = "{0}, unlock={1}: third book at {2} -> excluded {3}")
    @MethodSource("floorBoundaries")
    void atTheFloorExactlyOnTheLimitPassesAndOneCentBeyondIsExcluded(String label, boolean inUnlock, String thirdMid,
                                                                      boolean isExcluded) {
        Snapshot snapshot = inUnlock ? snapshotAt(T, "2026-10-27T00:00:00Z") : NORMAL;

        assertThat(excluded(snapshot, tight(thirdMid))).isEqualTo(isExcluded ? List.of("C") : List.of());
    }

    static Stream<Arguments> floorBoundaries() {
        return Stream.of(
                Arguments.of("normal floor 1%", false, "101", false),
                Arguments.of("normal floor 1%", false, "101.01", true),
                Arguments.of("unlock floor 0.5%", true, "100.5", false),
                Arguments.of("unlock floor 0.5%", true, "100.51", true));
    }

    /** 8. Caps: with wide spreads the limit is 5% (normal) or 1% (unlock). Exactly at the limit passes. */
    @ParameterizedTest(name = "{0}, unlock={1}: third book at {2} -> excluded {3}")
    @MethodSource("capBoundaries")
    void atTheCapExactlyOnTheLimitPassesAndOneCentBeyondIsExcluded(String label, boolean inUnlock, String thirdMid,
                                                                    boolean isExcluded) {
        Snapshot snapshot = inUnlock ? snapshotAt(T, "2026-10-27T00:00:00Z") : NORMAL;

        assertThat(excluded(snapshot, wide(thirdMid))).isEqualTo(isExcluded ? List.of("C") : List.of());
    }

    static Stream<Arguments> capBoundaries() {
        return Stream.of(
                Arguments.of("normal cap 5%", false, "105", false),
                Arguments.of("normal cap 5%", false, "105.01", true),
                Arguments.of("unlock cap 1%", true, "101", false),
                Arguments.of("unlock cap 1%", true, "101.01", true));
    }

    @Test
    void theLowerSideIsJudgedLikeTheUpperSide() {
        // median stays 100: mids 99, 100, 100
        assertThat(excluded(NORMAL, tight("99"))).isEmpty();
        assertThat(excluded(NORMAL, tight("98.99"))).containsExactly("C");
    }

    /** 9. S is a median: one huge-spread book among tight ones does not widen the limit. */
    @Test
    void oneHugeSpreadBookDoesNotWidenTheLimit() {
        List<ValidBook> books = List.of(validWithSpread("A", "100", "0.04"), validWithSpread("B", "100", "0.04"),
                validWithSpread("C", "100", "0.04"), validWithSpread("D", "100", "0.04"),
                validWithSpread("E", "100", "50"));

        assertThat(SpreadBasedPedFilter.limit(books, false)).isEqualByComparingTo("0.01");
    }

    @Test
    void aSingleBookIsNeverExcludedAndNoBooksGiveNothing() {
        assertThat(excluded(NORMAL, validWithSpread("A", "100", "50"))).isEmpty();
        assertThat(filter.excluded(List.of(), NORMAL)).isEmpty();
    }

    /** 10. The mids 100.0 and 100 are the same price (compareTo). */
    @Test
    void equivalentScalesGiveTheSameResult() {
        ValidBook[] plain = tight("101.01");
        ValidBook[] scaled = {validWithSpread("A", "100.0", "0.04"), validWithSpread("B", "100.00", "0.040"),
                validWithSpread("C", "101.010", "0.04")};

        assertThat(excluded(NORMAL, scaled)).isEqualTo(excluded(NORMAL, plain)).containsExactly("C");
        assertThat(SpreadBasedPedFilter.limit(List.of(scaled), false))
                .isEqualByComparingTo(SpreadBasedPedFilter.limit(List.of(plain), false));
    }

    // ---------------------------------------------------------------- the unlock window

    /**
     * 11. Window [start, start + 48h). The discriminating book is 0.7% from the median with tight spreads: it passes
     * the normal floor (1%) and is excluded by the unlock floor (0.5%).
     */
    @ParameterizedTest(name = "T = {0} -> unlock {1}")
    @MethodSource("windowBoundaries")
    void theWindowIncludesItsStartAndExcludesItsEnd(String time, boolean inUnlock) {
        Snapshot snapshot = snapshotAt(time, "2026-10-26T12:00:00Z");

        assertThat(excluded(snapshot, tight("100.7"))).isEqualTo(inUnlock ? List.of("C") : List.of());
    }

    static Stream<Arguments> windowBoundaries() {
        return Stream.of(
                Arguments.of("2026-10-26T11:59:59.999999999Z", false),   // start - 1 ns
                Arguments.of("2026-10-26T12:00:00Z", true),              // start, included
                Arguments.of("2026-10-27T12:00:00Z", true),
                Arguments.of("2026-10-28T11:59:59.999999999Z", true),    // start + 48h - 1 ns
                Arguments.of("2026-10-28T12:00:00Z", false),             // start + 48h, excluded
                Arguments.of("2026-10-28T12:00:01Z", false));
    }

    @Test
    void withoutEventsTheNormalRegimeApplies() {
        assertThat(excluded(snapshotAt("2026-10-26T12:00:00Z"), tight("100.7"))).isEmpty();
    }

    /** 12. Several events: any covering window applies. The gap between windows, and the order of events, do not matter. */
    @ParameterizedTest(name = "T = {0} -> unlock {1}")
    @MethodSource("severalEvents")
    void anyCoveringWindowApplies(String time, boolean inUnlock) {
        // windows: [10-20, 10-22), [10-25, 10-27), and [10-30, 11-01) twice; given out of order
        Snapshot snapshot = snapshotAt(time, "2026-10-30T00:00:00Z", "2026-10-20T00:00:00Z",
                "2026-10-30T00:00:00Z", "2026-10-25T00:00:00Z");

        assertThat(excluded(snapshot, tight("100.7"))).isEqualTo(inUnlock ? List.of("C") : List.of());
    }

    static Stream<Arguments> severalEvents() {
        return Stream.of(
                Arguments.of("2026-10-21T00:00:00Z", true),
                Arguments.of("2026-10-23T00:00:00Z", false),   // gap
                Arguments.of("2026-10-26T12:00:00Z", true),
                Arguments.of("2026-10-28T00:00:00Z", false),   // gap
                Arguments.of("2026-10-31T00:00:00Z", true));
    }

    @Test
    void overlappingWindowsAreTheirUnion() {
        // [10-20, 10-22) and [10-21, 10-23): 10-22T12 is covered by the second only
        Snapshot snapshot = snapshotAt("2026-10-22T12:00:00Z", "2026-10-20T00:00:00Z", "2026-10-21T00:00:00Z");

        assertThat(excluded(snapshot, tight("100.7"))).containsExactly("C");
    }

    /**
     * P.2, as {@code StaticPedFilterTest.theFilterKeepsNoStateBetweenSnapshots}: no state between calls. One filter
     * instance sees a snapshot inside an unlock window, then one outside it, then inside again. The middle result uses
     * the normal band, so the unlock band left nothing behind, and the third is the same as the first.
     */
    @Test
    void theFilterKeepsNoStateBetweenSnapshots() {
        ValidBook[] books = tight("100.7");   // C is 0.7% from the median: out at the 0.5% unlock floor, in at the 1% floor
        Snapshot inside = snapshotAt("2026-10-27T12:00:00Z", "2026-10-26T12:00:00Z");
        Snapshot outside = snapshotAt("2026-10-29T12:00:00Z", "2026-10-26T12:00:00Z");   // start + 48h is 10-28T12:00

        assertThat(excluded(inside, books)).containsExactly("C");
        assertThat(excluded(outside, books)).isEmpty();   // normal band: the unlock band is not carried over
        assertThat(excluded(inside, books)).containsExactly("C");
    }

    /** 13. T24:00:00Z is next-day midnight (D-15), and a start at the end of time must not overflow. */
    @Test
    void t24IsReadAsNextDayMidnight() {
        Snapshot snapshot = snapshotAt("2026-10-26T24:00:00Z", "2026-10-27T00:00:00Z");

        assertThat(excluded(snapshot, tight("100.7"))).containsExactly("C");
    }

    @Test
    void aStartAtTheEndOfTimeDoesNotOverflow() {
        Snapshot before = new Snapshot(T, List.of(), List.of(Instant.MAX));
        Snapshot at = new Snapshot(Instant.MAX.toString(), List.of(), List.of(Instant.MAX));

        assertThat(excluded(before, tight("100.7"))).isEmpty();            // not in the window
        assertThat(excluded(at, tight("100.7"))).containsExactly("C");     // T == start, in the window
    }

    /** 14. (also in the limits table) The unlock band replaces the normal band: at 3% the unlock excludes, normal keeps. */
    @Test
    void theUnlockBandReplacesTheNormalBand() {
        assertThat(excluded(NORMAL, wide("103"))).isEmpty();                                       // cap 5%
        assertThat(excluded(snapshotAt(T, "2026-10-27T00:00:00Z"), wide("103"))).containsExactly("C");   // cap 1%
    }

    @Test
    void anUnlockedSnapshotWithoutBooksIsEmpty() {
        assertThat(filter.excluded(List.of(), snapshotAt(T, "2026-10-27T00:00:00Z"))).isEmpty();
    }

    /** The default test book is not a floor case: spread 0.2 at mid 100 is S = 0.2%, k·S = 2%. */
    @Test
    void validAtMidIsNotAFloorCase() {
        List<ValidBook> books = List.of(validAtMid("A", "100"), validAtMid("B", "100"), validAtMid("C", "100"));

        assertThat(SpreadBasedPedFilter.limit(books, false)).isEqualByComparingTo("0.02");
        assertThat(SpreadBasedPedFilter.limit(books, true)).isEqualByComparingTo("0.01");
    }
}
