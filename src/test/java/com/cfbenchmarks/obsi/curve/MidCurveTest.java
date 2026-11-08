package com.cfbenchmarks.obsi.curve;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/** §4, Eq. 1e (midPV) and Eq. 1f (midSV &lt;= D, checked without division). */
class MidCurveTest {

    private static BigDecimal v(String text) {
        return new BigDecimal(text);
    }

    @Test
    void midPriceIsTheExactAverage() {
        assertThat(MidCurve.midPV(v("100.4"), v("100.0"))).isEqualByComparingTo("100.2");
        assertThat(MidCurve.midPV(v("100.01"), v("100.00"))).isEqualByComparingTo("100.005");
    }

    /** K6. midSV exactly D counts as within: ask 100.5 over mid 100 is 1.005 = 1 + D. */
    @Test
    void midSpreadExactlyAtTheCutoffIsWithin() {
        BigDecimal mid = MidCurve.midPV(v("100.5"), v("99.5"));

        assertThat(mid).isEqualByComparingTo("100");
        assertThat(MidCurve.midSvWithinCutoff(v("100.5"), mid)).isTrue();
        // the same boundary at another size and with other scales
        assertThat(MidCurve.midSvWithinCutoff(v("201"), MidCurve.midPV(v("201.00"), v("199")))).isTrue();
        assertThat(MidCurve.midSvWithinCutoff(v("50.25"), MidCurve.midPV(v("50.250"), v("49.75")))).isTrue();
    }

    @Test
    void midSpreadJustAboveTheCutoffIsNotWithin() {
        assertThat(MidCurve.midSvWithinCutoff(v("100.5001"), MidCurve.midPV(v("100.5001"), v("99.5")))).isFalse();
        assertThat(MidCurve.midSvWithinCutoff(v("201.0001"), MidCurve.midPV(v("201.0001"), v("199")))).isFalse();
    }

    @Test
    void midSpreadWellInsideAndOutside() {
        assertThat(MidCurve.midSvWithinCutoff(v("100.4"), v("100.2"))).isTrue();
        assertThat(MidCurve.midSvWithinCutoff(v("101.1"), v("100.1"))).isFalse();
    }
}
