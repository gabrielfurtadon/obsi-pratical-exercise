package com.cfbenchmarks.obsi.book;

import com.cfbenchmarks.obsi.input.Level;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * §3: combines the surviving books into one book and uncrosses it.
 *
 * <p>Each side is a {@link TreeMap} keyed by price. {@code TreeMap} orders and merges keys with
 * {@code compareTo}, so {@code 100.0} and {@code 100} are one level. Nothing here uses {@code equals}
 * or hashing on a price. Sizes are added and subtracted as {@link BigDecimal}, so residuals are exact.
 */
public final class Consolidator {

    // §6 / D-08 / D-20: the reasons when uncrossing exhausts a side.
    static final String EXHAUSTED_BIDS = "uncrossing exhausted the bid side";
    static final String EXHAUSTED_ASKS = "uncrossing exhausted the ask side";
    static final String EXHAUSTED_BOTH = "uncrossing exhausted both sides";

    private Consolidator() {
    }

    /**
     * @param survivors the books that survived screening (§2), at least one. The screener fails with §6 before
     *                  it would pass on an empty list.
     * @throws IllegalArgumentException if {@code survivors} is empty
     */
    public static ConsolidationResult consolidate(List<ValidBook> survivors) {
        if (survivors.isEmpty()) {
            throw new IllegalArgumentException("consolidation needs at least one surviving book");
        }
        // §3 ¶1 / D-11: combine every surviving book; sizes at identical prices are summed in uncross().
        List<Level> bids = new ArrayList<>();
        List<Level> asks = new ArrayList<>();
        for (ValidBook book : survivors) {
            bids.addAll(book.bids());
            asks.addAll(book.asks());
        }
        return uncross(bids, asks);
    }

    /**
     * Sums the sizes at identical prices on each side (§3 ¶1), then uncrosses (§3 ¶2). The levels may be in any
     * order and may repeat a price. Package-private so that the exhaustion branch, which books that passed §2
     * cannot reach (D-20), can be tested with sides built by hand.
     */
    static ConsolidationResult uncross(List<Level> bidLevels, List<Level> askLevels) {
        // §3 ¶1: bids from highest to lowest price, asks from lowest to highest.
        TreeMap<BigDecimal, BigDecimal> bids = new TreeMap<>(Comparator.<BigDecimal>naturalOrder().reversed());
        TreeMap<BigDecimal, BigDecimal> asks = new TreeMap<>();
        for (Level level : bidLevels) {
            bids.merge(level.price(), level.size(), BigDecimal::add);
        }
        for (Level level : askLevels) {
            asks.merge(level.price(), level.size(), BigDecimal::add);
        }

        // §3 ¶2: while best bid >= best ask, take the smaller size off both levels and drop what reaches zero.
        // Every round removes at least one level, so the loop ends.
        while (!bids.isEmpty() && !asks.isEmpty() && bids.firstKey().compareTo(asks.firstKey()) >= 0) {
            Map.Entry<BigDecimal, BigDecimal> bid = bids.firstEntry();
            Map.Entry<BigDecimal, BigDecimal> ask = asks.firstEntry();
            BigDecimal traded = bid.getValue().min(ask.getValue());
            reduce(bids, bid, traded);
            reduce(asks, ask, traded);
        }

        // §3 ¶2, §6: the calculation fails if either side is exhausted (D-20).
        if (bids.isEmpty() && asks.isEmpty()) {
            return new ConsolidationResult.Failed(EXHAUSTED_BOTH);
        }
        if (bids.isEmpty()) {
            return new ConsolidationResult.Failed(EXHAUSTED_BIDS);
        }
        if (asks.isEmpty()) {
            return new ConsolidationResult.Failed(EXHAUSTED_ASKS);
        }
        return new ConsolidationResult.Consolidated(levels(bids), levels(asks));
    }

    /** Takes {@code traded} off a level and removes the level if nothing is left (compareTo, not equals). */
    private static void reduce(TreeMap<BigDecimal, BigDecimal> side, Map.Entry<BigDecimal, BigDecimal> level,
                               BigDecimal traded) {
        BigDecimal residual = level.getValue().subtract(traded);
        if (residual.signum() == 0) {
            side.remove(level.getKey());
        } else {
            side.put(level.getKey(), residual);
        }
    }

    private static List<Level> levels(TreeMap<BigDecimal, BigDecimal> side) {
        return side.entrySet().stream().map(e -> new Level(e.getKey(), e.getValue())).toList();
    }
}
