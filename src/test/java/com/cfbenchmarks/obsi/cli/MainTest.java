package com.cfbenchmarks.obsi.cli;

import com.cfbenchmarks.obsi.result.CalculationResult;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The whole CLI in-process: arguments, parsing, the calculation, and the brief's JSON (docs/brief.md, "Output") with the
 * exit codes of D-08. Success goes to stdout and a failure to stderr, never both.
 */
class MainTest {

    private static final String TIME = "2026-11-07T12:00:00Z";
    // Trailing content after the result is an error: each stream must hold exactly one JSON object.
    private static final ObjectMapper JSON = new ObjectMapper().enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS);

    @TempDir
    Path dir;

    /** What a run wrote and returned. */
    record Run(int exit, String out, String err) {
    }

    private static Run run(String... args) {
        return run(null, args);
    }

    private static Run run(Function<com.cfbenchmarks.obsi.input.Snapshot, CalculationResult> calculation,
                           String... args) {
        var out = new ByteArrayOutputStream();
        var err = new ByteArrayOutputStream();
        PrintStream outStream = new PrintStream(out, true, StandardCharsets.UTF_8);
        PrintStream errStream = new PrintStream(err, true, StandardCharsets.UTF_8);
        int exit = calculation == null
                ? Main.run(args, outStream, errStream)
                : Main.run(args, outStream, errStream, calculation);
        return new Run(exit, out.toString(StandardCharsets.UTF_8), err.toString(StandardCharsets.UTF_8));
    }

    private Path file(String content) throws IOException {
        return Files.writeString(dir.resolve("input.json"), content, StandardCharsets.UTF_8);
    }

    private static String document(String time, String... books) {
        return "{\"calculationTime\":\"" + time + "\",\"books\":[" + String.join(",", books) + "]}";
    }

    private static String book(String name, String bids, String asks) {
        return "{\"exchange\":\"" + name + "\",\"bids\":" + bids + ",\"asks\":" + asks + "}";
    }

    /** A book deep enough to publish on its own: v_T = 5 (the ask jumps to 101 after five units). */
    private static String goodBook(String name) {
        return book(name, "[[100,5],[99,5]]", "[[100.1,5],[101,5]]");
    }

    // ---------------------------------------------------------------- the shape of a failure

    /**
     * Every failure, whatever the cause: nothing on stdout, one JSON object on one line on stderr with the keys of the
     * brief in order, and the given exit code, calculationTime and reason.
     */
    private static void assertFailure(Run run, int exit, String calculationTime, String reason) {
        assertThat(run.exit()).as("exit code").isEqualTo(exit);
        assertThat(run.out()).as("stdout").isEmpty();
        assertThat(run.err()).as("stderr is one line").endsWith("\n");
        assertThat(run.err().indexOf('\n')).as("stderr is one line").isEqualTo(run.err().length() - 1);
        JsonNode failure;
        try {
            failure = JSON.readTree(run.err());
        } catch (IOException e) {
            throw new AssertionError("stderr is not one JSON object: " + run.err(), e);
        }
        assertThat(failure.properties()).extracting(e -> e.getKey())
                .containsExactly("index", "calculationTime", "status", "reason");
        assertThat(failure.get("index").asText()).isEqualTo("OBSI");
        assertThat(failure.get("status").asText()).isEqualTo("failed");
        if (calculationTime == null) {
            assertThat(failure.get("calculationTime").isNull()).as("calculationTime is null").isTrue();
        } else {
            assertThat(failure.get("calculationTime").asText()).isEqualTo(calculationTime);
        }
        assertThat(failure.get("reason").asText()).isEqualTo(reason);
    }

    // ---------------------------------------------------------------- success

    @Test
    void theSamplePublishesTheBriefsJsonOnStdout() throws IOException {
        Run run = run("data/sample-input.json");

        assertThat(run.exit()).isZero();
        assertThat(run.err()).as("stderr").isEmpty();
        // exactly one JSON line followed by a single newline, nothing else
        assertThat(run.out()).endsWith("\n");
        assertThat(run.out().indexOf('\n')).isEqualTo(run.out().length() - 1);
        JsonNode result = JSON.readTree(run.out());
        assertThat(result.properties()).extracting(e -> e.getKey())
                .containsExactly("index", "calculationTime", "status", "value", "metadata");
        assertThat(result.get("index").asText()).isEqualTo("OBSI");
        assertThat(result.get("calculationTime").asText()).isEqualTo("2026-10-27T12:00:00Z");
        assertThat(result.get("status").asText()).isEqualTo("published");
        assertThat(result.get("value").isTextual()).as("value is a string").isTrue();
        assertThat(result.get("value").asText()).isEqualTo("100.23");
        JsonNode metadata = result.get("metadata");
        assertThat(metadata.properties()).extracting(e -> e.getKey())
                .containsExactly("utilizedDepth", "excludedExchanges");
        assertThat(metadata.get("utilizedDepth").isTextual()).as("utilizedDepth is a string").isTrue();
        assertThat(metadata.get("utilizedDepth").asText()).isEqualTo("4");
        assertThat(metadata.get("excludedExchanges").toString()).isEqualTo("[\"Gamma\",\"Delta\"]");
    }

    @Test
    void anEmptyExcludedListIsAnEmptyArray() throws IOException {
        Run run = run(file(document(TIME, goodBook("A"))).toString());

        assertThat(run.exit()).isZero();
        assertThat(JSON.readTree(run.out()).get("metadata").get("excludedExchanges").toString()).isEqualTo("[]");
        assertThat(JSON.readTree(run.out()).get("metadata").get("utilizedDepth").asText()).isEqualTo("5");
    }

    @Test
    void calculationTimeIsEchoedExactlyAsGiven() throws IOException {
        // T24:00:00Z is accepted by Instant.parse (D-15) and must come back unchanged, not normalized
        Run run = run(file(document("2026-11-07T24:00:00Z", goodBook("A"))).toString());

        assertThat(run.exit()).isZero();
        assertThat(JSON.readTree(run.out()).get("calculationTime").asText()).isEqualTo("2026-11-07T24:00:00Z");
    }

    /** Exchange names are written as given, in UTF-8, whatever the platform encoding. */
    @Test
    void nonAsciiExchangeNamesComeBackAsGiven() throws IOException {
        String crossed = book("Kräken™ \\\"X\\\"", "[[101,1]]", "[[100,1]]");
        Run run = run(file(document(TIME, goodBook("A"), crossed)).toString());

        assertThat(run.exit()).isZero();
        assertThat(JSON.readTree(run.out()).get("metadata").get("excludedExchanges").get(0).asText())
                .isEqualTo("Kräken™ \"X\"");
    }

    // ---------------------------------------------------------------- input errors (exit 2, calculationTime null)

    @Test
    void invalidJsonIsAnInputError() throws IOException {
        assertFailure(run(file("{").toString()), 2, null, "input error: invalid JSON");
    }

    @Test
    void aMissingRequiredFieldIsAnInputErrorEvenWhenCalculationTimeIsReadable() throws IOException {
        // D-07: every input error reports calculationTime as null
        assertFailure(run(file("{\"calculationTime\":\"" + TIME + "\"}").toString()), 2, null,
                "input error: books is missing");
    }

    @Test
    void aMissingFileIsAnInputError() {
        assertFailure(run(dir.resolve("nope.json").toString()), 2, null, "input error: cannot read input file");
    }

    @Test
    void aDirectoryAsThePathIsAnInputError() {
        assertFailure(run(dir.toString()), 2, null, "input error: cannot read input file");
    }

    // ---------------------------------------------------------------- usage errors (exit 2, calculationTime null)

    @Test
    void noArgumentsIsAUsageError() {
        assertThat(run().exit()).isEqualTo(2);
        assertThat(run().out()).isEmpty();
        assertFailure(run(), 2, null, "usage error: missing input path (usage: [--new-ped] <path>)");
    }

    @Test
    void twoPathsIsAUsageError() {
        assertFailure(run("a.json", "b.json"), 2, null,
                "usage error: more than one input path (usage: [--new-ped] <path>)");
    }

    @Test
    void anUnknownFlagIsAUsageError() {
        assertFailure(run("--verbose", "data/sample-input.json"), 2, null, "usage error: unknown option --verbose");
    }

    /** Until Task 2 the flag is recognised and refused, wherever it is, and nothing is calculated. */
    @Test
    void newPedIsNotImplementedYetBeforeOrAfterThePath() {
        String reason = "usage error: --new-ped is not implemented yet";

        assertFailure(run("--new-ped", "data/sample-input.json"), 2, null, reason);
        assertFailure(run("data/sample-input.json", "--new-ped"), 2, null, reason);
    }

    /** The arguments are checked before the file is read, so the flag is reported even with a bad path. */
    @Test
    void newPedIsReportedBeforeTheFileIsRead() {
        assertFailure(run("--new-ped", "no-such-file.json"), 2, null, "usage error: --new-ped is not implemented yet");
    }

    // ---------------------------------------------------------------- calculation failures (exit 1, calculationTime echoed)

    @Test
    void noBooksIsANoSurvivorFailure() throws IOException {
        assertFailure(run(file(document(TIME)).toString()), 1, TIME, "no constituent book survived screening");
    }

    @Test
    void everyBookCrossedIsANoSurvivorFailure() throws IOException {
        String crossed = book("A", "[[101,1]]", "[[100,1]]");

        assertFailure(run(file(document(TIME, crossed)).toString()), 1, TIME, "no constituent book survived screening");
    }

    @Test
    void everyValidBookExcludedByPedSaysSo() throws IOException {
        // mids 100 and 120: the median is 110 and both are 9.09% away
        String a = book("A", "[[99.9,5]]", "[[100.1,5]]");
        String b = book("B", "[[119.9,5]]", "[[120.1,5]]");

        assertFailure(run(file(document(TIME, a, b)).toString()), 1, TIME,
                "no constituent book survived screening (every structurally valid book was excluded by PED)");
    }

    @Test
    void notEnoughDepthForTwoSamplesIsAnEmptyEquation2Set() throws IOException {
        String thin = book("A", "[[100,1.99]]", "[[100.1,5]]");

        assertFailure(run(file(document(TIME, thin)).toString()), 1, TIME,
                "empty Eq. 2 set: insufficient depth to evaluate consecutive samples");
    }

    @Test
    void noCrossingBeforeTheCurvesEndIsAnEmptyEquation2Set() throws IOException {
        String tight = book("A", "[[100,5]]", "[[100.1,5]]");

        assertFailure(run(file(document(TIME, tight)).toString()), 1, TIME,
                "empty Eq. 2 set: no mid-spread crossing before a curve ends");
    }

    // ---------------------------------------------------------------- unexpected errors (exit 3)

    @Test
    void anUnexpectedExceptionIsAFailureJsonAndNeverAStackTrace() throws IOException {
        Run run = run(snapshot -> {
            throw new IllegalStateException("boom");
        }, file(document(TIME, goodBook("A"))).toString());

        // the input was accepted by then, so its calculationTime is known and echoed
        assertFailure(run, 3, TIME, "internal error: IllegalStateException: boom");
        assertThat(run.err()).doesNotContain("\tat ").doesNotContain("Exception in thread");
    }

    @Test
    void anErrorThatIsNotAnExceptionIsCaughtToo() throws IOException {
        Run run = run(snapshot -> {
            throw new StackOverflowError();
        }, file(document(TIME, goodBook("A"))).toString());

        assertFailure(run, 3, TIME, "internal error: StackOverflowError");
    }

    @Test
    void theReasonOfAnUnexpectedErrorIsOneShortLine() throws IOException {
        String longMessage = "line one\nline two " + "x".repeat(500);
        Run run = run(snapshot -> {
            throw new IllegalStateException(longMessage);
        }, file(document(TIME, goodBook("A"))).toString());

        JsonNode failure = JSON.readTree(run.err());
        assertThat(run.exit()).isEqualTo(3);
        assertThat(failure.get("reason").asText()).startsWith("internal error: IllegalStateException: line one line two ")
                .hasSize(200).doesNotContain("\n");
        assertThat(run.err().indexOf('\n')).isEqualTo(run.err().length() - 1);
    }

    @Test
    void anUnexpectedFailureBeforeAnySnapshotHasANullCalculationTime() {
        // a calculation function that is never reached: the failure is in reading, so nothing is known
        Run run = run(snapshot -> {
            throw new AssertionError("must not be reached");
        }, dir.resolve("nope.json").toString());

        assertFailure(run, 2, null, "input error: cannot read input file");
    }

    // ---------------------------------------------------------------- a stream that cannot be written (PrintStream never throws)

    /** A stream whose every write fails, like a closed pipe. */
    private static PrintStream brokenStream() {
        return new PrintStream(new OutputStream() {
            @Override
            public void write(int b) throws IOException {
                throw new IOException("broken pipe");
            }
        }, true, StandardCharsets.UTF_8);
    }

    /** D-22: a result that could not be written is not a success. The failure is reported on stderr, exit 3. */
    @Test
    void aStdoutThatCannotBeWrittenIsAnInternalErrorNotASilentSuccess() {
        var err = new ByteArrayOutputStream();

        int exit = Main.run(new String[] {"data/sample-input.json"}, brokenStream(),
                new PrintStream(err, true, StandardCharsets.UTF_8));

        // the input was accepted, so its calculationTime is known and echoed
        assertFailure(new Run(exit, "", err.toString(StandardCharsets.UTF_8)), 3, "2026-10-27T12:00:00Z",
                "internal error: could not write the result to stdout");
    }

    /** If stderr is broken too there is nobody to tell: no exception, and the exit code still says what happened. */
    @Test
    void aBrokenStderrStillReturnsTheExitCode() {
        var out = new ByteArrayOutputStream();

        int inputError = Main.run(new String[] {"no-such-file.json"}, new PrintStream(out, true, StandardCharsets.UTF_8),
                brokenStream());
        int writeError = Main.run(new String[] {"data/sample-input.json"}, brokenStream(), brokenStream());

        assertThat(inputError).isEqualTo(2);
        assertThat(writeError).isEqualTo(3);
        assertThat(out.toString(StandardCharsets.UTF_8)).isEmpty();
    }

    /** A published result is not partly written when the calculation fails: stdout stays empty. */
    @Test
    void nothingIsWrittenToStdoutWhenTheCalculationThrows() throws IOException {
        Run run = run(snapshot -> {
            throw new RuntimeException("late");
        }, file(document(TIME, goodBook("A"))).toString());

        assertThat(run.out()).isEmpty();
        assertThat(List.of(run.exit())).containsExactly(3);
    }
}
