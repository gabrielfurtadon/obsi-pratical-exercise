# Brief

Notes from the take-home brief, only the parts the code has to follow. The calculation itself is in
obsi-methodology.md. Anything unclear goes to decisions.md.

## What to build

Task 1 is the methodology applied to one snapshot, with failures reported as described below, and a
README for the next Index Engineer. Task 2 is the PED extension: compare two approaches in proposal.md
and implement the chosen one behind `--new-ped`.

No live feed, publishing or deployment. Readable and testable is the bar.

## CLI

Java 25, takes one local JSON path. The repo root needs executable build.sh and run.sh, mvnw with the
wrapper files, and pom.xml. build.sh runs first and has to leave everything ready for run.sh. After
that, both of these must work offline with just Java 25:

    ./run.sh data/sample-input.json
    ./run.sh --new-ped data/new-ped.json

The reviewer may also run it against other files in data/.

## Input

    {
      "calculationTime": "2026-...Z",          ISO-8601 UTC, time of the snapshot
      "books": [
        {
          "exchange": "A",                     unique, nonempty
          "bids": [[price, size], ...],
          "asks": [[price, size], ...]
        }
      ]
    }

Price is USD per XYZ, size is XYZ, both positive JSON numbers. data/sample-input.json is the reference.

## Output

Success: one JSON object on stdout, exit 0.

    {
      "index": "OBSI",
      "calculationTime": "<same as input>",
      "status": "published",
      "value": "100.00",
      "metadata": {
        "utilizedDepth": "1",
        "excludedExchanges": []
      }
    }

value is a string with two decimals. utilizedDepth is a decimal string in XYZ. excludedExchanges lists
the screened-out exchanges in the same order they appear in the input.

Failure: one JSON object on stderr, nonzero exit.

    {
      "index": "OBSI",
      "calculationTime": "<same as input, or null>",
      "status": "failed",
      "reason": "<short explanation>"
    }

calculationTime is null when the input itself doesn't conform.

The result stays on its own stream and diagnostics go somewhere else (D9 in decisions.md).

## Task 2

What the PM raised in the meeting:

- PED exists to catch an exchange that deviates because of a temporary problem of its own.
- Weekends have less liquidity, so the static limit was set wide to cope. That probably makes it too
  wide on weekdays.
- Some of the newly added exchanges sit at a constant premium over the median. The PM thinks that's
  market structure, not bad data, and wants it left out of PED.
- Token unlocks are announced ahead of time. Liquidity goes up for roughly 48h from the start of the
  unlock, and the PM wants PED much tighter in that window.
- Not everything needs solving. Prefer something with a financial rationale that is easy to explain
  to clients.

What has to be delivered: proposal.md comparing two approaches and explaining the choice (it can cover
only some of the points). `--new-ped` runs the chosen approach; without the flag the original 5% rule
stays. If the input JSON gains or changes fields, document them in proposal.md and put a sample in
data/new-ped.json. If nothing changes, data/new-ped.json is just a copy of sample-input.json.

## Submission

Working code with deterministic tests and both fixtures. proposal.md and README.md, 1000 words max each.
The README covers implemented steps, assumptions, tests, production gaps and why each library was chosen
(keep them small and well known). The full AI sessions go in ai-transcript.md or as native exports,
redacting only secrets and personal data, and saying so when something was redacted. If something isn't
finished, ship it anyway and say what's missing.