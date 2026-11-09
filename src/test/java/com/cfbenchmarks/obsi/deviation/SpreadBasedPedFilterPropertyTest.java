package com.cfbenchmarks.obsi.deviation;

import com.cfbenchmarks.obsi.book.ValidBook;
import com.cfbenchmarks.obsi.input.Snapshot;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.stream.Stream;

import static com.cfbenchmarks.obsi.TestBooks.names;
import static com.cfbenchmarks.obsi.TestBooks.snapshotAt;
import static com.cfbenchmarks.obsi.TestBooks.valid;
import static com.cfbenchmarks.obsi.TestBooks.validWithSpread;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Property-style tests of the spread-based PED: inputs come from a {@link Random} with a fixed seed, so every run is
 * the same. proposal.md, Approach 2: the new rule is never more permissive than the 5% rule (normal cap = 5%).
 */
class SpreadBasedPedFilterPropertyTest {

    private static final long SEED = 20261109L;
    private static final MathContext PRECISE = new MathContext(60);
    private static final String T = "2026-10-27T12:00:00Z";
    private static final Snapshot NORMAL = snapshotAt(T);
    private static final Snapshot UNLOCK = snapshotAt(T, "2026-10-27T00:00:00Z");

    private final SpreadBasedPedFilter filter = new SpreadBasedPedFilter();
    private final StaticPedFilter fivePercent = new StaticPedFilter();

    /**
     * Random sets of 1 to 9 books: mids 80.00 to 124.99, absolute spreads 0.01 to 1.50. A third of the sets put one book
     * on the 5% boundary of the median (exactly, or one cent beyond), so the sets are not all far from any limit.
     */
    static Stream<List<ValidBook>> bookSets() {
        Random random = new Random(SEED);
        List<List<ValidBook>> sets = new ArrayList<>();
        for (int i = 0; i < 400; i++) {
            List<ValidBook> books = new ArrayList<>();
            if (i % 3 == 0) {
                // two books at M (a multiple of 20 cents, so +-5% is exact in cents) and one on the boundary
                long cents = (random.nextInt(4000) + 100) * 20L;
                BigDecimal median = BigDecimal.valueOf(cents, 2);
                BigDecimal factor = new BigDecimal(random.nextBoolean() ? "1.05" : "0.95");
                BigDecimal edge = median.multiply(factor);
                if (random.nextBoolean()) {
                    edge = edge.add(new BigDecimal(factor.compareTo(BigDecimal.ONE) > 0 ? "0.01" : "-0.01"));
                }
                String spread = BigDecimal.valueOf(1 + random.nextInt(150), 2).toPlainString();
                books.add(validWithSpread("A", median.toPlainString(), spread));
                books.add(validWithSpread("B", median.toPlainString(), spread));
                books.add(validWithSpread("C", edge.toPlainString(), spread));
            } else {
                int count = 1 + random.nextInt(9);
                for (int b = 0; b < count; b++) {
                    String mid = BigDecimal.valueOf(8000 + random.nextInt(4500), 2).toPlainString();
                    String spread = BigDecimal.valueOf(2 * (1 + random.nextInt(75)), 2).toPlainString();   // even cents
                    books.add(validWithSpread("E" + b, mid, spread));
                }
            }
            sets.add(books);
        }
        return sets.stream();
    }

    private Set<String> excluded(DeviationFilter f, Snapshot snapshot, List<ValidBook> books) {
        return new HashSet<>(names(f.excluded(books, snapshot)));
    }

    /** 15. Never more permissive than 5%: whatever the 5% rule excludes, the new rule excludes, in either regime. */
    @ParameterizedTest
    @MethodSource("bookSets")
    void everyBookTheFivePercentRuleExcludesIsExcludedToo(List<ValidBook> books) {
        Set<String> old = excluded(fivePercent, NORMAL, books);

        assertThat(excluded(filter, NORMAL, books)).containsAll(old);
        assertThat(excluded(filter, UNLOCK, books)).containsAll(old);
    }

    /** 16. The unlock band is never looser than the normal band: the window only excludes more. */
    @ParameterizedTest
    @MethodSource("bookSets")
    void theWindowExcludesEverythingTheNormalRegimeExcludes(List<ValidBook> books) {
        assertThat(excluded(filter, UNLOCK, books)).containsAll(excluded(filter, NORMAL, books));
    }

    /** 17. The limit is the clamp of k·S to its regime's band, S recomputed here by an independent route. */
    @ParameterizedTest
    @MethodSource("bookSets")
    void theLimitIsKTimesSClampedToTheBand(List<ValidBook> books) {
        List<BigDecimal> spreads = new ArrayList<>(books.stream().map(b ->
                b.bestAsk().subtract(b.bestBid()).divide(b.mid(), MathContext.DECIMAL128)).toList());
        Collections.sort(spreads);
        int n = spreads.size();
        BigDecimal s = n % 2 == 1 ? spreads.get(n / 2)
                : spreads.get(n / 2 - 1).add(spreads.get(n / 2)).divide(BigDecimal.valueOf(2));
        BigDecimal ks = s.multiply(BigDecimal.TEN);

        assertThat(SpreadBasedPedFilter.limit(books, false))
                .isEqualByComparingTo(ks.max(new BigDecimal("0.01")).min(new BigDecimal("0.05")));
        assertThat(SpreadBasedPedFilter.limit(books, true))
                .isEqualByComparingTo(ks.max(new BigDecimal("0.005")).min(new BigDecimal("0.01")));
    }

    /**
     * 18. The division-free comparison agrees with a 60-digit division: a book is excluded exactly when
     * |m - median| / median is above the limit. The limit is the filter's own {@code limit()}, because k·S is a
     * DECIMAL128-rounded number and a 60-digit oracle could disagree with it only through that rounding.
     */
    @ParameterizedTest
    @MethodSource("bookSets")
    void theComparisonAgreesWithADivisionOracle(List<ValidBook> books) {
        List<BigDecimal> mids = books.stream().map(ValidBook::mid).toList();
        BigDecimal median = StaticPedFilter.median(mids);
        for (Snapshot snapshot : List.of(NORMAL, UNLOCK)) {
            BigDecimal limit = SpreadBasedPedFilter.limit(books, snapshot == UNLOCK);
            Set<String> expected = new HashSet<>();
            for (ValidBook book : books) {
                BigDecimal deviation = book.mid().subtract(median).abs().divide(median, PRECISE);
                if (deviation.compareTo(limit) > 0) {
                    expected.add(book.exchange());
                }
            }
            assertThat(excluded(filter, snapshot, books)).isEqualTo(expected);
        }
    }

    /**
     * 18 (exact boundaries). Where the limit is clamped it is an exact number, so a book exactly on it must pass and one
     * cent beyond must be excluded. Two books at the median M and one at M·(1 + limit) or M·(1 - limit).
     */
    @ParameterizedTest
    @MethodSource("clampedBoundaryCases")
    void exactlyOnAClampedLimitPassesAndOneCentBeyondIsExcluded(BoundaryCase c) {
        List<ValidBook> books = List.of(
                validWithSpread("A", c.median(), c.spread()),
                validWithSpread("B", c.median(), c.spread()),
                validWithSpread("C", c.edge(), c.spread()));

        assertThat(SpreadBasedPedFilter.limit(books, c.unlock())).isEqualByComparingTo(c.limit());
        assertThat(names(filter.excluded(books, c.unlock() ? UNLOCK : NORMAL))).isEqualTo(c.excluded() ? List.of("C") : List.of());
    }

    record BoundaryCase(String median, String edge, String spread, boolean unlock, String limit, boolean excluded) {
    }

    static Stream<BoundaryCase> clampedBoundaryCases() {
        Random random = new Random(SEED + 1);
        List<BoundaryCase> cases = new ArrayList<>();
        for (int i = 0; i < 200; i++) {
            // M is a whole even number of dollars, 80 to 124: 1.01·M, 1.005·M, 1.05·M are exact in cents
            BigDecimal median = BigDecimal.valueOf(40 + random.nextInt(23)).multiply(BigDecimal.valueOf(2));
            boolean unlock = random.nextBoolean();
            boolean tight = random.nextBoolean();
            // tight: spread 0.02 (S below 0.03%, k·S below both floors); wide: spread 1 (S above 0.8%, above both caps)
            String spread = tight ? "0.02" : "1";
            String limit = tight ? (unlock ? "0.005" : "0.01") : (unlock ? "0.01" : "0.05");
            boolean up = random.nextBoolean();
            boolean beyond = random.nextBoolean();
            BigDecimal factor = BigDecimal.ONE.add(new BigDecimal(limit).multiply(BigDecimal.valueOf(up ? 1 : -1)));
            BigDecimal edge = median.multiply(factor).add(
                    beyond ? new BigDecimal(up ? "0.01" : "-0.01") : BigDecimal.ZERO);
            cases.add(new BoundaryCase(median.toPlainString(), edge.toPlainString(), spread, unlock, limit, beyond));
        }
        return cases.stream();
    }

    /** 19. Order of the books is irrelevant, and the result holds only books that were given (the same objects). */
    @ParameterizedTest
    @MethodSource("bookSets")
    void theResultDoesNotDependOnOrderAndOnlyHoldsTheGivenBooks(List<ValidBook> books) {
        List<ValidBook> shuffled = new ArrayList<>(books);
        Collections.shuffle(shuffled, new Random(SEED + 2));

        for (Snapshot snapshot : List.of(NORMAL, UNLOCK)) {
            List<ValidBook> result = filter.excluded(books, snapshot);
            assertThat(excluded(filter, snapshot, shuffled)).isEqualTo(excluded(filter, snapshot, books));
            assertThat(result).allSatisfy(b -> assertThat(books.stream().anyMatch(given -> given == b)).isTrue());
        }
    }

    /** Adding a book exactly at the median keeps the median and has deviation 0, so that book is never excluded. */
    @ParameterizedTest
    @MethodSource("bookSets")
    void aBookAtTheMedianIsNeverExcluded(List<ValidBook> books) {
        List<ValidBook> withMedianBook = new ArrayList<>(books);
        List<BigDecimal> mids = new ArrayList<>(books.stream().map(ValidBook::mid).toList());
        BigDecimal median = StaticPedFilter.median(mids);
        withMedianBook.add(valid("AtMedian", median.subtract(new BigDecimal("0.1")).toPlainString(),
                median.add(new BigDecimal("0.1")).toPlainString()));

        assertThat(excluded(filter, UNLOCK, withMedianBook)).doesNotContain("AtMedian");
    }
}
