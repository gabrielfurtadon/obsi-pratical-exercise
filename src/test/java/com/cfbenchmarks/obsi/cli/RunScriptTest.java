package com.cfbenchmarks.obsi.cli;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/** {@code run.sh} when there is no jar to run: the failure shape of the brief, with the exit code of D-08. */
class RunScriptTest {

    @TempDir
    Path dir;

    /** D-08: exit 3 when target/obsi.jar is missing. The script is copied next to no jar, so it cannot find one. */
    @Test
    void aMissingJarIsAFailureJsonOnStderrWithExitCode3() throws IOException, InterruptedException {
        Path script = Files.copy(Path.of("run.sh"), dir.resolve("run.sh"));

        Process process = new ProcessBuilder("bash", script.toString(), "data/sample-input.json").start();
        String out = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        String err = new String(process.getErrorStream().readAllBytes(), StandardCharsets.UTF_8);
        int exit = process.waitFor();

        assertThat(exit).isEqualTo(3);
        assertThat(out).isEmpty();
        JsonNode failure = new ObjectMapper().readTree(err);
        assertThat(failure.properties()).extracting(e -> e.getKey())
                .containsExactly("index", "calculationTime", "status", "reason");
        assertThat(failure.get("calculationTime").isNull()).isTrue();
        assertThat(failure.get("status").asText()).isEqualTo("failed");
        assertThat(failure.get("reason").asText()).contains("target/obsi.jar not found");
    }
}
