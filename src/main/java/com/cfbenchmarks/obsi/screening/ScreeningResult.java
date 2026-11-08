package com.cfbenchmarks.obsi.screening;

import com.cfbenchmarks.obsi.book.ValidBook;

import java.util.List;
import java.util.Objects;

/** Outcome of the §2 screens: the books that survive, or the §6 "no book survives" failure. */
public sealed interface ScreeningResult {

    /**
     * @param survivors          the books that passed every screen, in input order
     * @param excludedExchanges  every excluded exchange, in input order, names as given (brief, "Output")
     */
    record Screened(List<ValidBook> survivors, List<String> excludedExchanges) implements ScreeningResult {
        public Screened {
            survivors = List.copyOf(survivors);
            excludedExchanges = List.copyOf(excludedExchanges);
        }
    }

    /** @param reason the §6 failure reason (D-08, D-12, D-19) */
    record Failed(String reason) implements ScreeningResult {
        public Failed {
            Objects.requireNonNull(reason, "reason");
        }
    }
}
