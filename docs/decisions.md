# Open decisions

Questions that neither the spec (`obsi-methodology.md`) nor the brief (`docs/brief.md`) answers. **Agents never decide here.** Each entry stays `Open` until a human resolves it. An entry is `Partly settled by brief` when the brief answers part of it: the quoted text is settled, and the rest stays open.

Format: ID, question, spec/brief reference, options seen so far, status.

## Input

### D-01 String numbers
Is a price or size given as a JSON string (`"100"`) "numeric" for the §2 entry screen, or is it discarded?
- Spec: §2 ¶1 "discard a price/size entry if either component is not numeric".
- Options: accept numeric strings / discard entry.
- Status: Open

### D-02 Duplicate exchange names
What happens when two books share an `exchange` name?
- Spec: silent. §2 treats each book as "a constituent exchange book".
- Options: treat as separate books / merge / input error / exclude both.
- Status: Open

### D-03 Unreadable books in `excludedExchanges`
How does a book whose structure can't be read (§2) show up in `excludedExchanges`?
- Spec: §2 ¶1 "Exclude a book if its structure cannot be read".
- Settled by brief, "Output": `excludedExchanges` sits under `metadata` in the success object, and "excludedExchanges lists the screened-out exchanges in the same order they appear in the input." §2 makes an unreadable structure one of the screens. So a book with a readable `exchange` name is listed by that name, in input order.
- Still open: how a book with no readable or nonempty `exchange` name shows up (omitted / placeholder / index-based label). Also open: whether such a book makes the input non-conforming instead (see D-06; brief, "Input": `exchange` is "unique, nonempty").
- Status: Partly settled by brief

### D-04 Unknown input fields
Are unknown top-level, book-level or entry-level fields ignored or rejected?
- Spec: silent. §2 only says "Malformed top-level input ... are input errors".
- Status: Open

### D-05 Entry shape
The `[price, size]` array shape is only shown by the sample. How are arrays of length ≠ 2, `null`, booleans, `NaN`/`Infinity` and nested arrays handled: discarded entry (§2 entry screen) or unreadable book (§2 book screen)?
- Status: Open

### D-06 Top-level vs book-level malformation
Where is the line between "malformed top-level input" (input error) and "structure cannot be read" (book excluded)? Examples: `books` is not an array; a book is a string; a book is missing `bids`; `bids` is an object.
- Spec: §2 ¶1, §6.
- Status: Open

### D-07 Required top-level fields and `calculationTime`
Which top-level fields are required, what format is `calculationTime`, and is it echoed?
- Spec: §1 names only `calculationTime` ("Effective calculation time"); §2 "missing required top-level fields are input errors".
- Settled by brief, "Input": the top level is `calculationTime` and `books`. `calculationTime` is "ISO-8601 UTC, time of the snapshot". Each book has `exchange` ("unique, nonempty"), `bids` and `asks` as `[[price, size], ...]`. "Price is USD per XYZ, size is XYZ, both positive JSON numbers."
- Settled by brief, "Output": both objects carry `"calculationTime": "<same as input>"`, and on failure "<same as input, or null>". "calculationTime is null when the input itself doesn't conform."
- Still open: is an empty `books` array non-conforming input (null `calculationTime`), or the §6 failure "no constituent book survives screening"? The brief's input example doesn't say whether `books` may be empty.
- Status: Partly settled by brief

## Output and CLI

### D-08 Output schema, error schema, exit codes
The spec defines no output format.
- Spec: §6 "Do not publish an index value for a failed snapshot."
- Settled by brief, "Output":
  - "Success: one JSON object on stdout, exit 0." The fields are `"index": "OBSI"`, `calculationTime`, `"status": "published"`, `"value"`, and `metadata` with `utilizedDepth` and `excludedExchanges`.
  - "value is a string with two decimals. utilizedDepth is a decimal string in XYZ."
  - "Failure: one JSON object on stderr, nonzero exit." The fields are `"index": "OBSI"`, `calculationTime` (null when the input doesn't conform), `"status": "failed"` and `"reason": "<short explanation>"`.
  - Exclusion reasons are not part of the output: `excludedExchanges` lists names only.
- Still open:
  - The exact nonzero exit code values, including whether input errors and calculation failures (§6) get different codes. The scaffold currently uses 1 in `Main` and 2 in `run.sh` when the jar is missing.
  - The wording of `reason`.
- Status: Partly settled by brief

### D-09 Where `--verbose` output goes
Should diagnostic output (intermediate books, curves, v_T) go to stderr, into an extra field in the stdout JSON, or to a file? And is it JSON or text?
- Brief, "Output": "The result stays on its own stream and diagnostics go somewhere else (D6 in decisions.md)." The brief numbers this entry D6. The failure result is on stderr, so stderr is a result stream too. The brief's CLI section doesn't mention `--verbose`.
- Status: Open

### D-10 CLI input
How is the input passed, and what happens with bad arguments?
- Settled by brief, "CLI": "Java 25, takes one local JSON path." The two required invocations are `./run.sh data/sample-input.json` and `./run.sh --new-ped data/new-ped.json`. Input is a path argument, not stdin. `--new-ped` is the only flag the brief names, shown before the path. Brief, "Task 2": "without the flag the original 5% rule stays."
- Still open: behaviour for a missing path, extra arguments, unknown flags, `--new-ped` given after the path, and a path that doesn't exist or can't be read. Is each of these a failure object with null `calculationTime`, or a usage error, and with which exit code (D-08)?
- Status: Partly settled by brief

## Book handling

### D-11 Unsorted levels and duplicate prices within one book
Is "best bid" the highest bid price, or the first entry as given? Are duplicate prices within a single book summed before the §2 checks? (§3 sums only across books.)
- Spec: §1 notation, §2 ¶1, §3 ¶1.
- Status: Open

### D-12 Does "survives screening" (§6) include the PED screen?
§2 groups entry, book and PED checks under "screens", but §2's first paragraph calls only the first set "checks". This matters for which failure reason is reported when every book is a PED outlier.
- Status: Open

## Numerics

### D-13 double vs BigDecimal for the exponential weights
`λe^{-λv}` and `NF` (§5) involve `exp`, which `BigDecimal` has no built-in for. Options: compute weights in `double`; use a BigDecimal series to a fixed precision. HALF_UP at two decimals (§5) is sensitive when the unrounded value sits near `x.xx5`.
- Status: Open

### D-14 MathContext for divisions
What precision and rounding apply to the OBSI sum divided by NF, and to any other non-terminating division? (Medians and mids divide by 2 and are exact. PED and midSV checks avoid division, see AGENTS.md.)
- Status: Open
