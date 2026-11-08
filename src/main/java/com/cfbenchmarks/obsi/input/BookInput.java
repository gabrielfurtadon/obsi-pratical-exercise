package com.cfbenchmarks.obsi.input;

import java.util.List;
import java.util.Objects;

/**
 * One element of {@code books}. A book with a valid exchange name is always represented, even when
 * its sides cannot be read, so the screening phase can exclude it and list it in
 * {@code excludedExchanges} (D-03, spec §2: "Exclude a book if its structure cannot be read").
 */
public sealed interface BookInput {

    /** The exchange name exactly as given in the input (D-16). */
    String exchange();

    /**
     * A book whose {@code bids} and {@code asks} are both arrays. Entries that are not a two-number
     * array have already been discarded (D-01, D-05); levels keep their input order. Either side may
     * be empty, and values may still be non-positive or crossed: that is §2 screening.
     */
    record ReadableBook(String exchange, List<Level> bids, List<Level> asks) implements BookInput {
        public ReadableBook {
            Objects.requireNonNull(exchange, "exchange");
            bids = List.copyOf(bids);
            asks = List.copyOf(asks);
        }
    }

    /** A book with a valid name whose {@code bids} or {@code asks} is missing or not an array (D-03, D-06). */
    record UnreadableBook(String exchange) implements BookInput {
        public UnreadableBook {
            Objects.requireNonNull(exchange, "exchange");
        }
    }
}
