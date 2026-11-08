package com.cfbenchmarks.obsi.book;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;
import java.util.List;

import static com.cfbenchmarks.obsi.TestBooks.lv;
import static com.cfbenchmarks.obsi.TestBooks.valid;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ValidBookTest {

    /** M1. Spec §2 / 2.9: m = (best bid + best ask) / 2, exact (D-14), compared with compareTo. */
    @ParameterizedTest(name = "({0} + {1}) / 2 = {2}")
    @CsvSource({"100.1,100.2,100.15", "0.01,0.02,0.015", "100,101,100.5", "99.95,100.05,100",
            "1,2,1.5", "104.900,105.100,105", "0.0000001,0.0000002,0.00000015"})
    void midIsTheExactMeanOfBestBidAndBestAsk(String bid, String ask, String expected) {
        BigDecimal mid = valid("A", bid, ask).mid();

        assertThat(mid).isEqualByComparingTo(new BigDecimal(expected));
    }

    /** §2: a structurally valid book has 0 < best bid < best ask. */
    @Test
    void rejectsCrossedEqualAndNonPositiveBests() {
        List<com.cfbenchmarks.obsi.input.Level> side = List.of(lv("1", "1"));

        assertThatThrownBy(() -> new ValidBook("A", side, side, new BigDecimal("100"), new BigDecimal("100")))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ValidBook("A", side, side, new BigDecimal("101"), new BigDecimal("100")))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ValidBook("A", side, side, BigDecimal.ZERO, new BigDecimal("100")))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
