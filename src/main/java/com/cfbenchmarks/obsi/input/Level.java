package com.cfbenchmarks.obsi.input;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * One {@code [price, size]} entry as read from the input. Price is USD per XYZ, size is XYZ
 * (spec §1). Values are not screened here: zero and negative numbers are kept, because the value
 * rules are §2 and belong to the screening phase.
 */
public record Level(BigDecimal price, BigDecimal size) {

    public Level {
        Objects.requireNonNull(price, "price");
        Objects.requireNonNull(size, "size");
    }
}
