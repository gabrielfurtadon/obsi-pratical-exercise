package com.cfbenchmarks.obsi.curve;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static com.cfbenchmarks.obsi.TestBooks.levels;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** §4, Eq. 1a-1d: the price reached by cumulative volume v. */
class PriceVolumeCurveTest {

    // The sample's consolidated sides (fractional sizes, total depth 12.3 on each side).
    private static final PriceVolumeCurve SAMPLE_ASKS = PriceVolumeCurve.of(levels(
            "100.3:0.8", "100.4:1.1", "100.6:2.4", "101.1:2", "101.2:2", "102:2", "102.1:2"));
    private static final PriceVolumeCurve SAMPLE_BIDS = PriceVolumeCurve.of(levels(
            "100.2:0.8", "100:1.2", "99.8:2.3", "99.1:2", "99:2", "98.1:2", "98:2"));

    private static BigDecimal v(String text) {
        return new BigDecimal(text);
    }

    /** K1. 0.8 &lt; 1 and 0.8 + 1.1 &gt;= 1, so one unit of volume reaches the second ask level. */
    @Test
    void oneUnitWithFractionalSizesReachesThe100_4Ask() {
        assertThat(SAMPLE_ASKS.unspaced(v("1"))).hasValueSatisfying(p -> assertThat(p).isEqualByComparingTo("100.4"));
        assertThat(SAMPLE_ASKS.priceAt(v("1"))).hasValueSatisfying(p -> assertThat(p).isEqualByComparingTo("100.4"));
        assertThat(SAMPLE_BIDS.priceAt(v("1"))).hasValueSatisfying(p -> assertThat(p).isEqualByComparingTo("100.0"));
    }

    /** K2. Eq. 1a: the cumulative sum must be &gt;= v, so a level that ends exactly at v is the one reached. */
    @Test
    void aCumulativeSizeExactlyEqualToVisReached() {
        // bids: 0.8, then 0.8 + 1.2 = 2.0 exactly
        assertThat(SAMPLE_BIDS.priceAt(v("2"))).hasValueSatisfying(p -> assertThat(p).isEqualByComparingTo("100"));
        assertThat(SAMPLE_BIDS.unspaced(v("2.0"))).hasValueSatisfying(p -> assertThat(p).isEqualByComparingTo("100"));
        // one step beyond that sum moves to the next level
        assertThat(SAMPLE_BIDS.unspaced(v("2.01"))).hasValueSatisfying(p -> assertThat(p).isEqualByComparingTo("99.8"));
        // the last level ends exactly at the total depth
        assertThat(PriceVolumeCurve.of(levels("100:1", "99:2")).unspaced(v("3")))
                .hasValueSatisfying(p -> assertThat(p).isEqualByComparingTo("99"));
    }

    /** K3. Eq. 1c/1d with s = 1: v is rounded up to a whole unit before the lookup. */
    @Test
    void theSpacedCurveRoundsVolumeUpToAWholeUnit() {
        BigDecimal atOne = SAMPLE_ASKS.priceAt(v("1")).orElseThrow();
        assertThat(SAMPLE_ASKS.priceAt(v("0.5")).orElseThrow()).isEqualByComparingTo(atOne);
        assertThat(SAMPLE_ASKS.priceAt(v("0.0001")).orElseThrow()).isEqualByComparingTo(atOne);
        assertThat(SAMPLE_ASKS.priceAt(v("1.0")).orElseThrow()).isEqualByComparingTo(atOne);
        // 1.01 rounds up to 2, the unspaced curve at 1.01 is still the 100.4 level
        assertThat(SAMPLE_ASKS.priceAt(v("1.01")).orElseThrow()).isEqualByComparingTo("100.6");
        assertThat(SAMPLE_ASKS.unspaced(v("1.01")).orElseThrow()).isEqualByComparingTo("100.4");
    }

    /** K4. A curve is undefined beyond the book's depth; the spaced one is undefined as soon as v rounds past it. */
    @Test
    void aCurveIsUndefinedBeyondTheDepth() {
        assertThat(SAMPLE_ASKS.depth()).isEqualByComparingTo("12.3");
        assertThat(SAMPLE_ASKS.wholeUnits()).isEqualTo(12);
        assertThat(SAMPLE_ASKS.priceAt(v("12"))).isPresent();
        assertThat(SAMPLE_ASKS.unspaced(v("12.3"))).isPresent();
        assertThat(SAMPLE_ASKS.unspaced(v("12.31"))).isEmpty();
        // 12.1 rounds up to 13, past the depth of 12.3
        assertThat(SAMPLE_ASKS.priceAt(v("12.1"))).isEmpty();
        assertThat(SAMPLE_ASKS.priceAt(v("13"))).isEmpty();
    }

    /** K5. Eq. 1a needs v &gt; 0 (an empty sum is zero and is never &gt;= v), and a curve needs at least one level. */
    @Test
    void invalidArgumentsAreRejected() {
        assertThatThrownBy(() -> SAMPLE_ASKS.unspaced(v("0"))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> SAMPLE_ASKS.priceAt(v("-1"))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> PriceVolumeCurve.of(List.of())).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> PriceVolumeCurve.of(levels("100:0"))).isInstanceOf(IllegalArgumentException.class);
    }

    /** Sizes are added as decimals, so 0.1 + 0.2 reaches v = 0.3 and not the next level (a double sum is 0.30000000000000004). */
    @Test
    void cumulativeSizesAreExactDecimals() {
        PriceVolumeCurve curve = PriceVolumeCurve.of(levels("100:0.1", "101:0.2", "102:1"));

        assertThat(curve.unspaced(v("0.3")).orElseThrow()).isEqualByComparingTo("101");
    }
}
