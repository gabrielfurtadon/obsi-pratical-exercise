package com.cfbenchmarks.obsi.curve;

import com.cfbenchmarks.obsi.input.Level;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/** Property-style tests for the curves: inputs come from a {@link Random} with a fixed seed. */
class PriceVolumeCurvePropertyTest {

    private static final long SEED = 20261109L;

    /** A side with sizes in hundredths, and the same side expanded into one price per hundredth of volume. */
    record Side(List<Level> levels, List<BigDecimal> hundredths) {
    }

    static Stream<Side> randomSides() {
        Random random = new Random(SEED);
        List<Side> cases = new ArrayList<>();
        for (int i = 0; i < 300; i++) {
            List<Level> levels = new ArrayList<>();
            List<BigDecimal> hundredths = new ArrayList<>();
            for (int n = 1 + random.nextInt(6); n > 0; n--) {
                BigDecimal price = BigDecimal.valueOf(10000 + levels.size() * 7 + random.nextInt(7), 2);
                int sizeHundredths = 1 + random.nextInt(400);
                levels.add(new Level(price, BigDecimal.valueOf(sizeHundredths, 2)));
                for (int k = 0; k < sizeHundredths; k++) {
                    hundredths.add(price);
                }
            }
            cases.add(new Side(levels, hundredths));
        }
        return cases.stream();
    }

    /**
     * Oracle for Eq. 1a/1b that shares no code with the curve: volume v (in hundredths) falls in the hundredth
     * numbered ceil(100 v), and that hundredth belongs to the level whose price is the answer.
     */
    @ParameterizedTest
    @MethodSource("randomSides")
    void theUnspacedCurveMatchesAUnitByUnitExpansion(Side side) {
        PriceVolumeCurve curve = PriceVolumeCurve.of(side.levels());
        int total = side.hundredths().size();

        assertThat(curve.depth()).isEqualByComparingTo(BigDecimal.valueOf(total, 2));
        for (int h = 1; h <= total + 2; h++) {
            Optional<BigDecimal> actual = curve.unspaced(BigDecimal.valueOf(h, 2));
            if (h <= total) {
                assertThat(actual).as("v = %d/100", h).isPresent();
                assertThat(actual.orElseThrow()).isEqualByComparingTo(side.hundredths().get(h - 1));
            } else {
                assertThat(actual).as("v = %d/100 is beyond the depth", h).isEmpty();
            }
        }
    }

    /** Eq. 1c/1d: the spaced curve at v is the unspaced curve at the next whole unit. */
    @ParameterizedTest
    @MethodSource("randomSides")
    void theSpacedCurveIsTheUnspacedCurveAtTheNextWholeUnit(Side side) {
        PriceVolumeCurve curve = PriceVolumeCurve.of(side.levels());
        int total = side.hundredths().size();

        for (int h = 1; h <= total + 150; h += 3) {
            BigDecimal v = BigDecimal.valueOf(h, 2);
            BigDecimal nextWhole = BigDecimal.valueOf((h + 99) / 100);
            assertThat(curve.priceAt(v)).as("v = %d/100", h).isEqualTo(curve.unspaced(nextWhole));
        }
    }
}
