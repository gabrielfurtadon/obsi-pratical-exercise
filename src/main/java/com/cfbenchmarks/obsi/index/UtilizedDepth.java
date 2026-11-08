package com.cfbenchmarks.obsi.index;

import com.cfbenchmarks.obsi.curve.MidCurve;
import com.cfbenchmarks.obsi.curve.PriceVolumeCurve;

import java.math.BigDecimal;
import java.util.OptionalLong;

/** §5, Eq. 2: the utilized depth v_T. */
final class UtilizedDepth {

    private UtilizedDepth() {
    }

    /**
     * The number of samples v_i = i·s (i = 1, 2, ...) at which both curves are defined. Eq. 2 needs two consecutive
     * ones, so fewer than two means there is nothing to evaluate (§6: "insufficient depth").
     */
    static long commonSamples(PriceVolumeCurve bids, PriceVolumeCurve asks) {
        return Math.min(bids.wholeUnits(), asks.wholeUnits());
    }

    /**
     * Eq. 2: v_T = max {v_i : midSV(v_i) &lt;= D and midSV(v_{i+1}) &gt; D}, over the i for which both curves are
     * defined at v_i and v_{i+1}. Empty if the set is empty.
     *
     * <p><b>This stops at the first crossing instead of taking the maximum over the whole book (D-21).</b> midSV is
     * (a - b) / (a + b) for the ask a and bid b at that volume. It rises with a and falls with b, and as volume grows
     * the ask never falls and the bid never rises, so midSV never decreases from one sample to the next. Hence the
     * samples within D are a prefix: at most one i has midSV(v_i) &lt;= D and midSV(v_{i+1}) &gt; D, and the first
     * crossing is that maximum. Likewise, once a sample is above D no later sample is within D, so there is no
     * candidate and the scan can stop. The cost depends on v_T and not on the depth of the book. The tests compare
     * this against a literal full scan of Eq. 2 on random books.
     */
    static OptionalLong find(PriceVolumeCurve bids, PriceVolumeCurve asks) {
        long samples = commonSamples(bids, asks);
        // Need a consecutive pair, and v_1 within D: if v_1 is already above D, no sample is (see above).
        if (samples < 2 || !withinCutoff(bids, asks, 1)) {
            return OptionalLong.empty();
        }
        // Invariant: v_i is within D. The first i whose next sample is above D is the crossing.
        for (long i = 1; i + 1 <= samples; i++) {
            if (!withinCutoff(bids, asks, i + 1)) {
                return OptionalLong.of(i); // midSV(v_i) <= D and midSV(v_{i+1}) > D
            }
        }
        return OptionalLong.empty(); // every defined sample is within D: no crossing before a curve ends
    }

    /** midSV(v) &lt;= D at the sample v = i·s, with s = 1 (§7), by the division-free test (Eq. 1f). */
    private static boolean withinCutoff(PriceVolumeCurve bids, PriceVolumeCurve asks, long i) {
        BigDecimal v = BigDecimal.valueOf(i).multiply(PriceVolumeCurve.SPACING);
        BigDecimal askPV = asks.priceAt(v).orElseThrow();
        BigDecimal bidPV = bids.priceAt(v).orElseThrow();
        return MidCurve.midSvWithinCutoff(askPV, MidCurve.midPV(askPV, bidPV));
    }
}
