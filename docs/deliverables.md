# Deliverables checklist

From `docs/brief.md`. "If something isn't finished, ship it anyway and say what's missing."

## Task 1: methodology on one snapshot
- [ ] The methodology (`obsi-methodology.md`) applied to one snapshot, with failures reported in the brief's failure shape
- [ ] Success: one JSON object on stdout, exit 0. Fields: `index`, `calculationTime`, `status: "published"`, `value` (two-decimal string), `metadata.utilizedDepth` (decimal string), `metadata.excludedExchanges` (input order)
- [ ] Failure: one JSON object on stderr, nonzero exit. Fields: `index`, `calculationTime` (null when the input doesn't conform), `status: "failed"`, `reason`
- [ ] Diagnostics never share a stream with the result (D-09)

## Task 2: PED extension
- [ ] `proposal.md` compares two approaches and explains the choice. It may cover only some of the PM's points; prefer a financial rationale that's easy to explain to clients
- [ ] `--new-ped` runs the chosen approach. Without the flag, the original 5% rule is unchanged
- [ ] Any new or changed input fields are documented in `proposal.md`
- [ ] `data/new-ped.json`: a sample input for `--new-ped`. If the input format doesn't change, it's a copy of `data/sample-input.json`

## Repo and CLI
- [ ] Executable `build.sh` and `run.sh`, `mvnw` with the wrapper files, and `pom.xml` at the repo root
- [ ] `build.sh` runs first and leaves everything ready for `run.sh`
- [ ] After the build, these work offline with only Java 25:
  - `./run.sh data/sample-input.json`
  - `./run.sh --new-ped data/new-ped.json`
- [ ] Works on other inputs a reviewer drops into `data/`
- [ ] Deterministic tests, and both fixtures (`data/sample-input.json`, `data/new-ped.json`)
- [ ] `obsi-methodology.md` unchanged (`SpecIntegrityTest`), `src/test/resources/expected/` unchanged
- [ ] No `@Tag("wip")` tests left (they would fail `build.sh`)

## README.md (for the next Index Engineer)
- [ ] ≤ 1000 words (`DeliverableWordLimitTest`)
- [ ] Implemented steps
- [ ] Assumptions, including how each `docs/decisions.md` entry was resolved
- [ ] Tests: what is covered and how to run them
- [ ] Production gaps (no live feed, publishing or deployment are in scope) and anything unfinished
- [ ] Libraries, and why each was chosen. Keep them small and well known: Jackson, JUnit 5, AssertJ
- [ ] Why jqwik isn't used: its test output contained a message telling AI agents to ignore its results, and its author asks agents not to use it. That output is fed back to the agent by the hooks. Property-style tests use JUnit `@ParameterizedTest` with a fixed-seed `Random` instead (see `ai-transcript.md`)

## proposal.md
- [ ] ≤ 1000 words (`DeliverableWordLimitTest`)
- [ ] Contents as in Task 2 above

## AI sessions
- [ ] Full sessions in `ai-transcript.md` or as native exports in `docs/ai-sessions/`
  - Claude Code sessions are saved there automatically by the Stop/SessionEnd hook
  - claude.ai sessions must be **exported manually** into `docs/ai-sessions/`
- [ ] `ai-transcript.md`: key moments table filled in
- [ ] Redaction: remove only secrets and personal data (e.g. email addresses, tokens, personal paths), and **say so wherever something was redacted**. Check every saved transcript before committing
