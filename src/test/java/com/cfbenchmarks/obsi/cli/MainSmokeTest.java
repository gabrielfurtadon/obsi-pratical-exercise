package com.cfbenchmarks.obsi.cli;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

/** Brief, "Output": a failure is one JSON object on stderr with a nonzero exit. */
class MainSmokeTest {

    @Test
    void reportsFailureShapeOnStderrWithNonzeroExit() throws Exception {
        var out = new ByteArrayOutputStream();
        var err = new ByteArrayOutputStream();

        int exit = Main.run(new String[] {"data/sample-input.json"},
                new PrintStream(out, true, StandardCharsets.UTF_8),
                new PrintStream(err, true, StandardCharsets.UTF_8));

        assertThat(exit).isNotZero();
        assertThat(out.toString(StandardCharsets.UTF_8)).isEmpty();
        JsonNode failure = new ObjectMapper().readTree(err.toString(StandardCharsets.UTF_8));
        assertThat(failure.properties()).extracting(e -> e.getKey())
                .containsExactly("index", "calculationTime", "status", "reason");
        assertThat(failure.get("index").asText()).isEqualTo("OBSI");
        assertThat(failure.get("calculationTime").isNull()).isTrue();
        assertThat(failure.get("status").asText()).isEqualTo("failed");
        assertThat(failure.get("reason").asText()).isNotBlank();
    }
}
