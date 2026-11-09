package com.cfbenchmarks.obsi.cli;

import com.cfbenchmarks.obsi.deviation.DeviationFilter;
import com.cfbenchmarks.obsi.deviation.SpreadBasedPedFilter;
import com.cfbenchmarks.obsi.deviation.StaticPedFilter;
import com.cfbenchmarks.obsi.input.InputParser;
import com.cfbenchmarks.obsi.input.InputResult;
import com.cfbenchmarks.obsi.input.Snapshot;
import com.cfbenchmarks.obsi.result.CalculationResult;
import com.cfbenchmarks.obsi.result.ObsiCalculator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.function.Function;

/**
 * Command-line entry point: {@code [--new-ped] <path>} (D-10). A published result is one JSON object on stdout with
 * exit 0; a failure is one JSON object on stderr with a nonzero exit (docs/brief.md, "Output"). Nothing else is
 * written to either stream (D-09).
 *
 * <p>Exit codes (D-08): 0 published, 1 calculation failure (§6), 2 input or usage error, 3 unexpected internal error.
 * {@code calculationTime} is echoed on a calculation failure and is {@code null} on an input or usage error (D-07,
 * D-22).
 */
public final class Main {

    static final int EXIT_PUBLISHED = 0;
    static final int EXIT_CALCULATION_FAILED = 1;
    static final int EXIT_INPUT_OR_USAGE = 2;
    static final int EXIT_INTERNAL = 3;

    static final String STDOUT_WRITE_FAILED = "internal error: could not write the result to stdout";
    private static final int INTERNAL_REASON_LIMIT = 200;

    private static final ObjectMapper JSON = new ObjectMapper();

    private Main() {
    }

    public static void main(String[] args) {
        // D-22: UTF-8 explicitly, so exchange names come out as given whatever the platform's default encoding.
        PrintStream out = new PrintStream(System.out, true, StandardCharsets.UTF_8);
        PrintStream err = new PrintStream(System.err, true, StandardCharsets.UTF_8);
        System.exit(run(args, out, err));
    }

    /**
     * Runs the original 5% rule, or the spread-based rule of Task 2 with {@code --new-ped} (proposal.md). Returns the
     * exit code.
     */
    static int run(String[] args, PrintStream out, PrintStream err) {
        return run(args, out, err, null);
    }

    /**
     * @param calculation the calculation to run on an accepted snapshot, or null for the real one; a parameter so a
     *                    test can make it throw
     */
    static int run(String[] args, PrintStream out, PrintStream err, Function<Snapshot, CalculationResult> calculation) {
        // Known once the input is accepted; a failure after that point echoes it (D-22).
        String calculationTime = null;
        try {
            // D-10: arguments first, so a usage error is reported before any file is touched.
            CliArgs.Result parsed = CliArgs.parse(args);
            if (parsed instanceof CliArgs.Invalid invalid) {
                return fail(err, null, invalid.reason(), EXIT_INPUT_OR_USAGE);
            }
            CliArgs.Valid arguments = (CliArgs.Valid) parsed;

            // D-06, D-07: an input error has no calculationTime to report. D-04, D-23: unlockEvents is read, and
            // validated, only with --new-ped.
            InputResult input = InputParser.parse(arguments.path(), arguments.newPed());
            if (input instanceof InputResult.Rejected rejected) {
                return fail(err, null, rejected.reason(), EXIT_INPUT_OR_USAGE);
            }
            Snapshot snapshot = ((InputResult.Accepted) input).snapshot();
            calculationTime = snapshot.calculationTime();

            // Task 2: without the flag, the original 5% rule is unchanged; with it, the spread-based rule.
            DeviationFilter filter = arguments.newPed() ? new SpreadBasedPedFilter() : new StaticPedFilter();
            CalculationResult result = calculation != null
                    ? calculation.apply(snapshot)
                    : ObsiCalculator.calculate(snapshot, filter);
            if (result instanceof CalculationResult.Failed failed) {
                return fail(err, failed.calculationTime(), failed.reason(), EXIT_CALCULATION_FAILED);
            }
            // Built completely before anything is written, so stdout never holds a partial result.
            String json = published((CalculationResult.Published) result);
            write(out, json);
            // D-22: a PrintStream never throws; a failed write only sets a flag. A result that could not be
            // written was not published, so say so (best effort on stderr) instead of exiting 0.
            if (out.checkError()) {
                return failBestEffort(err, calculationTime, STDOUT_WRITE_FAILED);
            }
            return EXIT_PUBLISHED;
        } catch (Throwable t) {
            // D-08, D-22: whatever went wrong, the caller gets the failure JSON and never a stack trace. This is
            // best effort: under a real OutOfMemoryError even printing the JSON may fail. Throwable and not just
            // Exception, so an Error is reported the same way.
            return failBestEffort(err, calculationTime, internalReason(t));
        }
    }

    /** Brief, "Output" (success): value and utilizedDepth are strings, written with toPlainString() (D-14). */
    private static String published(CalculationResult.Published p) {
        ObjectNode result = JSON.createObjectNode()
                .put("index", "OBSI")
                .put("calculationTime", p.calculationTime())
                .put("status", "published")
                .put("value", p.value().toPlainString());
        ObjectNode metadata = result.putObject("metadata");
        metadata.put("utilizedDepth", p.utilizedDepth().toPlainString());
        ArrayNode excluded = metadata.putArray("excludedExchanges");
        p.excludedExchanges().forEach(excluded::add);
        return result.toString();
    }

    /** Brief, "Output" (failure). {@code calculationTime} is null when the input itself doesn't conform (D-07). */
    private static int fail(PrintStream err, String calculationTime, String reason, int exit) {
        ObjectNode failure = JSON.createObjectNode().put("index", "OBSI");
        if (calculationTime == null) {
            failure.putNull("calculationTime");
        } else {
            failure.put("calculationTime", calculationTime);
        }
        failure.put("status", "failed").put("reason", reason);
        write(err, failure.toString());
        return exit;
    }

    private static int failBestEffort(PrintStream err, String calculationTime, String reason) {
        try {
            fail(err, calculationTime, reason, EXIT_INTERNAL);
        } catch (Throwable ignored) {
            // nothing more can be done: the exit code still says what happened
        }
        return EXIT_INTERNAL;
    }

    /** D-22: {@code internal error: <SimpleName>: <message>}, on one line, at most 200 characters. */
    private static String internalReason(Throwable t) {
        String name = t.getClass().getSimpleName();
        if (name.isEmpty()) {
            name = t.getClass().getName();
        }
        String reason = "internal error: " + name + (t.getMessage() == null ? "" : ": " + t.getMessage());
        reason = reason.replaceAll("\\R", " ");
        return reason.length() > INTERNAL_REASON_LIMIT ? reason.substring(0, INTERNAL_REASON_LIMIT) : reason;
    }

    /** One JSON object on one line, ended by a single newline (not the platform's line separator). */
    private static void write(PrintStream stream, String json) {
        stream.print(json);
        stream.print('\n');
        stream.flush();
    }
}
