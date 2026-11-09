package com.cfbenchmarks.obsi.deviation;

import com.cfbenchmarks.obsi.book.ValidBook;
import com.cfbenchmarks.obsi.input.Snapshot;

import java.math.BigDecimal;
import java.math.MathContext;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * The Task 2 deviation rule: a liquidity-adaptive PED with an unlock regime (proposal.md, "Approach 2: a spread-based
 * limit with an unlock regime"). For every snapshot it takes the relative spread {@code (best ask - best bid) / mid}
 * of each structurally valid book, the median of those spreads {@code S}, and the limit {@code k × S} clamped between
 * a floor and a cap. If {@code calculationTime} is within 48 hours of the start of an announced unlock, the unlock
 * band's floor and cap apply. A book is excluded when {@code |m - median| > limit × median}, the same comparison as the
 * 5% rule (spec §2) but with this limit; it needs no division.
 *
 * <p>The limit is never rounded (D-24). The only division is the spread, at {@code MathContext.DECIMAL128}. The check
 * is stateless: only this snapshot's books and its {@code unlockEvents} are used (spec §2, "each snapshot is evaluated
 * independently"), and there is no reentry or half-limit test (proposal.md, "Risks and limitations").
 */
public final class SpreadBasedPedFilter implements DeviationFilter {

    // The illustrative parameters of proposal.md, Approach 2. In production they would be calibrated and approved by
    // the committee (proposal.md, "Risks and limitations").
    /** proposal.md, Approach 2: {@code k = 10}, the multiple of the spread that stands for the rest of the arbitrage cost. */
    static final BigDecimal K = BigDecimal.TEN;
    /** proposal.md, Approach 2: normal regime, floor 1%. */
    static final BigDecimal NORMAL_FLOOR = new BigDecimal("0.01");
    /** proposal.md, Approach 2: normal regime, cap 5%, today's static limit (spec §7). */
    static final BigDecimal NORMAL_CAP = new BigDecimal("0.05");
    /** proposal.md, Approach 2: unlock regime, floor 0.5%. */
    static final BigDecimal UNLOCK_FLOOR = new BigDecimal("0.005");
    /** proposal.md, Approach 2: unlock regime, cap 1%. */
    static final BigDecimal UNLOCK_CAP = new BigDecimal("0.01");
    /** proposal.md, "Input change": the window runs from {@code start} (inclusive) to {@code start + 48h} (exclusive). */
    static final Duration UNLOCK_WINDOW = Duration.ofHours(48);

    @Override
    public List<ValidBook> excluded(List<ValidBook> structurallyValid, Snapshot snapshot) {
        if (structurallyValid.isEmpty()) {
            return List.of();
        }
        boolean inUnlock = inUnlockWindow(Instant.parse(snapshot.calculationTime()), snapshot.unlockStarts());
        BigDecimal limit = limit(structurallyValid, inUnlock);
        // §2: the median of the structurally valid mids, as in the 5% rule.
        BigDecimal median = StaticPedFilter.median(structurallyValid.stream().map(ValidBook::mid).toList());

        List<ValidBook> excluded = new ArrayList<>();
        for (ValidBook book : structurallyValid) {
            if (deviatesMoreThanTheLimit(book.mid(), median, limit)) {
                excluded.add(book);
            }
        }
        return excluded;
    }

    /**
     * proposal.md, Approach 2: exclude when {@code |m - median| / median > limit}, written without a division
     * (CLAUDE.md "Numbers"): {@code |m - median| > limit × median}. The median is a mean of positive prices, so it is
     * positive and multiplying by it keeps the direction of the inequality. Exactly on the limit passes, as in §2.
     */
    private static boolean deviatesMoreThanTheLimit(BigDecimal mid, BigDecimal median, BigDecimal limit) {
        return mid.subtract(median).abs().compareTo(limit.multiply(median)) > 0;
    }

    /**
     * proposal.md, Approach 2: {@code limit = k × S}, clamped to {@code [floor, cap]} of the regime. A fraction, not a
     * percentage, and not rounded (D-24).
     *
     * @param structurallyValid at least one book
     * @param inUnlock          whether the unlock band applies instead of the normal band
     */
    static BigDecimal limit(List<ValidBook> structurallyValid, boolean inUnlock) {
        BigDecimal floor = inUnlock ? UNLOCK_FLOOR : NORMAL_FLOOR;
        BigDecimal cap = inUnlock ? UNLOCK_CAP : NORMAL_CAP;
        BigDecimal limit = K.multiply(medianSpread(structurallyValid));
        if (limit.compareTo(floor) < 0) {
            return floor;
        }
        return limit.compareTo(cap) > 0 ? cap : limit;
    }

    /**
     * proposal.md, Approach 2: {@code S}, the median of the relative spreads. An even count takes the mean of the middle
     * two, as the median of the mids does (D-24). The median spread, unlike the mean, cannot be widened by one exchange's
     * own spread.
     */
    static BigDecimal medianSpread(List<ValidBook> structurallyValid) {
        return StaticPedFilter.median(structurallyValid.stream().map(SpreadBasedPedFilter::spread).toList());
    }

    /**
     * proposal.md, Approach 2: {@code (best ask - best bid) / mid}. The only division of this rule. Mid is positive
     * (a valid book has 0 < best bid < best ask), so it is never a division by zero. D-24: DECIMAL128.
     */
    static BigDecimal spread(ValidBook book) {
        return book.bestAsk().subtract(book.bestBid()).divide(book.mid(), MathContext.DECIMAL128);
    }

    /**
     * proposal.md, "Input change": in the window when {@code start <= T < start + 48h} for any announced unlock. Written as
     * {@code T - start < 48h} so it cannot overflow near the end of the {@link Instant} range (D-24).
     */
    private static boolean inUnlockWindow(Instant calculationTime, List<Instant> unlockStarts) {
        for (Instant start : unlockStarts) {
            if (!calculationTime.isBefore(start)
                    && Duration.between(start, calculationTime).compareTo(UNLOCK_WINDOW) < 0) {
                return true;
            }
        }
        return false;
    }
}
