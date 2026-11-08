package com.cfbenchmarks.obsi.curve;

import java.math.BigDecimal;

/** The mid-price curve (Eq. 1e) and the mid-spread cutoff test (Eq. 1f), both exact. */
public final class MidCurve {

    /** §1, §7: the mid-spread cutoff D = 0.005 (0.5%). */
    public static final BigDecimal CUTOFF = new BigDecimal("0.005");

    private static final BigDecimal TWO = BigDecimal.valueOf(2);

    private MidCurve() {
    }

    /** Eq. 1e: midPV = (askPV + bidPV) / 2. Division by 2 always terminates, so it is exact (D-14). */
    public static BigDecimal midPV(BigDecimal askPV, BigDecimal bidPV) {
        return askPV.add(bidPV).divide(TWO);
    }

    /**
     * Eq. 1f: midSV = askPV / midPV - 1, and midSV &lt;= D. Without dividing: midSV &lt;= D is
     * askPV &lt;= (1 + D) · midPV, because midPV &gt; 0 (prices are positive). Equal counts as within.
     */
    public static boolean midSvWithinCutoff(BigDecimal askPV, BigDecimal midPV) {
        return askPV.compareTo(BigDecimal.ONE.add(CUTOFF).multiply(midPV)) <= 0;
    }
}
