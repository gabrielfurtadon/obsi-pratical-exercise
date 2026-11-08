# Spec trace

One row per clause of `obsi-methodology.md`, plus the I/O contract and `--new-ped` clauses from `docs/brief.md` ([Brief contract](#brief-contract)). Clause text is quoted or closely paraphrased from the source. Code and Test columns are filled in as the code is written. Open questions are linked as `D-xx` (see `docs/decisions.md`).

| ID | Section | Clause | Code | Test |
| --- | --- | --- | --- | --- |
| P.1 | Preamble | OBSI measures the USD price of XYZ at effective time T, using the books in one captured snapshot | TBD | TBD |
| P.2 | Preamble | Each snapshot is evaluated independently | TBD | TBD |
| P.3 | Preamble | Baseline orderbook deviation limit is 5% | `deviation/StaticPedFilter` (`LIMIT_PERCENT` = 5) | `StaticPedFilterTest` |
| 1.1 | §1 | T is `calculationTime` in the input (D-07) | `input/InputParser` (read, validated, echoed as given; D-15) | `InputParserTest` |
| 1.2 | §1 | Spacing s = 1 XYZ | `curve/PriceVolumeCurve.SPACING` | `PriceVolumeCurveTest` (K3), `UtilizedDepthTest` |
| 1.3 | §1 | Mid-spread cutoff D = 0.005 (0.5%) | `curve/MidCurve.CUTOFF` | `MidCurveTest`, `UtilizedDepthTest` (exactly at D and just above) |
| 1.4 | §1 | v is cumulative XYZ volume, in multiples of s | `curve/PriceVolumeCurve.unspaced` / `priceAt`; `index/UtilizedDepth` samples v_i = i·s | `PriceVolumeCurveTest`, `PriceVolumeCurvePropertyTest` |
| 1.5 | §1 | Exchange e is one book in the snapshot (D-02) | `input/InputParser`, `BookInput` (one book per element; duplicate names rejected; D-16) | `InputParserTest` |
| 1.6 | §1 | m_{T,e} is the top-of-book mid, determined before consolidation | `book/ValidBook.mid` (exact; D-14) | `ValidBookTest`, `ScreenerTest` |
| 1.7 | §1 | Median is of structurally valid book mids; mean of middle two when count is even | `deviation/StaticPedFilter.median` over the structurally valid books only (`screening/Screener`) | `StaticPedFilterTest`, `ScreenerTest` (crossed, unreadable and empty-side books don't count) |
| 1.8 | §1 | PED is compared with the 5% limit | `deviation/StaticPedFilter.deviatesMoreThanTheLimit` | `StaticPedFilterTest`, `StaticPedFilterPropertyTest` |
| 1.9 | §1 | A_T: consolidated asks, ascending price, raw uncapped sizes | `book/ConsolidationResult.Consolidated.asks` (built by `book/Consolidator`; sizes summed, never capped) | `ConsolidatorTest`, `ConsolidatorPropertyTest` |
| 1.10 | §1 | B_T: consolidated bids, descending price, raw uncapped sizes | `book/ConsolidationResult.Consolidated.bids` (built by `book/Consolidator`; sizes summed, never capped) | `ConsolidatorTest`, `ConsolidatorPropertyTest` |
| 1.11 | §1 | v_T is determined by Eq. 2 | `index/UtilizedDepth.find` (D-21) | `UtilizedDepthTest`, `IndexPropertyTest` |
| 1.12 | §1 | λ = 1/(0.3·v_T) | `index/ObsiIndex.weightedMean` (D-13, D-14, D-21) | `ObsiIndexTest` (v_T = 2 closed form) |
| 1.13 | §1 | NF is the sum of weights through v_T | `index/ObsiIndex.weightedMean` (`normalizationFactor`) | `ObsiIndexTest` (v_T = 1 and 2, constant-mid cases) |
| 1.14 | §1 | Prices are USD per XYZ; sizes and depths are in XYZ | TBD | TBD |
| 1.15 | §1 | A_T and B_T are the levels remaining after screens, consolidation and uncrossing | `book/Consolidator.consolidate` on the screener's survivors (screens: rows 2.x; consolidation and uncrossing: rows 3.x) | `ConsolidatorTest` (sample and edge-cases fixtures end to end) |
| 1.16 | §1 | No order size cap applies | `book/Consolidator` (no cap anywhere; sizes are summed and reduced exactly) | `ConsolidatorTest` (the consolidated sizes are the raw sums, e.g. 99.8 → 2.3); `curve/PriceVolumeCurve` uses those sizes as they are, no cap |
| 2.1 | §2 | Discard a price/size entry if either component is not numeric (D-01, D-05) | `input/InputParser.readLevels` (implements this clause fully: only JSON numbers are numeric; "strictly positive" is row 2.2, phase 2) | `InputParserTest` |
| 2.2 | §2 | Discard a price/size entry if either component is not strictly positive | `screening/BookScreen.strictlyPositive` | `EntryScreenTest`, `ScreenerPropertyTest` |
| 2.3 | §2 | Exclude a book if its structure cannot be read (D-03, D-06) | `input/InputParser.readBook` yields `UnreadableBook`; `screening/Screener` excludes it | `InputParserTest`, `BookScreenTest` |
| 2.4 | §2 | Exclude a book if either side is empty after entry screening | `screening/BookScreen.structurallyValid` | `BookScreenTest`, `ScreenerPropertyTest` |
| 2.5 | §2 | Exclude a book if its best bid ≥ its best ask (D-11) | `screening/BookScreen.structurallyValid` (best by price, D-11) | `BookScreenTest`, `ScreenerPropertyTest` |
| 2.6 | §2 | Apply these checks to each book before combining exchanges | `screening/BookScreen`, applied per book before `Screener` combines anything | `BookScreenTest` (a bad book doesn't affect its neighbours) |
| 2.7 | §2 | Malformed top-level input is an input error (D-06) | `input/InputParser` returns `InputResult.Rejected` (D-15, D-16, D-17) | `InputParserTest`, `InputParserPropertyTest` |
| 2.8 | §2 | Missing required top-level fields are an input error (D-07) | `input/InputParser` (`calculationTime`, `books`) | `InputParserTest` |
| 2.9 | §2 | For each structurally valid book, m = (b_best + a_best) / 2 | `book/ValidBook.mid` | `ValidBookTest`, `ScreenerTest` |
| 2.10 | §2 | Median of those mids; average middle two for an even number of books | `deviation/StaticPedFilter.median` | `StaticPedFilterTest`, `StaticPedFilterPropertyTest` |
| 2.11 | §2 | PED = 100·\|m − median\| / median % | `deviation/StaticPedFilter.deviatesMoreThanTheLimit` (division-free: `abs(m − med)·100 > 5·med`) | `StaticPedFilterTest`, `StaticPedFilterPropertyTest` (60-digit division oracle) |
| 2.12 | §2 | Exclude a book only if PED > 5% | `deviation/StaticPedFilter.deviatesMoreThanTheLimit` | `StaticPedFilterTest`, `StaticPedFilterPropertyTest` |
| 2.13 | §2 | A book at exactly 5% passes | `deviation/StaticPedFilter.deviatesMoreThanTheLimit` (equal passes) | `StaticPedFilterTest` (exact ±5% incl. cases where double arithmetic fails), `ScreenerFixturesTest` |
| 2.14 | §2 | Deviation check per snapshot, using only that snapshot's books | `deviation/StaticPedFilter` (stateless; judges only the books it is given) | `StaticPedFilterTest` |
| 2.15 | §2 | No reentry state and no half-limit test | `deviation/StaticPedFilter` (no state, no half-limit) | `StaticPedFilterTest` |
| 3.1 | §3 | Combine the surviving books into one book | `book/Consolidator.consolidate` (flattens every survivor's levels; an empty list is a caller bug, D-20) | `ConsolidatorTest` (sample, edge-cases, empty list) |
| 3.2 | §3 | Sum sizes at identical bid prices and, separately, at identical ask prices (D-11) | `book/Consolidator.uncross` (`TreeMap.merge` with `BigDecimal::add`; `compareTo` keys, so 100.0 and 100 merge) | `ConsolidatorTest` (scales, same book and across books, 0.1 + 0.2, bid and ask at one price stay separate), `ConsolidatorPropertyTest` |
| 3.3 | §3 | Order bids highest to lowest and asks lowest to highest | `book/Consolidator.uncross` (bids `TreeMap` in reverse natural order, asks in natural order) | `ConsolidatorTest`, `ConsolidatorPropertyTest` (strict order, input order irrelevant) |
| 3.4 | §3 | While best bid ≥ best ask, subtract the smaller of their sizes from both levels | `book/Consolidator.uncross` (`compareTo(...) >= 0`; `min` of the two sizes) | `ConsolidatorTest` (equal price, unequal sizes, several rounds, edge-cases fixture at 104.9 / 100.05), `ConsolidatorPropertyTest` (equal volume removed from both sides); mutation-checked (`>=` to `>`, `min` to `max`) |
| 3.5 | §3 | Remove every level whose residual size becomes zero | `book/Consolidator.reduce` (`signum() == 0`, so a residual of `0.0` goes too) | `ConsolidatorTest` (zero residual at a different scale), `ConsolidatorPropertyTest` (all sizes positive); mutation-checked (`equals(ZERO)` loops forever, caught by the suite-wide 10 s timeout) |
| 3.6 | §3 | Repeat until best bid < best ask | `book/Consolidator.uncross` (loop condition) | `ConsolidatorTest`, `ConsolidatorPropertyTest` (best bid below best ask) |
| 3.7 | §3 | Calculation fails if either side is exhausted (D-20: unreachable after §2) | `book/Consolidator.uncross` returns `ConsolidationResult.Failed` (bid, ask or both sides) | `ConsolidatorTest` (hand-built sides), `ConsolidatorPropertyTest` (valid books never fail) |
| 4.1 | §4 Eq. 1a | Unspaced askPV(v) = ap_{j+1} where Σ_{i≤j} as_i < v and Σ_{i≤j+1} as_i ≥ v | `curve/PriceVolumeCurve.unspaced` (exact cumulative sums, first level with sum ≥ v by binary search; empty beyond the depth) | `PriceVolumeCurveTest` (fractional sizes reach 100.4; sum exactly equal to v; beyond depth), `PriceVolumeCurvePropertyTest` (unit-by-unit oracle); mutation-checked (`≥` to `>`) |
| 4.2 | §4 Eq. 1b | Unspaced bidPV(v) = bp_{j+1} where Σ_{i≤j} bs_i < v and Σ_{i≤j+1} bs_i ≥ v | `curve/PriceVolumeCurve.unspaced` (same code on the bid side, levels best first) | `PriceVolumeCurveTest` (bid at v = 2 is 100.0, not 99.8), `PriceVolumeCurvePropertyTest` |
| 4.3 | §4 | j ≥ 0; an empty sum equals zero | `curve/PriceVolumeCurve.unspaced` (a `v ≤ 0` is rejected; the first level is reached when its own size ≥ v) | `PriceVolumeCurveTest` (invalid arguments; 0.1 + 0.2 reaches 0.3) |
| 4.4 | §4 Eq. 1c | askPV(v) = unspaced askPV(s·⌈v/s⌉) | `curve/PriceVolumeCurve.priceAt` (`setScale(0, CEILING)`; undefined once the rounded v is beyond the depth) | `PriceVolumeCurveTest` (0.5 and 1 agree, 1.01 → 2, 12.1 undefined at depth 12.3), `PriceVolumeCurvePropertyTest`; mutation-checked (ceil to floor) |
| 4.5 | §4 Eq. 1d | bidPV(v) = unspaced bidPV(s·⌈v/s⌉) | `curve/PriceVolumeCurve.priceAt` (same code on the bid side) | `PriceVolumeCurveTest`, `PriceVolumeCurvePropertyTest` |
| 4.6 | §4 Eq. 1e | midPV(v) = (askPV(v) + bidPV(v)) / 2 | `curve/MidCurve.midPV` (exact, D-14) | `MidCurveTest` |
| 4.7 | §4 Eq. 1f | midSV(v) = askPV(v) / midPV(v) − 1 | `curve/MidCurve.midSvWithinCutoff` (division-free: `askPV ≤ (1 + D)·midPV`) | `MidCurveTest` (exactly D is within, just above is not), `IndexPropertyTest` (agrees with real division on every sample); mutation-checked (`≤` to `<`) |
| 5.1 | §5 Eq. 2 | v_T = **max** {v_i : midSV(v_i) ≤ D ∧ midSV(v_{i+1}) > D}: the last crossing, not the first | `index/UtilizedDepth.find` (stops at the first crossing, which is the only one: midSV never decreases, D-21) | `UtilizedDepthTest` (sample 4, edge-cases 6), `IndexPropertyTest` (equals the literal full-scan max on 500 random books; midSV monotone) |
| 5.2 | §5 Eq. 2 | v_i = i·s, i = 1, 2, … | `index/UtilizedDepth.find` (samples start at i = 1) | `UtilizedDepthTest` |
| 5.3 | §5 | Evaluate a candidate only when both curves are defined at v_i and v_{i+1} | `index/UtilizedDepth.commonSamples`, `find` (i + 1 ≤ common samples) | `UtilizedDepthTest` (bids end at 4.5, depth 1.99, depth exactly 2); mutation-checked (last pair dropped) |
| 5.4 | §5 | λ = 1/(0.3·v_T) | `index/ObsiIndex.weightedMean` (DECIMAL128, then double; D-21) | `ObsiIndexTest` (v_T = 2 closed form) |
| 5.5 | §5 | NF = Σ_{v ∈ {s, 2s, …, v_T}} λe^{−λv} (D-13, D-14) | `index/ObsiIndex.weightedMean` (`StrictMath.exp`, `new BigDecimal(double)`, exact sum) | `ObsiIndexTest` |
| 5.6 | §5 Eq. 3 | OBSI = Σ_{v ∈ {s, …, v_T}} midPV(v)·λe^{−λv} / NF | `index/ObsiIndex.weightedMean` (Σ midPV·weight, one DECIMAL128 division by NF) | `ObsiIndexTest` (sample ≈ 100.2255, edge-cases ≈ 100.0331), `IndexPropertyTest` (value between the smallest and largest midPV) |
| 5.7 | §5 | Round the published value to 2 dp with Java HALF_UP | `index/ObsiIndex.calculate` (`setScale(2, HALF_UP)`, once); `IndexResult.Published` enforces scale 2 | `ObsiIndexTest` (100.005 → 100.01, 100.025 → 100.03, 100.0049 → 100.00), `IndexResultTest`; mutation-checked (HALF_UP to HALF_EVEN) |
| 6.1 | §6 | Fails for malformed required input | `input/InputParser` returns a typed `Rejected(reason)` (parse part only; the failure JSON and exit code are phase 5) | `InputParserTest` |
| 6.2 | §6 | Fails if no constituent book survives screening (D-12) | `screening/Screener` returns `ScreeningResult.Failed` (reasons per D-12, D-19; failure JSON and exit code are phase 5) | `ScreenerTest` |
| 6.3 | §6 | Fails if uncrossing exhausts either side | `book/Consolidator.uncross` returns `ConsolidationResult.Failed` (reasons per D-08, D-20; failure JSON and exit code are phase 5) | `ConsolidatorTest` |
| 6.4 | §6 | Fails if the Eq. 2 set is empty: insufficient depth to evaluate consecutive samples | `index/ObsiIndex.calculate` returns `IndexResult.Failed` (reason per D-21; failure JSON and exit code are phase 5) | `ObsiIndexTest`, `UtilizedDepthTest` |
| 6.5 | §6 | Fails if the Eq. 2 set is empty: no cutoff crossing before either curve ends | `index/ObsiIndex.calculate` returns `IndexResult.Failed` (reason per D-08, D-21; failure JSON and exit code are phase 5) | `ObsiIndexTest`, `UtilizedDepthTest`, `IndexPropertyTest` |
| 6.6 | §6 | Do not publish an index value for a failed snapshot (D-08) | TBD | TBD |
| 6.7 | §6 | One offline calculation from one supplied snapshot; no collection or publication infrastructure | TBD | TBD |
| 7.1 | §7 | Deviation limit 5% (consistent with P.3, 2.12) | `deviation/StaticPedFilter.LIMIT_PERCENT` | `StaticPedFilterTest` |
| 7.2 | §7 | D = 0.005 (consistent with 1.3) | `curve/MidCurve.CUTOFF` | `MidCurveTest` |
| 7.3 | §7 | s = 1 XYZ (consistent with 1.2) | `curve/PriceVolumeCurve.SPACING` | `PriceVolumeCurveTest` |
| 7.4 | §7 | λ = 1/(0.3·v_T) (consistent with 1.12, 5.4) | `index/ObsiIndex.LAMBDA_FACTOR`, `weightedMean` | `ObsiIndexTest` |
| 7.5 | §7 | Two decimal places, Java HALF_UP (consistent with 5.7) | `index/ObsiIndex.calculate` | `ObsiIndexTest`, `IndexResultTest` |
| R.1 | Reference | Adapted from CME CF RTI Methodology v17.0; the exercise parameters and the per-snapshot deviation check govern | n/a | n/a |

## Brief contract

Clauses from `docs/brief.md` that the code must follow; the spec doesn't define these.

| ID | Section | Clause | Code | Test |
| --- | --- | --- | --- | --- |
| C.1 | Brief CLI | Java 25; takes one local JSON path (D-10) | TBD | TBD |
| C.2 | Brief CLI | `./run.sh data/sample-input.json` and `./run.sh --new-ped data/new-ped.json` work offline with only Java 25, after `build.sh` | TBD | TBD |
| C.3 | Brief CLI | `build.sh` runs first and leaves everything ready for `run.sh` | TBD | TBD |
| I.1 | Brief Input | `calculationTime`: ISO-8601 UTC, time of the snapshot (D-07) | `input/InputParser.isUtcInstant` (D-15) | `InputParserTest` |
| I.2 | Brief Input | `exchange`: unique, nonempty (D-02, D-03) | `input/InputParser` (blank and duplicate checks; D-16, D-18) | `InputParserTest` |
| I.3 | Brief Input | `bids` / `asks`: `[[price, size], ...]`; price USD per XYZ, size XYZ, both positive JSON numbers (D-01, D-05) | `input/InputParser.readLevels`: BigDecimal, shape and type only; "positive" is phase 2 | `InputParserTest`, `InputParserPropertyTest` |
| O.1 | Brief Output | Success: one JSON object on stdout, exit 0 | TBD | `GoldenTest` (`wip` until the calculation exists) |
| O.2 | Brief Output | Success fields: `index` = `"OBSI"`, `calculationTime` same as input, `status` = `"published"`, `value`, `metadata` | TBD | `GoldenTest` (`wip` until the calculation exists) |
| O.3 | Brief Output | `value` is a string with two decimals (with 5.7) | `index/IndexResult.Published.value` (scale exactly 2; the JSON string is phase 5) | `IndexResultTest`, `ObsiIndexTest` |
| O.4 | Brief Output | `metadata.utilizedDepth` is a decimal string in XYZ (v_T, 5.1) | `index/IndexResult.Published.utilizedDepth` (positive whole number, scale 0, so `toPlainString()` is `"4"`; the JSON string is phase 5) | `IndexResultTest`, `ObsiIndexTest` |
| O.5 | Brief Output | `metadata.excludedExchanges` lists the screened-out exchanges in input order (D-03) | `screening/Screener` lists every excluded exchange in input order (the JSON output is phase 5) | `ScreenerTest`, `ScreenerFixturesTest` (matches both `expected/` files) |
| O.6 | Brief Output | Failure: one JSON object on stderr, nonzero exit (D-08) | `cli/Main.java` (scaffold) | `MainSmokeTest` |
| O.7 | Brief Output | Failure fields: `index` = `"OBSI"`, `calculationTime` same as input or null, `status` = `"failed"`, `reason` | `cli/Main.java` (scaffold) | `MainSmokeTest` |
| O.8 | Brief Output | `calculationTime` is null when the input itself doesn't conform | `InputResult.Rejected` carries no `calculationTime`, so the CLI (phase 5) reports null (D-07) | `InputParserTest` (rejection cases) |
| O.9 | Brief Output | The result stays on its own stream; diagnostics go somewhere else (D-09) | TBD | TBD |
| N.1 | Brief Task 2 | `--new-ped` runs the approach chosen in `proposal.md` | TBD | TBD |
| N.2 | Brief Task 2 | Without `--new-ped`, the original 5% rule (2.12, 2.13) stays | TBD | TBD |
| N.3 | Brief Task 2 | New or changed input fields are documented in `proposal.md` with a sample in `data/new-ped.json`; if nothing changes, `data/new-ped.json` is a copy of `data/sample-input.json` | TBD | TBD |
