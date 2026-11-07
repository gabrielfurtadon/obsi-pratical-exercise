package com.cfbenchmarks.obsi;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;

import static org.assertj.core.api.Assertions.assertThat;

/** The spec is the source of truth and must never change. See AGENTS.md. */
class SpecIntegrityTest {

    private static final String PINNED_SHA256 =
            "07acba3c91d8e7605949a71c5657a04309324ded56ed1d9a30eac8f9504ee655";

    @Test
    void specMatchesPinnedHash() throws Exception {
        byte[] spec = Files.readAllBytes(Path.of("obsi-methodology.md"));
        String actual = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(spec));
        assertThat(actual)
                .as("obsi-methodology.md has been modified; the spec must not be edited")
                .isEqualTo(PINNED_SHA256);
    }
}
