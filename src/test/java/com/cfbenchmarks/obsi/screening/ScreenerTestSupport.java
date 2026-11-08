package com.cfbenchmarks.obsi.screening;

import com.cfbenchmarks.obsi.book.ValidBook;
import com.cfbenchmarks.obsi.deviation.DeviationFilter;
import com.cfbenchmarks.obsi.input.Snapshot;
import com.cfbenchmarks.obsi.screening.ScreeningResult.Failed;
import com.cfbenchmarks.obsi.screening.ScreeningResult.Screened;

import java.util.List;

/** Shared helpers for the screener tests. */
final class ScreenerTestSupport {

    /** A deviation filter that excludes nothing, so a test sees only the entry and structural screens. */
    static final DeviationFilter NO_DEVIATION_SCREEN = (books, snapshot) -> List.of();

    static final String PLAIN_REASON = "no constituent book survived screening";
    static final String PED_REASON =
            "no constituent book survived screening (every structurally valid book was excluded by PED)";

    private ScreenerTestSupport() {
    }

    static Screened screened(ScreeningResult result) {
        if (!(result instanceof Screened s)) {
            throw new AssertionError("expected Screened but was " + result);
        }
        return s;
    }

    static String failureReason(ScreeningResult result) {
        if (!(result instanceof Failed f)) {
            throw new AssertionError("expected Failed but was " + result);
        }
        return f.reason();
    }

    static Screened screenWithoutDeviation(Snapshot snapshot) {
        return screened(Screener.screen(snapshot, NO_DEVIATION_SCREEN));
    }

    static List<String> survivorNames(Screened screened) {
        return screened.survivors().stream().map(ValidBook::exchange).toList();
    }
}
