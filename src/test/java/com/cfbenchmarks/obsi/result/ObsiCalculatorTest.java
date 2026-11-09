package com.cfbenchmarks.obsi.result;

import com.cfbenchmarks.obsi.TestBooks;
import com.cfbenchmarks.obsi.deviation.DeviationFilter;
import com.cfbenchmarks.obsi.deviation.StaticPedFilter;
import com.cfbenchmarks.obsi.input.Snapshot;
import com.cfbenchmarks.obsi.result.CalculationResult.Failed;
import com.cfbenchmarks.obsi.result.CalculationResult.Published;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

import static com.cfbenchmarks.obsi.TestBooks.book;
import static com.cfbenchmarks.obsi.TestBooks.lv;
import static com.cfbenchmarks.obsi.TestBooks.snapshot;
import static org.assertj.core.api.Assertions.assertThat;

/** The calculation chain: screen (§2), consolidate (§3), index (§4/§5), each stage's failure passed through. */
class ObsiCalculatorTest {

    private static Published published(CalculationResult result) {
        if (!(result instanceof Published p)) {
            throw new AssertionError("expected Published but was " + result);
        }
        return p;
    }

    private static Failed failed(CalculationResult result) {
        if (!(result instanceof Failed f)) {
            throw new AssertionError("expected Failed but was " + result);
        }
        return f;
    }

    @Test
    void theSamplePublishes100_23() throws IOException {
        Published p = published(ObsiCalculator.calculate(TestBooks.fixture("sample-input.json"), new StaticPedFilter()));

        assertThat(p.calculationTime()).isEqualTo("2026-10-27T12:00:00Z");
        assertThat(p.value().toPlainString()).isEqualTo("100.23");
        assertThat(p.utilizedDepth().toPlainString()).isEqualTo("4");
        assertThat(p.excludedExchanges()).containsExactly("Gamma", "Delta");
    }

    @Test
    void theEdgeCasesFixturePublishes100_03() throws IOException {
        Published p = published(ObsiCalculator.calculate(TestBooks.fixture("edge-cases.json"), new StaticPedFilter()));

        assertThat(p.calculationTime()).isEqualTo("2026-11-07T12:00:00Z");
        assertThat(p.value().toPlainString()).isEqualTo("100.03");
        assertThat(p.utilizedDepth().toPlainString()).isEqualTo("6");
        assertThat(p.excludedExchanges()).containsExactly("Delta", "Echo");
    }

    /** §6: a stage that fails ends the calculation with its own reason, and the snapshot's calculationTime. */
    @Test
    void aFailureCarriesTheReasonOfTheStageThatFailedAndTheSnapshotsTime() {
        Snapshot crossed = snapshot(book("A", "101", "100"));
        Snapshot thin = snapshot(book("A", List.of(lv("100", "1.99")), List.of(lv("100.1", "5"))));
        Snapshot tight = snapshot(book("A", List.of(lv("100", "5")), List.of(lv("100.1", "5"))));

        Failed noSurvivor = failed(ObsiCalculator.calculate(crossed, new StaticPedFilter()));
        Failed insufficient = failed(ObsiCalculator.calculate(thin, new StaticPedFilter()));
        Failed noCrossing = failed(ObsiCalculator.calculate(tight, new StaticPedFilter()));

        assertThat(noSurvivor.reason()).isEqualTo("no constituent book survived screening");
        assertThat(insufficient.reason()).isEqualTo("empty Eq. 2 set: insufficient depth to evaluate consecutive samples");
        assertThat(noCrossing.reason()).isEqualTo("empty Eq. 2 set: no mid-spread crossing before a curve ends");
        assertThat(List.of(noSurvivor, insufficient, noCrossing)).extracting(Failed::calculationTime)
                .containsOnly(TestBooks.TIME);
    }

    /** The deviation rule is a parameter (D-10, Task 2): a filter that excludes everything gives the PED reason. */
    @Test
    void theDeviationFilterIsWhateverTheCallerPasses() throws IOException {
        DeviationFilter excludeAll = (books, snapshot) -> books;

        Failed f = failed(ObsiCalculator.calculate(TestBooks.fixture("sample-input.json"), excludeAll));

        assertThat(f.reason())
                .isEqualTo("no constituent book survived screening (every structurally valid book was excluded by PED)");
        assertThat(f.calculationTime()).isEqualTo("2026-10-27T12:00:00Z");
    }
}
