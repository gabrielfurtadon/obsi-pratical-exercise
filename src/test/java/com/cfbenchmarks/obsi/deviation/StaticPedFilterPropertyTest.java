package com.cfbenchmarks.obsi.deviation;

import com.cfbenchmarks.obsi.book.ValidBook;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.Stream;

import static com.cfbenchmarks.obsi.TestBooks.names;
import static com.cfbenchmarks.obsi.TestBooks.snapshot;
import static com.cfbenchmarks.obsi.TestBooks.validAtMid;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/** Property-style tests: inputs come from a {@link Random} with a fixed seed. */
class StaticPedFilterPropertyTest {

    private static final long SEED = 20261109L;
    private static final MathContext PRECISE = new MathContext(60);
    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);
    private static final BigDecimal FIVE = BigDecimal.valueOf(5);

    /** A list of mids in cents. About a third are built around exact +-5% boundaries. */
    static Stream<List<BigDecimal>> midSets() {
        Random random = new Random(SEED);
        List<List<BigDecimal>> sets = new ArrayList<>();
        for (int i = 0; i < 400; i++) {
            List<BigDecimal> mids = new ArrayList<>();
            if (i % 3 == 0) {
                // median M (a multiple of 20 cents, so +-5% is exact in cents), two books at M and one on the boundary
                long cents = (random.nextInt(4000) + 100) * 20L;
                BigDecimal median = BigDecimal.valueOf(cents, 2);
                BigDecimal factor = new BigDecimal(random.nextBoolean() ? "1.05" : "0.95");
                BigDecimal edge = median.multiply(factor);
                // and sometimes one cent beyond the boundary
                if (random.nextBoolean()) {
                    edge = edge.add(new BigDecimal(factor.compareTo(BigDecimal.ONE) > 0 ? "0.01" : "-0.01"));
                }
                mids.add(median);
                mids.add(median);
                mids.add(edge);
            } else {
                int count = 1 + random.nextInt(9);
                for (int b = 0; b < count; b++) {
                    mids.add(BigDecimal.valueOf(8000 + random.nextInt(4500), 2));   // 80.00 .. 124.99
                }
            }
            sets.add(mids);
        }
        return sets.stream();
    }

    private static List<ValidBook> books(List<BigDecimal> mids) {
        List<ValidBook> books = new ArrayList<>();
        for (int i = 0; i < mids.size(); i++) {
            books.add(validAtMid("B" + i, mids.get(i).toPlainString()));
        }
        return books;
    }

    private static BigDecimal referenceMedian(List<BigDecimal> mids) {
        List<BigDecimal> sorted = mids.stream().sorted().toList();
        int n = sorted.size();
        return n % 2 == 1
                ? sorted.get(n / 2)
                : sorted.get(n / 2 - 1).add(sorted.get(n / 2)).divide(BigDecimal.valueOf(2));
    }

    /**
     * R1. §2 (2.11, 2.12): the exclusion set equals a reference that divides at 60 digits
     * (PED = 100·|m − median| / median, excluded if &gt; 5). The 60-digit division is exact at the
     * boundary, where the ratio is exactly 0.05.
     */
    @ParameterizedTest
    @MethodSource("midSets")
    void excludesExactlyTheBooksWhosePedExceedsFivePercent(List<BigDecimal> mids) {
        BigDecimal median = referenceMedian(mids);
        List<String> expected = new ArrayList<>();
        for (int i = 0; i < mids.size(); i++) {
            BigDecimal ped = mids.get(i).subtract(median).abs().divide(median, PRECISE).multiply(HUNDRED);
            if (ped.compareTo(FIVE) > 0) {
                expected.add("B" + i);
            }
        }

        List<String> actual = names(new StaticPedFilter().excluded(books(mids), snapshot()));

        assertThat(actual).isEqualTo(expected);
    }

    /** R2. D-12: with an odd count the median book has PED 0, so at least one book always survives. */
    @ParameterizedTest
    @MethodSource("midSets")
    void anOddCountAlwaysLeavesTheMedianBook(List<BigDecimal> mids) {
        assumeTrue(mids.size() % 2 == 1, "only odd counts carry the guarantee");
        BigDecimal median = referenceMedian(mids);
        List<ValidBook> books = books(mids);

        List<String> excluded = names(new StaticPedFilter().excluded(books, snapshot()));

        assertThat(excluded.size()).isLessThan(books.size());
        for (int i = 0; i < mids.size(); i++) {
            if (mids.get(i).compareTo(median) == 0) {
                assertThat(excluded).doesNotContain("B" + i);
            }
        }
    }
}
