package com.cfbenchmarks.obsi.index;

import com.cfbenchmarks.obsi.book.ConsolidationResult.Consolidated;
import com.cfbenchmarks.obsi.curve.MidCurve;
import com.cfbenchmarks.obsi.curve.PriceVolumeCurve;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.OptionalLong;

/** §4/§5: the index value from the consolidated book. */
public final class ObsiIndex {

    // §6 / D-08 / D-21: the reasons when the Eq. 2 set is empty.
    static final String INSUFFICIENT_DEPTH =
            "empty Eq. 2 set: insufficient depth to evaluate consecutive samples";
    static final String NO_CROSSING = "empty Eq. 2 set: no mid-spread crossing before a curve ends";

    /** §5: lambda = 1 / (0.3 · v_T). */
    private static final BigDecimal LAMBDA_FACTOR = new BigDecimal("0.3");

    private ObsiIndex() {
    }

    public static IndexResult calculate(Consolidated book) {
        PriceVolumeCurve bids = PriceVolumeCurve.of(book.bids());
        PriceVolumeCurve asks = PriceVolumeCurve.of(book.asks());

        // Eq. 2, §6: the calculation fails when the set is empty, either for want of two consecutive samples or
        // because the mid-spread never crosses D before a curve ends.
        OptionalLong utilizedDepth = UtilizedDepth.find(bids, asks);
        if (utilizedDepth.isEmpty()) {
            return new IndexResult.Failed(
                    UtilizedDepth.commonSamples(bids, asks) < 2 ? INSUFFICIENT_DEPTH : NO_CROSSING);
        }

        long vT = utilizedDepth.getAsLong();
        // §5: round the published value once, to two decimals, Java HALF_UP. Intermediate values are never rounded.
        BigDecimal value = weightedMean(bids, asks, vT).setScale(2, RoundingMode.HALF_UP);
        return new IndexResult.Published(value, BigDecimal.valueOf(vT));
    }

    /**
     * Eq. 3: OBSI = sum over v in {s, 2s, ..., v_T} of midPV(v) · lambda·e^(-lambda·v) / NF, with NF the sum of those
     * weights over the same v (§5). Not rounded.
     *
     * <p>Arithmetic (D-13, D-14): lambda = 1 / (0.3 · v_T) is a BigDecimal division at DECIMAL128, converted to double
     * once. Each weight lambda·e^(-lambda·v) is computed in double with {@code StrictMath.exp} and converted exactly
     * with {@code new BigDecimal(double)}. The numerator and NF are exact BigDecimal sums, and the single division
     * at the end uses DECIMAL128. Dividing the sums once (and not each weight by NF) means a constant midPV comes
     * out exactly, which matters at an x.xx5 rounding boundary.
     */
    static BigDecimal weightedMean(PriceVolumeCurve bids, PriceVolumeCurve asks, long utilizedDepth) {
        BigDecimal vT = BigDecimal.valueOf(utilizedDepth);
        double lambda = BigDecimal.ONE.divide(LAMBDA_FACTOR.multiply(vT), MathContext.DECIMAL128).doubleValue();

        BigDecimal numerator = BigDecimal.ZERO;
        BigDecimal normalizationFactor = BigDecimal.ZERO;
        for (long i = 1; i <= utilizedDepth; i++) {
            BigDecimal v = BigDecimal.valueOf(i).multiply(PriceVolumeCurve.SPACING);
            BigDecimal weight = new BigDecimal(lambda * StrictMath.exp(-lambda * i));
            // both curves are defined at every v <= v_T, because Eq. 2 only accepts a v_i with v_{i+1} defined too
            BigDecimal midPV = MidCurve.midPV(asks.priceAt(v).orElseThrow(), bids.priceAt(v).orElseThrow());
            numerator = numerator.add(midPV.multiply(weight));
            normalizationFactor = normalizationFactor.add(weight);
        }
        return numerator.divide(normalizationFactor, MathContext.DECIMAL128);
    }
}
