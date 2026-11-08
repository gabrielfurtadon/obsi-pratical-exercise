package com.cfbenchmarks.obsi.cli;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Tag;
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
 * Inputs without an expected file are skipped.
 */
class GoldenTest {

    private static final Path DATA = Path.of("data");
    private static final Path EXPECTED = Path.of("src/test/resources/expected");
    private static final String SUFFIX = ".expected.json";
    // Trailing content after the result is an error: stdout must be exactly one JSON object
    // (CLAUDE.md: "Nothing else goes to stdout").
    private static final ObjectMapper JSON = new ObjectMapper()
            .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS);

    /** Pairs of (input, expected) found on disk; the walk is recursive so directory names don't matter. */
    static Stream<Path[]> goldenPairs() throws IOException {
        List<Path[]> pairs;
        try (Stream<Path> inputs = Files.walk(DATA)) {
            pairs = inputs
                    .filter(p -> Files.isRegularFile(p) && p.getFileName().toString().endsWith(".json"))
                    .sorted()
                    .map(p -> new Path[] {p, EXPECTED.resolve(stem(p) + SUFFIX)})
                    .filter(pair -> Files.isRegularFile(pair[1]))
                    .toList();
        }
        return pairs.stream();
    }

    private static String stem(Path p) {
        String name = p.getFileName().toString();
        return name.substring(0, name.length() - ".json".length());
    }

    @Tag("wip")
    @ParameterizedTest(name = "{0}")
    @MethodSource("goldenPairs")
    void producesTheExpectedResult(Path input, Path expected) throws Exception {
        var out = new ByteArrayOutputStream();
        var err = new ByteArrayOutputStream();

        int exit = Main.run(new String[] {input.toString()},
                new PrintStream(out, true, StandardCharsets.UTF_8),
                new PrintStream(err, true, StandardCharsets.UTF_8));

        assertThat(err.toString(StandardCharsets.UTF_8)).as("stderr").isEmpty();
        assertThat(exit).as("exit code").isZero();
        JsonNode actual = JSON.readTree(out.toString(StandardCharsets.UTF_8));
        assertThat(actual).as("stdout JSON").isEqualTo(JSON.readTree(expected.toFile()));
    }

    /** Guard: a renamed or mistyped file must not turn the golden test into a silent zero-case pass. */
    @Test
    void everyExpectedFileHasAnInputAndSomePairExists() throws IOException {
        List<Path[]> pairs = goldenPairs().toList();
        assertThat(pairs).as("input/expected pairs found").isNotEmpty();

        List<String> paired = pairs.stream().map(pair -> pair[1].getFileName().toString()).toList();
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
