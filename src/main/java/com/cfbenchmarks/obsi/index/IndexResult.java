package com.cfbenchmarks.obsi.index;

import java.math.BigDecimal;
import java.util.Objects;

/** Outcome of §4/§5: the published index value and utilized depth, or the §6 "Eq. 2 set is empty" failure. */
public sealed interface IndexResult {

    /**
     * @param value         the OBSI value rounded to two decimal places, HALF_UP (§5), so its scale is exactly 2
     * @param utilizedDepth v_T in XYZ (Eq. 2): a positive whole number with scale 0, so
     *                      {@code toPlainString()} gives {@code "4"} and never {@code "4.0"} or {@code "1E+2"} (D-14)
     * @throws IllegalArgumentException if either figure breaks those rules
     */
    record Published(BigDecimal value, BigDecimal utilizedDepth) implements IndexResult {
        public Published {
            Objects.requireNonNull(value, "value");
            Objects.requireNonNull(utilizedDepth, "utilizedDepth");
            if (value.scale() != 2) {
                throw new IllegalArgumentException("the published value has exactly two decimals, got " + value);
            }
            if (utilizedDepth.scale() != 0 || utilizedDepth.signum() <= 0) {
                throw new IllegalArgumentException(
                        "the utilized depth is a positive whole number with scale 0, got " + utilizedDepth);
            }
        }
    }

    /** @param reason the §6 failure reason (D-08, D-21) */
    record Failed(String reason) implements IndexResult {
        public Failed {
            Objects.requireNonNull(reason, "reason");
        }
    }
}
