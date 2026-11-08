package com.cfbenchmarks.obsi.input;

import java.util.Objects;

/**
 * Outcome of reading the input: either a snapshot or an input error with a short reason (D-08).
 * Input errors are values, never exceptions (spec §2: malformed input is an input error).
 */
public sealed interface InputResult {

    record Accepted(Snapshot snapshot) implements InputResult {
        public Accepted {
            Objects.requireNonNull(snapshot, "snapshot");
        }
    }

    /** @param reason short, stable, prefixed {@code "input error: "} (D-08) */
    record Rejected(String reason) implements InputResult {
        public Rejected {
            Objects.requireNonNull(reason, "reason");
        }
    }
}
