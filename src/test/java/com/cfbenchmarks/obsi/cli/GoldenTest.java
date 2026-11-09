package com.cfbenchmarks.obsi.cli;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Golden test: the definition of "done" for the whole project. Every input under {@code data/}
 * that has a matching {@code src/test/resources/expected/<name>.expected.json} must produce
 * exactly that JSON tree (compared as parsed JSON, not text), with empty stderr and exit 0.
 * {@code <name>.new-ped.expected.json} is the same input run with {@code --new-ped}.
 * Inputs without an expected file are skipped.
 */
class GoldenTest {

    private static final Path DATA = Path.of("data");
    private static final Path EXPECTED = Path.of("src/test/resources/expected");
    private static final String SUFFIX = ".expected.json";
    private static final String NEW_PED_SUFFIX = ".new-ped.expected.json";
    // Trailing content after the result is an error: stdout must be exactly one JSON object
    // (CLAUDE.md: "Nothing else goes to stdout").
    private static final ObjectMapper JSON = new ObjectMapper()
            .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS);

    /**
     * One input run in one mode against one expected file. The mode is in the expected file's name:
     * {@code X.expected.json} is the run without a flag and {@code X.new-ped.expected.json} the run with
     * {@code --new-ped}, both for {@code data/**}/X.json.
     */
    record Pair(Path input, Path expected, boolean newPed) {
        @Override
        public String toString() {
            return (newPed ? "--new-ped " : "") + input;
        }
    }

    /** Pairs found on disk, in either mode; the walk is recursive so directory names don't matter. */
    static Stream<Pair> goldenPairs() throws IOException {
        List<Pair> pairs = new java.util.ArrayList<>();
        try (Stream<Path> inputs = Files.walk(DATA)) {
            for (Path p : inputs.filter(f -> Files.isRegularFile(f) && f.getFileName().toString().endsWith(".json"))
                    .sorted().toList()) {
                Path plain = EXPECTED.resolve(stem(p) + SUFFIX);
                Path flagged = EXPECTED.resolve(stem(p) + NEW_PED_SUFFIX);
                if (Files.isRegularFile(plain)) {
                    pairs.add(new Pair(p, plain, false));
                }
                if (Files.isRegularFile(flagged)) {
                    pairs.add(new Pair(p, flagged, true));
                }
            }
        }
        return pairs.stream();
    }

    private static String stem(Path p) {
        String name = p.getFileName().toString();
        return name.substring(0, name.length() - ".json".length());
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("goldenPairs")
    void producesTheExpectedResult(Pair pair) throws Exception {
        var out = new ByteArrayOutputStream();
        var err = new ByteArrayOutputStream();

        String[] args = pair.newPed()
                ? new String[] {"--new-ped", pair.input().toString()}
                : new String[] {pair.input().toString()};
        int exit = Main.run(args,
                new PrintStream(out, true, StandardCharsets.UTF_8),
                new PrintStream(err, true, StandardCharsets.UTF_8));

        assertThat(err.toString(StandardCharsets.UTF_8)).as("stderr").isEmpty();
        assertThat(exit).as("exit code").isZero();
        JsonNode actual = JSON.readTree(out.toString(StandardCharsets.UTF_8));
        assertThat(actual).as("stdout JSON").isEqualTo(JSON.readTree(pair.expected().toFile()));
    }

    /** Guard: a renamed or mistyped file must not turn the golden test into a silent zero-case pass. */
    @Test
    void everyExpectedFileHasAnInputAndSomePairExists() throws IOException {
        List<Pair> pairs = goldenPairs().toList();
        assertThat(pairs).as("input/expected pairs found").isNotEmpty();

        List<String> paired = pairs.stream().map(pair -> pair.expected().getFileName().toString()).toList();
        try (Stream<Path> expected = Files.list(EXPECTED)) {
            List<String> orphans = expected
                    .map(p -> p.getFileName().toString())
                    .filter(n -> n.endsWith(SUFFIX))
                    .filter(n -> !paired.contains(n))
                    .toList();
            assertThat(orphans).as("expected files without a matching input under data/").isEmpty();
        }
    }
}
