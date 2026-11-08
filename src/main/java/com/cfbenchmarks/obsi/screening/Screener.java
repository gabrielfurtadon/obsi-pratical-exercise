package com.cfbenchmarks.obsi.screening;

import com.cfbenchmarks.obsi.book.ValidBook;
import com.cfbenchmarks.obsi.deviation.DeviationFilter;
import com.cfbenchmarks.obsi.input.BookInput;
import com.cfbenchmarks.obsi.input.BookInput.ReadableBook;
import com.cfbenchmarks.obsi.input.BookInput.UnreadableBook;
import com.cfbenchmarks.obsi.input.Snapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;

/**
 * Applies the §2 screens to a snapshot: the entry screen and the structural screens per book, then
 * the deviation screen on the structurally valid books. Which deviation filter is used is the
 * caller's choice; this class only knows the {@link DeviationFilter} interface.
 */
public final class Screener {

    // §6 / D-08 / D-19: the reasons when no constituent book survives screening.
    static final String NO_SURVIVOR = "no constituent book survived screening";
    static final String NO_SURVIVOR_BY_PED =
            "no constituent book survived screening (every structurally valid book was excluded by PED)";

    private Screener() {
    }

    /**
     * @throws IllegalStateException if the filter returns a book it was not given. Books are matched by
     *                               identity: {@link ValidBook} is a record, so a value-equal copy is not
     *                               the book that was passed in.
     */
    public static ScreeningResult screen(Snapshot snapshot, DeviationFilter deviationFilter) {
        List<BookInput> inputs = snapshot.books();

        // Structural screens, book by book, in input order (§2). validAt[i] is the structurally valid book
        // made from inputs.get(i), or null if that book is excluded. It ties each result back to its
        // position in the input, so nothing below needs to look a book up by name.
        ValidBook[] validAt = new ValidBook[inputs.size()];
        List<ValidBook> structurallyValid = new ArrayList<>();
        for (int i = 0; i < inputs.size(); i++) {
            switch (inputs.get(i)) {
                // §2 / D-03: a book whose structure cannot be read is excluded. It stays null here, so it
                // is reported in excludedExchanges below.
                case UnreadableBook _ -> { }
                case ReadableBook readable -> {
                    validAt[i] = BookScreen.structurallyValid(readable).orElse(null);
                    if (validAt[i] != null) {
                        structurallyValid.add(validAt[i]);
                    }
                }
            }
        }

        // Deviation screen: only on the structurally valid books (§1: the median is of structurally
        // valid mids), with the snapshot for filters that need more than the books.
        List<ValidBook> given = List.copyOf(structurallyValid);
        Set<ValidBook> deviating = identitySet(given, deviationFilter.excluded(given, snapshot));

        // Walk the input once, in order: each book is a survivor or an excluded exchange (brief "Output":
        // excludedExchanges lists every screened-out exchange in input order, names as given).
        List<ValidBook> survivors = new ArrayList<>();
        List<String> excludedExchanges = new ArrayList<>();
        for (int i = 0; i < inputs.size(); i++) {
            if (validAt[i] != null && !deviating.contains(validAt[i])) {
                survivors.add(validAt[i]);
            } else {
                excludedExchanges.add(inputs.get(i).exchange());
            }
        }

        // §6: the calculation fails if no constituent book survives screening. D-12, D-19: say so when the
        // structurally valid books were all removed by the deviation screen; if none was structurally
        // valid (or there are no books), PED never ran.
        if (survivors.isEmpty()) {
            return new ScreeningResult.Failed(structurallyValid.isEmpty() ? NO_SURVIVOR : NO_SURVIVOR_BY_PED);
        }
        return new ScreeningResult.Screened(survivors, excludedExchanges);
    }

    /** The books the filter returned, as an identity set, after checking each is one of those it was given. */
    private static Set<ValidBook> identitySet(List<ValidBook> given, List<ValidBook> returned) {
        Set<ValidBook> givenSet = Collections.newSetFromMap(new IdentityHashMap<>());
        givenSet.addAll(given);
        Set<ValidBook> result = Collections.newSetFromMap(new IdentityHashMap<>());
        for (ValidBook book : returned) {
            if (!givenSet.contains(book)) {
                throw new IllegalStateException(
                        "deviation filter returned a book it was not given (matched by identity): " + book.exchange());
            }
            result.add(book);
        }
        return result;
    }
}
