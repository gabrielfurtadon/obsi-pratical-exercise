package com.cfbenchmarks.obsi.screening;

import com.cfbenchmarks.obsi.deviation.StaticPedFilter;
import com.cfbenchmarks.obsi.input.BookInput;
import com.cfbenchmarks.obsi.input.BookInput.ReadableBook;
import com.cfbenchmarks.obsi.input.Level;
import com.cfbenchmarks.obsi.input.Snapshot;
import com.cfbenchmarks.obsi.screening.ScreeningResult.Failed;
import com.cfbenchmarks.obsi.screening.ScreeningResult.Screened;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.stream.Stream;

import static com.cfbenchmarks.obsi.TestBooks.bookAtMid;
import static com.cfbenchmarks.obsi.TestBooks.crossedAtMid;
import static com.cfbenchmarks.obsi.TestBooks.snapshot;
import static com.cfbenchmarks.obsi.TestBooks.unreadable;
import static com.cfbenchmarks.obsi.screening.ScreenerTestSupport.NO_DEVIATION_SCREEN;
import static com.cfbenchmarks.obsi.screening.ScreenerTestSupport.screened;
import static org.assertj.core.api.Assertions.assertThat;

/** Property-style tests: inputs come from a {@link Random} with a fixed seed. */
class ScreenerPropertyTest {

    private static final long SEED = 20261110L;

    // ---------------------------------------------------------------- R4: the entry screen

    private static Level randomLevel(Random random) {
        // prices and sizes from -3.00 to 6.00 in cents, so about a third of the components are not positive
        BigDecimal price = BigDecimal.valueOf(random.nextInt(901) - 300, 2);
        BigDecimal size = BigDecimal.valueOf(random.nextInt(901) - 300, 2);
        return new Level(price, size);
    }

    static Stream<List<List<Level>>> randomSides() {
        Random random = new Random(SEED);
        List<List<List<Level>>> cases = new ArrayList<>();
        for (int i = 0; i < 300; i++) {
            List<Level> bids = new ArrayList<>();
            List<Level> asks = new ArrayList<>();
            for (int n = random.nextInt(6); n > 0; n--) {
                bids.add(randomLevel(random));
            }
            for (int n = random.nextInt(6); n > 0; n--) {
                asks.add(randomLevel(random));
            }
            cases.add(List.of(bids, asks));
        }
        return cases.stream();
    }

    private static List<Level> positiveOnly(List<Level> side) {
        return side.stream().filter(l -> l.price().signum() > 0 && l.size().signum() > 0).toList();
    }

    /**
     * R4. §2 (2.2-2.5): every level that survives has price and size above zero, in input order, and a book
     * survives exactly when both sides keep a level and the best bid is below the best ask.
     */
    @ParameterizedTest
    @MethodSource("randomSides")
    void onlyStrictlyPositiveEntriesSurviveAndBooksAreJudgedOnTheirBestPrices(List<List<Level>> sides) {
        List<Level> bids = positiveOnly(sides.get(0));
        List<Level> asks = positiveOnly(sides.get(1));
        boolean valid = !bids.isEmpty() && !asks.isEmpty()
                && bids.stream().map(Level::price).max(BigDecimal::compareTo).orElseThrow()
                        .compareTo(asks.stream().map(Level::price).min(BigDecimal::compareTo).orElseThrow()) < 0;

        Screened s = screened(Screener.screen(
                snapshot(new ReadableBook("A", sides.get(0), sides.get(1)), bookAtMid("keep", "100")),
                NO_DEVIATION_SCREEN));

        if (valid) {
            assertThat(s.survivors().get(0).exchange()).isEqualTo("A");
            assertThat(s.survivors().get(0).bids()).isEqualTo(bids);
            assertThat(s.survivors().get(0).asks()).isEqualTo(asks);
            assertThat(s.excludedExchanges()).isEmpty();
        } else {
            assertThat(s.excludedExchanges()).containsExactly("A");
        }
    }

    // ---------------------------------------------------------------- R3: order independence

    /** Snapshots of 3 to 9 books mixing valid books around 100 (some outliers), crossed and unreadable books. */
    static Stream<List<BookInput>> mixedBooks() {
        Random random = new Random(SEED + 1);
        List<List<BookInput>> cases = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            List<BookInput> books = new ArrayList<>();
            int count = 3 + random.nextInt(7);
            for (int b = 0; b < count; b++) {
                String name = "B" + b;
                switch (random.nextInt(6)) {
                    case 0 -> books.add(unreadable(name));
                    case 1 -> books.add(crossedAtMid(name, "100"));
                    case 2 -> books.add(bookAtMid(name, BigDecimal.valueOf(5000 + random.nextInt(10000), 2).toPlainString()));
                    default -> books.add(bookAtMid(name, BigDecimal.valueOf(9000 + random.nextInt(2000), 2).toPlainString()));
                }
            }
            cases.add(books);
        }
        return cases.stream();
    }

    /**
     * R3. D-11, §2 (2.6): shuffling the input changes neither which exchanges are excluded nor which survive,
     * and in every run both lists follow that run's input order.
     */
    @ParameterizedTest
    @MethodSource("mixedBooks")
    void theOrderOfTheInputDoesNotChangeWhoIsExcluded(List<BookInput> books) {
        ScreeningResult original = Screener.screen(snapshot(books), new StaticPedFilter());
        Random random = new Random(SEED + 2);

        for (int run = 0; run < 3; run++) {
            List<BookInput> shuffled = new ArrayList<>(books);
            Collections.shuffle(shuffled, random);
            ScreeningResult result = Screener.screen(snapshot(shuffled), new StaticPedFilter());

            if (original instanceof Failed f) {
                assertThat(result).isEqualTo(f);
                continue;
            }
            Screened expected = (Screened) original;
            Screened actual = screened(result);
            assertThat(new HashSet<>(actual.excludedExchanges())).isEqualTo(new HashSet<>(expected.excludedExchanges()));
            assertThat(actual.survivors().stream().map(b -> b.exchange()).collect(java.util.stream.Collectors.toSet()))
                    .isEqualTo(expected.survivors().stream().map(b -> b.exchange()).collect(java.util.stream.Collectors.toSet()));
            // each list is in the shuffled input order
            List<String> order = shuffled.stream().map(BookInput::exchange).toList();
            assertThat(order).containsSubsequence(actual.excludedExchanges());
            assertThat(order).containsSubsequence(actual.survivors().stream().map(b -> b.exchange()).toList());
        }
    }
}
