package com.cfbenchmarks.obsi.screening;

import com.cfbenchmarks.obsi.book.ValidBook;
import com.cfbenchmarks.obsi.deviation.DeviationFilter;
import com.cfbenchmarks.obsi.deviation.StaticPedFilter;
import com.cfbenchmarks.obsi.input.Level;
import com.cfbenchmarks.obsi.input.Snapshot;
import com.cfbenchmarks.obsi.screening.ScreeningResult.Screened;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static com.cfbenchmarks.obsi.TestBooks.book;
import static com.cfbenchmarks.obsi.TestBooks.bookAtMid;
import static com.cfbenchmarks.obsi.TestBooks.crossedAtMid;
import static com.cfbenchmarks.obsi.TestBooks.lv;
import static com.cfbenchmarks.obsi.TestBooks.names;
import static com.cfbenchmarks.obsi.TestBooks.snapshot;
import static com.cfbenchmarks.obsi.TestBooks.unreadable;
import static com.cfbenchmarks.obsi.TestBooks.validAtMid;
import static com.cfbenchmarks.obsi.screening.ScreenerTestSupport.NO_DEVIATION_SCREEN;
import static com.cfbenchmarks.obsi.screening.ScreenerTestSupport.PED_REASON;
import static com.cfbenchmarks.obsi.screening.ScreenerTestSupport.PLAIN_REASON;
import static com.cfbenchmarks.obsi.screening.ScreenerTestSupport.failureReason;
import static com.cfbenchmarks.obsi.screening.ScreenerTestSupport.screened;
import static com.cfbenchmarks.obsi.screening.ScreenerTestSupport.survivorNames;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** The screener end to end with the static 5% filter, and how it uses the {@link DeviationFilter} interface. */
class ScreenerTest {

    private static Screened withStaticPed(Snapshot snapshot) {
        return screened(Screener.screen(snapshot, new StaticPedFilter()));
    }

    // ---------------------------------------------------------------- what counts toward the median

    /**
     * M5. §1/§2 (1.7, 2.9): a crossed book is not structurally valid, so its mid does not count toward
     * the median. Valid mids 100 and 104 have median 102 and both pass. Had the two crossed books
     * (mid 200) been counted, the median would be 152 and both valid books would be excluded.
     */
    @Test
    void aCrossedBookDoesNotCountTowardTheMedian() {
        Screened s = withStaticPed(snapshot(bookAtMid("A", "100"), bookAtMid("B", "104"),
                crossedAtMid("C", "200"), crossedAtMid("D", "200")));

        assertThat(survivorNames(s)).containsExactly("A", "B");
        assertThat(s.excludedExchanges()).containsExactly("C", "D");
    }

    /** M6. §1 (1.7): unreadable books and books with an empty side after the entry screen have no mid to count. */
    @Test
    void unreadableAndEmptySideBooksDoNotCountTowardTheMedian() {
        // "Z" has a bid at 500 but its only ask is non-positive, so after the entry screen it has no ask side.
        Screened s = withStaticPed(snapshot(bookAtMid("A", "100"), bookAtMid("B", "104"),
                unreadable("U"), book("Z", List.of(lv("500", "1")), List.of(lv("0", "5")))));

        assertThat(survivorNames(s)).containsExactly("A", "B");
        assertThat(s.excludedExchanges()).containsExactly("U", "Z");
    }

    /**
     * M7. §2 (2.2, 2.9): the mid uses the best prices left after the entry screen. If the zero-size bid at 130
     * were taken as the best bid, book A would look crossed and be excluded.
     */
    @Test
    void midUsesTheBestPricesLeftAfterTheEntryScreen() {
        Screened s = withStaticPed(snapshot(
                book("A", List.of(lv("130", "0"), lv("99.9", "1")), List.of(lv("100.1", "1"))),
                bookAtMid("B", "100"), bookAtMid("C", "100")));

        assertThat(survivorNames(s)).containsExactly("A", "B", "C");
        assertThat(s.excludedExchanges()).isEmpty();
        assertThat(s.survivors().get(0).mid()).isEqualByComparingTo("100");
    }

    // ---------------------------------------------------------------- the DeviationFilter interface

    /** W1. The filter gets only the structurally valid books, in input order, and the same snapshot instance. */
    @Test
    void theFilterReceivesOnlyStructurallyValidBooksAndTheSnapshot() {
        List<List<ValidBook>> seenBooks = new ArrayList<>();
        List<Snapshot> seenSnapshots = new ArrayList<>();
        DeviationFilter recording = (books, snap) -> {
            seenBooks.add(books);
            seenSnapshots.add(snap);
            return List.of();
        };
        Snapshot snapshot = snapshot(bookAtMid("A", "100"), unreadable("U"), crossedAtMid("X", "100"),
                book("E", List.of(), List.of(lv("101", "1"))), bookAtMid("B", "101"));

        Screener.screen(snapshot, recording);

        assertThat(seenBooks).hasSize(1);
        assertThat(names(seenBooks.get(0))).containsExactly("A", "B");
        assertThat(seenBooks.get(0).get(0).bestBid()).isEqualByComparingTo("99.9");
        assertThat(seenBooks.get(0).get(0).bestAsk()).isEqualByComparingTo("100.1");
        assertThat(seenSnapshots.get(0)).isSameAs(snapshot);
    }

    /** W2. Whatever the filter excludes is excluded: the screener doesn't know which filter it has. */
    @Test
    void aCustomFilterDecidesWhichBooksAreExcluded() {
        Set<String> drop = Set.of("B", "D");
        DeviationFilter dropNamed = (books, snap) -> books.stream().filter(b -> drop.contains(b.exchange())).toList();

        Screened s = screened(Screener.screen(
                snapshot(bookAtMid("A", "100"), bookAtMid("B", "100"), bookAtMid("C", "100"), bookAtMid("D", "100")),
                dropNamed));

        assertThat(survivorNames(s)).containsExactly("A", "C");
        assertThat(s.excludedExchanges()).containsExactly("B", "D");
    }

    /**
     * W5. ValidBook is a record, so equals compares values. The screener matches the filter's answer to the
     * books it gave out by identity, so a value-equal copy is not one of them: it is a contract violation.
     */
    @Test
    void aFilterThatReturnsAnEqualCopyOfABookIsRejected() {
        DeviationFilter copying = (books, snap) -> {
            ValidBook given = books.get(0);
            ValidBook copy = new ValidBook(given.exchange(), given.bids(), given.asks(), given.bestBid(), given.bestAsk());
            assertThat(copy).as("premise: equal by value").isEqualTo(given).isNotSameAs(given);
            return List.of(copy);
        };

        assertThatThrownBy(() -> Screener.screen(snapshot(bookAtMid("A", "100"), bookAtMid("B", "100")), copying))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("A");
    }

    /** W5. A book that was never given to the filter is rejected too. */
    @Test
    void aFilterThatReturnsABookItWasNotGivenIsRejected() {
        DeviationFilter foreign = (books, snap) -> List.of(validAtMid("Z", "100"));

        assertThatThrownBy(() -> Screener.screen(snapshot(bookAtMid("A", "100")), foreign))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Z");
    }

    /**
     * W6. A hand-built snapshot can hold two books named "A" (the parser would reject it, D-02, but a Snapshot
     * does not). Excluding one instance removes that instance only, in either position, and the exclusion
     * is reported at that book's position in the input.
     */
    @Test
    void excludingOneOfTwoBooksWithTheSameNameRemovesOnlyThatInstance() {
        Snapshot twoAs = snapshot(book("A", "100", "101"), book("A", "90", "91"), bookAtMid("B", "100"));

        DeviationFilter dropSecond = (books, snap) -> List.of(books.get(1));
        Screened second = screened(Screener.screen(twoAs, dropSecond));
        assertThat(survivorNames(second)).containsExactly("A", "B");
        assertThat(second.survivors().get(0).bestBid()).isEqualByComparingTo("100");
        assertThat(second.excludedExchanges()).containsExactly("A");

        DeviationFilter dropFirst = (books, snap) -> List.of(books.get(0));
        Screened first = screened(Screener.screen(twoAs, dropFirst));
        assertThat(survivorNames(first)).containsExactly("A", "B");
        assertThat(first.survivors().get(0).bestBid()).isEqualByComparingTo("90");
        assertThat(first.excludedExchanges()).containsExactly("A");
    }

    static Stream<Arguments> filters() {
        Supplier<DeviationFilter> staticPed = StaticPedFilter::new;
        Supplier<DeviationFilter> none = () -> NO_DEVIATION_SCREEN;
        Supplier<DeviationFilter> everything = () -> (books, snap) -> List.copyOf(books);
        return Stream.of(Arguments.of("StaticPedFilter", staticPed), Arguments.of("exclude nothing", none),
                Arguments.of("exclude everything", everything));
    }

    /** W3. Any filter is interchangeable behind the interface: survivors plus exclusions always partition the input. */
    @ParameterizedTest(name = "{0}")
    @MethodSource("filters")
    void everyFilterLeavesSurvivorsAndExclusionsThatPartitionTheInput(String label, Supplier<DeviationFilter> filter) {
        Snapshot snapshot = snapshot(bookAtMid("A", "100"), bookAtMid("B", "100"), unreadable("U"),
                bookAtMid("C", "150"), crossedAtMid("X", "100"));

        ScreeningResult result = Screener.screen(snapshot, filter.get());

        List<String> all = new ArrayList<>();
        if (result instanceof Screened s) {
            all.addAll(survivorNames(s));
            all.addAll(s.excludedExchanges());
        } else {
            all.addAll(List.of("A", "B", "U", "C", "X"));   // a failure has no survivors; nothing to partition
        }
        assertThat(all).containsExactlyInAnyOrder("A", "B", "U", "C", "X");
    }

    // ---------------------------------------------------------------- output

    /**
     * O1. Brief "Output": excludedExchanges follow input order, with structural and PED exclusions interleaved
     * (not grouped by cause). Survivors keep input order too.
     */
    @Test
    void exclusionsAndSurvivorsFollowInputOrder() {
        Screened s = withStaticPed(snapshot(
                bookAtMid("X", "150"),         // PED outlier, listed first
                bookAtMid("A", "100"),
                crossedAtMid("B", "100"),
                unreadable("C"),
                bookAtMid("D", "60"),          // PED outlier, after structural exclusions
                bookAtMid("E", "101"),
                bookAtMid("F", "100.5")));

        assertThat(survivorNames(s)).containsExactly("A", "E", "F");
        assertThat(s.excludedExchanges()).containsExactly("X", "B", "C", "D");
    }

    /** O2. D-16: excluded names are listed exactly as given (case and padding kept). */
    @Test
    void excludedNamesAreKeptExactlyAsGiven() {
        Screened s = withStaticPed(snapshot(bookAtMid("ok", "100"), crossedAtMid(" kRaKeN ", "100"), unreadable("Bitstamp é")));

        assertThat(s.excludedExchanges()).containsExactly(" kRaKeN ", "Bitstamp é");
    }

    // ---------------------------------------------------------------- §6: no book survives

    /** O5. §6 (6.2), D-12, D-19: two books at 100 and 120 are both out by PED. */
    @Test
    void whenPedExcludesEveryBookTheReasonSaysSo() {
        assertThat(failureReason(Screener.screen(snapshot(bookAtMid("A", "100"), bookAtMid("B", "120")),
                new StaticPedFilter()))).isEqualTo(PED_REASON);
        // four books, two clusters: median 110, all four deviate by 9.09%
        assertThat(failureReason(Screener.screen(snapshot(bookAtMid("A", "100"), bookAtMid("B", "100"),
                bookAtMid("C", "120"), bookAtMid("D", "120")), new StaticPedFilter()))).isEqualTo(PED_REASON);
    }

    /** O5. D-19: structural exclusions elsewhere don't change the reason when PED removed every valid book. */
    @Test
    void structuralExclusionsDoNotChangeThePedReason() {
        assertThat(failureReason(Screener.screen(
                snapshot(unreadable("U"), bookAtMid("A", "100"), crossedAtMid("X", "100"), bookAtMid("B", "120")),
                new StaticPedFilter()))).isEqualTo(PED_REASON);
    }

    /** O6. D-19: no structurally valid book means PED never ran: plain reason, no suffix. */
    @Test
    void whenNoBookIsStructurallyValidTheReasonHasNoPedSuffix() {
        Snapshot snapshot = snapshot(unreadable("U"), crossedAtMid("X", "100"),
                book("E", List.of(), List.of(new Level(new java.math.BigDecimal("101"), new java.math.BigDecimal("1")))));

        assertThat(failureReason(Screener.screen(snapshot, new StaticPedFilter()))).isEqualTo(PLAIN_REASON);
    }

    /** O7. D-07, D-19: an empty books array fails as §6 with the plain reason. */
    @Test
    void anEmptySnapshotFailsWithThePlainReason() {
        assertThat(failureReason(Screener.screen(snapshot(), new StaticPedFilter()))).isEqualTo(PLAIN_REASON);
    }

    /** O8. D-19: the PED reason follows the books the filter removed, not which filter it is. */
    @Test
    void aFilterThatExcludesEveryValidBookGivesThePedReason() {
        DeviationFilter everything = (books, snap) -> List.copyOf(books);

        assertThat(failureReason(Screener.screen(snapshot(bookAtMid("A", "100"), bookAtMid("B", "100")), everything)))
                .isEqualTo(PED_REASON);
    }

    /** One surviving book is enough to publish (no failure). */
    @Test
    void aSingleSurvivorIsNotAFailure() {
        Screened s = withStaticPed(snapshot(bookAtMid("A", "100"), crossedAtMid("X", "100")));

        assertThat(survivorNames(s)).containsExactly("A");
        assertThat(s.excludedExchanges()).containsExactly("X");
    }
}
