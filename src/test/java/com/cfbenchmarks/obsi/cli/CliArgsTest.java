package com.cfbenchmarks.obsi.cli;

import com.cfbenchmarks.obsi.cli.CliArgs.Invalid;
import com.cfbenchmarks.obsi.cli.CliArgs.Result;
import com.cfbenchmarks.obsi.cli.CliArgs.Valid;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/** D-10: {@code [--new-ped] <path>}, the flag before or after the path; anything else is a usage error. */
class CliArgsTest {

    private static Valid valid(String... args) {
        Result result = CliArgs.parse(args);
        if (!(result instanceof Valid v)) {
            throw new AssertionError("expected Valid but was " + result);
        }
        return v;
    }

    private static String invalid(String... args) {
        Result result = CliArgs.parse(args);
        if (!(result instanceof Invalid i)) {
            throw new AssertionError("expected Invalid but was " + result);
        }
        return i.reason();
    }

    @Test
    void aPathAloneIsAccepted() {
        assertThat(valid("data/sample-input.json")).isEqualTo(new Valid(Path.of("data/sample-input.json"), false));
    }

    @Test
    void theFlagIsAcceptedBeforeOrAfterThePath() {
        Valid expected = new Valid(Path.of("in.json"), true);

        assertThat(valid("--new-ped", "in.json")).isEqualTo(expected);
        assertThat(valid("in.json", "--new-ped")).isEqualTo(expected);
    }

    @Test
    void theFlagGivenTwiceIsTheSameFlag() {
        assertThat(valid("--new-ped", "in.json", "--new-ped")).isEqualTo(new Valid(Path.of("in.json"), true));
    }

    @Test
    void noArgumentsIsAUsageError() {
        assertThat(invalid()).startsWith("usage error: ").contains("input path");
    }

    @Test
    void aFlagWithoutAPathIsAUsageError() {
        assertThat(invalid("--new-ped")).startsWith("usage error: ").contains("input path");
    }

    @Test
    void morePathsThanOneIsAUsageError() {
        assertThat(invalid("a.json", "b.json")).startsWith("usage error: ").contains("more than one");
        assertThat(invalid("--new-ped", "a.json", "b.json")).contains("more than one");
    }

    @Test
    void anUnknownFlagIsAUsageErrorNamingIt() {
        assertThat(invalid("--verbose", "in.json")).isEqualTo("usage error: unknown option --verbose");
        assertThat(invalid("in.json", "-x")).isEqualTo("usage error: unknown option -x");
    }

    /** A string this system cannot name as a path is a usage error, not an internal one. */
    @Test
    void aPathThatCannotBeNamedIsAUsageError() {
        assertThat(invalid("bad\0path.json")).isEqualTo("usage error: invalid input path");
    }

    /** The unknown flag is reported before the path count, so the first thing wrong is what the caller reads. */
    @Test
    void anUnknownFlagIsReportedBeforeTooManyPaths() {
        assertThat(invalid("a.json", "--verbose", "b.json")).contains("unknown option --verbose");
    }
}
