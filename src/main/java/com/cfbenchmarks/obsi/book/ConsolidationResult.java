package com.cfbenchmarks.obsi.book;

import com.cfbenchmarks.obsi.input.Level;

import java.util.List;
import java.util.Objects;

/** Outcome of §3: the consolidated, uncrossed book, or the §6 "uncrossing exhausts a side" failure. */
public sealed interface ConsolidationResult {

    /**
     * A book after §3, with the guarantees later steps rely on, checked on construction (like {@link ValidBook}).
     * Prices are compared with {@code compareTo}, so {@code 100.0} and {@code 100} are the same price.
     *
     * @param bids the consolidated bids, highest price first (B_T, spec §1): strictly descending prices, sizes
     *             greater than zero, at least one level
     * @param asks the consolidated asks, lowest price first (A_T, spec §1): strictly ascending prices, sizes
     *             greater than zero, at least one level
     * @throws IllegalArgumentException if a side is empty, out of order or has a repeated price, a size is not
     *                                  greater than zero, or the best bid is not below the best ask (§3: uncrossing
     *                                  ends only when best bid &lt; best ask)
     */
    record Consolidated(List<Level> bids, List<Level> asks) implements ConsolidationResult {
        public Consolidated {
            bids = List.copyOf(bids);
            asks = List.copyOf(asks);
            // An empty side has no best price: §3/§6 call that an exhausted side, which is a Failed, not a book.
            if (bids.isEmpty() || asks.isEmpty()) {
                throw new IllegalArgumentException("a consolidated book needs at least one bid and one ask");
            }
            requireOrderedWithPositiveSizes("bids", bids, -1);
            requireOrderedWithPositiveSizes("asks", asks, 1);
            // §3: the loop ends only when best bid < best ask.
            if (bids.get(0).price().compareTo(asks.get(0).price()) >= 0) {
                throw new IllegalArgumentException("best bid " + bids.get(0).price()
                        + " is not below best ask " + asks.get(0).price());
            }
        }

        /** @param direction -1 when prices must fall from one level to the next (bids), 1 when they must rise (asks) */
        private static void requireOrderedWithPositiveSizes(String side, List<Level> levels, int direction) {
            for (int i = 0; i < levels.size(); i++) {
                if (levels.get(i).size().signum() <= 0) {
                    throw new IllegalArgumentException(side + "[" + i + "] has a size that is not positive: "
                            + levels.get(i).size());
                }
                // Strict: a repeated price (compareTo == 0) is rejected as well as the wrong direction.
                if (i > 0 && levels.get(i).price().compareTo(levels.get(i - 1).price()) * direction <= 0) {
                    throw new IllegalArgumentException(side + " are not strictly " + (direction < 0 ? "descending" : "ascending")
                            + " at index " + i + ": " + levels.get(i - 1).price() + " then " + levels.get(i).price());
                }
            }
        }
    }

    /** @param reason the §6 failure reason (D-08, D-20) */
    record Failed(String reason) implements ConsolidationResult {
        public Failed {
            Objects.requireNonNull(reason, "reason");
        }
    }
}
