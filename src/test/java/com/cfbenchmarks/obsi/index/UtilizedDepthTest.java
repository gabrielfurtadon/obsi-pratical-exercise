package com.cfbenchmarks.obsi.index;

import com.cfbenchmarks.obsi.book.ConsolidationResult.Consolidated;
import com.cfbenchmarks.obsi.curve.PriceVolumeCurve;
import com.cfbenchmarks.obsi.input.Level;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;
import java.util.OptionalLong;

import static com.cfbenchmarks.obsi.TestBooks.levels;
import static com.cfbenchmarks.obsi.index.IndexTestSupport.consolidatedFixture;
import static org.assertj.core.api.Assertions.assertThat;

/** §5, Eq. 2: the utilized depth v_T. */
class UtilizedDepthTest {

    private static OptionalLong find(List<Level> bids, List<Level> asks) {
        return UtilizedDepth.find(PriceVolumeCurve.of(bids), PriceVolumeCurve.of(asks));
    }

    private static OptionalLong find(Consolidated book) {
        return find(book.bids(), book.asks());
    }

    /** E1. v = 4 is within D (0.00399) and v = 5 is not (0.00999). */
    @Test
    void theSampleHasAUtilizedDepthOf4() throws IOException {
        assertThat(find(consolidatedFixture("sample-input.json"))).hasValue(4);
    }

    /** E2. v = 6 is within D (0.004498) and v = 7 is not (0.00599). */
    @Test
    void theEdgeCasesFixtureHasAUtilizedDepthOf6() throws IOException {
        assertThat(find(consolidatedFixture("edge-cases.json"))).hasValue(6);
    }

    /** E3. midSV exactly D is within: ask 100.5 over mid 100. The next sample (ask 102, bid 98) is outside. */
    @Test
    void aMidSpreadExactlyAtTheCutoffCountsAsWithin() {
        assertThat(find(levels("99.5:1", "98:5"), levels("100.5:1", "102:5"))).hasValue(1);
        assertThat(find(levels("99.50:1", "98:5"), levels("100.50:1", "102:5"))).hasValue(1);
    }

    /** E3b. A hair above D at v = 1 means no sample is within D, so there is no candidate. */
    @Test
    void aMidSpreadJustAboveTheCutoffIsNotWithin() {
        assertThat(find(levels("99.5:1", "98:5"), levels("100.5001:1", "102:5"))).isEmpty();
    }

    /** E4. Every defined sample is within D, so no crossing happens before the curves end. */
    @Test
    void noCrossingBeforeTheCurvesEndGivesNoDepth() {
        assertThat(find(levels("100:5"), levels("100.1:5"))).isEmpty();
        assertThat(UtilizedDepth.commonSamples(PriceVolumeCurve.of(levels("100:5")),
                PriceVolumeCurve.of(levels("100.1:5")))).isEqualTo(5);
    }

    /** E4b. Already above D at v = 1: the spread only widens, so no later sample is within D either. */
    @Test
    void aBookAboveTheCutoffFromTheFirstSampleGivesNoDepth() {
        assertThat(find(levels("99:5"), levels("100.5:5"))).isEmpty();
    }

    /** E5. Fewer than two whole samples on either curve: no consecutive pair can be evaluated. */
    @Test
    void lessThanTwoSamplesCannotBeEvaluated() {
        PriceVolumeCurve thin = PriceVolumeCurve.of(levels("100:1.99"));
        PriceVolumeCurve deep = PriceVolumeCurve.of(levels("100.1:5"));

        assertThat(UtilizedDepth.commonSamples(thin, deep)).isEqualTo(1);
        assertThat(UtilizedDepth.commonSamples(deep, thin)).isEqualTo(1);
        assertThat(UtilizedDepth.find(thin, deep)).isEmpty();
        assertThat(UtilizedDepth.find(deep, thin)).isEmpty();
    }

    /** E5b. Exactly two whole units on both sides is enough for one pair. */
    @Test
    void exactlyTwoSamplesEvaluateOnePair() {
        assertThat(find(levels("100:1", "80:1"), levels("100.1:1", "120:1"))).hasValue(1);
        assertThat(UtilizedDepth.commonSamples(PriceVolumeCurve.of(levels("100:1", "80:1")),
                PriceVolumeCurve.of(levels("100.1:1", "120:1")))).isEqualTo(2);
    }

    /**
     * E6. A candidate needs both curves at v_i and v_{i+1}. The crossing between 4 and 5 is only evaluated when the
     * bids reach 5; with bids ending at 4.5 the asks alone cannot make it count.
     */
    @Test
    void aCandidateNeedsBothCurvesDefinedAtTheNextSample() {
        List<Level> asks = levels("100.1:1", "100.2:1", "100.3:1", "100.4:1", "105:6");

        assertThat(find(levels("100:1", "99.9:1", "99.8:1", "99.7:1.5"), asks)).isEmpty();
        assertThat(find(levels("100:1", "99.9:1", "99.8:1", "99.7:2"), asks)).hasValue(4);
    }

    /**
     * E8. The scan stops at the first crossing (D-21), so its cost depends on v_T and not on the book's depth. With a
     * trillion units behind the crossing, a walk over every sample would not finish within the test timeout.
     */
    @Test
    void theScanStopsAtTheFirstCrossingWhateverTheDepth() {
        List<Level> bids = levels("100:1", "99.8:1", "90:1000000000000");
        List<Level> asks = levels("100.1:1", "100.2:1", "103:1000000000000");

        assertThat(find(bids, asks)).hasValue(2);
    }

    /** E8b. The same for a book that is above D from the first sample: no walk over the depth. */
    @Test
    void aBookAboveTheCutoffAtTheFirstSampleIsRejectedWithoutWalkingTheDepth() {
        assertThat(find(levels("99:1000000000000"), levels("100.5:1000000000000"))).isEmpty();
    }
}
