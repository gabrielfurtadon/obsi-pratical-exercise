package com.cfbenchmarks.obsi.index;

import com.cfbenchmarks.obsi.index.IndexResult.Published;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** What {@link Published} promises about the two figures the CLI prints. */
class IndexResultTest {

    @Test
    void aWellFormedResultIsAccepted() {
        Published p = new Published(new BigDecimal("100.23"), new BigDecimal("4"));

        assertThat(p.value().toPlainString()).isEqualTo("100.23");
        assertThat(p.utilizedDepth().toPlainString()).isEqualTo("4");
    }

    @Test
    void aValueThatIsNotAtTwoDecimalsIsRejected() {
        assertThatThrownBy(() -> new Published(new BigDecimal("100.2"), BigDecimal.ONE))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Published(new BigDecimal("100.230"), BigDecimal.ONE))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Published(new BigDecimal("100"), BigDecimal.ONE))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void aDepthThatIsNotAPositiveWholeNumberWithScaleZeroIsRejected() {
        assertThatThrownBy(() -> new Published(new BigDecimal("100.00"), new BigDecimal("4.0")))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Published(new BigDecimal("100.00"), new BigDecimal("1E+2")))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Published(new BigDecimal("100.00"), BigDecimal.ZERO))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Published(new BigDecimal("100.00"), new BigDecimal("-1")))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
