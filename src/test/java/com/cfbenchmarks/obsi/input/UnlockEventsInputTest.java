package com.cfbenchmarks.obsi.input;

import com.cfbenchmarks.obsi.input.InputResult.Accepted;
import com.cfbenchmarks.obsi.input.InputResult.Rejected;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The optional top-level {@code unlockEvents} (proposal.md, "Input change"; D-23). Read only for {@code --new-ped};
 * without it the field is ignored whatever it holds (D-04). Parsing the rest of the document is tested in
 * {@link InputParserTest}.
 */
class UnlockEventsInputTest {

    private static final String HEAD = "{\"calculationTime\":\"2026-10-27T12:00:00Z\",";
    private static final String BOOKS = "\"books\":[{\"exchange\":\"A\",\"bids\":[[100,1]],\"asks\":[[101,1]]}]";

    private static String doc(String unlockEvents) {
        return HEAD + BOOKS + (unlockEvents == null ? "" : ",\"unlockEvents\":" + unlockEvents) + "}";
    }

    private static InputResult parse(String json, boolean readUnlockEvents) {
        return InputParser.parse(json.getBytes(StandardCharsets.UTF_8), readUnlockEvents);
    }

    private static Snapshot accepted(String json, boolean readUnlockEvents) {
        InputResult result = parse(json, readUnlockEvents);
        if (!(result instanceof Accepted a)) {
            throw new AssertionError("expected Accepted but was " + result + " for " + json);
        }
        return a.snapshot();
    }

    private static String rejection(String json) {
        InputResult result = parse(json, true);
        if (!(result instanceof Rejected r)) {
            throw new AssertionError("expected Rejected but was " + result + " for " + json);
        }
        return r.reason();
    }

    // ---------------------------------------------------------------- 20. well-formed

    @Test
    void anAbsentFieldOrAnEmptyListIsNoUnlock() {
        assertThat(accepted(doc(null), true).unlockStarts()).isEmpty();
        assertThat(accepted(doc("[]"), true).unlockStarts()).isEmpty();
    }

    @Test
    void validEventsAreReadAsInstantsInInputOrder() {
        Snapshot snapshot = accepted(doc("[{\"start\":\"2026-10-26T18:00:00Z\"},{\"start\":\"2026-10-20T00:00:00.5Z\"}]"), true);

        assertThat(snapshot.unlockStarts())
                .containsExactly(Instant.parse("2026-10-26T18:00:00Z"), Instant.parse("2026-10-20T00:00:00.5Z"));
    }

    @Test
    void t24DuplicatesAndExtraKeysAreAccepted() {
        Snapshot snapshot = accepted(doc("[{\"start\":\"2026-10-26T24:00:00Z\",\"note\":\"x\",\"n\":1},"
                + "{\"start\":\"2026-10-27T00:00:00Z\"}]"), true);

        assertThat(snapshot.unlockStarts()).containsExactly(Instant.parse("2026-10-27T00:00:00Z"),
                Instant.parse("2026-10-27T00:00:00Z"));
    }

    // ---------------------------------------------------------------- 21. malformed, with the flag

    static Stream<Arguments> malformed() {
        String array = "input error: unlockEvents must be an array";
        String object = "input error: unlockEvents[0] must be an object";
        String start = "input error: unlockEvents[0].start must be an ISO-8601 UTC timestamp ending in Z";
        return Stream.of(
                Arguments.of("{}", array),
                Arguments.of("{\"start\":\"2026-10-26T18:00:00Z\"}", array),
                Arguments.of("\"2026-10-26T18:00:00Z\"", array),
                Arguments.of("7", array),
                Arguments.of("null", array),
                Arguments.of("true", array),
                Arguments.of("[\"2026-10-26T18:00:00Z\"]", object),
                Arguments.of("[null]", object),
                Arguments.of("[[\"2026-10-26T18:00:00Z\"]]", object),
                Arguments.of("[{}]", start),
                Arguments.of("[{\"start\":null}]", start),
                Arguments.of("[{\"start\":1793037600}]", start),
                Arguments.of("[{\"start\":true}]", start),
                Arguments.of("[{\"start\":\"tomorrow\"}]", start),
                Arguments.of("[{\"start\":\"2026-10-26T18:00:00+01:00\"}]", start),
                Arguments.of("[{\"start\":\"2026-10-26T18:00:00z\"}]", start),
                Arguments.of("[{\"start\":\"2026-10-26T18:00:00\"}]", start),
                Arguments.of("[{\"start\":\"\"}]", start));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("malformed")
    void malformedEventsAreAnInputErrorWithTheFlag(String unlockEvents, String reason) {
        assertThat(rejection(doc(unlockEvents))).isEqualTo(reason);
    }

    @Test
    void theIndexOfTheOffendingEventIsReported() {
        assertThat(rejection(doc("[{\"start\":\"2026-10-26T18:00:00Z\"},{\"start\":\"x\"}]")))
                .isEqualTo("input error: unlockEvents[1].start must be an ISO-8601 UTC timestamp ending in Z");
    }

    // ---------------------------------------------------------------- 22. ignored without the flag (D-04)

    @ParameterizedTest(name = "{0}")
    @MethodSource("malformed")
    void theSameInputIsAcceptedAndIgnoredWithoutTheFlag(String unlockEvents, String ignored) {
        assertThat(accepted(doc(unlockEvents), false).unlockStarts()).isEmpty();
    }

    @Test
    void wellFormedEventsAreAlsoIgnoredWithoutTheFlag() {
        assertThat(accepted(doc("[{\"start\":\"2026-10-26T18:00:00Z\"}]"), false).unlockStarts()).isEmpty();
        assertThat(((Accepted) InputParser.parse(doc("[{\"start\":\"2026-10-26T18:00:00Z\"}]")
                .getBytes(StandardCharsets.UTF_8))).snapshot().unlockStarts()).isEmpty();
    }

    // ---------------------------------------------------------------- 23. precedence

    @Test
    void existingInputErrorsComeBeforeUnlockEventErrors() {
        String badBooks = HEAD + "\"books\":{},\"unlockEvents\":{}}";
        String missingTime = "{\"books\":[],\"unlockEvents\":{}}";

        assertThat(((Rejected) parse(badBooks, true)).reason()).isEqualTo("input error: books must be an array");
        assertThat(((Rejected) parse(missingTime, true)).reason()).isEqualTo("input error: calculationTime is missing");
    }

    // ---------------------------------------------------------------- 24. Snapshot

    @Test
    void theTwoArgumentSnapshotHasNoUnlockStarts() {
        assertThat(new Snapshot("2026-10-27T12:00:00Z", List.of()).unlockStarts()).isEmpty();
    }
}
