package com.cfbenchmarks.obsi.screening;

import com.cfbenchmarks.obsi.TestBooks;
import com.cfbenchmarks.obsi.deviation.StaticPedFilter;
import com.cfbenchmarks.obsi.screening.ScreeningResult.Screened;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static com.cfbenchmarks.obsi.screening.ScreenerTestSupport.screened;
import static com.cfbenchmarks.obsi.screening.ScreenerTestSupport.survivorNames;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * The screening step against the reference fixtures. The excluded exchanges must match the
 * {@code excludedExchanges} of the matching file in {@code src/test/resources/expected/}.
 */
class ScreenerFixturesTest {

    /** O3. Gamma is crossed (bid 100.6 above ask 100.5); Delta (mid 150 against a median of 100.25) fails PED. */
    @Test
    void sampleInputExcludesGammaAndDelta() throws IOException {
        Screened s = screened(Screener.screen(TestBooks.fixture("sample-input.json"), new StaticPedFilter()));

        assertThat(survivorNames(s)).containsExactly("Alpha", "Beta");
        assertThat(s.excludedExchanges()).containsExactly("Gamma", "Delta");
    }

    /**
     * O4. Echo is crossed; Delta (mid 110) fails PED against a median of 100. Charlie (mid 105) sits at exactly
     * +5% and passes (§2: a book at exactly 5% passes). Foxtrot's string bid and negative-size bid are dropped.
     */
    @Test
    void edgeCasesExcludesDeltaAndEchoAndKeepsCharlieAtExactlyFivePercent() throws IOException {
        Screened s = screened(Screener.screen(TestBooks.fixture("edge-cases.json"), new StaticPedFilter()));

        assertThat(survivorNames(s)).containsExactly("Alpha", "Bravo", "Charlie", "Foxtrot");
        assertThat(s.excludedExchanges()).containsExactly("Delta", "Echo");
        assertThat(s.survivors().get(3).bestBid()).isEqualByComparingTo("99.85");
    }
}
