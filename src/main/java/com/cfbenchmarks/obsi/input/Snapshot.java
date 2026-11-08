package com.cfbenchmarks.obsi.input;

import java.util.List;
import java.util.Objects;

/**
 * A parsed input snapshot.
 *
 * @param calculationTime T (spec §1), the original string, validated but never reformatted (D-07)
 * @param books           the books in input order; may be empty (D-07)
 */
public record Snapshot(String calculationTime, List<BookInput> books) {

    public Snapshot {
        Objects.requireNonNull(calculationTime, "calculationTime");
        books = List.copyOf(books);
    }
}
