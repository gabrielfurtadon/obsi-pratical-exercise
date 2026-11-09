package com.cfbenchmarks.obsi.result;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

/**
 * Outcome of one OBSI calculation from one snapshot (spec §6): a published value or a calculation failure.
 * Input and usage errors are not here: they happen before there is a snapshot to calculate from (D-07, D-08).
 */
public sealed interface CalculationResult {

    /**
     * @param calculationTime   the input's {@code calculationTime}, exactly as given (D-07)
     * @param value             the index rounded to two decimals (§5)
     * @param utilizedDepth     v_T in XYZ, a whole number (Eq. 2)
     * @param excludedExchanges every screened-out exchange in input order, names as given (brief, "Output")
     */
    record Published(String calculationTime, BigDecimal value, BigDecimal utilizedDepth,
                     List<String> excludedExchanges) implements CalculationResult {
        public Published {
            Objects.requireNonNull(calculationTime, "calculationTime");
            Objects.requireNonNull(value, "value");
            Objects.requireNonNull(utilizedDepth, "utilizedDepth");
            excludedExchanges = List.copyOf(excludedExchanges);
        }
    }

    /**
     * @param calculationTime the input's {@code calculationTime}, exactly as given: a calculation failure happens
     *                        on a conforming input (D-07)
     * @param reason          the §6 failure reason of the stage that failed (D-08, D-19, D-20, D-21)
     */
    record Failed(String calculationTime, String reason) implements CalculationResult {
        public Failed {
            Objects.requireNonNull(calculationTime, "calculationTime");
            Objects.requireNonNull(reason, "reason");
        }
    }
}
