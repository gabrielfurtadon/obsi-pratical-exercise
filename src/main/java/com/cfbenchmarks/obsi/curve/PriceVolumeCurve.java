package com.cfbenchmarks.obsi.curve;

import com.cfbenchmarks.obsi.input.Level;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Optional;

/**
 * One side of the consolidated book as a price-volume curve (§4): the price level reached by a cumulative volume.
 * The levels are given best first (bids descending, asks ascending, as in {@code Consolidated}). Cumulative sizes
 * are exact {@link BigDecimal} sums, and every comparison uses {@code compareTo}.
 */
public final class PriceVolumeCurve {

    /** §1, §7: the curve spacing s is 1 XYZ. */
    public static final BigDecimal SPACING = BigDecimal.ONE;

    private final BigDecimal[] prices;
    /** {@code cumulative[j]} is the sum of the first {@code j + 1} sizes. */
    private final BigDecimal[] cumulative;

    private PriceVolumeCurve(BigDecimal[] prices, BigDecimal[] cumulative) {
        this.prices = prices;
        this.cumulative = cumulative;
    }

    /**
     * @throws IllegalArgumentException if there is no level or a size is not greater than zero
     */
    public static PriceVolumeCurve of(List<Level> levels) {
        if (levels.isEmpty()) {
            throw new IllegalArgumentException("a price-volume curve needs at least one level");
        }
        BigDecimal[] prices = new BigDecimal[levels.size()];
        BigDecimal[] cumulative = new BigDecimal[levels.size()];
        BigDecimal sum = BigDecimal.ZERO;
        for (int j = 0; j < prices.length; j++) {
            Level level = levels.get(j);
            if (level.size().signum() <= 0) {
                throw new IllegalArgumentException("level " + j + " has a size that is not positive: " + level.size());
            }
            sum = sum.add(level.size());
            prices[j] = level.price();
            cumulative[j] = sum;
        }
        return new PriceVolumeCurve(prices, cumulative);
    }

    /** The total volume on this side: the curve is defined for cumulative volumes up to this. */
    public BigDecimal depth() {
        return cumulative[cumulative.length - 1];
    }

    /**
     * The number of whole spacing steps (s = 1) in the depth, floor(depth / s): the largest sample v_i = i·s at which
     * the spaced curve is defined.
     */
    public long wholeUnits() {
        return depth().divide(SPACING).setScale(0, RoundingMode.FLOOR).longValueExact();
    }

    /**
     * Eq. 1a / 1b: the price of the level j+1 where the first j sizes sum to less than v and the first j+1 sum to at
     * least v. Empty if v is beyond the depth (the curve is undefined there).
     *
     * @throws IllegalArgumentException if v is not greater than zero
     */
    public Optional<BigDecimal> unspaced(BigDecimal v) {
        if (v.signum() <= 0) {
            throw new IllegalArgumentException("volume must be greater than zero, got " + v);
        }
        if (v.compareTo(depth()) > 0) {
            return Optional.empty();
        }
        // The first level whose cumulative size is >= v (binary search; cumulative sizes only grow).
        int low = 0;
        int high = cumulative.length - 1;
        while (low < high) {
            int middle = (low + high) >>> 1;
            if (cumulative[middle].compareTo(v) >= 0) {
                high = middle;
            } else {
                low = middle + 1;
            }
        }
        return Optional.of(prices[low]);
    }

    /**
     * Eq. 1c / 1d: the spaced curve, the unspaced curve at s·ceil(v/s). Division by s = 1 is exact. Empty if that
     * volume is beyond the depth.
     *
     * @throws IllegalArgumentException if v is not greater than zero
     */
    public Optional<BigDecimal> priceAt(BigDecimal v) {
        BigDecimal rounded = v.divide(SPACING).setScale(0, RoundingMode.CEILING).multiply(SPACING);
        return unspaced(rounded);
    }
}
