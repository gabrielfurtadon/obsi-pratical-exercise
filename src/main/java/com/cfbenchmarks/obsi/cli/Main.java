package com.cfbenchmarks.obsi.cli;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.PrintStream;

/**
 * Command-line entry point. A published result goes to stdout; a failure goes to stderr with a
 * nonzero exit (docs/brief.md, "Output").
 *
 * <p>Scaffold only: no calculation is implemented yet, so every invocation reports a failure.
 */
public final class Main {

    static final int EXIT_FAILED = 1;

    private static final ObjectMapper JSON = new ObjectMapper();

    private Main() {
    }

    public static void main(String[] args) {
        System.exit(run(args, System.out, System.err));
    }

    static int run(String[] args, PrintStream out, PrintStream err) {
        // Brief, "Output" (failure): calculationTime is null when the input itself doesn't
        // conform; the input is not read yet, so it is always null here.
        ObjectNode failure = JSON.createObjectNode()
                .put("index", "OBSI")
                .putNull("calculationTime")
                .put("status", "failed")
                .put("reason", "calculation not implemented yet");
        err.println(failure.toString());
        err.flush();
        return EXIT_FAILED;
    }
}
