package com.cfbenchmarks.obsi.cli;

import java.nio.file.InvalidPathException;
import java.nio.file.Path;

/**
 * The command line (D-10): {@code [--new-ped] <path>}, the flag before or after the path. An unknown option, no path
 * or more than one path is a usage error. This class only reads the arguments; whether {@code --new-ped} is
 * available is decided by {@link Main}.
 */
final class CliArgs {

    static final String NEW_PED = "--new-ped";
    static final String USAGE = "usage: [--new-ped] <path>";

    sealed interface Result permits Valid, Invalid {
    }

    /** @param newPed whether {@code --new-ped} was given, once or more */
    record Valid(Path path, boolean newPed) implements Result {
    }

    /** @param reason short and stable, prefixed {@code "usage error: "} (D-08) */
    record Invalid(String reason) implements Result {
    }

    private CliArgs() {
    }

    static Result parse(String[] args) {
        Path path = null;
        int paths = 0;
        boolean newPed = false;
        for (String arg : args) {
            if (arg.equals(NEW_PED)) {
                newPed = true;
            } else if (arg.startsWith("-")) {
                // An unknown option is reported first, before the path count, so the first thing wrong is
                // what the caller reads.
                return new Invalid("usage error: unknown option " + arg);
            } else {
                paths++;
                try {
                    path = Path.of(arg);
                } catch (InvalidPathException e) {
                    // not a path this system can name (a NUL character, say): the caller's mistake, not ours
                    return new Invalid("usage error: invalid input path");
                }
            }
        }
        if (paths == 0) {
            return new Invalid("usage error: missing input path (" + USAGE + ")");
        }
        if (paths > 1) {
            return new Invalid("usage error: more than one input path (" + USAGE + ")");
        }
        return new Valid(path, newPed);
    }
}
