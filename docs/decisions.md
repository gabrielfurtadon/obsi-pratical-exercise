# Decisions

Questions that neither the spec (`obsi-methodology.md`) nor the brief (`docs/brief.md`) answers.
**Agents never decide here.** Each entry was resolved by a human; agents may add new entries as `Open`.

General principle: a failure is scoped to the smallest unit that can be safely isolated (entry, then
book, then document), and nothing is accepted silently when an exchange's identity or the integrity
of the data is in doubt.

Format: ID, question, spec/brief reference, decision, rationale, status.

## Input

### D-01 String numbers
Is a price or size given as a JSON string (`"100"`) "numeric" for the §2 entry screen, or is it discarded?
- Spec: §2 ¶1 "discard a price/size entry if either component is not numeric".
- Decision: only JSON numbers (integer or decimal) are numeric. An entry with a string price or size
  is discarded; the rest of the book is kept.
- Rationale: the brief says "both positive JSON numbers". Accepting strings would invite ambiguous
  formats (`"1e3"`, `"100,5"`) into a published index. Numbers are read as BigDecimal, never as double.
- Status: Decided

### D-02 Duplicate exchange names
What happens when two books share an `exchange` name?
- Spec: silent. §2 treats each book as "a constituent exchange book".
- Decision: duplicate exchange names are an input error.
- Rationale: the brief requires names to be unique. Keeping both would double-weight an exchange in the
  median and the consolidated book; excluding them would hide a feed problem and make
  `excludedExchanges` ambiguous. For a benchmark, not publishing beats publishing a wrong value. In
  production this should raise an alert instead of only failing (README, production gaps).
- Status: Decided

### D-03 Unreadable books in `excludedExchanges`
How does a book whose structure can't be read (§2) show up in `excludedExchanges`?
- Spec: §2 ¶1 "Exclude a book if its structure cannot be read".
- Brief, "Output": "excludedExchanges lists the screened-out exchanges in the same order they appear in
  the input."
- Decision: a book with a valid exchange name but unreadable bids or asks is excluded from the
  calculation, listed in `excludedExchanges`, and the index is calculated from the other books. A book
  whose exchange name is missing, empty or not a string is an input error.
- Rationale: a book can only be excluded and reported if it can be identified. Omitting an unnamed
  book would hide that data was dropped, and a placeholder label would invent an identifier in the
  published metadata. The brief also requires a nonempty name.
- Status: Decided

### D-04 Unknown input fields
Are unknown top-level, book-level or entry-level fields ignored or rejected?
- Spec: silent. §2 only says "Malformed top-level input ... are input errors".
- Decision: unknown fields are ignored at every level.
- Rationale: tolerant reader. It also keeps `./run.sh data/new-ped.json` working without `--new-ped`
  if the Task 2 input adds fields.
- Status: Decided

### D-05 Entry shape
How are arrays of length ≠ 2, `null`, booleans, `NaN`/`Infinity` and nested arrays handled?
- Decision: an entry that is not an array of exactly two elements, or that contains `null`, a boolean
  or a nested array, is discarded (§2 entry screen). `NaN` and `Infinity` are not valid JSON, so the
  whole document is an input error (Jackson's `ALLOW_NON_NUMERIC_NUMBERS` stays off).
- Rationale: smallest scope for problems inside a book; invalid JSON can't be parsed at all.
- Status: Decided

### D-06 Top-level vs book-level malformation
Where is the line between "malformed top-level input" (input error) and "structure cannot be read"
(book excluded)?
- Spec: §2 ¶1, §6.
- Decision: input error if the file isn't valid JSON, the root isn't an object, `calculationTime` is
  missing or not valid ISO-8601 UTC, `books` is missing or not an array, or a `books` element isn't an
  object with a valid exchange name (D-03). A book with a valid exchange name but a missing or
  non-array `bids`/`asks` is excluded and listed in `excludedExchanges`.
- Rationale: see the general principle. A book can only be excluded if it can be identified.
- Status: Decided

### D-07 Required top-level fields and `calculationTime`
Which top-level fields are required, what format is `calculationTime`, and is it echoed?
- Spec: §1, §2 "missing required top-level fields are input errors". Brief, "Input" and "Output".
- Decision: `calculationTime` and `books` are required; a missing field is an input error.
  `calculationTime` must parse as an ISO-8601 UTC instant (`Instant.parse`), and the output echoes the
  original string unchanged. An empty `books` array is valid input and fails as §6 "no constituent book
  survives screening" (exit 1, `calculationTime` echoed). Every input error reports `calculationTime`
  as null.
- Rationale: the brief says "same as input", so the value is validated but never reformatted. An empty
  snapshot is a market state, not malformed data, and §6 already covers it. Null on every input error
  follows the brief literally ("null if non-conforming input"); echoing a readable `calculationTime`
  would help debugging but reads more into the brief.
- Status: Decided

### D-15 `calculationTime` must be UTC, not just parseable
D-07 says "ISO-8601 UTC instant (`Instant.parse`)", but on JDK 25 `Instant.parse` also accepts offsets
(`2026-10-27T12:00:00+01:00`), a lowercase `z` and `T24:00:00Z`.
- Spec: §1 (T is `calculationTime`). Brief, "Input": "ISO-8601 UTC".
- Decision: the value must be a JSON string that `Instant.parse` accepts **and** that ends in an
  uppercase `Z`. Offsets and a lowercase `z` are an input error. `T24:00:00Z` is still accepted, because
  `Instant.parse` accepts it. The original string is echoed unchanged (D-07).
- Rationale: the brief says UTC; the parenthetical in D-07 names the parser, not the whole rule.
- Status: Decided (resolved by the user during Phase 1 planning; the rationale wording is the agent's)

### D-16 Exchange name: blank and duplicate comparison
D-02 and D-03 require names to be unique and nonempty, but not how names are compared or whether a
whitespace-only name is empty.
- Spec: silent. Brief, "Input": "unique, nonempty".
- Decision: a name that is empty or whitespace-only (`String.isBlank()`) is an input error. For the
  duplicate check (D-02), names are compared after `strip()` and `toLowerCase(Locale.ROOT)` (see D-18),
  so `Kraken`, `kraken` and `Kraken ` are duplicates. Names in the output and in `excludedExchanges` are
  always kept exactly as given.
- Rationale: two names that differ only in case or padding are almost certainly the same exchange, and
  keeping both would double-weight it (D-02). The identifier that is printed is never altered.
- Status: Decided (resolved by the user during Phase 1 planning; the rationale wording is the agent's)

### D-17 Duplicate keys inside a JSON object
Jackson resolves a repeated key (two `bids`, two `exchange`) silently as last-wins.
- Spec: silent. D-04 and D-06 cover unknown fields and malformation, not repeated keys.
- Decision: a duplicate key in any object is invalid JSON, so the whole document is an input error
  (Jackson `STRICT_DUPLICATE_DETECTION`).
- Rationale: general principle: nothing is accepted silently when the integrity of the data is in doubt.
- Status: Decided (resolved by the user during Phase 1 planning; the rationale wording is the agent's)

### D-18 Whitespace definition in D-16
D-16 says duplicate names are compared "trimmed", and blank names are rejected. Which characters count as
whitespace? `trim()` only removes characters up to U+0020, while `isBlank()` (the blank check) uses
Unicode whitespace, so with `trim()` a name padded with an em space (U+2003) would not be a duplicate of
the unpadded name.
- Spec: silent.
- Decision: the duplicate check uses `strip()` instead of `trim()`. `strip()` and `isBlank()` use the same
  whitespace definition (`Character.isWhitespace`), so both rules agree on what a space is. `Kraken` and
  `Kraken` followed by U+2003 are duplicates. U+00A0 (no-break space) is outside that definition for both
  rules: it is neither stripped nor counted as blank. That is accepted, not handled.
- Rationale: one definition of "space" for both rules, instead of two that disagree on non-ASCII spaces.
- Status: Decided (resolved by the user)

### D-19 Reason text when no book survives screening
D-08 gave `no constituent book survived screening (all excluded by PED)` as an example and D-12 says the
reason must say when the exclusions were by PED. Open were what triggers the PED wording when structural
and PED exclusions are mixed, and what the reason is when PED did not cause the failure.
- Spec: §6 "if no constituent book survives screening". D-12 ("every structurally valid book is
  excluded by PED").
- Decision: D-12's reading.
  - If at least one book was structurally valid and every structurally valid book was excluded by PED,
    the reason is `no constituent book survived screening (every structurally valid book was excluded by
    PED)`. Other books excluded structurally do not change this, so it covers both two books at 100 and
    120 alone and the same two plus an unreadable book.
  - If no book was structurally valid (all books unreadable, with an empty side or crossed), or `books`
    is empty, PED never ran and the reason is `no constituent book survived screening`, with no suffix.
- Rationale: the PED suffix explains the failure when PED is what removed the last candidates.
  Structural exclusions are reported separately in `excludedExchanges` and are not the deciding cause.
- Status: Decided (resolved by the user)

### D-20 Uncrossing cannot exhaust a side after §2; reasons for the failure
§3 and §6 fail the calculation when uncrossing exhausts a side, and D-08 only gives the ask-side reason as
an example. Open were the wording for the other cases, and what to do with a failure that cannot occur.
- Spec: §3 ¶2 "The calculation fails if either side is exhausted"; §6; §2 (best bid < best ask per book).
- Finding: after the §2 screens the failure is unreachable. Every surviving book has best bid < best ask,
  so every bid level is strictly below its own book's best ask, which is at most the highest consolidated
  ask. No bid is ever at or above the highest ask level, so that level is never consumed and the ask side
  is never exhausted. Symmetrically, every ask level is strictly above its own book's best bid, which is at
  least the lowest consolidated bid, so the lowest bid level is never consumed. This holds for any number
  of books, sizes and rounds. `ConsolidatorPropertyTest` checks it on 400 random snapshots.
- Decision:
  - The failure branch is kept as the spec requires. `Consolidator.uncross(bids, asks)` is package-private
    so that the branch is tested with sides built by hand, since no valid book reaches it.
  - The reasons are `uncrossing exhausted the bid side`, `uncrossing exhausted the ask side`, and
    `uncrossing exhausted both sides` when the last crossing empties both at once.
  - An empty survivor list passed to `Consolidator.consolidate` is a caller bug and throws
    `IllegalArgumentException`. The screener fails with §6 before it would pass one on.
- Rationale: the spec requires the check even though §2 makes it unreachable, so the code follows the spec
  and says why it cannot fire. Naming the side that was exhausted costs nothing and helps whoever reads the
  failure.
- Status: Decided (resolved by the user)

### D-21 Empty Eq. 2 set: reasons, weight arithmetic, and a scan that stops at the first crossing
§6 says the calculation fails when the Eq. 2 set is empty and names two cases ("insufficient depth to
evaluate consecutive samples" and "no mid-spread cutoff crossing before either curve ends"). D-08 gives only
the second as an example string. D-13 and D-14 fix the numeric types but not the order of operations. Eq. 2
takes a maximum over every sample, which on a deep book is a long walk.
- Spec: §4 Eq. 1a-1f, §5 Eq. 2 and Eq. 3, §6; D-08, D-13, D-14.
- Decision:
  - Reasons. `empty Eq. 2 set: insufficient depth to evaluate consecutive samples` when fewer than two
    whole-unit samples (s = 1) are defined on both curves. `empty Eq. 2 set: no mid-spread crossing before a
    curve ends` for every other empty set, including a book that is already above D at v = 1.
  - Weight arithmetic. lambda = 1 / (0.3 · v_T) is a BigDecimal division at DECIMAL128, converted to double once.
    Each weight lambda·e^(-lambda·v) is computed in double with `StrictMath.exp` and converted exactly with
    `new BigDecimal(double)`. The numerator Σ midPV(v)·weight and NF are exact BigDecimal sums, followed by one
    DECIMAL128 division. The result is rounded once, HALF_UP, to two decimals. The cutoff is tested without
    division as `askPV <= (1 + D) · midPV`, and "equal" counts as within.
  - Monotonicity. midSV(v) = askPV/midPV - 1 = (a - b) / (a + b) for the ask a and bid b at volume v. This
    increases with a and decreases with b. As v grows the ask never falls and the bid never rises (the
    consolidated asks are ascending and the bids descending), so midSV never decreases from one sample to the
    next. Hence the samples with midSV <= D form a prefix, and the Eq. 2 set has at most one element: the last
    sample of that prefix, if the next sample is defined and above D. Eq. 2 takes the maximum of a set with at
    most one element, so it is that element.
  - The scan stops at the first crossing, and stops at once if v = 1 is already above D, because no later
    sample can be within D. This equals the literal maximum, and its cost depends on v_T and not on the depth of
    the book. `UtilizedDepth.find` does this. A test keeps the literal full scan of Eq. 2, with real division at
    60 digits, as an oracle and compares the two on 500 random books (`IndexPropertyTest`). A second test checks
    the monotonicity premise on the same books.
- Rationale: the two reasons follow §6's two bullets, so a reader can tell a thin book from a wide one. Summing
  before the single division keeps a constant midPV exact, which matters at an x.xx5 rounding boundary. Stopping
  early is provably equal to the literal maximum, so the spec is not changed, and a book with a very large size
  no longer costs time proportional to that size.
- Not covered: a book that stays within D over a very large depth is still walked to its end before it fails;
  that is inherent to "no crossing before a curve ends". Listed as a production gap for the README.
- Status: Decided (resolved by the user)

### D-22 CLI wiring: usage errors, unexpected errors, encoding and `run.sh`
D-08 fixes the exit codes and D-10 the argument form. Open were the wording of usage errors, what an
unexpected error reports, and a few wiring details the brief does not cover.
- Spec: none (CLI behaviour: brief, "CLI" and "Output"); D-07, D-08, D-09, D-10.
- Decision:
  - Usage errors (exit 2, `calculationTime` null) are prefixed `usage error: `: `missing input path`,
    `more than one input path`, `unknown option <arg>`, `invalid input path` (a string the system cannot name as
    a path), each with `(usage: [--new-ped] <path>)` where it helps. The arguments are checked before the file is
    read, in this order: an unknown option, then the path count, then `--new-ped`.
  - Until Task 2, `--new-ped` is recognised and refused with exit 2 and
    `usage error: --new-ped is not implemented yet`, wherever it appears and even if the path is bad. Giving it
    twice is the same as once.
  - An unexpected error (exit 3) reports `internal error: <SimpleClassName>: <message>`, with the message omitted
    when null, newlines replaced by spaces, and the whole reason cut at 200 characters. `calculationTime` is
    echoed if the input had already been accepted, otherwise null.
  - The catch-all in `Main` catches `Throwable`, not only `Exception`, so an `Error` such as a stack overflow
    also ends as failure JSON and never a stack trace. This is best effort: under a real `OutOfMemoryError`,
    building or printing the JSON may itself fail, and then only the exit code 3 is left.
  - A `PrintStream` never throws, so after writing the result `Main` checks `out.checkError()`. If stdout
    could not be written (a closed pipe, say) the result was not published: the exit code is 3 and the reason
    `internal error: could not write the result to stdout` goes to stderr, best effort (if stderr is broken
    too, only the exit code is left). The write is not retried, and bytes that did get through before the
    error stay written.
  - A result is built completely before anything is written, so stdout never holds part of a result. Each
    stream gets one JSON object on one line ended by a single `\n` (not the platform line separator).
  - `main()` writes through UTF-8 `PrintStream`s, so exchange names come out as given whatever the platform's
    default encoding.
  - `run.sh` exits 3 (not 2) when `target/obsi.jar` is missing, as D-08 says.
- Rationale: a caller can tell bad usage from bad data from a bug by the exit code and the prefix, and the
  one-JSON-object contract (D-09) holds on every path. Echoing `calculationTime` where it is known helps whoever
  reads an internal error, and D-07 only requires null for input errors.
- Not covered: the uncrossing-exhausted failure (D-20) cannot be reached through the CLI, so it has no CLI test;
  `ObsiCalculator` passes its reason through. A `--` end-of-options marker is not supported, so an input path
  cannot start with `-`.
- Status: Decided (resolved by the user)

## Output and CLI

### D-08 Exit codes and `reason`
The output and failure shapes are settled by the brief ("Output"). Open were the exit code values and
the wording of `reason`.
- Decision:
  - `0` published; `1` calculation failure (§6); `2` input or usage error; `3` unexpected internal
    error. `run.sh` uses `3` when the JAR is missing.
  - `Main` catches every exception, so a failure always produces the failure JSON, never a stack trace.
  - `reason` is short, stable and specific, prefixed by its kind, e.g.
    `input error: books must be an array`,
    `no constituent book survived screening (every structurally valid book was excluded by PED)`,
    `uncrossing exhausted the ask side`,
    `empty Eq. 2 set: no mid-spread crossing before a curve ends`.
- Rationale: distinct codes let a caller tell bad data from a legitimate no-publish without parsing
  text; the catch-all keeps the stderr contract even on bugs.
- Status: Decided

### D-09 Diagnostics
Where does diagnostic output go, given that a failure result already uses stderr?
- Brief, "Output": "The result stays on its own stream and diagnostics go somewhere else."
- Decision: the program writes nothing to stdout or stderr except the single result JSON. There is no
  `--verbose`. Intermediate state (screened books, curves, v_T) is covered by tests, and a trace output
  is listed as a production gap.
- Rationale: stderr carries the failure result, so any log line there would break the
  one-JSON-object contract. The JVM itself may print to stderr (e.g. "Picked up JAVA_TOOL_OPTIONS");
  that is outside the program's control and noted in the README.
- Status: Decided

### D-10 CLI arguments
Settled by brief, "CLI": one local JSON path, plus the `--new-ped` flag.
- Decision: the form is `[--new-ped] <path>`; the flag is accepted before or after the path. An unknown
  flag, no path or more than one path is a usage error (exit 2, `calculationTime` null). A path that
  doesn't exist or can't be read is an input error (exit 2).
- Rationale: strict about what is invalid, lenient about harmless order, so a reviewer typing the flag
  after the path still gets a result.
- Status: Decided

## Book handling

### D-11 Unsorted levels and duplicate prices within one book
Is "best bid" the highest bid price, or the first entry as given? Are duplicate prices within a single
book summed?
- Spec: §1 notation, §2 ¶1, §3 ¶1.
- Decision: best bid is the highest bid price and best ask the lowest ask price after the entry screen,
  regardless of input order. Duplicate prices don't affect the §2 checks (best prices and mid are the
  same). In §3, sizes at identical prices are summed across all surviving levels, whether they come
  from one book or several.
- Rationale: the brief doesn't guarantee sorted levels and the spec defines "best" by price. Summing
  identical prices everywhere is the plain reading of "sum sizes at identical bid prices".
- Status: Decided

### D-12 Does "survives screening" (§6) include the PED screen?
- Decision: yes. A snapshot where every structurally valid book is excluded by PED fails as §6 "no
  constituent book survives screening", and `reason` says the exclusions were by PED.
- Rationale: PED is part of §2, "Constituent order book screens". The case is reachable: with an even
  number of books the median is the mean of the middle two, so two books at 100 and 120 (median 110,
  both 9.09% away) are both excluded. With an odd count the median book has PED 0 and always survives.
- Status: Decided

## Numerics

### D-13 double vs BigDecimal for the exponential weights
`λe^{-λv}` and `NF` (§5) need `exp`, which BigDecimal doesn't provide.
- Decision: each weight `λe^{-λv}` is computed with `StrictMath.exp` in double and converted exactly
  with `new BigDecimal(double)`. Every sum and product after that is BigDecimal.
- Rationale: double carries about 16 significant digits; with an index near 100 the error is around
  1e-14, so HALF_UP at two decimals only changes if the exact value sits within that distance of
  `x.xx5`. `StrictMath` is used instead of `Math` because its results are bit-for-bit reproducible
  across JVMs and CPUs, which a published index and deterministic tests require. A BigDecimal Taylor
  series would add precision that never changes the published value, at the cost of more code to get
  wrong. The residual boundary risk is documented in the README. λ cancels between the weights and NF;
  the formula is still implemented as written in §5.
- Status: Decided

### D-14 MathContext for divisions
- Decision: divisions by 2 (mids, median) are exact and use no MathContext. The division by NF, and
  any other non-terminating division, uses `MathContext.DECIMAL128` (34 digits). The value is rounded
  once, at the end, with `setScale(2, RoundingMode.HALF_UP)`; intermediate values are never rounded.
  `utilizedDepth` is an integer with s = 1 and is written with `toPlainString()` (e.g. `"12"`).
- Rationale: 34 digits is far beyond the two published decimals, and a single final rounding is what
  §5 specifies.
- Status: Decided