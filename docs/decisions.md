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
    `no constituent book survived screening (all excluded by PED)`,
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