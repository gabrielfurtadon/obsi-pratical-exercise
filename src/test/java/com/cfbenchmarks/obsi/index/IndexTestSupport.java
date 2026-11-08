package com.cfbenchmarks.obsi.index;

import com.cfbenchmarks.obsi.TestBooks;
import com.cfbenchmarks.obsi.book.ConsolidationResult;
import com.cfbenchmarks.obsi.book.ConsolidationResult.Consolidated;
import com.cfbenchmarks.obsi.book.Consolidator;
import com.cfbenchmarks.obsi.curve.MidCurve;
import com.cfbenchmarks.obsi.curve.PriceVolumeCurve;
import com.cfbenchmarks.obsi.deviation.StaticPedFilter;
import com.cfbenchmarks.obsi.index.IndexResult.Failed;
import com.cfbenchmarks.obsi.index.IndexResult.Published;
import com.cfbenchmarks.obsi.input.Level;
import com.cfbenchmarks.obsi.screening.Screener;
import com.cfbenchmarks.obsi.screening.ScreeningResult;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.MathContext;
import java.util.List;
import java.util.OptionalLong;

/** Shared helpers for the §4/§5 tests. */
final class IndexTestSupport {

    private IndexTestSupport() {
    }

    /** A fixture taken through the real pipeline: parse, screen (§2), consolidate (§3). */
    static Consolidated consolidatedFixture(String fileName) throws IOException {
        ScreeningResult screened = Screener.screen(TestBooks.fixture(fileName), new StaticPedFilter());
        ConsolidationResult result = Consolidator.consolidate(((ScreeningResult.Screened) screened).survivors());
        return (Consolidated) result;
    }

    static Consolidated book(List<Level> bids, List<Level> asks) {
        return new Consolidated(bids, asks);
    }

    static Published published(IndexResult result) {
        if (!(result instanceof Published p)) {
            throw new AssertionError("expected Published but was " + result);
        }
        return p;
    }

    static String failureReason(IndexResult result) {
        if (!(result instanceof Failed f)) {
            throw new AssertionError("expected Failed but was " + result);
        }
        return f.reason();
    }

    /**
     * Eq. 2 written out literally, as an oracle for the early-stopping scan: the maximum, over all i for which both
     * curves are defined at v_i and v_{i+1}, of the v_i with midSV(v_i) &lt;= D and midSV(v_{i+1}) &gt; D. Unlike the
     * code under test it walks every sample, and it computes midSV = askPV / midPV - 1 with a real division
     * (60 digits) instead of the division-free comparison.
     */
    static OptionalLong literalEquation2(PriceVolumeCurve bids, PriceVolumeCurve asks) {
        long samples = Math.min(bids.wholeUnits(), asks.wholeUnits());
        OptionalLong max = OptionalLong.empty();
        for (long i = 1; i + 1 <= samples; i++) {
            if (midSv(bids, asks, i).compareTo(MidCurve.CUTOFF) <= 0
                    && midSv(bids, asks, i + 1).compareTo(MidCurve.CUTOFF) > 0) {
                max = OptionalLong.of(max.isPresent() ? Math.max(max.getAsLong(), i) : i);
            }
        }
        return max;
    }

    static BigDecimal midSv(PriceVolumeCurve bids, PriceVolumeCurve asks, long v) {
        BigDecimal ask = asks.priceAt(BigDecimal.valueOf(v)).orElseThrow();
        BigDecimal bid = bids.priceAt(BigDecimal.valueOf(v)).orElseThrow();
        BigDecimal mid = ask.add(bid).divide(BigDecimal.valueOf(2));
        return ask.divide(mid, new MathContext(60)).subtract(BigDecimal.ONE);
    }
}
