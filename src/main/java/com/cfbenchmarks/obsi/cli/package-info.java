/**
 * Command-line interface: reads one snapshot from a local JSON path (optionally with
 * {@code --new-ped}), writes a published result to stdout and a failure to stderr.
 *
 * <p>Contract: docs/brief.md, "CLI" and "Output" (not defined in obsi-methodology.md). Failure
 * handling follows spec §6 "Calculation failures" ("Do not publish an index value for a failed
 * snapshot").
 */
package com.cfbenchmarks.obsi.cli;
