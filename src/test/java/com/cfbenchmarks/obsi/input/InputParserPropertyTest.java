package com.cfbenchmarks.obsi.input;

import com.cfbenchmarks.obsi.input.BookInput.ReadableBook;
import com.cfbenchmarks.obsi.input.InputResult.Accepted;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/** Property-style tests: inputs come from a {@link Random} with a fixed seed, so every run is identical. */
class InputParserPropertyTest {

    private static final long SEED = 20261108L;

    // ---------------------------------------------------------------- numbers

    /** A random JSON number token: integer or decimal, 1 to 40 digits, scale 0 to 20, trailing zeros allowed. */
    static Stream<String> numberTexts() {
        Random random = new Random(SEED);
        List<String> texts = new ArrayList<>();
        for (int i = 0; i < 300; i++) {
            int fractionDigits = random.nextInt(3) == 0 ? 0 : random.nextInt(21);
            int integerDigits = 1 + random.nextInt(Math.min(20, 40 - fractionDigits));
            StringBuilder sb = new StringBuilder();
            sb.append(integerDigits == 1 ? (char) ('0' + random.nextInt(10)) : (char) ('1' + random.nextInt(9)));
            for (int d = 1; d < integerDigits; d++) {
                sb.append((char) ('0' + random.nextInt(10)));
            }
            if (fractionDigits > 0) {
                sb.append('.');
                for (int d = 0; d < fractionDigits; d++) {
                    sb.append((char) ('0' + random.nextInt(10)));
                }
            }
            texts.add(sb.toString());
        }
        return texts.stream();
    }

    /** P7. CLAUDE.md numbers: any JSON number round-trips exactly as a BigDecimal, scale included. */
    @ParameterizedTest
    @MethodSource("numberTexts")
    void numbersRoundTripExactly(String text) {
        String json = "{\"calculationTime\":\"2026-10-27T12:00:00Z\",\"books\":["
                + "{\"exchange\":\"A\",\"bids\":[[" + text + "," + text + "]],\"asks\":[]}]}";

        InputResult result = InputParser.parse(json.getBytes(StandardCharsets.UTF_8));

        Level level = ((ReadableBook) ((Accepted) result).snapshot().books().get(0)).bids().get(0);
        assertThat(level.price()).isEqualTo(new BigDecimal(text));
        assertThat(level.size()).isEqualTo(new BigDecimal(text));
    }

    // ---------------------------------------------------------------- robustness

    /** Random truncations and byte edits of the reference fixture. */
    static Stream<byte[]> mutatedDocuments() throws IOException {
        byte[] original = Files.readAllBytes(Path.of("data/sample-input.json"));
        Random random = new Random(SEED);
        List<byte[]> docs = new ArrayList<>();
        for (int i = 0; i < 400; i++) {
            byte[] bytes = original.clone();
            switch (random.nextInt(4)) {
                case 0 -> bytes = java.util.Arrays.copyOf(bytes, random.nextInt(bytes.length + 1));
                case 1 -> bytes[random.nextInt(bytes.length)] = (byte) random.nextInt(256);
                case 2 -> {
                    int at = random.nextInt(bytes.length);
                    byte[] shorter = new byte[bytes.length - 1];
                    System.arraycopy(bytes, 0, shorter, 0, at);
                    System.arraycopy(bytes, at + 1, shorter, at, bytes.length - at - 1);
                    bytes = shorter;
                }
                default -> {
                    int at = random.nextInt(bytes.length + 1);
                    byte[] longer = new byte[bytes.length + 1];
                    System.arraycopy(bytes, 0, longer, 0, at);
                    longer[at] = (byte) random.nextInt(256);
                    System.arraycopy(bytes, at, longer, at + 1, bytes.length - at);
                    bytes = longer;
                }
            }
            docs.add(bytes);
        }
        return docs.stream();
    }

    /** P29. D-08: whatever the bytes are, the parser returns a typed result; nothing leaks as an exception. */
    @ParameterizedTest
    @MethodSource("mutatedDocuments")
    void neverThrowsOnDamagedInput(byte[] bytes) {
        InputResult result = InputParser.parse(bytes);

        assertThat(result).isNotNull();
        if (result instanceof InputResult.Rejected rejected) {
            assertThat(rejected.reason()).startsWith("input error: ");
        }
    }
}
