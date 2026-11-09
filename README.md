# OBSI: Order Book Snapshot Index

A Java 25 CLI implementing `obsi-methodology.md` (Task 1) and the PED extension in `proposal.md` (Task 2). This handoff covers what was built, what was assumed, how it is tested and what production still needs.

## Build and run

```
./build.sh                                 # runs every test, builds target/obsi.jar
./run.sh data/sample-input.json            # 5% PED rule
./run.sh --new-ped data/new-ped.json       # Task 2 rule
```

After the build, `run.sh` only runs the JAR and works offline. A published value goes to stdout with exit 0. A failure goes to stderr as one JSON object, with exit 1 for a calculation failure, 2 for an input or usage error (`calculationTime` is then null) and 3 for an unexpected error. Nothing else is written to either stream.

## How the calculation is organised

There is one package per stage of the methodology, and each stage hands the next one a type that checks its own invariants, so a later stage can rely on what it receives.

- `input`: parse the JSON into records, BigDecimal throughout.
- `screening`: entry and book screens, then the deviation filter.
- `deviation`: `DeviationFilter`, with `StaticPedFilter` (5%) and `SpreadBasedPedFilter`.
- `book`: consolidation and uncrossing.
- `curve`: price-volume curves and the mid-spread check.
- `index`: utilized depth, weights and the published value.
- `result`, `cli`: chain the stages, map outcomes to JSON and exit codes.

`docs/spec-trace.md` maps every clause of the methodology and the brief to its code and tests.

## Assumptions and decisions

The spec and brief leave real gaps. Each one is recorded in `docs/decisions.md` with its rationale. The principle behind most of them: a problem is scoped to the smallest unit that can be safely isolated (entry, then book, then document), and nothing is accepted silently when an exchange's identity or the data's integrity is in doubt. The decisions that change results:

- A string price or size is not numeric and its entry is discarded. Duplicate exchange names, compared trimmed and case-insensitively, are an input error. Duplicate JSON keys are an input error.
- "Best" bid and ask are by price, not input order. PED is part of screening, so two books at 100 and 120 both fail and the reason says so.
- Prices are BigDecimal end to end and boundary checks avoid division, so a book at exactly 5% passes. Weights use `StrictMath.exp`, reproducible bit for bit, with an error near 1e-14. The value is rounded once, HALF_UP.

Two findings: after the screens, uncrossing can never exhaust a side, so that failure is unreachable but still implemented and tested directly; and midSV never decreases with volume, so there is at most one crossing and the scan stops there, checked against a literal full scan.

## Tests

Each stage has unit tests written from the spec before the implementation, citing the clause or decision they cover. Property-style tests use `@ParameterizedTest` with fixed seeds, so every run is deterministic. `GoldenTest` runs each input in `data/` against `src/test/resources/expected/` (`X.expected.json` without the flag, `X.new-ped.expected.json` with it). Those files come from an independent oracle, a spreadsheet cross-checked with a separate Python implementation (`docs/oracle/`), not from this code. Boundary operators were mutated by hand to confirm the tests catch them, and a 10-second default timeout turns a hang into a failure. `SpecIntegrityTest` fails if the methodology changes, and `DeliverableWordLimitTest` enforces the word limits.

## Libraries

Jackson, configured to read numbers as BigDecimal and reject duplicate keys and trailing data. JUnit 5 and AssertJ for tests. The Maven enforcer requires Java 25; the shade plugin builds one runnable JAR. jqwik was removed: its build output tells AI agents to ignore its test results, a prompt injection in the very output this project's hooks feed back to an agent.

## Task 2 in one paragraph

`--new-ped` swaps the 5% rule for `k × median relative spread`, clamped to a band that tightens for 48 hours after an announced unlock (`unlockEvents` in the input). It is never looser than the current rule, and only the deviation filter changes. Rationale and parameters are in `proposal.md`, and the handling of a malformed `unlockEvents` is in `docs/decisions.md`.

## Production gaps

- **Scope.** One offline snapshot: no live feed, publication, monitoring or alerting. In production, exclusions and failures should raise alerts, and a failed snapshot needs a defined fallback: today one duplicated exchange name fails the whole snapshot.
- **Simplified methodology.** The exercise fixes spacing at 1 XYZ and drops the order size cap and half-limit re-entry of the CME CF methodology. Restoring them needs dynamic parameters and state across snapshots.
- **Task 2 calibration.** `k`, the floors and the caps are illustrative. They need a backtest on historical snapshots and committee approval. The persistent-premium concern is not addressed (see `proposal.md`).
- **Constituents.** Exchange names are not checked against an approved constituent list.
- **Scale limits.** A book that stays within D over a very large depth is walked to its end before failing, and a side deeper than about 9.2×10^18 units overflows a `long` (exit 3).
- **Streams.** The JVM itself can write to stderr (for example "Picked up JAVA_TOOL_OPTIONS"), which is outside the program's control.

## How AI was used

I used Claude Code to help write the code, implementing the methodology one section at a time. Its full sessions are in `docs/ai-sessions/`, and `ai-transcript.md` has the complete transcripts. The agent worked under guardrails, not only instructions: a short `AGENTS.md`, hooks that block writes to the spec and expected files, run the tests after each edit and save every session, and a read-only reviewer subagent. Before each stage it listed its planned tests for approval and recorded ambiguities in `decisions.md` instead of resolving them. Several AI claims were checked, found wrong and corrected with evidence, and expected values never came from the code under test.