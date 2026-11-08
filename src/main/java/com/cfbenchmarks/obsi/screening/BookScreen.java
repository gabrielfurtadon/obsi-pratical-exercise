package com.cfbenchmarks.obsi.screening;

import com.cfbenchmarks.obsi.book.ValidBook;
import com.cfbenchmarks.obsi.input.BookInput.ReadableBook;
import com.cfbenchmarks.obsi.input.Level;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * Spec §2, first paragraph: the entry screen and the structural screens for one book, applied
 * before any exchanges are combined.
 */
final class BookScreen {

    private BookScreen() {
    }

    /**
     * Applies the entry screen, then the structural screens.
     *
     * @return the structurally valid book, or empty if the book is excluded
     */
    static Optional<ValidBook> structurallyValid(ReadableBook book) {
        // §2: discard an entry if its price or size is not strictly positive. (That it is numeric was
        // already decided when the input was parsed: D-01, D-05.)
        List<Level> bids = strictlyPositive(book.bids());
        List<Level> asks = strictlyPositive(book.asks());

        // §2: exclude a book if either side is empty after entry screening.
        if (bids.isEmpty() || asks.isEmpty()) {
            return Optional.empty();
        }

        // D-11: best means best by price, not first in the input. Prices compare with compareTo.
        BigDecimal bestBid = bids.stream().map(Level::price).max(BigDecimal::compareTo).orElseThrow();
        BigDecimal bestAsk = asks.stream().map(Level::price).min(BigDecimal::compareTo).orElseThrow();

        // §2: exclude a book if its best bid is greater than or equal to its best ask.
        if (bestBid.compareTo(bestAsk) >= 0) {
            return Optional.empty();
        }
        return Optional.of(new ValidBook(book.exchange(), bids, asks, bestBid, bestAsk));
    }

    private static List<Level> strictlyPositive(List<Level> side) {
        return side.stream()
                .filter(level -> level.price().signum() > 0 && level.size().signum() > 0)
                .toList();
    }
}
