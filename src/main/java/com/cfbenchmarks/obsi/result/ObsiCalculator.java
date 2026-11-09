package com.cfbenchmarks.obsi.result;

import com.cfbenchmarks.obsi.book.ConsolidationResult;
import com.cfbenchmarks.obsi.book.Consolidator;
import com.cfbenchmarks.obsi.deviation.DeviationFilter;
import com.cfbenchmarks.obsi.index.IndexResult;
import com.cfbenchmarks.obsi.index.ObsiIndex;
import com.cfbenchmarks.obsi.input.Snapshot;
import com.cfbenchmarks.obsi.screening.Screener;
import com.cfbenchmarks.obsi.screening.ScreeningResult;

/**
 * The calculation chain for one snapshot: screens (§2), consolidation and uncrossing (§3), curves, utilized depth
 * and index value (§4, §5). A stage that fails ends the calculation (§6: no index value is published for a failed
 * snapshot) and its reason is passed through unchanged.
 *
 * <p>The deviation filter is a parameter, so the original 5% rule and the Task 2 rule use the same chain.
 */
public final class ObsiCalculator {

    private ObsiCalculator() {
    }

    public static CalculationResult calculate(Snapshot snapshot, DeviationFilter deviationFilter) {
        String time = snapshot.calculationTime();

        // §2: entry and structural screens, then the deviation screen.
        ScreeningResult screening = Screener.screen(snapshot, deviationFilter);
        if (screening instanceof ScreeningResult.Failed failed) {
            return new CalculationResult.Failed(time, failed.reason());
        }
        ScreeningResult.Screened screened = (ScreeningResult.Screened) screening;

        // §3: consolidation and uncrossing.
        ConsolidationResult consolidation = Consolidator.consolidate(screened.survivors());
        if (consolidation instanceof ConsolidationResult.Failed failed) {
            return new CalculationResult.Failed(time, failed.reason());
        }
        ConsolidationResult.Consolidated book = (ConsolidationResult.Consolidated) consolidation;

        // §4, §5: curves, utilized depth, index value.
        IndexResult index = ObsiIndex.calculate(book);
        if (index instanceof IndexResult.Failed failed) {
            return new CalculationResult.Failed(time, failed.reason());
        }
        IndexResult.Published published = (IndexResult.Published) index;
        return new CalculationResult.Published(
                time, published.value(), published.utilizedDepth(), screened.excludedExchanges());
    }
}
