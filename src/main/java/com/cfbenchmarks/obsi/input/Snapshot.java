package com.cfbenchmarks.obsi.input;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * A parsed input snapshot.
 *
 * @param calculationTime T (spec §1), the original string, validated but never reformatted (D-07)
 * @param books           the books in input order; may be empty (D-07)
 * @param unlockStarts    the {@code start} of every announced unlock (proposal.md, "Input change"). Empty unless the
 *                        input was read for {@code --new-ped} (D-04, D-23)
 */
public record Snapshot(String calculationTime, List<BookInput> books, List<Instant> unlockStarts) {

    public Snapshot {
        Objects.requireNonNull(calculationTime, "calculationTime");
        books = List.copyOf(books);
        unlockStarts = List.copyOf(unlockStarts);
    }

    /** A snapshot with no announced unlock. */
    public Snapshot(String calculationTime, List<BookInput> books) {
        this(calculationTime, books, List.of());
    }
}
