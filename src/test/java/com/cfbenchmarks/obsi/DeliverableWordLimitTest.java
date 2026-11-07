package com.cfbenchmarks.obsi;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/** README.md and proposal.md are each limited to 1000 words (whitespace-separated tokens). */
class DeliverableWordLimitTest {

    private static final int MAX_WORDS = 1000;

    @ParameterizedTest
    @ValueSource(strings = {"README.md", "proposal.md"})
    void staysWithinWordLimit(String file) throws Exception {
        Path path = Path.of(file);
        assertThat(path).as("%s must exist", file).isRegularFile();
        String text = Files.readString(path).strip();
        long words = text.isEmpty() ? 0 : text.split("\\s+").length;
        assertThat(words).as("%s word count", file).isLessThanOrEqualTo(MAX_WORDS);
    }
}
