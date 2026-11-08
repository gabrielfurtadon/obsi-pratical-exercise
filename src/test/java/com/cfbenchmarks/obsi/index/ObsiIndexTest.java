package com.cfbenchmarks.obsi.index;

import com.cfbenchmarks.obsi.book.ConsolidationResult.Consolidated;
import com.cfbenchmarks.obsi.curve.PriceVolumeCurve;
import com.cfbenchmarks.obsi.index.IndexResult.Published;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.math.BigDecimal;

import static com.cfbenchmarks.obsi.TestBooks.levels;
import static com.cfbenchmarks.obsi.index.IndexTestSupport.book;
import static com.cfbenchmarks.obsi.index.IndexTestSupport.consolidatedFixture;
import static com.cfbenchmarks.obsi.index.IndexTestSupport.failureReason;
import static com.cfbenchmarks.obsi.index.IndexTestSupport.published;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/** §5: weights, normalization, Eq. 3 and rounding. */
class ObsiIndexTest {

    private static PriceVolumeCurve bidCurve(Consolidated b) {
        return PriceVolumeCurve.of(b.bids());
    }

    private static PriceVolumeCurve askCurve(Consolidated b) {
        return PriceVolumeCurve.of(b.asks());
    }

    /**
     * A book whose mid is {@code bid}/{@code ask} for the first five units and then jumps (ask 102), so v_T = 5 and the
     * mid is constant over the whole average. The weighted mean is then the mid itself, whatever the weights are.
     */
    private static Consolidated constantMid(String bid, String ask) {
        return book(levels(bid + ":10"), levels(ask + ":5", "102:5"));
    }

    // ---------------------------------------------------------------- the reference fixtures

    /** I1. About 100.2255 before rounding. */
    @Test
    void theSamplePublishes100_23WithADepthOf4() throws IOException {
        Consolidated consolidated = consolidatedFixture("sample-input.json");

        Published result = published(ObsiIndex.calculate(consolidated));

        assertThat(result.value().toPlainString()).isEqualTo("100.23");
        assertThat(result.utilizedDepth().toPlainString()).isEqualTo("4");
        assertThat(ObsiIndex.weightedMean(bidCurve(consolidated), askCurve(consolidated), 4))
                .isCloseTo(new BigDecimal("100.22548128"), within(new BigDecimal("1e-8")));
    }

    /** I2. About 100.0331 before rounding. */
    @Test
    void theEdgeCasesFixturePublishes100_03WithADepthOf6() throws IOException {
        Consolidated consolidated = consolidatedFixture("edge-cases.json");

        Published result = published(ObsiIndex.calculate(consolidated));

        assertThat(result.value().toPlainString()).isEqualTo("100.03");
        assertThat(result.utilizedDepth().toPlainString()).isEqualTo("6");
        assertThat(ObsiIndex.weightedMean(bidCurve(consolidated), askCurve(consolidated), 6))
                .isCloseTo(new BigDecimal("100.03306727"), within(new BigDecimal("1e-8")));
    }

    // ---------------------------------------------------------------- weights (D-13, D-14)

    /** I3a. v_T = 1: one weight, so NF cancels it and the value is midPV(1). */
    @Test
    void withADepthOfOneTheValueIsTheFirstMid() {
        Consolidated b = book(levels("99.5:1", "98:5"), levels("100.5:1", "102:5"));

        assertThat(ObsiIndex.weightedMean(bidCurve(b), askCurve(b), 1)).isEqualByComparingTo("100");
        assertThat(published(ObsiIndex.calculate(b)).value()).isEqualByComparingTo("100.00");
    }

    /** I3b. v_T = 2: lambda = 1/0.6, and the mean follows (m1 e^-l + m2 e^-2l) / (e^-l + e^-2l). */
    @Test
    void withADepthOfTwoTheWeightsFollowLambdaExponentialOfMinusLambdaV() {
        // midPV(1) = (100.1 + 100.0) / 2 = 100.05, midPV(2) = (100.2 + 99.8) / 2 = 100.0, and v = 3 is outside D
        Consolidated b = book(levels("100.0:1", "99.8:1", "90:5"), levels("100.1:1", "100.2:1", "103:5"));
        double lambda = 1 / (0.3 * 2);
        double w1 = lambda * StrictMath.exp(-lambda);
        double w2 = lambda * StrictMath.exp(-2 * lambda);
        double expected = (100.05 * w1 + 100.0 * w2) / (w1 + w2);

        assertThat(ObsiIndex.weightedMean(bidCurve(b), askCurve(b), 2).doubleValue()).isCloseTo(expected, within(1e-12));
        assertThat(published(ObsiIndex.calculate(b)).utilizedDepth().toPlainString()).isEqualTo("2");
    }

    // ---------------------------------------------------------------- rounding (§5)

    /** I4. HALF_UP: a value of exactly 100.005 rounds up (HALF_EVEN would give 100.00). */
    @Test
    void anExactHalfCentRoundsUp() {
        assertThat(published(ObsiIndex.calculate(constantMid("100.00", "100.01"))).value().toPlainString())
                .isEqualTo("100.01");
    }

    /** I4b. 100.025 rounds to 100.03 under HALF_UP (HALF_EVEN would give 100.02). */
    @Test
    void anotherExactHalfCentRoundsUp() {
        assertThat(published(ObsiIndex.calculate(constantMid("100.02", "100.03"))).value().toPlainString())
                .isEqualTo("100.03");
    }

    /** I4c. Just below the half does not round up. */
    @Test
    void justBelowAHalfCentRoundsDown() {
        assertThat(published(ObsiIndex.calculate(constantMid("100.00", "100.0098"))).value().toPlainString())
                .isEqualTo("100.00");
    }

    // ---------------------------------------------------------------- formatting and failures

    /** I5. The depth is a plain whole number (no exponent, no decimals); the value always has two decimals. */
    @Test
    void theDepthAndValueAreFormattedAsPlainStrings() {
        // v_T = 100: the mids are constant at 100.005 for 100 units, then the ask jumps
        Consolidated b = book(levels("100.00:200"), levels("100.01:100", "102:100"));

        Published result = published(ObsiIndex.calculate(b));

        assertThat(result.utilizedDepth().toPlainString()).isEqualTo("100");
        assertThat(result.value().scale()).isEqualTo(2);
        assertThat(result.value().toPlainString()).isEqualTo("100.01");
    }

    /** I6. Both empty-set cases of §6 come back as a failure with their own reason (D-21). */
    @Test
    void anEmptyEquation2SetFailsWithTheMatchingReason() {
        assertThat(failureReason(ObsiIndex.calculate(book(levels("100:1.99"), levels("100.1:5")))))
                .isEqualTo("empty Eq. 2 set: insufficient depth to evaluate consecutive samples");
        assertThat(failureReason(ObsiIndex.calculate(book(levels("100:5"), levels("100.1:5")))))
                .isEqualTo("empty Eq. 2 set: no mid-spread crossing before a curve ends");
        assertThat(failureReason(ObsiIndex.calculate(book(levels("99:5"), levels("100.5:5")))))
                .isEqualTo("empty Eq. 2 set: no mid-spread crossing before a curve ends");
    }
}
