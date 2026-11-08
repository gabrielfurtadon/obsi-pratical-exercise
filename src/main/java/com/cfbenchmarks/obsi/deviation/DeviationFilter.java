package com.cfbenchmarks.obsi.deviation;

import com.cfbenchmarks.obsi.book.ValidBook;
import com.cfbenchmarks.obsi.input.Snapshot;

import java.util.List;

/**
 * The orderbook percentage deviation (PED) screen (spec §2). Nothing outside this package knows
 * which implementation is in use: {@link StaticPedFilter} is the 5% rule, and the Task 2 filter
 * is a second implementation.
 *
 * <p>The interface is not shaped around mids only. An implementation gets each structurally valid
 * book with its best bid, best ask and levels, and the whole {@link Snapshot}, so it can use
 * other books and any further input fields. It computes whatever it needs (such as the median)
 * itself.
 */
public interface DeviationFilter {

    /**
     * @param structurallyValid the books that passed the structural screens, in input order; never
     *                          unreadable, with an empty side, or crossed
     * @param snapshot          the snapshot the books came from
     * @return the books this filter excludes: a subset of {@code structurallyValid}
     */
    List<ValidBook> excluded(List<ValidBook> structurallyValid, Snapshot snapshot);
}
