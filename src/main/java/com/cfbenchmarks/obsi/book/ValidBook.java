package com.cfbenchmarks.obsi.book;

import com.cfbenchmarks.obsi.input.Level;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

/**
 * A structurally valid constituent book (spec §2): both sides are non-empty after the entry
 * screen and the best bid is below the best ask. It is what the deviation screen sees and what
 * consolidation (§3) receives for the books that survive.
 *
 * @param exchange the exchange name exactly as given in the input
 * @param bids     the entries that passed the entry screen, in input order (not sorted)
 * @param asks     the entries that passed the entry screen, in input order (not sorted)
 * @param bestBid  the highest bid price, by price and not by position (D-11)
 * @param bestAsk  the lowest ask price, by price and not by position (D-11)
 */
public record ValidBook(String exchange, List<Level> bids, List<Level> asks, BigDecimal bestBid,
                        BigDecimal bestAsk) {

    public ValidBook {
        Objects.requireNonNull(exchange, "exchange");
        bids = List.copyOf(bids);
        asks = List.copyOf(asks);
        Objects.requireNonNull(bestBid, "bestBid");
        Objects.requireNonNull(bestAsk, "bestAsk");
        // §2: a book is only valid with positive prices and best bid < best ask.
        if (bestBid.signum() <= 0 || bestBid.compareTo(bestAsk) >= 0) {
            throw new IllegalArgumentException(
                    "a structurally valid book needs 0 < bestBid < bestAsk, got " + bestBid + " / " + bestAsk);
        }
    }

    private static final BigDecimal TWO = BigDecimal.valueOf(2);

    /**
     * The top-of-book mid-price m = (b_best + a_best) / 2 (spec §2, §1). Dividing by 2 always
     * terminates, so the division is exact and needs no MathContext (D-14).
     */
    public BigDecimal mid() {
        return bestBid.add(bestAsk).divide(TWO);
    }
}
