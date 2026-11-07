# Spec trace

One row per clause of `obsi-methodology.md`, plus the I/O contract and `--new-ped` clauses from `docs/brief.md` ([Brief contract](#brief-contract)). Clause text is quoted or closely paraphrased from the source. Code and Test columns are filled in as the code is written. Open questions are linked as `D-xx` (see `docs/decisions.md`).

| ID | Section | Clause | Code | Test |
| --- | --- | --- | --- | --- |
| P.1 | Preamble | OBSI measures the USD price of XYZ at effective time T, using the books in one captured snapshot | TBD | TBD |
| P.2 | Preamble | Each snapshot is evaluated independently | TBD | TBD |
| P.3 | Preamble | Baseline orderbook deviation limit is 5% | TBD | TBD |
| 1.1 | §1 | T is `calculationTime` in the input (D-07) | TBD | TBD |
| 1.2 | §1 | Spacing s = 1 XYZ | TBD | TBD |
| 1.3 | §1 | Mid-spread cutoff D = 0.005 (0.5%) | TBD | TBD |
| 1.4 | §1 | v is cumulative XYZ volume, in multiples of s | TBD | TBD |
| 1.5 | §1 | Exchange e is one book in the snapshot (D-02) | TBD | TBD |
| 1.6 | §1 | m_{T,e} is the top-of-book mid, determined before consolidation | TBD | TBD |
| 1.7 | §1 | Median is of structurally valid book mids; mean of middle two when count is even | TBD | TBD |
| 1.8 | §1 | PED is compared with the 5% limit | TBD | TBD |
| 1.9 | §1 | A_T: consolidated asks, ascending price, raw uncapped sizes | TBD | TBD |
| 1.10 | §1 | B_T: consolidated bids, descending price, raw uncapped sizes | TBD | TBD |
| 1.11 | §1 | v_T is determined by Eq. 2 | TBD | TBD |
| 1.12 | §1 | λ = 1/(0.3·v_T) | TBD | TBD |
| 1.13 | §1 | NF is the sum of weights through v_T | TBD | TBD |
| 1.14 | §1 | Prices are USD per XYZ; sizes and depths are in XYZ | TBD | TBD |
| 1.15 | §1 | A_T and B_T are the levels remaining after screens, consolidation and uncrossing | TBD | TBD |
| 1.16 | §1 | No order size cap applies | TBD | TBD |
| 2.1 | §2 | Discard a price/size entry if either component is not numeric (D-01, D-05) | TBD | TBD |
| 2.2 | §2 | Discard a price/size entry if either component is not strictly positive | TBD | TBD |
| 2.3 | §2 | Exclude a book if its structure cannot be read (D-03, D-06) | TBD | TBD |
| 2.4 | §2 | Exclude a book if either side is empty after entry screening | TBD | TBD |
| 2.5 | §2 | Exclude a book if its best bid ≥ its best ask (D-11) | TBD | TBD |
| 2.6 | §2 | Apply these checks to each book before combining exchanges | TBD | TBD |
| 2.7 | §2 | Malformed top-level input is an input error (D-06) | TBD | TBD |
| 2.8 | §2 | Missing required top-level fields are an input error (D-07) | TBD | TBD |
| 2.9 | §2 | For each structurally valid book, m = (b_best + a_best) / 2 | TBD | TBD |
| 2.10 | §2 | Median of those mids; average middle two for an even number of books | TBD | TBD |
| 2.11 | §2 | PED = 100·\|m − median\| / median % | TBD | TBD |
| 2.12 | §2 | Exclude a book only if PED > 5% | TBD | TBD |
| 2.13 | §2 | A book at exactly 5% passes | TBD | TBD |
| 2.14 | §2 | Deviation check per snapshot, using only that snapshot's books | TBD | TBD |
| 2.15 | §2 | No reentry state and no half-limit test | TBD | TBD |
| 3.1 | §3 | Combine the surviving books into one book | TBD | TBD |
| 3.2 | §3 | Sum sizes at identical bid prices and, separately, at identical ask prices | TBD | TBD |
| 3.3 | §3 | Order bids highest to lowest and asks lowest to highest | TBD | TBD |
| 3.4 | §3 | While best bid ≥ best ask, subtract the smaller of their sizes from both levels | TBD | TBD |
| 3.5 | §3 | Remove every level whose residual size becomes zero | TBD | TBD |
| 3.6 | §3 | Repeat until best bid < best ask | TBD | TBD |
| 3.7 | §3 | Calculation fails if either side is exhausted | TBD | TBD |
| 4.1 | §4 Eq. 1a | Unspaced askPV(v) = ap_{j+1} where Σ_{i≤j} as_i < v and Σ_{i≤j+1} as_i ≥ v | TBD | TBD |
| 4.2 | §4 Eq. 1b | Unspaced bidPV(v) = bp_{j+1} where Σ_{i≤j} bs_i < v and Σ_{i≤j+1} bs_i ≥ v | TBD | TBD |
| 4.3 | §4 | j ≥ 0; an empty sum equals zero | TBD | TBD |
| 4.4 | §4 Eq. 1c | askPV(v) = unspaced askPV(s·⌈v/s⌉) | TBD | TBD |
| 4.5 | §4 Eq. 1d | bidPV(v) = unspaced bidPV(s·⌈v/s⌉) | TBD | TBD |
| 4.6 | §4 Eq. 1e | midPV(v) = (askPV(v) + bidPV(v)) / 2 | TBD | TBD |
| 4.7 | §4 Eq. 1f | midSV(v) = askPV(v) / midPV(v) − 1 | TBD | TBD |
| 5.1 | §5 Eq. 2 | v_T = **max** {v_i : midSV(v_i) ≤ D ∧ midSV(v_{i+1}) > D}: the last crossing, not the first | TBD | TBD |
| 5.2 | §5 Eq. 2 | v_i = i·s, i = 1, 2, … | TBD | TBD |
| 5.3 | §5 | Evaluate a candidate only when both curves are defined at v_i and v_{i+1} | TBD | TBD |
| 5.4 | §5 | λ = 1/(0.3·v_T) | TBD | TBD |
| 5.5 | §5 | NF = Σ_{v ∈ {s, 2s, …, v_T}} λe^{−λv} (D-13, D-14) | TBD | TBD |
| 5.6 | §5 Eq. 3 | OBSI = Σ_{v ∈ {s, …, v_T}} midPV(v)·λe^{−λv} / NF | TBD | TBD |
| 5.7 | §5 | Round the published value to 2 dp with Java HALF_UP | TBD | TBD |
| 6.1 | §6 | Fails for malformed required input | TBD | TBD |
| 6.2 | §6 | Fails if no constituent book survives screening (D-12) | TBD | TBD |
| 6.3 | §6 | Fails if uncrossing exhausts either side | TBD | TBD |
| 6.4 | §6 | Fails if the Eq. 2 set is empty: insufficient depth to evaluate consecutive samples | TBD | TBD |
| 6.5 | §6 | Fails if the Eq. 2 set is empty: no cutoff crossing before either curve ends | TBD | TBD |
| 6.6 | §6 | Do not publish an index value for a failed snapshot (D-08) | TBD | TBD |
| 6.7 | §6 | One offline calculation from one supplied snapshot; no collection or publication infrastructure | TBD | TBD |
| 7.1 | §7 | Deviation limit 5% (consistent with P.3, 2.12) | TBD | TBD |
| 7.2 | §7 | D = 0.005 (consistent with 1.3) | TBD | TBD |
| 7.3 | §7 | s = 1 XYZ (consistent with 1.2) | TBD | TBD |
| 7.4 | §7 | λ = 1/(0.3·v_T) (consistent with 1.12, 5.4) | TBD | TBD |
| 7.5 | §7 | Two decimal places, Java HALF_UP (consistent with 5.7) | TBD | TBD |
| R.1 | Reference | Adapted from CME CF RTI Methodology v17.0; the exercise parameters and the per-snapshot deviation check govern | n/a | n/a |

## Brief contract

Clauses from `docs/brief.md` that the code must follow; the spec doesn't define these.

| ID | Section | Clause | Code | Test |
| --- | --- | --- | --- | --- |
| C.1 | Brief CLI | Java 25; takes one local JSON path (D-10) | TBD | TBD |
| C.2 | Brief CLI | `./run.sh data/sample-input.json` and `./run.sh --new-ped data/new-ped.json` work offline with only Java 25, after `build.sh` | TBD | TBD |
| C.3 | Brief CLI | `build.sh` runs first and leaves everything ready for `run.sh` | TBD | TBD |
| I.1 | Brief Input | `calculationTime`: ISO-8601 UTC, time of the snapshot (D-07) | TBD | TBD |
| I.2 | Brief Input | `exchange`: unique, nonempty (D-02, D-03) | TBD | TBD |
| I.3 | Brief Input | `bids` / `asks`: `[[price, size], ...]`; price USD per XYZ, size XYZ, both positive JSON numbers (D-01, D-05) | TBD | TBD |
| O.1 | Brief Output | Success: one JSON object on stdout, exit 0 | TBD | TBD |
| O.2 | Brief Output | Success fields: `index` = `"OBSI"`, `calculationTime` same as input, `status` = `"published"`, `value`, `metadata` | TBD | TBD |
| O.3 | Brief Output | `value` is a string with two decimals (with 5.7) | TBD | TBD |
| O.4 | Brief Output | `metadata.utilizedDepth` is a decimal string in XYZ (v_T, 5.1) | TBD | TBD |
| O.5 | Brief Output | `metadata.excludedExchanges` lists the screened-out exchanges in input order (D-03) | TBD | TBD |
| O.6 | Brief Output | Failure: one JSON object on stderr, nonzero exit (D-08) | `cli/Main.java` (scaffold) | `MainSmokeTest` |
| O.7 | Brief Output | Failure fields: `index` = `"OBSI"`, `calculationTime` same as input or null, `status` = `"failed"`, `reason` | `cli/Main.java` (scaffold) | `MainSmokeTest` |
| O.8 | Brief Output | `calculationTime` is null when the input itself doesn't conform | TBD | TBD |
| O.9 | Brief Output | The result stays on its own stream; diagnostics go somewhere else (D-09) | TBD | TBD |
| N.1 | Brief Task 2 | `--new-ped` runs the approach chosen in `proposal.md` | TBD | TBD |
| N.2 | Brief Task 2 | Without `--new-ped`, the original 5% rule (2.12, 2.13) stays | TBD | TBD |
| N.3 | Brief Task 2 | New or changed input fields are documented in `proposal.md` with a sample in `data/new-ped.json`; if nothing changes, `data/new-ped.json` is a copy of `data/sample-input.json` | TBD | TBD |
