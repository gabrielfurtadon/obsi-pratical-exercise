package com.cfbenchmarks.obsi.index;

import com.cfbenchmarks.obsi.book.ConsolidationResult.Consolidated;
import com.cfbenchmarks.obsi.curve.MidCurve;
import com.cfbenchmarks.obsi.curve.PriceVolumeCurve;
import com.cfbenchmarks.obsi.index.IndexResult.Failed;
import com.cfbenchmarks.obsi.index.IndexResult.Published;
import com.cfbenchmarks.obsi.input.Level;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.OptionalLong;
import java.util.Random;
import java.util.stream.Stream;

import static com.cfbenchmarks.obsi.index.IndexTestSupport.literalEquation2;
import static com.cfbenchmarks.obsi.index.IndexTestSupport.midSv;
import static org.assertj.core.api.Assertions.assertThat;

/** Property-style tests for §5: inputs come from a {@link Random} with a fixed seed. */
class IndexPropertyTest {

    private static final long SEED = 20261110L;
    private static final int CASES = 500;

    /**
     * A consolidated book around 100: the best ask a few cents above 100.00, the best bid a few cents below, then
     * up to eleven further levels a few cents apart. Sizes are 0.05 to 3.00, so depths run from under one unit to
     * about thirty, and the mid-spread crosses D = 0.5% at very different volumes (or not at all).
     */
    static Stream<Consolidated> randomBooks() {
        Random random = new Random(SEED);
        List<Consolidated> cases = new ArrayList<>();
        for (int i = 0; i < CASES; i++) {
            List<Level> bids = new ArrayList<>();
            List<Level> asks = new ArrayList<>();
            long bid = 10000 - random.nextInt(25);
            long ask = 10000 + random.nextInt(25);
            if (bid >= ask) {
                bid = ask - 1;
            }
            for (int n = 1 + random.nextInt(8); n > 0; n--) {
                bids.add(new Level(BigDecimal.valueOf(bid, 2), size(random)));
                bid -= 1 + random.nextInt(30);
            }
            for (int n = 1 + random.nextInt(8); n > 0; n--) {
                asks.add(new Level(BigDecimal.valueOf(ask, 2), size(random)));
                ask += 1 + random.nextInt(30);
            }
            cases.add(new Consolidated(bids, asks));
        }
        return cases.stream();
    }

    private static BigDecimal size(Random random) {
        return BigDecimal.valueOf(5 + random.nextInt(296), 2);
    }

    /**
     * D-21: the midSV curve never decreases, so the first crossing is the only one and stopping there equals the
     * literal maximum of Eq. 2. The oracle walks every sample and divides for real.
     */
    @ParameterizedTest
    @MethodSource("randomBooks")
    void stoppingAtTheFirstCrossingEqualsTheLiteralMaximumOfEquation2(Consolidated book) {
        PriceVolumeCurve bids = PriceVolumeCurve.of(book.bids());
        PriceVolumeCurve asks = PriceVolumeCurve.of(book.asks());

        OptionalLong actual = UtilizedDepth.find(bids, asks);

        assertThat(actual).isEqualTo(literalEquation2(bids, asks));
    }

    /** The premise behind D-21, checked on the same books: midSV never decreases from one sample to the next. */
    @ParameterizedTest
    @MethodSource("randomBooks")
    void midSpreadNeverDecreasesWithVolume(Consolidated book) {
        PriceVolumeCurve bids = PriceVolumeCurve.of(book.bids());
        PriceVolumeCurve asks = PriceVolumeCurve.of(book.asks());
        long samples = Math.min(bids.wholeUnits(), asks.wholeUnits());

        for (long v = 1; v < samples; v++) {
            assertThat(midSv(bids, asks, v + 1)).isGreaterThanOrEqualTo(midSv(bids, asks, v));
        }
    }

    /** The division-free cutoff agrees with the division it replaces, on every sample of every random book. */
    @ParameterizedTest
    @MethodSource("randomBooks")
    void theDivisionFreeCutoffAgreesWithDividing(Consolidated book) {
        PriceVolumeCurve bids = PriceVolumeCurve.of(book.bids());
        PriceVolumeCurve asks = PriceVolumeCurve.of(book.asks());
        long samples = Math.min(bids.wholeUnits(), asks.wholeUnits());

        for (long v = 1; v <= samples; v++) {
            BigDecimal ask = asks.priceAt(BigDecimal.valueOf(v)).orElseThrow();
            BigDecimal bid = bids.priceAt(BigDecimal.valueOf(v)).orElseThrow();
            boolean divided = midSv(bids, asks, v).compareTo(MidCurve.CUTOFF) <= 0;
            assertThat(MidCurve.midSvWithinCutoff(ask, MidCurve.midPV(ask, bid))).isEqualTo(divided);
        }
    }

    /** The value is a weighted mean of midPV(1..v_T), so it lies between the smallest and largest of them. */
    @ParameterizedTest
    @MethodSource("randomBooks")
    void thePublishedValueLiesBetweenTheSmallestAndLargestMid(Consolidated book) {
        PriceVolumeCurve bids = PriceVolumeCurve.of(book.bids());
        PriceVolumeCurve asks = PriceVolumeCurve.of(book.asks());

        IndexResult result = ObsiIndex.calculate(book);

        if (result instanceof Published p) {
            long vT = p.utilizedDepth().longValueExact();
            assertThat(OptionalLong.of(vT)).isEqualTo(literalEquation2(bids, asks));
            BigDecimal low = null;
            BigDecimal high = null;
            for (long v = 1; v <= vT; v++) {
                BigDecimal mid = MidCurve.midPV(asks.priceAt(BigDecimal.valueOf(v)).orElseThrow(),
                        bids.priceAt(BigDecimal.valueOf(v)).orElseThrow());
                low = low == null || mid.compareTo(low) < 0 ? mid : low;
                high = high == null || mid.compareTo(high) > 0 ? mid : high;
            }
            // the published value is rounded to cents, so allow half a cent either way
            assertThat(p.value()).isGreaterThanOrEqualTo(low.subtract(new BigDecimal("0.005")));
            assertThat(p.value()).isLessThanOrEqualTo(high.add(new BigDecimal("0.005")));
            assertThat(p.value().scale()).isEqualTo(2);
        } else {
            assertThat(literalEquation2(bids, asks)).isEmpty();
            assertThat(((Failed) result).reason()).startsWith("empty Eq. 2 set: ");
        }
    }

    /** Guards the properties against passing vacuously: both outcomes, and deeper crossings, occur. */
    @Test
    void theGeneratedBooksCoverPublishedAndFailedOutcomes() {
        List<IndexResult> results = randomBooks().map(ObsiIndex::calculate).toList();
        long published = results.stream().filter(r -> r instanceof Published).count();
        long deeper = results.stream()
                .filter(r -> r instanceof Published p && p.utilizedDepth().compareTo(new BigDecimal("3")) >= 0)
                .count();

        assertThat(published).isGreaterThan(CASES / 5);
        assertThat(results.size() - published).isGreaterThan(CASES / 20);
        assertThat(deeper).isGreaterThan(CASES / 20);
    }
}
