package com.cfbenchmarks.obsi.book;

import com.cfbenchmarks.obsi.book.ConsolidationResult.Consolidated;
import com.cfbenchmarks.obsi.input.Level;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.stream.Stream;

import static com.cfbenchmarks.obsi.TestBooks.validBook;
import static com.cfbenchmarks.obsi.book.ConsolidatorTest.consolidated;
import static com.cfbenchmarks.obsi.book.ConsolidatorTest.show;
import static org.assertj.core.api.Assertions.assertThat;

/** Property-style tests for §3: inputs come from a {@link Random} with a fixed seed. */
class ConsolidatorPropertyTest {

    private static final long SEED = 20261108L;
    private static final int CASES = 400;

    /**
     * A structurally valid book around a mid between 99 and 101: its bids are at or below a random best bid, its
     * asks above it, so books overlap and cross each other often. Prices and sizes have scale 1 or 2.
     */
    private static ValidBook randomBook(Random random, String name) {
        long bestBidCents = 9900 + random.nextInt(201);
        List<Level> bids = new ArrayList<>();
        List<Level> asks = new ArrayList<>();
        for (int i = 1 + random.nextInt(4); i > 0; i--) {
            bids.add(new Level(price(bestBidCents - random.nextInt(60), random), size(random)));
        }
        bids.add(new Level(price(bestBidCents, random), size(random)));
        for (int i = 1 + random.nextInt(4); i > 0; i--) {
            asks.add(new Level(price(bestBidCents + 1 + random.nextInt(60), random), size(random)));
        }
        return validBook(name, bids, asks);
    }

    private static BigDecimal price(long cents, Random random) {
        BigDecimal p = BigDecimal.valueOf(cents, 2);
        return cents % 10 == 0 && random.nextBoolean() ? p.setScale(1) : p;
    }

    private static BigDecimal size(Random random) {
        BigDecimal s = BigDecimal.valueOf(1 + random.nextInt(300), 2);
        return random.nextBoolean() ? s : s.setScale(3);
    }

    static Stream<List<ValidBook>> randomSnapshots() {
        Random random = new Random(SEED);
        List<List<ValidBook>> cases = new ArrayList<>();
        for (int i = 0; i < CASES; i++) {
            List<ValidBook> books = new ArrayList<>();
            for (int n = 1 + random.nextInt(5); n > 0; n--) {
                books.add(randomBook(random, "E" + n));
            }
            cases.add(books);
        }
        return cases.stream();
    }

    private static BigDecimal total(List<Level> levels) {
        return levels.stream().map(Level::size).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static BigDecimal totalOf(List<ValidBook> books, boolean bids) {
        return books.stream().map(b -> total(bids ? b.bids() : b.asks())).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Valid books never exhaust a side (D-20); the result is strictly ordered, uncrossed, with positive sizes, and
     * the same volume came off both sides.
     */
    @ParameterizedTest
    @MethodSource("randomSnapshots")
    void theResultIsOrderedUncrossedAndTheRemovedVolumeIsEqualOnBothSides(List<ValidBook> books) {
        Consolidated book = consolidated(Consolidator.consolidate(books));

        assertThat(book.bids()).isNotEmpty();
        assertThat(book.asks()).isNotEmpty();
        for (int i = 0; i < book.bids().size(); i++) {
            assertThat(book.bids().get(i).size().signum()).isPositive();
            if (i > 0) {
                assertThat(book.bids().get(i - 1).price()).isGreaterThan(book.bids().get(i).price());
            }
        }
        for (int i = 0; i < book.asks().size(); i++) {
            assertThat(book.asks().get(i).size().signum()).isPositive();
            if (i > 0) {
                assertThat(book.asks().get(i - 1).price()).isLessThan(book.asks().get(i).price());
            }
        }
        assertThat(book.bids().get(0).price()).isLessThan(book.asks().get(0).price());

        BigDecimal removedBids = totalOf(books, true).subtract(total(book.bids()));
        BigDecimal removedAsks = totalOf(books, false).subtract(total(book.asks()));
        assertThat(removedBids).isEqualByComparingTo(removedAsks);
    }

    /** The result does not depend on the order of the books or of the levels within them. */
    @ParameterizedTest
    @MethodSource("randomSnapshots")
    void theResultDoesNotDependOnInputOrder(List<ValidBook> books) {
        Random random = new Random(books.hashCode());
        List<ValidBook> shuffled = new ArrayList<>();
        for (ValidBook b : books) {
            List<Level> bids = new ArrayList<>(b.bids());
            List<Level> asks = new ArrayList<>(b.asks());
            Collections.shuffle(bids, random);
            Collections.shuffle(asks, random);
            shuffled.add(new ValidBook(b.exchange(), bids, asks, b.bestBid(), b.bestAsk()));
        }
        Collections.shuffle(shuffled, random);

        Consolidated expected = consolidated(Consolidator.consolidate(books));
        Consolidated actual = consolidated(Consolidator.consolidate(shuffled));

        assertThat(show(actual.bids())).isEqualTo(show(expected.bids()));
        assertThat(show(actual.asks())).isEqualTo(show(expected.asks()));
    }

    /** Guards the property tests against passing vacuously: many of the generated snapshots do cross. */
    @Test
    void manyGeneratedSnapshotsActuallyCross() {
        long crossing = randomSnapshots().filter(books -> {
            Consolidated book = consolidated(Consolidator.consolidate(books));
            return totalOf(books, true).compareTo(total(book.bids())) != 0;
        }).count();

        assertThat(crossing).isGreaterThan(CASES / 4);
    }
}
