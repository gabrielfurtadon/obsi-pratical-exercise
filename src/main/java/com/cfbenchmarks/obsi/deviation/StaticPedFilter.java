package com.cfbenchmarks.obsi.deviation;

import com.cfbenchmarks.obsi.book.ValidBook;
import com.cfbenchmarks.obsi.input.Snapshot;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * The original static deviation rule (spec §2, §7): compute the median of the structurally valid
 * mids and exclude a book only if its PED exceeds 5%. A book at exactly 5% passes. The check is made
 * independently for every snapshot, using only that snapshot's books: no reentry state and no
 * half-limit test.
 */
public final class StaticPedFilter implements DeviationFilter {

    private static final BigDecimal TWO = BigDecimal.valueOf(2);
    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);
    /** §7: orderbook percentage deviation limit, 5%. */
    private static final BigDecimal LIMIT_PERCENT = BigDecimal.valueOf(5);

    @Override
    public List<ValidBook> excluded(List<ValidBook> structurallyValid, Snapshot snapshot) {
        if (structurallyValid.isEmpty()) {
            return List.of();
        }
        BigDecimal median = median(structurallyValid.stream().map(ValidBook::mid).toList());

        List<ValidBook> excluded = new ArrayList<>();
        for (ValidBook book : structurallyValid) {
            if (deviatesMoreThanTheLimit(book.mid(), median)) {
                excluded.add(book);
            }
        }
        return excluded;
    }

    /**
     * §2: PED = 100·|m − median| / median %, and a book is excluded only if PED &gt; 5%. Written without
     * a division (CLAUDE.md "Numbers"): |m − median|·100 &gt; 5·median. The median is a mean of positive
     * prices, so it is positive and multiplying by it keeps the direction of the inequality.
     */
    private static boolean deviatesMoreThanTheLimit(BigDecimal mid, BigDecimal median) {
        BigDecimal deviation = mid.subtract(median).abs().multiply(HUNDRED);
        BigDecimal allowed = LIMIT_PERCENT.multiply(median);
        return deviation.compareTo(allowed) > 0;   // exactly 5% (equal) passes
    }

    /**
     * §2: the median of the mids, averaging the middle two for an even number of books. Division by 2
     * is exact (D-14). Sorting uses {@code compareTo}, so 100.0 and 100 are the same price.
     */
    static BigDecimal median(List<BigDecimal> mids) {
        if (mids.isEmpty()) {
            throw new IllegalArgumentException("median of no books");
        }
        List<BigDecimal> sorted = new ArrayList<>(mids);
        sorted.sort(BigDecimal::compareTo);
        int n = sorted.size();
        if (n % 2 == 1) {
            return sorted.get(n / 2);
        }
        return sorted.get(n / 2 - 1).add(sorted.get(n / 2)).divide(TWO);
    }
}
