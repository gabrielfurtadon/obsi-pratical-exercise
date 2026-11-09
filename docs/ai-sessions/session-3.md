▐▛███▛█   Claude Code v2.1.319
▝▜██████▀  Sonnet 5.5 · Claude Pro
▝▝   ▝▝   ~/Projects/obsi


❯ /model                                                                        
⎿  Set model to Sonnet 5.5 and saved as your default for new sessions

❯ /effort                                                                       
⎿  Set effort level to high (saved as your default for new sessions):
Comprehensive implementation with extensive testing and documentation ·
burns fastest — medium handles most tasks

❯ /advisor                                                                      
⎿  Advisor set to Opus 5.5

❯ /plan                                                                         
⎿  Enabled plan mode

❯ NOw we are going to dive into task 2. implement the PED rule from proposal.md
(read it fully first) as a second DeviationFilter in the deviation package,   
and wire --new-ped to it.                                                     
Rule: for each structurally valid book, relative spread = (bestAsk - bestBid)
/ mid; S = median of those spreads; limit = k * S, clamped to [floor, cap].   
Exclude a book if |m - median| > limit * median (no division in the           
comparison; the spread itself needs one, use DECIMAL128). Constants: k = 10,  
normal band [1%, 5%], unlock band [0.5%, 1%], window 48h. Name them and cite  
proposal.md.                                                                  
Unlock: optional top-level "unlockEvents": [{"start": "<ISO-8601 UTC>"}]. The
unlock band applies when calculationTime is in [start, start + 48h). Without  
--new-ped the field is ignored (D-04). With --new-ped, propose how a          
malformed unlockEvents is handled and record it as D-23 before implementing.  
Main: --new-ped uses the new filter; remove the "not implemented" refusal.    
data/new-ped.json: start from the sample, add unlockEvents covering its       
calculationTime, and add one exchange that passes the 5% rule but fails the   
new one, so the two modes  different results. Don't write any expected file;  
I'll produce it independently.

List the tests first for my OK, including: the sample's limit is 3.99%        
(proposal example), clamping to floor and cap, the unlock window boundaries   
(start included, start + 48h excluded), the new rule never excluding a book   
that the 5% rule keeps outside an unlock, and new-ped.json giving different   
results with and without the flag. At the end, run ./build.sh, update         
spec-trace.md (N.1-N.3) and show me both runs of data/new-ped.json.

● I'll start by reading proposal.md in full, then the deviation package and
Main.

Searched for 2 patterns, read 2 files, ran 4 shell commands (ctrl+o to expand)
⎿  1 action not run · permission denied
│ Reason: PreToolUse:Bash hook error: Bash command references the spec or src/test/resources/expected/ and runs something that is not on the read-only allowlist: 'sed -n 1,14p docs/decisions.md'. Those files m…

● I read the proposal, CLI, input and test code, but a guard hook blocked one
Bash call that touched the protected expected/ directory. I'm rerunning the
check in a compliant form next. · summary

Searched for 1 pattern, read 1 file (ctrl+o to expand)
⎿  1 action not run · permission denied
│ Reason: PreToolUse:Bash hook error: Bash command references the spec or src/test/resources/expected/ and runs something that is not on the read-only allowlist: 'newPed\\'. Those files must never be modified …

● The second block failed for the same reason: my grep regex's | split the
command, and src/test counts as an ancestor of the protected path. I won't
reshape the command to get around the hook, so I'll read the specific test
files with Read instead. · summary

Read 3 files (ctrl+o to expand)

● Next I'm checking the README, the deliverables doc and the word-limit test, to
see which proposal.md the build counts and what else refers to --new-ped.

Read 2 files, ran 1 shell command (ctrl+o to expand)

● Now I'll write the plan.

● Updated plan
⎿  /plan to preview

● Advising using Opus 5.5
⎿  ✔ Advisor has reviewed the conversation and will apply the feedback

● The advisor flagged other tests that read fixtures, so I'm reading those
before I ask you anything.

Searched for 1 pattern, read 2 files (ctrl+o to expand)

● Only GoldenTest walks data/ generically. ScreenerFixturesTest and
ConsolidatorTest load their two fixtures by name, so they aren't affected. Two
points need your input before I finalize.

● User answered Claude's questions:
⎿  · Your test line says "the new rule never excluding a book that the 5% rule
keeps outside an unlock". That contradicts the proposal: a 2.7% deviation
passes 5% but is excluded at a 2% limit, and your new-ped.json needs
exactly such a book. I read it as "the new rule never KEEPS a book that the
5% rule excludes" (new-excluded ⊇ static-excluded, i.e. never more
permissive). Is that what you meant? → Yes, never more permissive
(Recommended)
· GoldenTest runs every data/*.json that has an expected file WITHOUT
--new-ped. Once you add new-ped.expected.json (presumably the `--new-ped`
output, as in the brief's command), GoldenTest would compare it against the
5%-rule run and fail the build. How should I handle that? → Neither. Make
the mode explicit in the expected file name: X.expected.json runs without  
flag, X.new-ped.expected.json runs with --new-ped. GoldenTest runs every
pair that exists, and the orphan check covers both suffixes. Don't create
any expected file; I'll add them.

● Both answers are in, and I'll fold them and the advisor's corrections into the
plan.

● Updated plan
⎿  /plan to preview
⎿  User rejected Claude's plan:
╭─────────────────────────────────────────────────────────────────────────╮
│ Task 2: spread-based PED behind --new-ped                               │
│                                                                         │
│ Context                                                                 │
│                                                                         │
│ docs/proposal.md picks Approach 2: limit = clamp(k·S, floor, cap),      │
│ where S is the median relative spread of the                            │
│ structurally valid books. An unlock window swaps in a tighter band.     │
│ Task 2 implements it as a second                                        │
│ DeviationFilter, wires --new-ped to it, and ships data/new-ped.json.    │
│ Without the flag the 5% rule and the                                    │
│ output are unchanged (D-04: unlockEvents is ignored).                   │
│                                                                         │
│ Sample check from the proposal: spreads Alpha 0.40%, Beta 0.10%, Delta  │
│ 1.33% (Gamma is crossed and not counted).                               │
│ S = 0.3992%, limit = 3.99%.                                             │
│                                                                         │
│ Plan approval is your OK for the test list, D-23 and D-24. There is no  │
│ second gate after the tests.                                            │
│                                                                         │
│ Settled / to know                                                       │
│                                                                         │
│ - Property reading confirmed: the new rule is never more permissive     │
│   than 5% (every book StaticPedFilter                                   │
│   excludes, the new filter excludes too).                               │
│ - GoldenTest naming decided by you: X.expected.json runs data/**/X.json │
│   without the flag,                                                     │
│   X.new-ped.expected.json runs it with --new-ped. The orphan check      │
│   covers both suffixes. I create no expected                            │
│   file. Consequence: the flagged expected file for data/new-ped.json is │
│   named new-ped.new-ped.expected.json                                   │
│   (stem new-ped + .new-ped.expected.json). The plain                    │
│   new-ped.expected.json would be the no-flag run.                       │
│ - The real proposal is docs/proposal.md (991 words, staged,             │
│   uncommitted). Root proposal.md is an 11-word                          │
│   placeholder, and it is the file DeliverableWordLimitTest counts. I    │
│   cite docs/proposal.md and don't edit it.                              │
│ - Hook blocks this session: two read-only Bash calls were blocked by    │
│   guard_protected.py (a sed, then a grep                                │
│   with \|, each next to src/test, an ancestor of the protected          │
│   expected/ dir). I reran the first once via Read                       │
│   and read the test files with Read for the second instead of reshaping │
│   the command. Nothing was modified.                                    │
│ - Only GoldenTest walks data/ generically. ScreenerFixturesTest and     │
│   ConsolidatorTest load their fixtures by                               │
│   name. CliArgsTest doesn't depend on the old refusal (the parser only  │
│   reads the flag).                                                      │
│                                                                         │
│ Decisions to append to docs/decisions.md (status Decided (resolved by   │
│ the user during planning; the rationale wording is the agent's), as     │
│ D-15 to D-18, on your approval)                                         │
│                                                                         │
│ ▎ D-23 Malformed unlockEvents under --new-ped                           │
│ ▎                                                                       │
│ ▎ The Task 2 input gains an optional top-level unlockEvents: [{"start": │
│ ▎ "<ISO-8601 UTC>"}] (proposal.md, "Input                               │
│ ▎ change"). D-04 says unknown fields are ignored; a malformed calendar  │
│ ▎ must not be silently used.                                            │
│ ▎ - Spec: silent (the field is not in the methodology). Brief, Task 2.  │
│ ▎ D-04, D-06, D-15, D-17.                                               │
│ ▎ - Decision:                                                           │
│ ▎   - Without --new-ped the field is not read at all, so a malformed    │
│ ▎ value changes nothing (D-04).                                         │
│ ▎   - With --new-ped: absent or [] means no unlock. Anything else must  │
│ ▎ be an array of objects, each with a                                   │
│ ▎     start that is a JSON string passing the D-15 rule (Instant.parse  │
│ ▎ accepts it and it ends in uppercase                                   │
│ ▎     Z). Otherwise the whole input is an input error: exit 2,          │
│ ▎ calculationTime null, reason                                          │
│ ▎     input error: unlockEvents must be an array / input error:         │
│ ▎ unlockEvents[i] must be an object /                                   │
│ ▎     input error: unlockEvents[i].start must be an ISO-8601 UTC        │
│ ▎ timestamp ending in Z. null and a missing                             │
│ ▎     start are malformed. Extra keys inside an event are ignored       │
│ ▎ (D-04). Overlapping or duplicate events are                           │
│ ▎     fine (the window is their union). Checked after every other input │
│ ▎ check, so existing input errors keep                                  │
│ ▎     precedence.                                                       │
│ ▎   - Supersedes the "--new-ped is refused until Task 2" bullet of      │
│ ▎ D-22: the flag now runs the filter, and a bad                         │
│ ▎     path with the flag is the usual input error: cannot read input    │
│ ▎ file (exit 2).                                                        │
│ ▎ - Rationale: general principle (nothing accepted silently when the    │
│ ▎ data is in doubt). Silently falling back to                           │
│ ▎   the normal band during an announced unlock would apply a looser     │
│ ▎ limit than the PM asked for and publish a                             │
│ ▎   different index with no trace; there is no warning channel (D-09).  │
│ ▎ A bad calendar is malformed top-level input                           │
│ ▎   (D-06).                                                             │
│ ▎ - Alternatives rejected: ignore the bad entry (partial calendar,      │
│ ▎ silent); treat as no unlock (silent, looser).                         │
│                                                                         │
│ ▎ D-24 Spread-based limit: no rounding, median of spreads               │
│ ▎                                                                       │
│ ▎ - Spec: silent (Task 2 only). docs/proposal.md, Approach 2.           │
│ ▎ - Decision: the limit k·S is never rounded; "3.99%" in the proposal   │
│ ▎ is a display value. The spread                                        │
│ ▎   (ask − bid)/mid is the only division, at MathContext.DECIMAL128;    │
│ ▎ the comparison itself has none. S is the                              │
│ ▎   median of the spreads of the structurally valid books, with the     │
│ ▎ same even-count rule as the median of mids                            │
│ ▎   (mean of the middle two, exact). The window test uses               │
│ ▎ Duration.between(start, T) < 48h rather than                          │
│ ▎   start + 48h, so it cannot overflow.                                 │
│ ▎ - Rationale: one rounding, at the single division, as D-14 does for   │
│ ▎ the index.                                                            │
│                                                                         │
│ Design                                                                  │
│                                                                         │
│ - deviation/SpreadBasedPedFilter implements DeviationFilter. Per call:  │
│   a. Spread per valid book (bestAsk − bestBid) / mid, divide(...,       │
│      MathContext.DECIMAL128). S = median of                             │
│      spreads, reusing StaticPedFilter.median (package-private static).  │
│   b. Regime from snapshot.calculationTime() and                         │
│      snapshot.unlockStarts(): in window iff !T.isBefore(start) and      │
│      Duration.between(start, T) is less than 48h, for any event. T is   │
│      Instant.parse of the already validated                             │
│      string (so T24:00:00Z works, D-15).                                │
│   c. limit = clamp(K·S, floor, cap) via compareTo, a fraction, not      │
│      rounded.                                                           │
│   d. Exclude iff |m − median| > limit·median. No division. Median of    │
│      mids reuses StaticPedFilter.median.                                │
│ - Named constants, each commented with docs/proposal.md, "Approach 2":  │
│   K = 10, NORMAL_FLOOR = 0.01,                                          │
│   NORMAL_CAP = 0.05, UNLOCK_FLOOR = 0.005, UNLOCK_CAP = 0.01,           │
│   UNLOCK_WINDOW = Duration.ofHours(48).                                 │
│   Package-private limit(List<ValidBook>, boolean inUnlock) so tests     │
│   assert the limit directly.                                            │
│ - input/Snapshot gains List<Instant> unlockStarts; the existing 2-arg   │
│   constructor stays and defaults to empty,                              │
│   so existing tests compile. InputParser.parse(Path) unchanged (field   │
│   not read). New overload                                               │
│   parse(Path, boolean readUnlockEvents); isUtcInstant is shared         │
│   (package-private).                                                    │
│ - cli/Main: remove NEW_PED_NOT_IMPLEMENTED and its check; parse with    │
│   arguments.newPed(); the default                                       │
│   calculation is ObsiCalculator.calculate(snapshot, newPed ? new        │
│   SpreadBasedPedFilter() : new StaticPedFilter()).                      │
│   The test seam (explicit Function) stays.                              │
│ - cli/GoldenTest (test code): per your rule above. goldenPairs yields   │
│   (input, expected, newPed) and the orphan                              │
│   check understands both suffixes.                                      │
│ - data/new-ped.json = data/sample-input.json +                          │
│   "unlockEvents":[{"start":"2026-10-26T18:00:00Z"}] (window ends        │
│   2026-10-28T18:00Z, covers 2026-10-27T12:00:00Z) + exchange Epsilon    │
│   appended last:                                                        │
│   bids [[97.9, 0.5]], asks [[98.1, 0.5]].                               │
│   - Hand check: valid mids 98.0, 100.2, 100.25, 150, so the median is   │
│     100.225. Epsilon is 2.22% away: the 5% rule                         │
│     keeps it. Spreads: Alpha 0.3992%, Beta 0.0997%, Delta 1.333%,       │
│     Epsilon 0.204%, so S = 0.3016% and k·S = 3.02%,                     │
│     which the unlock band clamps to its cap of 1%. Epsilon is excluded. │
│     Alpha and Beta are 0.025% away and kept.                            │
│     Delta is excluded by both. Outside the window Epsilon would be kept │
│     (2.22% < 3.02%): the window does the work.                          │
│   - Without the flag: excluded [Gamma, Delta]; the consolidated book    │
│     has exactly one uncrossing step (Epsilon's                          │
│     ask 98.1 × 0.5 is removed against Beta's best bid 100.2, leaving    │
│     0.3), so the value and depth change as well.                        │
│     With the flag: excluded [Gamma, Delta, Epsilon], equal to the       │
│     sample's 100.23, depth 4. Values are hand-                          │
│     derived and confirmed by the real runs, not pinned in tests.        │
│ - Not changed: StaticPedFilter, Screener, the DeviationFilter           │
│   interface, docs/proposal.md, the spec,                                │
│   src/test/resources/expected/. No expected file is written.            │
│                                                                         │
│ Tests (the list for your OK; all @Tag("wip") until their code lands,    │
│ then the tag is removed)                                                │
│                                                                         │
│ Order: compile-only stubs (SpreadBasedPedFilter throwing                │
│ UnsupportedOperationException; the new Snapshot and                     │
│ InputParser signatures; TestBooks helpers) so wip tests compile, then   │
│ the tests, then implementation.                                         │
│                                                                         │
│ Test helpers (TestBooks): validWithSpread(name, mid, spread) and        │
│ fixture(name, readUnlockEvents). The default                            │
│ validAtMid has spread 0.2, which at mid 100 gives S = 0.2% and k·S =    │
│ 2%: that is not a floor case (normal 2%,                                │
│ unlock 1% cap). Floor tests therefore use spread ≤ 0.04 at mid 100 (S ≤ │
│ 0.04%, k·S ≤ 0.4%, below both floors).                                  │
│ fixture(name) parses without the flag, so filter-level tests on         │
│ new-ped.json must use the new overload or they                          │
│ silently get no unlock events.                                          │
│                                                                         │
│ SpreadBasedPedFilterTest                                                │
│ 1. Spread = (ask−bid)/mid at DECIMAL128: bid 100 / ask 100.4 →          │
│    0.003992015968063872255489…; a non-terminating                       │
│    case (1/3) does not throw.                                           │
│ 2. S is the median of spreads: odd → middle; even → mean of the middle  │
│    two; input order irrelevant.                                         │
│ 3. Sample limit is 3.99%: valid books of sample-input.json (Alpha,      │
│    Beta, Delta) → spreads 0.40/0.10/1.33                                │
│    (2 dp), S = 0.40%, normal regime. The limit equals 10·S exactly      │
│    (unrounded, D-24) and displays as 3.99%                              │
│    (setScale(4, DOWN) = 0.0399, i.e. between 0.0399 and 0.0400). Gamma  │
│    (crossed) is not counted.                                            │
│ 4. Sample end to end: Delta excluded (49.6%), Alpha/Beta kept; through  │
│    ObsiCalculator the value stays 100.23.                               │
│ 5. Limit = k·S inside the band (normal S = 0.3% → 3%; unlock S = 0.07%  │
│    → 0.7%).                                                             │
│ 6. Clamp to floor (spread ≤ 0.04 at mid 100): 1% normal, 0.5% unlock.   │
│    k·S exactly equal to the floor stays.                                │
│ 7. Clamp to cap: normal S ≥ 0.5% → 5%; unlock S ≥ 0.1% → 1%. k·S        │
│    exactly equal to the cap stays.                                      │
│ 8. Book-level boundary, division-free, exact decimals, only where the   │
│    limit is clamped: median 100, spreads ≤ 0.04,                        │
│    normal 1%: mid 101 passes, 101.01 excluded; unlock 0.5%: 100.5       │
│    passes, 100.51 excluded; the same at the caps                        │
│    (5% normal, 1% unlock) with wide spreads.                            │
│ 9. One huge-spread book among tight ones does not widen the limit       │
│    (median of spreads, odd count). A single book is                     │
│    never excluded. An empty list gives an empty result.                 │
│ 10. Equivalent scales (100.0 vs 100) give the same result (compareTo).  │
│ 11. Unlock window: no events → normal; T == start → unlock (start       │
│     included); T == start − 1ns → normal;                               │
│     T == start + 48h − 1ns → unlock; T == start + 48h → normal (end     │
│     excluded); start + 48h + 1s → normal.                               │
│     Discriminating book: 0.7% from the median with spreads ≤ 0.04       │
│     (passes at the 1% normal floor, excluded at the                     │
│     0.5% unlock floor).                                                 │
│ 12. Several events: any covering window applies; a gap between windows  │
│     → normal; overlapping and duplicate events                          │
│     fine; order of events irrelevant.                                   │
│ 13. T24:00:00Z is read as next-day midnight. A start near Instant.MAX   │
│     does not throw.                                                     │
│ 14. The unlock band replaces the normal band: large S gives 1% in the   │
│     window, not 5%.                                                     │
│                                                                         │
│ SpreadBasedPedFilterPropertyTest (JUnit @ParameterizedTest, Random with │
│ a fixed seed, like                                                      │
│ StaticPedFilterPropertyTest)                                            │
│ 15. Never more permissive than 5%: for random book sets and times       │
│ outside any unlock, every book                                          │
│     StaticPedFilter excludes is also excluded by the new filter. Same   │
│ inside a window.                                                        │
│ 16. The exclusion set inside a window ⊇ the one outside it (unlock cap  │
│ 1% ≤ normal floor 1%).                                                  │
│ 17. The limit is always within [floor, cap] of its regime and equals    │
│ the clamp of k·S.                                                       │
│ 18. Division-free comparison agrees with a 60-digit division oracle.    │
│ Exact-boundary inputs are built only where the                          │
│     limit is clamped (exact floor/cap). For an unclamped limit the      │
│ oracle compares the 60-digit |m−med|/med with                           │
│     the filter's own limit(), since k·S is a DECIMAL128-rounded number. │
│ 19. Result independent of book order, and a subset of the books given   │
│ (identity, as Screener requires).                                       │
│                                                                         │
│ InputParserTest (additions)                                             │
│ 20. With the flag: absent / [] → empty; valid events → instants         │
│ (fractional seconds, T24:00:00Z, duplicates ok;                         │
│     extra keys in an event ignored).                                    │
│ 21. With the flag, each of these is Rejected with the D-23 reason: not  │
│ an array (object, string, number, null),                                │
│     element not an object, start missing / not a string / unparseable / │
│ with an offset / lowercase z.                                           │
│ 22. Without the flag the same malformed inputs are accepted and         │
│ unlockStarts is empty (D-04).                                           │
│ 23. Existing input errors keep precedence (bad books plus bad           │
│ unlockEvents reports the books error).                                  │
│ 24. The Snapshot 2-arg constructor still works, with empty              │
│ unlockStarts.                                                           │
│                                                                         │
│ MainTest (changes and additions)                                        │
│ 25. Replace the two "not implemented yet" tests: --new-ped before or    │
│ after the path now publishes; with a missing                            │
│     file it is input error: cannot read input file; twice is the same   │
│ as once.                                                                │
│ 26. --new-ped on sample-input.json publishes 100.23, excludes [Gamma,   │
│ Delta].                                                                 │
│ 27. Malformed unlockEvents with the flag: exit 2, stdout empty,         │
│ calculationTime null, D-23 reason. Without the                          │
│     flag the same file publishes normally.                              │
│ 28. data/new-ped.json gives different results with and without the      │
│ flag: both exit 0 and the outputs differ;                               │
│     excluded [Gamma, Delta] vs [Gamma, Delta, Epsilon]. It asserts      │
│ excluded lists and "outputs differ" and                                 │
│     does not pin the index value, so your independent expected file     │
│ stays independent.                                                      │
│ 29. With the flag but calculationTime moved outside the window (temp    │
│ copy), Epsilon is kept.                                                 │
│                                                                         │
│ GoldenTest                                                              │
│ 30. Pairs follow your naming rule. The guard test still fails on an     │
│ expected file without an input, for both                                │
│     suffixes, and on zero pairs.                                        │
│                                                                         │
│ Finish                                                                  │
│                                                                         │
│ - Remove @Tag("wip") as tests pass. Run ./build.sh (full suite, builds  │
│   target/obsi.jar).                                                     │
│ - docs/spec-trace.md: fill N.1 (filter, constants, Main, tests), N.2    │
│   (Main picks StaticPedFilter without the                               │
│   flag), N.3 (docs/proposal.md "Input change", InputParser              │
│   unlockEvents, data/new-ped.json, D-23). Also fix                      │
│   the stale --new-ped text in C.2 ("refused until Task 2 … TBD").       │
│ - Run and show both: ./run.sh data/new-ped.json and ./run.sh --new-ped  │
│   data/new-ped.json (stdout, stderr, exit                               │
│   codes).                                                               │
│ - Optional, only if you want it: one sentence on malformed handling in  │
│   docs/proposal.md (needs about 10 words of                             │
│   headroom, so something else would have to be cut).                    │
╰─────────────────────────────────────────────────────────────────────────╯

Ran 1 shell command (ctrl+o to expand)

● Root proposal.md now holds the real text, identical to docs/proposal.md.
Updating the plan with your three corrections.

● Updated plan
⎿  /plan to preview

● Updated plan  ⎿  /plan to preview

● Updated plan
⎿  /plan to preview

Ran 2 shell commands (ctrl+o to expand)

● User approved Claude's plan
⎿  Plan saved to:
~/.claude/plans/pasted-content-id-2cbc-now-we-quizzical-snowglobe.md ·
/plan to edit
Task 2: spread-based PED behind --new-ped

     Context

     proposal.md picks Approach 2: limit = clamp(k·S, floor, cap), where S is
     the median relative spread of the
     structurally valid books. An unlock window swaps in a tighter band. Task 2
     implements it as a second
     DeviationFilter, wires --new-ped to it, and ships data/new-ped.json.
     Without the flag the 5% rule and the
     output are unchanged (D-04: unlockEvents is ignored).

     Sample check from the proposal: spreads Alpha 0.40%, Beta 0.10%, Delta
     1.33% (Gamma is crossed and not counted).
     S = 0.3992%, limit = 3.99%.

     Plan approval is your OK for the test list, D-23 and D-24. There is no
     second gate after the tests.

     Settled / to know

     - Property reading confirmed: the new rule is never more permissive than 5%
       (every book StaticPedFilter
       excludes, the new filter excludes too).
     - GoldenTest naming decided by you: X.expected.json runs data/**/X.json
       without the flag,
       X.new-ped.expected.json runs it with --new-ped. The orphan check covers
       both suffixes. I create no expected
       file. Consequence: the flagged expected file for data/new-ped.json is
       named new-ped.new-ped.expected.json
       (stem new-ped + .new-ped.expected.json). The plain new-ped.expected.json
       would be the no-flag run.
     - The proposal is now in the right place: root proposal.md (991 words,
       identical to docs/proposal.md). Code
       comments and the spec trace cite proposal.md (the root file). I don't
       edit it.
     - Hook blocks this session: two read-only Bash calls were blocked by
       guard_protected.py (a sed, then a grep
       with \|, each next to src/test, an ancestor of the protected expected/
       dir). I reran the first once via Read
       and read the test files with Read for the second instead of reshaping the
       command. Nothing was modified.
     - Only GoldenTest walks data/ generically. ScreenerFixturesTest and
       ConsolidatorTest load their fixtures by
       name. CliArgsTest doesn't depend on the old refusal (the parser only
       reads the flag).

     Decisions to append to docs/decisions.md as D-23 and D-24 (status Decided 
     (resolved by the user during planning; the rationale wording is the 
     agent's), on your approval)

     ▎ D-23 Malformed unlockEvents under --new-ped
     ▎
     ▎ The Task 2 input gains an optional top-level unlockEvents: [{"start": 
     ▎ "<ISO-8601 UTC>"}] (proposal.md, "Input
     ▎ change"). D-04 says unknown fields are ignored; a malformed calendar must
     ▎ not be silently used.
     ▎ - Spec: silent (the field is not in the methodology). Brief, Task 2. 
     ▎ D-04, D-06, D-15, D-17.
     ▎ - Decision:
     ▎   - Without --new-ped the field is not read at all, so a malformed value 
     ▎ changes nothing (D-04).
     ▎   - With --new-ped: absent or [] means no unlock. Anything else must be 
     ▎ an array of objects, each with a
     ▎     start that is a JSON string passing the D-15 rule (Instant.parse 
     ▎ accepts it and it ends in uppercase
     ▎     Z). Otherwise the whole input is an input error: exit 2, 
     ▎ calculationTime null, reason
     ▎     input error: unlockEvents must be an array / input error: 
     ▎ unlockEvents[i] must be an object /
     ▎     input error: unlockEvents[i].start must be an ISO-8601 UTC timestamp 
     ▎ ending in Z. null and a missing
     ▎     start are malformed. Extra keys inside an event are ignored (D-04). 
     ▎ Overlapping or duplicate events are
     ▎     fine (the window is their union). Checked after every other input 
     ▎ check, so existing input errors keep
     ▎     precedence.
     ▎   - Supersedes the "--new-ped is refused until Task 2" bullet of D-22: 
     ▎ the flag now runs the filter, and a bad
     ▎     path with the flag is the usual input error: cannot read input file 
     ▎ (exit 2).
     ▎ - Rationale: general principle (nothing accepted silently when the data 
     ▎ is in doubt). Silently falling back to
     ▎   the normal band during an announced unlock would apply a looser limit 
     ▎ than the PM asked for and publish a
     ▎   different index with no trace; there is no warning channel (D-09). A 
     ▎ bad calendar is malformed top-level input
     ▎   (D-06).
     ▎ - Alternatives rejected: ignore the bad entry (partial calendar, silent);
     ▎ treat as no unlock (silent, looser).

     ▎ D-24 Spread-based limit: no rounding, median of spreads
     ▎
     ▎ - Spec: silent (Task 2 only). proposal.md, Approach 2.
     ▎ - Decision: the limit k·S is never rounded; "3.99%" in the proposal is a 
     ▎ display value. The spread
     ▎   (ask − bid)/mid is the only division, at MathContext.DECIMAL128; the 
     ▎ comparison itself has none. S is the
     ▎   median of the spreads of the structurally valid books, with the same 
     ▎ even-count rule as the median of mids
     ▎   (mean of the middle two, exact). The window test uses 
     ▎ Duration.between(start, T) < 48h rather than
     ▎   start + 48h, so it cannot overflow.
     ▎ - Rationale: one rounding, at the single division, as D-14 does for the 
     ▎ index.

     Design

     - deviation/SpreadBasedPedFilter implements DeviationFilter. Per call:
       a. Spread per valid book (bestAsk − bestBid) / mid, divide(..., 
          MathContext.DECIMAL128). S = median of
          spreads, reusing StaticPedFilter.median (package-private static).
       b. Regime from snapshot.calculationTime() and snapshot.unlockStarts(): in
          window iff !T.isBefore(start) and
          Duration.between(start, T) is less than 48h, for any event. T is
          Instant.parse of the already validated
          string (so T24:00:00Z works, D-15).
       c. limit = clamp(K·S, floor, cap) via compareTo, a fraction, not rounded.
       d. Exclude iff |m − median| > limit·median. No division. Median of mids
          reuses StaticPedFilter.median.
     - Named constants, each commented with proposal.md, "Approach 2": K = 10,
       NORMAL_FLOOR = 0.01,
       NORMAL_CAP = 0.05, UNLOCK_FLOOR = 0.005, UNLOCK_CAP = 0.01, UNLOCK_WINDOW
       = Duration.ofHours(48).
       Package-private limit(List<ValidBook>, boolean inUnlock) so tests assert
       the limit directly.
     - input/Snapshot gains List<Instant> unlockStarts; the existing 2-arg
       constructor stays and defaults to empty,
       so existing tests compile. InputParser.parse(Path) unchanged (field not
       read). New overload
       parse(Path, boolean readUnlockEvents); isUtcInstant is shared
       (package-private).
     - cli/Main: remove NEW_PED_NOT_IMPLEMENTED and its check; parse with
       arguments.newPed(); the default
       calculation is ObsiCalculator.calculate(snapshot, newPed ? new 
       SpreadBasedPedFilter() : new StaticPedFilter()).
       The test seam (explicit Function) stays.
     - cli/GoldenTest (test code): per your rule above. goldenPairs yields
       (input, expected, newPed) and the orphan
       check understands both suffixes.
     - data/new-ped.json = data/sample-input.json +
       "unlockEvents":[{"start":"2026-10-26T18:00:00Z"}] (window ends
       2026-10-28T18:00Z, covers 2026-10-27T12:00:00Z) + exchange Epsilon
       appended last:
       bids [[97.9, 0.5]], asks [[98.1, 0.5]].
       - Hand check: valid mids 98.0, 100.2, 100.25, 150, so the median is
         100.225. Epsilon is 2.22% away: the 5% rule
         keeps it. Spreads: Alpha 0.3992%, Beta 0.0997%, Delta 1.333%, Epsilon
         0.204%, so S = 0.3016% and k·S = 3.02%,
         which the unlock band clamps to its cap of 1%. Epsilon is excluded.
         Alpha and Beta are 0.025% away and kept.
         Delta is excluded by both. Outside the window Epsilon would be kept
         (2.22% < 3.02%): the window does the work.
       - Without the flag: excluded [Gamma, Delta]; the consolidated book has
         exactly one uncrossing step (Epsilon's
         ask 98.1 × 0.5 is removed against Beta's best bid 100.2, leaving 0.3),
         so the value and depth change as well.
         With the flag: excluded [Gamma, Delta, Epsilon], equal to the sample's
         100.23, depth 4. Values are hand-
         derived and confirmed by the real runs, not pinned in tests.
     - Not changed: StaticPedFilter, Screener, the DeviationFilter interface,
       proposal.md, the spec,
       src/test/resources/expected/. No expected file is written.

     Tests (the list for your OK; all @Tag("wip") until their code lands, then 
     the tag is removed)

     Order: compile-only stubs (SpreadBasedPedFilter throwing
     UnsupportedOperationException; the new Snapshot and
     InputParser signatures; TestBooks helpers) so wip tests compile, then the
     tests, then implementation.

     Test helpers (TestBooks): validWithSpread(name, mid, spread) and
     fixture(name, readUnlockEvents). The default
     validAtMid has spread 0.2, which at mid 100 gives S = 0.2% and k·S = 2%:
     that is not a floor case (normal 2%,
     unlock 1% cap). Floor tests therefore use spread ≤ 0.04 at mid 100 (S ≤
     0.04%, k·S ≤ 0.4%, below both floors).
     fixture(name) parses without the flag, so filter-level tests on
     new-ped.json must use the new overload or they
     silently get no unlock events.

     SpreadBasedPedFilterTest
     1. Spread = (ask−bid)/mid at DECIMAL128: bid 100 / ask 100.4 →
        0.003992015968063872255489…; a non-terminating
        case (1/3) does not throw.
     2. S is the median of spreads: odd → middle; even → mean of the middle two;
        input order irrelevant.
     3. Sample limit is 3.99%: valid books of sample-input.json (Alpha, Beta,
        Delta) → spreads 0.40/0.10/1.33
        (2 dp), S = 0.40%, normal regime. The limit equals 10·S exactly
        (unrounded, D-24) and displays as 3.99%
        (setScale(4, DOWN) = 0.0399, i.e. between 0.0399 and 0.0400). Gamma
        (crossed) is not counted.
     4. Sample end to end: Delta excluded (49.6%), Alpha/Beta kept; through
        ObsiCalculator the value stays 100.23.
     5. Limit = k·S inside the band (normal S = 0.3% → 3%; unlock S = 0.07% →
        0.7%).
     6. Clamp to floor (spread ≤ 0.04 at mid 100): 1% normal, 0.5% unlock. k·S
        exactly equal to the floor stays.
     7. Clamp to cap: normal S ≥ 0.5% → 5%; unlock S ≥ 0.1% → 1%. k·S exactly
        equal to the cap stays.
     8. Book-level boundary, division-free, exact decimals, only where the limit
        is clamped: median 100, spreads ≤ 0.04,
        normal 1%: mid 101 passes, 101.01 excluded; unlock 0.5%: 100.5 passes,
        100.51 excluded; the same at the caps
        (5% normal, 1% unlock) with wide spreads.
     9. One huge-spread book among tight ones does not widen the limit (median
        of spreads, odd count). A single book is
        never excluded. An empty list gives an empty result.
     10. Equivalent scales (100.0 vs 100) give the same result (compareTo).
     11. Unlock window: no events → normal; T == start → unlock (start
         included); T == start − 1ns → normal;
         T == start + 48h − 1ns → unlock; T == start + 48h → normal (end
         excluded); start + 48h + 1s → normal.
         Discriminating book: 0.7% from the median with spreads ≤ 0.04 (passes
         at the 1% normal floor, excluded at the
         0.5% unlock floor).
     12. Several events: any covering window applies; a gap between windows →
         normal; overlapping and duplicate events
         fine; order of events irrelevant.
     13. T24:00:00Z is read as next-day midnight. A start near Instant.MAX does
         not throw.
     14. The unlock band replaces the normal band: large S gives 1% in the
         window, not 5%.

     SpreadBasedPedFilterPropertyTest (JUnit @ParameterizedTest, Random with a
     fixed seed, like
     StaticPedFilterPropertyTest)
     15. Never more permissive than 5%: for random book sets and times outside
     any unlock, every book
         StaticPedFilter excludes is also excluded by the new filter. Same
     inside a window.
     16. The exclusion set inside a window ⊇ the one outside it (unlock cap 1% ≤
     normal floor 1%).
     17. The limit is always within [floor, cap] of its regime and equals the
     clamp of k·S.
     18. Division-free comparison agrees with a 60-digit division oracle.
     Exact-boundary inputs are built only where the
         limit is clamped (exact floor/cap). For an unclamped limit the oracle
     compares the 60-digit |m−med|/med with
         the filter's own limit(), since k·S is a DECIMAL128-rounded number.
     19. Result independent of book order, and a subset of the books given
     (identity, as Screener requires).

     InputParserTest (additions)
     20. With the flag: absent / [] → empty; valid events → instants (fractional
     seconds, T24:00:00Z, duplicates ok;
         extra keys in an event ignored).
     21. With the flag, each of these is Rejected with the D-23 reason: not an
     array (object, string, number, null),
         element not an object, start missing / not a string / unparseable /
     with an offset / lowercase z.
     22. Without the flag the same malformed inputs are accepted and
     unlockStarts is empty (D-04).
     23. Existing input errors keep precedence (bad books plus bad unlockEvents
     reports the books error).
     24. The Snapshot 2-arg constructor still works, with empty unlockStarts.

     MainTest (changes and additions)
     25. Replace the two "not implemented yet" tests: --new-ped before or after
     the path now publishes; with a missing
         file it is input error: cannot read input file; twice is the same as
     once.
     26. --new-ped on sample-input.json publishes 100.23, excludes [Gamma, 
     Delta].
     27. Malformed unlockEvents with the flag: exit 2, stdout empty,
     calculationTime null, D-23 reason. Without the
         flag the same file publishes normally.
     28. data/new-ped.json gives different results with and without the flag:
     both exit 0 and the outputs differ;
         excluded [Gamma, Delta] vs [Gamma, Delta, Epsilon]. It asserts excluded
     lists and "outputs differ" and
         does not pin the index value, so your independent expected file stays
     independent.
     29. With the flag but calculationTime moved outside the window (temp copy),
     Epsilon is kept.

     GoldenTest
     30. Pairs follow your naming rule. The guard test still fails on an
     expected file without an input, for both
         suffixes, and on zero pairs.

     Finish

     - Remove @Tag("wip") as tests pass. Run ./build.sh (full suite, builds
       target/obsi.jar).
     - docs/spec-trace.md: fill N.1 (filter, constants, Main, tests), N.2 (Main
       picks StaticPedFilter without the
       flag), N.3 (proposal.md "Input change", InputParser unlockEvents,
       data/new-ped.json, D-23). Also fix
       the stale --new-ped text in C.2 ("refused until Task 2 … TBD").
     - Run and show both: ./run.sh data/new-ped.json and ./run.sh --new-ped 
       data/new-ped.json (stdout, stderr, exit
       codes).
     - proposal.md is not touched: D-23 covers malformed unlockEvents.

● Plan approved. First I'll append D-23 and D-24 to docs/decisions.md.

● Update(docs/decisions.md)
⎿  Added 33 lines
236    cannot start with `-`.
237  - Status: Decided (resolved by the user)
238  
239 +### D-23 Malformed `unlockEvents` under `--new-ped`           
240 +The Task 2 input gains an optional top-level `unlockEvents: [{
          +"start": "<ISO-8601 UTC>"}]` (proposal.md, "Input             
241 +change"). D-04 says unknown fields are ignored; a malformed ca
+lendar must not be silently used.                             
242 +- Spec: silent (the field is not in the methodology). Brief, T
+ask 2. D-04, D-06, D-15, D-17.                                
243 +- Decision:                                                   
244 +  - Without `--new-ped` the field is not read at all, so a mal
+formed value changes nothing (D-04).                          
245 +  - With `--new-ped`: absent or `[]` means no unlock. Anything
+ else must be an array of objects, each with a                
246 +    `start` that is a JSON string passing the D-15 rule (`Inst
          +ant.parse` accepts it **and** it ends in uppercase            
247 +    `Z`). Otherwise the whole input is an input error: exit 2,
+ `calculationTime` null, reason                               
248 +    `input error: unlockEvents must be an array` / `input erro
          +r: unlockEvents[i] must be an object` /                       
249 +    `input error: unlockEvents[i].start must be an ISO-8601 UT
          +C timestamp ending in Z`. `null` and a missing                
250 +    `start` are malformed. Extra keys inside an event are igno
+red (D-04). Overlapping or duplicate events are               
251 +    fine (the window is their union). Checked after every othe
+r input check, so existing input errors keep                  
252 +    precedence.                                               
253 +  - Supersedes the "`--new-ped` is refused until Task 2" bulle
+t of D-22: the flag now runs the filter, and a bad            
254 +    path with the flag is the usual `input error: cannot read 
          +input file` (exit 2).                                         
255 +- Rationale: general principle (nothing accepted silently when
+ the data is in doubt). Silently falling back to              
256 +  the normal band during an announced unlock would apply a loo
+ser limit than the PM asked for and publish a                 
257 +  different index with no trace; there is no warning channel (
+D-09). A bad calendar is malformed top-level input            
258 +  (D-06).                                                     
259 +- Alternatives rejected: ignore the bad entry (partial calenda
+r, silent); treat as no unlock (silent, looser).              
260 +- Status: Decided (resolved by the user during planning; the r
+ationale wording is the agent's)                              
261 +                                                              
262 +### D-24 Spread-based limit: no rounding, median of spreads   
263 +- Spec: silent (Task 2 only). proposal.md, Approach 2.        
264 +- Decision: the limit k·S is never rounded; "3.99%" in the pro
+posal is a display value. The spread                          
265 +  `(ask − bid)/mid` is the only division, at `MathContext.DECI
          +MAL128`; the comparison itself has none. S is the             
266 +  median of the spreads of the structurally valid books, with
+the same even-count rule as the median of mids                
267 +  (mean of the middle two, exact). The window test uses `Durat
          +ion.between(start, T) < 48h` rather than                      
268 +  `start + 48h`, so it cannot overflow.                       
269 +- Rationale: one rounding, at the single division, as D-14 doe
+s for the index.                                              
270 +- Status: Decided (resolved by the user during planning; the r
+ationale wording is the agent's)                              
271 +                                                              
272  ## Output and CLI
273  
274  ### D-08 Exit codes and `reason`

Read 1 file (ctrl+o to expand)

● Now the compile-only stubs. They carry signatures only, so the new tests can
compile and fail.

● Write(src/main/java/com/cfbenchmarks/obsi/input/Snapshot.java)
⎿  Added 10 lines, removed 1 line
1  package com.cfbenchmarks.obsi.input;
2  
3 +import java.time.Instant;                                       
4  import java.util.List;
5  import java.util.Objects;
6  
...
9   *
10   * @param calculationTime T (spec §1), the original string, val
idated but never reformatted (D-07)
11   * @param books           the books in input order; may be empt
y (D-07)
12 + * @param unlockStarts    the {@code start} of every announced
+unlock (proposal.md, "Input change"). Empty unless the         
13 + *                        input was read for {@code --new-ped}
+(D-04, D-23)                                                   
14   */
12 -public record Snapshot(String calculationTime, List<BookInput>
-books) {                                                       
15 +public record Snapshot(String calculationTime, List<BookInput>
+books, List<Instant> unlockStarts) {                           
16  
17      public Snapshot {
18          Objects.requireNonNull(calculationTime, "calculationTim
e");
19          books = List.copyOf(books);
20 +        unlockStarts = List.copyOf(unlockStarts);              
21      }
22 +                                                               
23 +    /** A snapshot with no announced unlock. */                
24 +    public Snapshot(String calculationTime, List<BookInput> boo
+ks) {                                                          
25 +        this(calculationTime, books, List.of());               
26 +    }                                                          
27  }

● Write(src/main/java/com/cfbenchmarks/obsi/deviation/SpreadBasedPedFilter.java)
⎿  Wrote 20 lines to src/main/java/com/cfbenchmarks/obsi/deviation/Spread
BasedPedFilter.java
1 package com.cfbenchmarks.obsi.deviation;
2
3 import com.cfbenchmarks.obsi.book.ValidBook;
4 import com.cfbenchmarks.obsi.input.Snapshot;
5
6 import java.math.BigDecimal;
7 import java.util.List;
8
9 /** Stub: the Task 2 rule (proposal.md, Approach 2). Implemented
after its tests. */
… +11 lines (ctrl+o to expand)

● Update(src/main/java/com/cfbenchmarks/obsi/input/InputParser.java)
⎿  Added 10 lines
66          return parse(bytes);
67      }
68  
69 +    /** Stub: as {@link #parse(Path)}, and reads {@code unlockE
+vents} when asked (D-23). */                                   
70 +    public static InputResult parse(Path file, boolean readUnlo
+ckEvents) {                                                    
71 +        return parse(file);                                    
72 +    }                                                          
73 +                                                               
74 +    /** Stub: as {@link #parse(byte[])}, and reads {@code unloc
+kEvents} when asked (D-23). */                                 
75 +    public static InputResult parse(byte[] json, boolean readUn
+lockEvents) {                                                  
76 +        return parse(json);                                    
77 +    }                                                          
78 +                                                               
79      /** Parses the UTF-8 bytes of an input document. */
80      public static InputResult parse(byte[] json) {
81          JsonNode root;

● Next the TestBooks helpers.

● Update(src/test/java/com/cfbenchmarks/obsi/TestBooks.java)
⎿  Added 17 lines
73          return valid(name, m.subtract(HALF_SPREAD).toPlainStrin
g(), m.add(HALF_SPREAD).toPlainString());
74      }
75  
76 +    /**                                                        
77 +     * A structurally valid book whose mid is exactly {@code mi
+d} and whose absolute spread (ask - bid) is exactly            
78 +     * {@code spread}. {@link #validAtMid} always has a spread
+of 0.2, which at mid 100 is S = 0.2% and so not a floor        
79 +     * case of the spread-based rule; use this one to choose S.
80 +     */                                                        
81 +    public static ValidBook validWithSpread(String name, String
+ mid, String spread) {                                         
82 +        BigDecimal m = new BigDecimal(mid);                    
83 +        BigDecimal half = new BigDecimal(spread).divide(BigDeci
+mal.valueOf(2));                                               
84 +        return valid(name, m.subtract(half).toPlainString(), m.
+add(half).toPlainString());                                    
85 +    }                                                          
86 +                                                               
87 +    /** A snapshot at {@code time} with the given announced unl
+ock starts (UTC instants as text). */                          
88 +    public static Snapshot snapshotAt(String time, String... un
+lockStarts) {                                                  
89 +        return new Snapshot(time, List.of(),                   
90 +                java.util.Arrays.stream(unlockStarts).map(java.
+time.Instant::parse).toList());                                
91 +    }                                                          
92 +                                                               
93      /** Levels written as {@code "price:size"}, in the order gi
ven. */
94      public static List<Level> levels(String... priceColonSize)
{
95          return java.util.Arrays.stream(priceColonSize).map(s ->
{

● Update(src/test/java/com/cfbenchmarks/obsi/TestBooks.java)
⎿  Added 8 lines
112  
113      /** Parses a fixture under {@code data/}, found by file na
me wherever it sits below that directory. */
114      public static Snapshot fixture(String fileName) throws IOE
xception {
115 +        return fixture(fileName, false);                      
116 +    }                                                         
117 +                                                              
118 +    /**                                                       
119 +     * As {@link #fixture(String)}; with {@code readUnlockEven
+ts} the file is read the way {@code --new-ped} reads it.      
120 +     * Without it the snapshot has no unlock events (D-04), so
+ a filter test on {@code new-ped.json} needs the flag.        
121 +     */                                                       
122 +    public static Snapshot fixture(String fileName, boolean re
+adUnlockEvents) throws IOException {                          
123          Path file;
124          try (Stream<Path> files = Files.walk(Path.of("data")))
{
125              file = files.filter(p -> p.getFileName().toString(
).equals(fileName)).findFirst()

● Update(src/test/java/com/cfbenchmarks/obsi/TestBooks.java)
⎿  Added 1 line, removed 1 line
125              file = files.filter(p -> p.getFileName().toString(
).equals(fileName)).findFirst()
126                      .orElseThrow(() -> new AssertionError(file
Name + " not found under data/"));
127          }
128 -        InputResult result = InputParser.parse(file);         
128 +        InputResult result = InputParser.parse(file, readUnloc
+kEvents);                                                     
129          if (!(result instanceof InputResult.Accepted accepted)
) {
130              throw new AssertionError("fixture " + fileName + "
was rejected: " + result);
131          }

Ran 1 shell command (ctrl+o to expand)
⎿  Not run · permission denied
│ Reason: PreToolUse:Bash hook error: Bash command references the spec or src/test/resources/expected/ and runs something that is not on the read-only allowlist: 'python3 -c "'. Those files must never be modif…

● A guard blocked a Bash call that touched no protected path, so I'm not working
around it; the tests can compute expected values with BigDecimal directly.
Next I'm adding spread and medianSpread to the stub and writing the filter
tests. · summary

● Update(src/main/java/com/cfbenchmarks/obsi/deviation/SpreadBasedPedFilter.java
)
⎿  Added 8 lines
14          throw new UnsupportedOperationException("not implemente
d");
15      }
16  
17 +    static BigDecimal spread(ValidBook book) {                 
18 +        throw new UnsupportedOperationException("not implemente
+d");                                                           
19 +    }                                                          
20 +                                                               
21 +    static BigDecimal medianSpread(List<ValidBook> structurally
+Valid) {                                                       
22 +        throw new UnsupportedOperationException("not implemente
+d");                                                           
23 +    }                                                          
24 +                                                               
25      static BigDecimal limit(List<ValidBook> structurallyValid,
boolean inUnlock) {
26          throw new UnsupportedOperationException("not implemente
d");
27      }

Read 1 file (ctrl+o to expand)

● Write(src/test/java/com/cfbenchmarks/obsi/deviation/SpreadBasedPedFilterTest.j
ava)
⎿  Wrote 360 lines to src/test/java/com/cfbenchmarks/obsi/deviation/Sprea
dBasedPedFilterTest.java
1 package com.cfbenchmarks.obsi.deviation;
2
3 import com.cfbenchmarks.obsi.book.ValidBook;
4 import com.cfbenchmarks.obsi.input.Snapshot;
5 import com.cfbenchmarks.obsi.result.CalculationResult;
6 import com.cfbenchmarks.obsi.result.ObsiCalculator;
7 import com.cfbenchmarks.obsi.screening.Screener;
8 import com.cfbenchmarks.obsi.screening.ScreeningResult;
9 import org.junit.jupiter.api.Tag;
10 import org.junit.jupiter.api.Test;
… +350 lines (ctrl+o to expand)

● I left some filler in that file (a helper class and meaningless assertions),
so I'll tidy it up.

Ran 1 shell command (ctrl+o to expand)
⎿  Not run · permission denied
│ Reason: PreToolUse:Bash hook error: Bash command references the spec or src/test/resources/expected/ and runs something that is not on the read-only allowlist: "python3 - <<'EOF'". Those files must never be …

● That block came from my own slip: I tried a Bash rewrite of a file, which
CLAUDE.md forbids. I'm redoing it with the Edit tool.

● Update(src/test/java/com/cfbenchmarks/obsi/deviation/SpreadBasedPedFilterTest.
java)
⎿  Added 1 line, removed 11 lines
337      @Test
338      void anUnlockedSnapshotWithoutBooksIsEmpty() {
339          assertThat(filter.excluded(List.of(), snapshotAt(T, "2
026-10-27T00:00:00Z"))).isEmpty();
340 -        assertThat(Collections.<ValidBook>emptyList()).isEmpty
-();                                                           
341 -        assertThat(snapshot()).isNotNull();                   
340      }
341  
344 -    /** Small helper so the tests read as (name, bid, ask). */
345 -    private static final class TestBooksHelper {              
346 -        static ValidBook book(String name, String bid, String
-ask) {                                                        
347 -            return com.cfbenchmarks.obsi.TestBooks.valid(name,
- bid, ask);                                                   
348 -        }                                                     
349 -    }                                                         
350 -                                                              
351 -    // keeps validAtMid import meaningful for the equal-scales
- case above                                                   
342 +    /** The default test book is not a floor case: spread 0.2
+at mid 100 is S = 0.2%, k·S = 2%. */                          
343      @Test
344      void validAtMidIsNotAFloorCase() {
354 -        // spread 0.2 at mid 100: S = 0.2%, k·S = 2%, between
-the floor and the cap of the normal band                      
345          List<ValidBook> books = List.of(validAtMid("A", "100")
, validAtMid("B", "100"), validAtMid("C", "100"));
346  
347          assertThat(SpreadBasedPedFilter.limit(books, false)).i
sEqualByComparingTo("0.02");
⎿  PostToolUse:Edit hook returned blocking error
⎿  Fast tests failed after this edit (./mvnw -q test
-DexcludedGroups=slow,wip):
[ERROR] COMPILATION ERROR :
[ERROR] /home/fur/Projects/obsi/src/test/java/com/cfbenchmarks/obsi/deviati
on/SpreadBasedPedFilterTest.java:[71,26] cannot find symbol
symbol:   variable TestBooksHelper
location: class com.cfbenchmarks.obsi.deviation.SpreadBasedPedFilterTest
[ERROR] /home/fur/Projects/obsi/src/test/java/com/cfbenchmarks/obsi/deviati
on/SpreadBasedPedFilterTest.java:[81,57] cannot find symbol
symbol:   variable TestBooksHelper
location: class com.cfbenchmarks.obsi.deviation.SpreadBasedPedFilterTest
[ERROR] Failed to execute goal
org.apache.maven.plugins:maven-compiler-plugin:3.16.0:testCompile
(default-testCompile) on project obsi: Compilation failure: Compilation
failure:
[ERROR] /home/fur/Projects/obsi/src/test/java/com/cfbenchmarks/obsi/deviati
on/SpreadBasedPedFilterTest.java:[71,26] cannot find symbol
[ERROR]   symbol:   variable TestBooksHelper
[ERROR]   location: class
com.cfbenchmarks.obsi.deviation.SpreadBasedPedFilterTest
[ERROR] /home/fur/Projects/obsi/src/test/java/com/cfbenchmarks/obsi/deviati
on/SpreadBasedPedFilterTest.java:[81,57] cannot find symbol
[ERROR]   symbol:   variable TestBooksHelper
[ERROR]   location: class
com.cfbenchmarks.obsi.deviation.SpreadBasedPedFilterTest
[ERROR] -> [Help 1]
[ERROR]
[ERROR] To see the full stack trace of the errors, re-run Maven with the -e
switch.
[ERROR] Re-run Maven using the -X switch to enable full debug logging.
[ERROR]
[ERROR] For more information about the errors and possible solutions,
please read the following articles:
[ERROR] [Help 1]
http://cwiki.apache.org/confluence/display/MAVEN/MojoFailureException

● Update(src/test/java/com/cfbenchmarks/obsi/deviation/SpreadBasedPedFilterTest.
java)
⎿  Added 1 line, removed 1 line
68      @Test
69      void theSpreadIsAskMinusBidOverMidAtDecimal128() {
70          // bid 100, ask 100.4: mid 100.2, spread 0.4 / 100.2 =
2 / 501
71 -        ValidBook book = TestBooksHelper.book("A", "100", "100.
-4");                                                           
71 +        ValidBook book = valid("A", "100", "100.4");           
72  
73          assertThat(SpreadBasedPedFilter.spread(book))
74                  .isEqualByComparingTo(BigDecimal.valueOf(2).div
ide(BigDecimal.valueOf(501), MathContext.DECIMAL128));
⎿  PostToolUse:Edit hook returned blocking error
⎿  Fast tests failed after this edit (./mvnw -q test
-DexcludedGroups=slow,wip):
[ERROR] COMPILATION ERROR :
[ERROR] /home/fur/Projects/obsi/src/test/java/com/cfbenchmarks/obsi/deviati
on/SpreadBasedPedFilterTest.java:[71,26] cannot find symbol
symbol:   method
valid(java.lang.String,java.lang.String,java.lang.String)
location: class com.cfbenchmarks.obsi.deviation.SpreadBasedPedFilterTest
[ERROR] /home/fur/Projects/obsi/src/test/java/com/cfbenchmarks/obsi/deviati
on/SpreadBasedPedFilterTest.java:[81,57] cannot find symbol
symbol:   variable TestBooksHelper
location: class com.cfbenchmarks.obsi.deviation.SpreadBasedPedFilterTest
[ERROR] Failed to execute goal
org.apache.maven.plugins:maven-compiler-plugin:3.16.0:testCompile
(default-testCompile) on project obsi: Compilation failure: Compilation
failure:
[ERROR] /home/fur/Projects/obsi/src/test/java/com/cfbenchmarks/obsi/deviati
on/SpreadBasedPedFilterTest.java:[71,26] cannot find symbol
[ERROR]   symbol:   method
valid(java.lang.String,java.lang.String,java.lang.String)
[ERROR]   location: class
com.cfbenchmarks.obsi.deviation.SpreadBasedPedFilterTest
[ERROR] /home/fur/Projects/obsi/src/test/java/com/cfbenchmarks/obsi/deviati
on/SpreadBasedPedFilterTest.java:[81,57] cannot find symbol
[ERROR]   symbol:   variable TestBooksHelper
[ERROR]   location: class
com.cfbenchmarks.obsi.deviation.SpreadBasedPedFilterTest
[ERROR] -> [Help 1]
[ERROR]
[ERROR] To see the full stack trace of the errors, re-run Maven with the -e
switch.
[ERROR] Re-run Maven using the -X switch to enable full debug logging.
[ERROR]
[ERROR] For more information about the errors and possible solutions,
please read the following articles:
[ERROR] [Help 1]
http://cwiki.apache.org/confluence/display/MAVEN/MojoFailureException
● Update(src/test/java/com/cfbenchmarks/obsi/deviation/SpreadBasedPedFilterTest.
java)
⎿  Added 1 line, removed 1 line
78      @Test
79      void aNonTerminatingSpreadDoesNotThrow() {
80          // bid 2, ask 4: mid 3, spread 2 / 3
81 -        BigDecimal spread = SpreadBasedPedFilter.spread(TestBoo
-ksHelper.book("A", "2", "4"));                                 
81 +        BigDecimal spread = SpreadBasedPedFilter.spread(valid("
+A", "2", "4"));                                                
82  
83          assertThat(spread.precision()).isEqualTo(34);
84          assertThat(spread).isBetween(new BigDecimal("0.66666666
66666666666666666666666666"),
⎿  PostToolUse:Edit hook returned blocking error
⎿  Fast tests failed after this edit (./mvnw -q test
-DexcludedGroups=slow,wip):
[ERROR] COMPILATION ERROR :
[ERROR] /home/fur/Projects/obsi/src/test/java/com/cfbenchmarks/obsi/deviati
on/SpreadBasedPedFilterTest.java:[71,26] cannot find symbol
symbol:   method
valid(java.lang.String,java.lang.String,java.lang.String)
location: class com.cfbenchmarks.obsi.deviation.SpreadBasedPedFilterTest
[ERROR] /home/fur/Projects/obsi/src/test/java/com/cfbenchmarks/obsi/deviati
on/SpreadBasedPedFilterTest.java:[81,57] cannot find symbol
symbol:   method
valid(java.lang.String,java.lang.String,java.lang.String)
location: class com.cfbenchmarks.obsi.deviation.SpreadBasedPedFilterTest
[ERROR] Failed to execute goal
org.apache.maven.plugins:maven-compiler-plugin:3.16.0:testCompile
(default-testCompile) on project obsi: Compilation failure: Compilation
failure:
[ERROR] /home/fur/Projects/obsi/src/test/java/com/cfbenchmarks/obsi/deviati
on/SpreadBasedPedFilterTest.java:[71,26] cannot find symbol
[ERROR]   symbol:   method
valid(java.lang.String,java.lang.String,java.lang.String)
[ERROR]   location: class
com.cfbenchmarks.obsi.deviation.SpreadBasedPedFilterTest
[ERROR] /home/fur/Projects/obsi/src/test/java/com/cfbenchmarks/obsi/deviati
on/SpreadBasedPedFilterTest.java:[81,57] cannot find symbol
[ERROR]   symbol:   method
valid(java.lang.String,java.lang.String,java.lang.String)
[ERROR]   location: class
com.cfbenchmarks.obsi.deviation.SpreadBasedPedFilterTest
[ERROR] -> [Help 1]
[ERROR]
[ERROR] To see the full stack trace of the errors, re-run Maven with the -e
switch.
[ERROR] Re-run Maven using the -X switch to enable full debug logging.
[ERROR]
[ERROR] For more information about the errors and possible solutions,
please read the following articles:
[ERROR] [Help 1]
http://cwiki.apache.org/confluence/display/MAVEN/MojoFailureException

● Update(src/test/java/com/cfbenchmarks/obsi/deviation/SpreadBasedPedFilterTest.
java)
⎿  Added 1 line, removed 1 line
24  
25  import static com.cfbenchmarks.obsi.TestBooks.fixture;
26  import static com.cfbenchmarks.obsi.TestBooks.names;
27 -import static com.cfbenchmarks.obsi.TestBooks.snapshot;        
27  import static com.cfbenchmarks.obsi.TestBooks.snapshotAt;
28 +import static com.cfbenchmarks.obsi.TestBooks.valid;           
29  import static com.cfbenchmarks.obsi.TestBooks.validAtMid;
30  import static com.cfbenchmarks.obsi.TestBooks.validWithSpread;
31  import static org.assertj.core.api.Assertions.assertThat;

● Update(src/test/java/com/cfbenchmarks/obsi/deviation/SpreadBasedPedFilterTest.
java)
⎿  Removed 1 line
18  import java.math.RoundingMode;
19  import java.time.Instant;
20  import java.util.ArrayList;
21 -import java.util.Collections;                                  
21  import java.util.List;
22  import java.util.stream.Stream;
23

● Those compile errors were intermediate states between my edits. The last edit
completes the change. Next is the property test.

● Write(src/test/java/com/cfbenchmarks/obsi/deviation/SpreadBasedPedFilterProper
tyTest.java)
⎿  Wrote 204 lines to src/test/java/com/cfbenchmarks/obsi/deviation/Sprea
dBasedPedFilterPropertyTest.java
1 package com.cfbenchmarks.obsi.deviation;
2
3 import com.cfbenchmarks.obsi.book.ValidBook;
4 import com.cfbenchmarks.obsi.input.Snapshot;
5 import org.junit.jupiter.api.Tag;
6 import org.junit.jupiter.params.ParameterizedTest;
7 import org.junit.jupiter.params.provider.MethodSource;
8
9 import java.math.BigDecimal;
10 import java.math.MathContext;
… +194 lines (ctrl+o to expand)

● Update(src/test/java/com/cfbenchmarks/obsi/deviation/SpreadBasedPedFilterPrope
rtyTest.java)
⎿  Added 1 line, removed 1 line
189          }
190      }
191  
192 -    // keeps the plain valid(...) builder in use for a one-boo
-k case                                                        
192 +    /** Adding a book exactly at the median keeps the median a
+nd has deviation 0, so that book is never excluded. */        
193      @ParameterizedTest
194      @MethodSource("bookSets")
195      void aBookAtTheMedianIsNeverExcluded(List<ValidBook> books
) {

● Write(src/test/java/com/cfbenchmarks/obsi/input/UnlockEventsInputTest.java)
⎿  Wrote 149 lines to
src/test/java/com/cfbenchmarks/obsi/input/UnlockEventsInputTest.java
1 package com.cfbenchmarks.obsi.input;
2
3 import com.cfbenchmarks.obsi.input.InputResult.Accepted;
4 import com.cfbenchmarks.obsi.input.InputResult.Rejected;
5 import org.junit.jupiter.api.Tag;
6 import org.junit.jupiter.api.Test;
7 import org.junit.jupiter.params.ParameterizedTest;
8 import org.junit.jupiter.params.provider.Arguments;
9 import org.junit.jupiter.params.provider.MethodSource;
… +140 lines (ctrl+o to expand)

● Now MainTest: I'm replacing the two refusal tests and adding the wiring tests.

● Update(src/test/java/com/cfbenchmarks/obsi/cli/MainTest.java)
⎿  Added 78 lines, removed 8 lines
198          assertFailure(run("--verbose", "data/sample-input.json
"), 2, null, "usage error: unknown option --verbose");
199      }
200  
201 -    /** Until Task 2 the flag is recognised and refused, where
-ver it is, and nothing is calculated. */                      
201 +    // -------------------------------------------------------
+--------- --new-ped (Task 2; D-22 superseded by D-23)         
202 +                                                              
203 +    /** 25. The flag runs the spread-based rule, before or aft
+er the path, once or twice. */                                
204      @Test
203 -    void newPedIsNotImplementedYetBeforeOrAfterThePath() {    
204 -        String reason = "usage error: --new-ped is not impleme
-nted yet";                                                    
205 +    @Tag("wip")                                               
206 +    void newPedPublishesBeforeOrAfterThePathAndTwiceIsOnce() t
+hrows IOException {                                           
207 +        for (String[] args : new String[][] {                 
208 +                {"--new-ped", "data/sample-input.json"},      
209 +                {"data/sample-input.json", "--new-ped"},      
210 +                {"--new-ped", "data/sample-input.json", "--new
+-ped"}}) {                                                    
211 +            Run run = run(args);                              
212  
206 -        assertFailure(run("--new-ped", "data/sample-input.json
-"), 2, null, reason);                                         
207 -        assertFailure(run("data/sample-input.json", "--new-ped
-"), 2, null, reason);                                         
213 +            assertThat(run.exit()).as(String.join(" ", args)).
+isZero();                                                     
214 +            assertThat(run.err()).isEmpty();                  
215 +            assertThat(JSON.readTree(run.out()).get("status").
+asText()).isEqualTo("published");                             
216 +        }                                                     
217      }
218  
210 -    /** The arguments are checked before the file is read, so
-the flag is reported even with a bad path. */                 
219 +    /** D-23: the flag no longer short-circuits, so a bad path
+ is the usual input error. */                                 
220      @Test
212 -    void newPedIsReportedBeforeTheFileIsRead() {              
213 -        assertFailure(run("--new-ped", "no-such-file.json"), 2
-, null, "usage error: --new-ped is not implemented yet");     
221 +    @Tag("wip")                                               
222 +    void newPedWithAMissingFileIsAnInputError() {             
223 +        assertFailure(run("--new-ped", "no-such-file.json"), 2
+, null, "input error: cannot read input file");               
224      }
225  
226 +    /** 26. On the sample, the spread-based rule gives the sam
+e result as the 5% rule (proposal.md). */                     
227 +    @Test                                                     
228 +    @Tag("wip")                                               
229 +    void newPedOnTheSamplePublishes100Point23AndExcludesGammaA
+ndDelta() throws IOException {                                
230 +        Run run = run("--new-ped", "data/sample-input.json");
231 +                                                              
232 +        JsonNode result = JSON.readTree(run.out());           
233 +        assertThat(run.exit()).isZero();                      
234 +        assertThat(result.get("value").asText()).isEqualTo("10
+0.23");                                                       
235 +        assertThat(result.get("metadata").get("excludedExchang
+es").toString()).isEqualTo("[\"Gamma\",\"Delta\"]");          
236 +    }                                                         
237 +                                                              
238 +    /** 27. D-23: malformed unlockEvents is an input error wit
+h the flag and is ignored (D-04) without it. */               
239 +    @Test                                                     
240 +    @Tag("wip")                                               
241 +    void malformedUnlockEventsFailWithTheFlagAndAreIgnoredWith
+out() throws IOException {                                    
242 +        String malformed = "{\"calculationTime\":\"" + TIME +
+"\",\"books\":[" + goodBook("A") + "],\"unlockEvents\":{}}";  
243 +        String path = file(malformed).toString();             
244 +                                                              
245 +        assertFailure(run("--new-ped", path), 2, null, "input
+error: unlockEvents must be an array");                       
246 +        Run without = run(path);                              
247 +        assertThat(without.exit()).isZero();                  
248 +        assertThat(without.err()).isEmpty();                  
249 +    }                                                         
250 +                                                              
251 +    // -------------------------------------------------------
+--------- data/new-ped.json                                   
252 +                                                              
253 +    /**                                                       
254 +     * 28. data/new-ped.json: sample + an unlock window coveri
+ng its calculationTime + Epsilon, which the 5% rule keeps     
255 +     * (2.2% from the median) and the unlock band excludes. On
+ly the excluded lists are pinned, not the index value.        
256 +     */                                                       
257 +    @Test                                                     
258 +    @Tag("wip")                                               
259 +    void newPedJsonGivesDifferentResultsWithAndWithoutTheFlag(
+) throws IOException {                                        
260 +        Run without = run("data/new-ped.json");               
261 +        Run with = run("--new-ped", "data/new-ped.json");     
262 +                                                              
263 +        assertThat(without.exit()).isZero();                  
264 +        assertThat(with.exit()).isZero();                     
265 +        assertThat(without.err()).isEmpty();                  
266 +        assertThat(with.err()).isEmpty();                     
267 +        assertThat(JSON.readTree(without.out()).get("metadata"
+).get("excludedExchanges").toString())                        
268 +                .isEqualTo("[\"Gamma\",\"Delta\"]");          
269 +        assertThat(JSON.readTree(with.out()).get("metadata").g
+et("excludedExchanges").toString())                           
270 +                .isEqualTo("[\"Gamma\",\"Delta\",\"Epsilon\"]"
+);                                                            
271 +        assertThat(JSON.readTree(with.out())).isNotEqualTo(JSO
+N.readTree(without.out()));                                   
272 +    }                                                         
273 +                                                              
274 +    /** 29. The window is what excludes Epsilon: the same file
+ with calculationTime after the window keeps it. */           
275 +    @Test                                                     
276 +    @Tag("wip")                                               
277 +    void outsideTheWindowNewPedKeepsEpsilon() throws IOExcepti
+on {                                                          
278 +        String text = Files.readString(Path.of("data/new-ped.j
+son")).replace("2026-10-27T12:00:00Z", "2026-10-29T12:00:00Z")
+;                                                             
279 +        Run run = run("--new-ped", file(text).toString());    
280 +                                                              
281 +        assertThat(run.exit()).isZero();                      
282 +        assertThat(JSON.readTree(run.out()).get("metadata").ge
+t("excludedExchanges").toString())                            
283 +                .isEqualTo("[\"Gamma\",\"Delta\"]");          
284 +    }                                                         
285 +                                                              
286      // -------------------------------------------------------
--------- calculation failures (exit 1, calculationTime echoed
)
287  
288      @Test
⎿  PostToolUse:Edit hook returned blocking error
⎿  Fast tests failed after this edit (./mvnw -q test
-DexcludedGroups=slow,wip):
[ERROR] COMPILATION ERROR :
[ERROR] /home/fur/Projects/obsi/src/test/java/com/cfbenchmarks/obsi/cli/Mai
nTest.java:[205,6] cannot find symbol
symbol:   class Tag
location: class com.cfbenchmarks.obsi.cli.MainTest
[ERROR] /home/fur/Projects/obsi/src/test/java/com/cfbenchmarks/obsi/cli/Mai
nTest.java:[221,6] cannot find symbol
symbol:   class Tag
location: class com.cfbenchmarks.obsi.cli.MainTest
[ERROR] /home/fur/Projects/obsi/src/test/java/com/cfbenchmarks/obsi/cli/Mai
nTest.java:[228,6] cannot find symbol
symbol:   class Tag
location: class com.cfbenchmarks.obsi.cli.MainTest
[ERROR] /home/fur/Projects/obsi/src/test/java/com/cfbenchmarks/obsi/cli/Mai
nTest.java:[240,6] cannot find symbol
symbol:   class Tag
location: class com.cfbenchmarks.obsi.cli.MainTest
[ERROR] /home/fur/Projects/obsi/src/test/java/com/cfbenchmarks/obsi/cli/Mai
nTest.java:[258,6] cannot find symbol
symbol:   class Tag
location: class com.cfbenchmarks.obsi.cli.MainTest
[ERROR] /home/fur/Projects/obsi/src/test/java/com/cfbenchmarks/obsi/cli/Mai
nTest.java:[276,6] cannot find symbol
symbol:   class Tag
location: class com.cfbenchmarks.obsi.cli.MainTest
[ERROR] Failed to execute goal
org.apache.maven.plugins:maven-compiler-plugin:3.16.0:testCompile
(default-testCompile) on project obsi: Compilation failure: Compilation
failure:
[ERROR] /home/fur/Projects/obsi/src/test/java/com/cfbenchmarks/obsi/cli/Mai
nTest.java:[205,6] cannot find symbol
[ERROR]   symbol:   class Tag
[ERROR]   location: class com.cfbenchmarks.obsi.cli.MainTest
[ERROR] /home/fur/Projects/obsi/src/test/java/com/cfbenchmarks/obsi/cli/Mai
nTest.java:[221,6] cannot find symbol
[ERROR]   symbol:   class Tag
[ERROR]   location: class com.cfbenchmarks.obsi.cli.MainTest
[ERROR] /home/fur/Projects/obsi/src/test/java/com/cfbenchmarks/obsi/cli/Mai
nTest.java:[228,6] cannot find symbol
[ERROR]   symbol:   class Tag
[ERROR]   location: class com.cfbenchmarks.obsi.cli.MainTest
[ERROR] /home/fur/Projects/obsi/src/test/java/com/cfbenchmarks/obsi/cli/Mai
nTest.java:[240,6] cannot find symbol
[ERROR]   symbol:   class Tag
[ERROR]   location: class com.cfbenchmarks.obsi.cli.MainTest
[ERROR] /home/fur/Projects/obsi/src/test/java/com/cfbenchmarks/obsi/cli/Mai
nTest.java:[258,6] cannot find symbol
[ERROR]   symbol:   class Tag
[ERROR]   location: class com.cfbenchmarks.obsi.cli.MainTest
[ERROR] /home/fur/Projects/obsi/src/test/java/com/cfbenchmarks/obsi/cli/Mai
nTest.java:[276,6] cannot find symbol
[ERROR]   symbol:   class Tag
[ERROR]   location: class com.cfbenchmarks.obsi.cli.MainTest
[ERROR] -> [Help 1]
[ERROR]
[ERROR] To see the full stack trace of the errors, re-run Maven with the -e
switch.
[ERROR] Re-run Maven using the -X switch to enable full debug logging.
[ERROR]
[ERROR] For more information about the errors and possible solutions,
please read the following articles:
[ERROR] [Help 1]
http://cwiki.apache.org/confluence/display/MAVEN/MojoFailureException

● Update(src/test/java/com/cfbenchmarks/obsi/cli/MainTest.java)
⎿  Added 1 line
4  import com.fasterxml.jackson.databind.DeserializationFeature;
5  import com.fasterxml.jackson.databind.JsonNode;
6  import com.fasterxml.jackson.databind.ObjectMapper;
7 +import org.junit.jupiter.api.Tag;                              
8  import org.junit.jupiter.api.Test;
9  import org.junit.jupiter.api.io.TempDir;
10

● Last, GoldenTest, with the explicit naming rule you chose.

● Update(src/test/java/com/cfbenchmarks/obsi/cli/GoldenTest.java)
⎿  Added 34 lines, removed 14 lines
34      private static final ObjectMapper JSON = new ObjectMapper()
35              .enable(DeserializationFeature.FAIL_ON_TRAILING_TOK
ENS);
36  
37 -    /** Pairs of (input, expected) found on disk; the walk is r
-ecursive so directory names don't matter. */                   
38 -    static Stream<Path[]> goldenPairs() throws IOException {   
39 -        List<Path[]> pairs;                                    
37 +    /**                                                        
38 +     * One input run in one mode against one expected file. The
+ mode is in the expected file's name:                          
39 +     * {@code X.expected.json} is the run without a flag and {@
+code X.new-ped.expected.json} the run with                     
40 +     * {@code --new-ped}, both for {@code data/**}/X.json.     
41 +     */                                                        
42 +    record Pair(Path input, Path expected, boolean newPed) {   
43 +        @Override                                              
44 +        public String toString() {                             
45 +            return (newPed ? "--new-ped " : "") + input;       
46 +        }                                                      
47 +    }                                                          
48 +                                                               
49 +    /** Pairs found on disk, in either mode; the walk is recurs
+ive so directory names don't matter. */                        
50 +    static Stream<Pair> goldenPairs() throws IOException {     
51 +        List<Pair> pairs = new java.util.ArrayList<>();        
52          try (Stream<Path> inputs = Files.walk(DATA)) {
41 -            pairs = inputs                                     
42 -                    .filter(p -> Files.isRegularFile(p) && p.ge
-tFileName().toString().endsWith(".json"))                      
43 -                    .sorted()                                  
44 -                    .map(p -> new Path[] {p, EXPECTED.resolve(s
-tem(p) + SUFFIX)})                                             
45 -                    .filter(pair -> Files.isRegularFile(pair[1]
-))                                                             
46 -                    .toList();                                 
53 +            for (Path p : inputs.filter(f -> Files.isRegularFil
+e(f) && f.getFileName().toString().endsWith(".json"))          
54 +                    .sorted().toList()) {                      
55 +                Path plain = EXPECTED.resolve(stem(p) + SUFFIX)
+;                                                              
56 +                Path flagged = EXPECTED.resolve(stem(p) + NEW_P
+ED_SUFFIX);                                                    
57 +                if (Files.isRegularFile(plain)) {              
58 +                    pairs.add(new Pair(p, plain, false));      
59 +                }                                              
60 +                if (Files.isRegularFile(flagged)) {            
61 +                    pairs.add(new Pair(p, flagged, true));     
62 +                }                                              
63 +            }                                                  
64          }
65          return pairs.stream();
66      }
...
72  
73      @ParameterizedTest(name = "{0}")
74      @MethodSource("goldenPairs")
58 -    void producesTheExpectedResult(Path input, Path expected)
-throws Exception {                                            
75 +    void producesTheExpectedResult(Pair pair) throws Exception
+ {                                                            
76          var out = new ByteArrayOutputStream();
77          var err = new ByteArrayOutputStream();
78  
62 -        int exit = Main.run(new String[] {input.toString()},  
79 +        String[] args = pair.newPed()                         
80 +                ? new String[] {"--new-ped", pair.input().toSt
+ring()}                                                       
81 +                : new String[] {pair.input().toString()};     
82 +        int exit = Main.run(args,                             
83                  new PrintStream(out, true, StandardCharsets.UT
F_8),
84                  new PrintStream(err, true, StandardCharsets.UT
F_8));
85  
86          assertThat(err.toString(StandardCharsets.UTF_8)).as("s
tderr").isEmpty();
87          assertThat(exit).as("exit code").isZero();
88          JsonNode actual = JSON.readTree(out.toString(StandardC
harsets.UTF_8));
69 -        assertThat(actual).as("stdout JSON").isEqualTo(JSON.re
-adTree(expected.toFile()));                                   
89 +        assertThat(actual).as("stdout JSON").isEqualTo(JSON.re
+adTree(pair.expected().toFile()));                            
90      }
91  
92      /** Guard: a renamed or mistyped file must not turn the go
lden test into a silent zero-case pass. */
93      @Test
94      void everyExpectedFileHasAnInputAndSomePairExists() throws
IOException {
75 -        List<Path[]> pairs = goldenPairs().toList();          
95 +        List<Pair> pairs = goldenPairs().toList();            
96          assertThat(pairs).as("input/expected pairs found").isN
otEmpty();
97  
78 -        List<String> paired = pairs.stream().map(pair -> pair[
-1].getFileName().toString()).toList();                        
98 +        List<String> paired = pairs.stream().map(pair -> pair.
+expected().getFileName().toString()).toList();                
99          try (Stream<Path> expected = Files.list(EXPECTED)) {
100              List<String> orphans = expected
101                      .map(p -> p.getFileName().toString())
⎿  PostToolUse:Edit hook returned blocking error
⎿  Fast tests failed after this edit (./mvnw -q test
-DexcludedGroups=slow,wip):
[ERROR] COMPILATION ERROR :
[ERROR] /home/fur/Projects/obsi/src/test/java/com/cfbenchmarks/obsi/cli/Gol
denTest.java:[56,59] cannot find symbol
symbol:   variable NEW_PED_SUFFIX
location: class com.cfbenchmarks.obsi.cli.GoldenTest
[ERROR] Failed to execute goal
org.apache.maven.plugins:maven-compiler-plugin:3.16.0:testCompile
(default-testCompile) on project obsi: Compilation failure
[ERROR] /home/fur/Projects/obsi/src/test/java/com/cfbenchmarks/obsi/cli/Gol
denTest.java:[56,59] cannot find symbol
[ERROR]   symbol:   variable NEW_PED_SUFFIX
[ERROR]   location: class com.cfbenchmarks.obsi.cli.GoldenTest
[ERROR] -> [Help 1]
[ERROR]
[ERROR] To see the full stack trace of the errors, re-run Maven with the -e
switch.
[ERROR] Re-run Maven using the -X switch to enable full debug logging.
[ERROR]
[ERROR] For more information about the errors and possible solutions,
please read the following articles:
[ERROR] [Help 1]
http://cwiki.apache.org/confluence/display/MAVEN/MojoFailureException

● Update(src/test/java/com/cfbenchmarks/obsi/cli/GoldenTest.java)
⎿  Added 1 line
29      private static final Path DATA = Path.of("data");
30      private static final Path EXPECTED = Path.of("src/test/reso
urces/expected");
31      private static final String SUFFIX = ".expected.json";
32 +    private static final String NEW_PED_SUFFIX = ".new-ped.expe
+cted.json";                                                    
33      // Trailing content after the result is an error: stdout mu
st be exactly one JSON object
34      // (CLAUDE.md: "Nothing else goes to stdout").
35      private static final ObjectMapper JSON = new ObjectMapper()

● Update(src/test/java/com/cfbenchmarks/obsi/cli/GoldenTest.java)
⎿  Added 1 line
22   * Golden test: the definition of "done" for the whole project.
Every input under {@code data/}
23   * that has a matching {@code src/test/resources/expected/<name
>.expected.json} must produce
24   * exactly that JSON tree (compared as parsed JSON, not text),
with empty stderr and exit 0.
25 + * {@code <name>.new-ped.expected.json} is the same input run w
+ith {@code --new-ped}.                                         
26   * Inputs without an expected file are skipped.
27   */
28  class GoldenTest {

Ran 1 shell command (ctrl+o to expand)

● Fast tests pass, and the wip tests are red as intended. Now the
implementation, beginning with the input parser.

● Update(src/main/java/com/cfbenchmarks/obsi/input/InputParser.java)
⎿  Added 17 lines, removed 7 lines
66          return parse(bytes);
67      }
68  
69 -    /** Stub: as {@link #parse(Path)}, and reads {@code unlockE
-vents} when asked (D-23). */                                   
69 +    /**                                                        
70 +     * As {@link #parse(Path)}, and with {@code readUnlockEvent
+s} also reads the optional {@code unlockEvents} (what          
71 +     * {@code --new-ped} needs). Without it the field is not lo
+oked at (D-04); with it a malformed value is an input          
72 +     * error (D-23).                                           
73 +     */                                                        
74      public static InputResult parse(Path file, boolean readUnlo
ckEvents) {
71 -        return parse(file);                                    
75 +        byte[] bytes;                                          
76 +        try {                                                  
77 +            bytes = Files.readAllBytes(file);                  
78 +        } catch (IOException | SecurityException e) {          
79 +            return reject(CANNOT_READ);                        
80 +        }                                                      
81 +        return parse(bytes, readUnlockEvents);                 
82      }
83  
74 -    /** Stub: as {@link #parse(byte[])}, and reads {@code unloc
-kEvents} when asked (D-23). */                                 
75 -    public static InputResult parse(byte[] json, boolean readUn
-lockEvents) {                                                  
76 -        return parse(json);                                    
84 +    /** Parses the UTF-8 bytes of an input document. {@code unl
+ockEvents} is ignored (D-04). */                               
85 +    public static InputResult parse(byte[] json) {             
86 +        return parse(json, false);                             
87      }
88  
79 -    /** Parses the UTF-8 bytes of an input document. */        
80 -    public static InputResult parse(byte[] json) {             
89 +    /** As {@link #parse(byte[])}, and with {@code readUnlockEv
+ents} also reads {@code unlockEvents} (D-23). */               
90 +    public static InputResult parse(byte[] json, boolean readUn
+lockEvents) {                                                  
91          JsonNode root;
92          try {
93              root = JSON.readTree(json);

● Update(src/main/java/com/cfbenchmarks/obsi/input/InputParser.java)
⎿  Added 39 lines, removed 1 line
148              }
149          }
150  
151 -        return new InputResult.Accepted(new Snapshot(time.text
-Value(), books));                                             
151 +        // D-23: checked last, so every other input error keep
+s its precedence. Without --new-ped the field is not          
152 +        // looked at (D-04).                                  
153 +        List<Instant> unlockStarts = new ArrayList<>();       
154 +        if (readUnlockEvents) {                               
155 +            String problem = readUnlockEvents(root.get("unlock
+Events"), unlockStarts);                                      
156 +            if (problem != null) {                            
157 +                return reject(problem);                       
158 +            }                                                 
159 +        }                                                     
160 +                                                              
161 +        return new InputResult.Accepted(new Snapshot(time.text
+Value(), books, unlockStarts));                               
162      }
163  
164      /**
165 +     * D-23: absent means no unlock; otherwise an array of obj
+ects, each with a {@code start} that is a UTC instant         
166 +     * (the D-15 rule). Extra keys in an event are ignored (D-
+04).                                                          
167 +     *
168 +     * @param starts receives the {@code start} of every event
+, in input order                                              
169 +     * @return the input error reason, or null if the field is
+ well formed                                                  
170 +     */                                                       
171 +    private static String readUnlockEvents(JsonNode events, Li
+st<Instant> starts) {                                         
172 +        if (events == null) {                                 
173 +            return null;                                      
174 +        }                                                     
175 +        if (!events.isArray()) {                              
176 +            return UNLOCK_NOT_ARRAY;                          
177 +        }                                                     
178 +        for (int i = 0; i < events.size(); i++) {             
179 +            JsonNode event = events.get(i);                   
180 +            if (!event.isObject()) {                          
181 +                return "input error: unlockEvents[" + i + "] m
+ust be an object";                                            
182 +            }                                                 
183 +            JsonNode start = event.get("start");              
184 +            if (start == null || !isUtcInstant(start)) {      
185 +                return "input error: unlockEvents[" + i + "].s
+tart must be an ISO-8601 UTC timestamp ending in Z";          
186 +            }                                                 
187 +            starts.add(Instant.parse(start.textValue()));     
188 +        }                                                     
189 +        return null;                                          
190 +    }                                                         
191 +                                                              
192 +    /**                                                       
193       * D-03, D-06: a book with a valid name whose {@code bids}
or {@code asks} is missing or not an
194       * array is kept as an {@link BookInput.UnreadableBook}, s
o it can be excluded and reported
195       * (spec §2: "Exclude a book if its structure cannot be re
ad").
⎿  PostToolUse:Edit hook returned blocking error
⎿  Fast tests failed after this edit (./mvnw -q test
-DexcludedGroups=slow,wip):
[ERROR] COMPILATION ERROR :
[ERROR] /home/fur/Projects/obsi/src/main/java/com/cfbenchmarks/obsi/input/I
nputParser.java:[176,20] cannot find symbol
symbol:   variable UNLOCK_NOT_ARRAY
location: class com.cfbenchmarks.obsi.input.InputParser
[ERROR] Failed to execute goal
org.apache.maven.plugins:maven-compiler-plugin:3.16.0:compile
(default-compile) on project obsi: Compilation failure
[ERROR] /home/fur/Projects/obsi/src/main/java/com/cfbenchmarks/obsi/input/I
nputParser.java:[176,20] cannot find symbol
[ERROR]   symbol:   variable UNLOCK_NOT_ARRAY
[ERROR]   location: class com.cfbenchmarks.obsi.input.InputParser
[ERROR] -> [Help 1]
[ERROR]
[ERROR] To see the full stack trace of the errors, re-run Maven with the -e
switch.
[ERROR] Re-run Maven using the -X switch to enable full debug logging.
[ERROR]
[ERROR] For more information about the errors and possible solutions,
please read the following articles:
[ERROR] [Help 1]
http://cwiki.apache.org/confluence/display/MAVEN/MojoFailureException

● Update(src/main/java/com/cfbenchmarks/obsi/input/InputParser.java)
⎿  Added 1 line
39              "input error: calculationTime must be an ISO-8601 U
TC timestamp ending in Z";
40      static final String BOOKS_MISSING = "input error: books is
missing";
41      static final String BOOKS_NOT_ARRAY = "input error: books m
ust be an array";
42 +    static final String UNLOCK_NOT_ARRAY = "input error: unlock
+Events must be an array";                                      
43  
44      private static final ObjectMapper JSON = JsonMapper.builder
()
45              // CLAUDE.md "Numbers": 100.2 must never pass throu
gh a double.

● Write(src/main/java/com/cfbenchmarks/obsi/deviation/SpreadBasedPedFilter.java)
⎿  Added 96 lines, removed 7 lines
4  import com.cfbenchmarks.obsi.input.Snapshot;
5  
6  import java.math.BigDecimal;
7 +import java.math.MathContext;                                 
8 +import java.time.Duration;                                    
9 +import java.time.Instant;                                     
10 +import java.util.ArrayList;                                   
11  import java.util.List;
12  
9 -/** Stub: the Task 2 rule (proposal.md, Approach 2). Implement
-ed after its tests. */                                        
13 +/**                                                           
14 + * The Task 2 deviation rule: a liquidity-adaptive PED with an
+ unlock regime (proposal.md, "Approach 2: a spread-based      
15 + * limit with an unlock regime"). For every snapshot it takes
+the relative spread {@code (best ask - best bid) / mid}       
16 + * of each structurally valid book, the median of those spread
+s {@code S}, and the limit {@code k × S} clamped between      
17 + * a floor and a cap. If {@code calculationTime} is within 48
+hours of the start of an announced unlock, the unlock         
18 + * band's floor and cap apply. A book is excluded when {@code
+|m - median| > limit × median}, the same comparison as the    
19 + * 5% rule (spec §2) but with this limit; it needs no division
+.                                                             
20 + *
21 + * <p>The limit is never rounded (D-24). The only division is
+the spread, at {@code MathContext.DECIMAL128}. The check      
22 + * is stateless: only this snapshot's books and its {@code unl
+ockEvents} are used (spec §2, "each snapshot is evaluated     
23 + * independently"), and there is no reentry or half-limit test
+ (proposal.md, "Risks and limitations").                      
24 + */                                                           
25  public final class SpreadBasedPedFilter implements DeviationFi
lter {
26  
27 +    // The illustrative parameters of proposal.md, Approach 2.
+ In production they would be calibrated and approved by       
28 +    // the committee (proposal.md, "Risks and limitations").  
29 +    /** proposal.md, Approach 2: {@code k = 10}, the multiple
+of the spread that stands for the rest of the arbitrage cost.
+*/                                                            
30 +    static final BigDecimal K = BigDecimal.TEN;               
31 +    /** proposal.md, Approach 2: normal regime, floor 1%. */  
32 +    static final BigDecimal NORMAL_FLOOR = new BigDecimal("0.0
+1");                                                          
33 +    /** proposal.md, Approach 2: normal regime, cap 5%, today'
+s static limit (spec §7). */                                  
34 +    static final BigDecimal NORMAL_CAP = new BigDecimal("0.05"
+);                                                            
35 +    /** proposal.md, Approach 2: unlock regime, floor 0.5%. */
36 +    static final BigDecimal UNLOCK_FLOOR = new BigDecimal("0.0
+05");                                                         
37 +    /** proposal.md, Approach 2: unlock regime, cap 1%. */    
38 +    static final BigDecimal UNLOCK_CAP = new BigDecimal("0.01"
+);                                                            
39 +    /** proposal.md, "Input change": the window runs from {@co
+de start} (inclusive) to {@code start + 48h} (exclusive). */  
40 +    static final Duration UNLOCK_WINDOW = Duration.ofHours(48)
+;                                                             
41 +                                                              
42      @Override
43      public List<ValidBook> excluded(List<ValidBook> structural
lyValid, Snapshot snapshot) {
14 -        throw new UnsupportedOperationException("not implement
-ed");                                                         
44 +        if (structurallyValid.isEmpty()) {                    
45 +            return List.of();                                 
46 +        }                                                     
47 +        boolean inUnlock = inUnlockWindow(Instant.parse(snapsh
+ot.calculationTime()), snapshot.unlockStarts());              
48 +        BigDecimal limit = limit(structurallyValid, inUnlock);
49 +        // §2: the median of the structurally valid mids, as i
+n the 5% rule.                                                
50 +        BigDecimal median = StaticPedFilter.median(structurall
+yValid.stream().map(ValidBook::mid).toList());                
51 +                                                              
52 +        List<ValidBook> excluded = new ArrayList<>();         
53 +        for (ValidBook book : structurallyValid) {            
54 +            if (deviatesMoreThanTheLimit(book.mid(), median, l
+imit)) {                                                      
55 +                excluded.add(book);                           
56 +            }                                                 
57 +        }                                                     
58 +        return excluded;                                      
59      }
60  
17 -    static BigDecimal spread(ValidBook book) {                
18 -        throw new UnsupportedOperationException("not implement
-ed");                                                         
61 +    /**                                                       
62 +     * proposal.md, Approach 2: exclude when {@code |m - media
+n| / median > limit}, written without a division              
63 +     * (CLAUDE.md "Numbers"): {@code |m - median| > limit × me
+dian}. The median is a mean of positive prices, so it is      
64 +     * positive and multiplying by it keeps the direction of t
+he inequality. Exactly on the limit passes, as in §2.         
65 +     */                                                       
66 +    private static boolean deviatesMoreThanTheLimit(BigDecimal
+ mid, BigDecimal median, BigDecimal limit) {                  
67 +        return mid.subtract(median).abs().compareTo(limit.mult
+iply(median)) > 0;                                            
68      }
69  
70 +    /**                                                       
71 +     * proposal.md, Approach 2: {@code limit = k × S}, clamped
+ to {@code [floor, cap]} of the regime. A fraction, not a     
72 +     * percentage, and not rounded (D-24).                    
73 +     *
74 +     * @param structurallyValid at least one book             
75 +     * @param inUnlock          whether the unlock band applie
+s instead of the normal band                                  
76 +     */                                                       
77 +    static BigDecimal limit(List<ValidBook> structurallyValid,
+ boolean inUnlock) {                                          
78 +        BigDecimal floor = inUnlock ? UNLOCK_FLOOR : NORMAL_FL
+OOR;                                                          
79 +        BigDecimal cap = inUnlock ? UNLOCK_CAP : NORMAL_CAP;  
80 +        BigDecimal limit = K.multiply(medianSpread(structurall
+yValid));                                                     
81 +        if (limit.compareTo(floor) < 0) {                     
82 +            return floor;                                     
83 +        }                                                     
84 +        return limit.compareTo(cap) > 0 ? cap : limit;        
85 +    }                                                         
86 +                                                              
87 +    /**                                                       
88 +     * proposal.md, Approach 2: {@code S}, the median of the r
+elative spreads. An even count takes the mean of the middle   
89 +     * two, as the median of the mids does (D-24). The median
+spread, unlike the mean, cannot be widened by one exchange's  
90 +     * own spread.                                            
91 +     */                                                       
92      static BigDecimal medianSpread(List<ValidBook> structurall
yValid) {
22 -        throw new UnsupportedOperationException("not implement
-ed");                                                         
93 +        return StaticPedFilter.median(structurallyValid.stream
+().map(SpreadBasedPedFilter::spread).toList());               
94      }
95  
25 -    static BigDecimal limit(List<ValidBook> structurallyValid,
- boolean inUnlock) {                                          
26 -        throw new UnsupportedOperationException("not implement
-ed");                                                         
96 +    /**                                                       
97 +     * proposal.md, Approach 2: {@code (best ask - best bid) /
+ mid}. The only division of this rule. Mid is positive        
98 +     * (a valid book has 0 < best bid < best ask), so it is ne
+ver a division by zero. D-24: DECIMAL128.                     
99 +     */                                                       
100 +    static BigDecimal spread(ValidBook book) {                
101 +        return book.bestAsk().subtract(book.bestBid()).divide(
+book.mid(), MathContext.DECIMAL128);                          
102      }
103 +                                                              
104 +    /**                                                       
105 +     * proposal.md, "Input change": in the window when {@code
+start <= T < start + 48h} for any announced unlock. Written as
106 +     * {@code T - start < 48h} so it cannot overflow near the
+end of the {@link Instant} range (D-24).                      
107 +     */                                                       
108 +    private static boolean inUnlockWindow(Instant calculationT
+ime, List<Instant> unlockStarts) {                            
109 +        for (Instant start : unlockStarts) {                  
110 +            if (!calculationTime.isBefore(start)              
111 +                    && Duration.between(start, calculationTime
+).compareTo(UNLOCK_WINDOW) < 0) {                             
112 +                return true;                                  
113 +            }                                                 
114 +        }                                                     
115 +        return false;                                         
116 +    }                                                         
117  }

● Now Main.
● Update(src/main/java/com/cfbenchmarks/obsi/cli/Main.java)
⎿  Added 7 lines, removed 3 lines
47          System.exit(run(args, out, err));
48      }
49  
50 -    /** Runs the original 5% rule (no {@code --new-ped} yet). R
-eturns the exit code. */                                       
50 +    /**                                                        
51 +     * Runs the original 5% rule, or the spread-based rule of T
+ask 2 with {@code --new-ped} (proposal.md). Returns the        
52 +     * exit code.                                              
53 +     */                                                        
54      static int run(String[] args, PrintStream out, PrintStream
err) {
52 -        return run(args, out, err, snapshot -> ObsiCalculator.c
-alculate(snapshot, new StaticPedFilter()));                    
55 +        return run(args, out, err, null);                      
56      }
57  
58      /**
56 -     * @param calculation the calculation to run on an accepted
- snapshot; a parameter so a test can make it throw             
59 +     * @param calculation the calculation to run on an accepted
+ snapshot, or null for the real one; a parameter so a          
60 +     *                    test can make it throw               
61       */
62      static int run(String[] args, PrintStream out, PrintStream
err, Function<Snapshot, CalculationResult> calculation) {
63          // Known once the input is accepted; a failure after th
at point echoes it (D-22).
⎿  PostToolUse:Edit hook returned blocking error
⎿  Fast tests failed after this edit (./mvnw -q test
-DexcludedGroups=slow,wip):
[exit code]
expected: 1
but was: 3
at
com.cfbenchmarks.obsi.cli.MainTest.assertFailure(MainTest.java:80)
at com.cfbenchmarks.obsi.cli.MainTest.noCrossingBeforeTheCurvesEndI
sAnEmptyEquation2Set(MainTest.java:323)
at java.base/java.lang.reflect.Method.invoke(Method.java:565)
at
java.base/java.util.concurrent.FutureTask.run(FutureTask.java:328)
at java.base/java.util.concurrent.ThreadPoolExecutor.runWorker(Thre
adPoolExecutor.java:1090)
at java.base/java.util.concurrent.ThreadPoolExecutor$Worker.run(Thr
eadPoolExecutor.java:614)
at java.base/java.lang.Thread.run(Thread.java:1474)

     [ERROR] Failures: 
     [ERROR]   GoldenTest.producesTheExpectedResult:88 [stderr] 
     Expecting empty but was: "{"index":"OBSI","calculationTime":"2026-11-07T12:
     00:00Z","status":"failed","reason":"internal error: NullPointerException: 
     Cannot invoke \"java.util.function.Function.apply(Object)\" because 
     \"calculation\" is null"}
     "
     [ERROR]   GoldenTest.producesTheExpectedResult:88 [stderr] 
     Expecting empty but was: "{"index":"OBSI","calculationTime":"2026-10-27T12:
     00:00Z","status":"failed","reason":"internal error: NullPointerException: 
     Cannot invoke \"java.util.function.Function.apply(Object)\" because 
     \"calculation\" is null"}
     "
     [ERROR]   MainTest.aStdoutThatCannotBeWrittenIsAnInternalErrorNotASilentSuc
     cess:394->assertFailure:99 
     expected: "internal error: could not write the result to stdout"
      but was: "internal error: NullPointerException: Cannot invoke 
     "java.util.function.Function.apply(Object)" because "calculation" is null"
     [ERROR]   MainTest.anEmptyExcludedListIsAnEmptyArray:133 
     expected: 0
      but was: 3
     [ERROR]   MainTest.calculationTimeIsEchoedExactlyAsGiven:143 
     expected: 0
      but was: 3
     [ERROR]   
     MainTest.everyBookCrossedIsANoSurvivorFailure:298->assertFailure:80 [exit 
     code] 
     expected: 1
      but was: 3
     [ERROR]   MainTest.everyValidBookExcludedByPedSaysSo:307->assertFailure:80 
     [exit code] 
     expected: 1
      but was: 3
     [ERROR]   MainTest.noBooksIsANoSurvivorFailure:291->assertFailure:80 [exit 
     code] 
     expected: 1
      but was: 3
     [ERROR]   MainTest.noCrossingBeforeTheCurvesEndIsAnEmptyEquation2Set:323->a
     ssertFailure:80 [exit code] 
     expected: 1
      but was: 3
     [ERROR]   MainTest.nonAsciiExchangeNamesComeBackAsGiven:153 
     expected: 0
      but was: 3
     [ERROR]   MainTest.notEnoughDepthForTwoSamplesIsAnEmptyEquation2Set:315->as
     sertFailure:80 [exit code] 
     expected: 1
      but was: 3
     [ERROR]   MainTest.theSamplePublishesTheBriefsJsonOnStdout:108 
     expected: 0
      but was: 3
     [ERROR] Tests run: 5607, Failures: 12, Errors: 0, Skipped: 110
     [ERROR] Failed to execute goal 
     org.apache.maven.plugins:maven-surefire-plugin:3.6.1:test (default-test) on
     project obsi: There are test failures.
     [ERROR] 
     [ERROR] See /home/fur/Projects/obsi/target/surefire-reports for the 
     individual test results.
     [ERROR] See dump files (if any exist) [date].dump, [date]-jvmRun[N].dump 
     and [date].dumpstream.
     [ERROR] -> [Help 1]
     [ERROR] 
     [ERROR] To see the full stack trace of the errors, re-run Maven with the -e
     switch.
     [ERROR] Re-run Maven using the -X switch to enable full debug logging.
     [ERROR] 
     [ERROR] For more information about the errors and possible solutions, 
     please read the following articles:
     [ERROR] [Help 1] 
     http://cwiki.apache.org/confluence/display/MAVEN/MojoFailureException

● Update(src/main/java/com/cfbenchmarks/obsi/cli/Main.java)
⎿  Added 8 lines, removed 6 lines
69                  return fail(err, null, invalid.reason(), EXIT_I
NPUT_OR_USAGE);
70              }
71              CliArgs.Valid arguments = (CliArgs.Valid) parsed;
72 -            if (arguments.newPed()) {                          
73 -                return fail(err, null, NEW_PED_NOT_IMPLEMENTED,
- EXIT_INPUT_OR_USAGE);                                         
74 -            }                                                  
72  
76 -            // D-06, D-07: an input error has no calculationTim
-e to report.                                                   
77 -            InputResult input = InputParser.parse(arguments.pat
-h());                                                          
73 +            // D-06, D-07: an input error has no calculationTim
+e to report. D-04, D-23: unlockEvents is read, and             
74 +            // validated, only with --new-ped.                 
75 +            InputResult input = InputParser.parse(arguments.pat
+h(), arguments.newPed());                                      
76              if (input instanceof InputResult.Rejected rejected)
{
77                  return fail(err, null, rejected.reason(), EXIT_
INPUT_OR_USAGE);
78              }
79              Snapshot snapshot = ((InputResult.Accepted) input).
snapshot();
80              calculationTime = snapshot.calculationTime();
81  
84 -            CalculationResult result = calculation.apply(snapsh
-ot);                                                           
82 +            // Task 2: without the flag, the original 5% rule i
+s unchanged; with it, the spread-based rule.                   
83 +            DeviationFilter filter = arguments.newPed() ? new S
+preadBasedPedFilter() : new StaticPedFilter();                 
84 +            CalculationResult result = calculation != null     
85 +                    ? calculation.apply(snapshot)              
86 +                    : ObsiCalculator.calculate(snapshot, filter
+);                                                             
87              if (result instanceof CalculationResult.Failed fail
ed) {
88                  return fail(err, failed.calculationTime(), fail
ed.reason(), EXIT_CALCULATION_FAILED);
89              }
⎿  PostToolUse:Edit hook returned blocking error
⎿  Fast tests failed after this edit (./mvnw -q test
-DexcludedGroups=slow,wip):
[ERROR] COMPILATION ERROR :
[ERROR] /home/fur/Projects/obsi/src/main/java/com/cfbenchmarks/obsi/cli/Mai
n.java:[83,13] cannot find symbol
symbol:   class DeviationFilter
location: class com.cfbenchmarks.obsi.cli.Main
[ERROR] /home/fur/Projects/obsi/src/main/java/com/cfbenchmarks/obsi/cli/Mai
n.java:[83,63] cannot find symbol
symbol:   class SpreadBasedPedFilter
location: class com.cfbenchmarks.obsi.cli.Main
[ERROR] Failed to execute goal
org.apache.maven.plugins:maven-compiler-plugin:3.16.0:compile
(default-compile) on project obsi: Compilation failure: Compilation
failure:
[ERROR] /home/fur/Projects/obsi/src/main/java/com/cfbenchmarks/obsi/cli/Mai
n.java:[83,13] cannot find symbol
[ERROR]   symbol:   class DeviationFilter
[ERROR]   location: class com.cfbenchmarks.obsi.cli.Main
[ERROR] /home/fur/Projects/obsi/src/main/java/com/cfbenchmarks/obsi/cli/Mai
n.java:[83,63] cannot find symbol
[ERROR]   symbol:   class SpreadBasedPedFilter
[ERROR]   location: class com.cfbenchmarks.obsi.cli.Main
[ERROR] -> [Help 1]
[ERROR]
[ERROR] To see the full stack trace of the errors, re-run Maven with the -e
switch.
[ERROR] Re-run Maven using the -X switch to enable full debug logging.
[ERROR]
[ERROR] For more information about the errors and possible solutions,
please read the following articles:
[ERROR] [Help 1]
http://cwiki.apache.org/confluence/display/MAVEN/MojoFailureException

● Update(src/main/java/com/cfbenchmarks/obsi/cli/Main.java)
⎿  Removed 2 lines
30      static final int EXIT_INPUT_OR_USAGE = 2;
31      static final int EXIT_INTERNAL = 3;
32  
33 -    // D-22: Task 2 is not built yet, so the flag is recognised
- and refused.                                                  
34 -    static final String NEW_PED_NOT_IMPLEMENTED = "usage error:
- --new-ped is not implemented yet";                            
33      static final String STDOUT_WRITE_FAILED = "internal error:
could not write the result to stdout";
34      private static final int INTERNAL_REASON_LIMIT = 200;
35  
⎿  PostToolUse:Edit hook returned blocking error
⎿  Fast tests failed after this edit (./mvnw -q test
-DexcludedGroups=slow,wip):
[ERROR] COMPILATION ERROR :
[ERROR] /home/fur/Projects/obsi/src/main/java/com/cfbenchmarks/obsi/cli/Mai
n.java:[81,13] cannot find symbol
symbol:   class DeviationFilter
location: class com.cfbenchmarks.obsi.cli.Main
[ERROR] /home/fur/Projects/obsi/src/main/java/com/cfbenchmarks/obsi/cli/Mai
n.java:[81,63] cannot find symbol
symbol:   class SpreadBasedPedFilter
location: class com.cfbenchmarks.obsi.cli.Main
[ERROR] Failed to execute goal
org.apache.maven.plugins:maven-compiler-plugin:3.16.0:compile
(default-compile) on project obsi: Compilation failure: Compilation
failure:
[ERROR] /home/fur/Projects/obsi/src/main/java/com/cfbenchmarks/obsi/cli/Mai
n.java:[81,13] cannot find symbol
[ERROR]   symbol:   class DeviationFilter
[ERROR]   location: class com.cfbenchmarks.obsi.cli.Main
[ERROR] /home/fur/Projects/obsi/src/main/java/com/cfbenchmarks/obsi/cli/Mai
n.java:[81,63] cannot find symbol
[ERROR]   symbol:   class SpreadBasedPedFilter
[ERROR]   location: class com.cfbenchmarks.obsi.cli.Main
[ERROR] -> [Help 1]
[ERROR]
[ERROR] To see the full stack trace of the errors, re-run Maven with the -e
switch.
[ERROR] Re-run Maven using the -X switch to enable full debug logging.
[ERROR]
[ERROR] For more information about the errors and possible solutions,
please read the following articles:
[ERROR] [Help 1]
http://cwiki.apache.org/confluence/display/MAVEN/MojoFailureException

● Update(src/main/java/com/cfbenchmarks/obsi/cli/Main.java)
⎿  Added 2 lines
1  package com.cfbenchmarks.obsi.cli;
2  
3 +import com.cfbenchmarks.obsi.deviation.DeviationFilter;         
4 +import com.cfbenchmarks.obsi.deviation.SpreadBasedPedFilter;    
5  import com.cfbenchmarks.obsi.deviation.StaticPedFilter;
6  import com.cfbenchmarks.obsi.input.InputParser;
7  import com.cfbenchmarks.obsi.input.InputResult;

● Main is wired; the failures shown were states between edits. Next is
data/new-ped.json, written in the sample's formatting.

● Write(data/new-ped.json)
⎿  Wrote 133 lines to data/new-ped.json
1 {
2   "calculationTime": "2026-10-27T12:00:00Z",
3   "unlockEvents": [
4     {
5       "start": "2026-10-26T18:00:00Z"
6     }
7   ],
8   "books": [
9     {
10       "exchange": "Alpha",
… +123 lines (ctrl+o to expand)

Ran 2 shell commands (ctrl+o to expand)

● All 2,696 wip tests passed on the first run, which is too easy. I'm running
two quick mutation checks to confirm the boundary tests actually bite. First,
> to >= in the comparison:

● Update(src/main/java/com/cfbenchmarks/obsi/deviation/SpreadBasedPedFilter.java
)
⎿  Added 1 line, removed 1 line
64       * positive and multiplying by it keeps the direction of th
e inequality. Exactly on the limit passes, as in §2.
65       */
66      private static boolean deviatesMoreThanTheLimit(BigDecimal
mid, BigDecimal median, BigDecimal limit) {
67 -        return mid.subtract(median).abs().compareTo(limit.multi
-ply(median)) > 0;                                              
67 +        return mid.subtract(median).abs().compareTo(limit.multi
+ply(median)) >= 0;                                             
68      }
69  
70      /**

Ran 1 shell command (ctrl+o to expand)

● Update(src/main/java/com/cfbenchmarks/obsi/deviation/SpreadBasedPedFilter.java
)
⎿  Added 1 line, removed 1 line
64       * positive and multiplying by it keeps the direction of th
e inequality. Exactly on the limit passes, as in §2.
65       */
66      private static boolean deviatesMoreThanTheLimit(BigDecimal
mid, BigDecimal median, BigDecimal limit) {
67 -        return mid.subtract(median).abs().compareTo(limit.multi
-ply(median)) >= 0;                                             
67 +        return mid.subtract(median).abs().compareTo(limit.multi
+ply(median)) > 0;                                              
68      }
69  
70      /**

● Update(src/main/java/com/cfbenchmarks/obsi/deviation/SpreadBasedPedFilter.java
)
⎿  Added 1 line, removed 1 line
108      private static boolean inUnlockWindow(Instant calculationT
ime, List<Instant> unlockStarts) {
109          for (Instant start : unlockStarts) {
110              if (!calculationTime.isBefore(start)
111 -                    && Duration.between(start, calculationTime
-).compareTo(UNLOCK_WINDOW) < 0) {                             
111 +                    && Duration.between(start, calculationTime
+).compareTo(UNLOCK_WINDOW) <= 0) {                            
112                  return true;
113              }
114          }

Ran 1 shell command (ctrl+o to expand)

● Update(src/main/java/com/cfbenchmarks/obsi/deviation/SpreadBasedPedFilter.java
)
⎿  Added 1 line, removed 1 line
108      private static boolean inUnlockWindow(Instant calculationT
ime, List<Instant> unlockStarts) {
109          for (Instant start : unlockStarts) {
110              if (!calculationTime.isBefore(start)
111 -                    && Duration.between(start, calculationTime
-).compareTo(UNLOCK_WINDOW) <= 0) {                            
111 +                    && Duration.between(start, calculationTime
+).compareTo(UNLOCK_WINDOW) < 0) {                             
112                  return true;
113              }
114          }

● Both mutants were caught, and I've reverted both. Now I'm removing the wip
tags, since everything passes.

● Update(src/test/java/com/cfbenchmarks/obsi/deviation/SpreadBasedPedFilterTest.
java)
⎿  Removed 1 line
36   * <p>{@code validWithSpread} chooses S. The default {@code val
idAtMid} has an absolute spread of 0.2, which at mid 100
37   * is S = 0.2% and k·S = 2%: not a floor case.
38   */
39 -@Tag("wip")                                                    
39  class SpreadBasedPedFilterTest {
40  
41      private static final String T = "2026-10-27T12:00:00Z";

● Update(src/test/java/com/cfbenchmarks/obsi/deviation/SpreadBasedPedFilterTest.
java)
⎿  Removed 1 line
6  import com.cfbenchmarks.obsi.result.ObsiCalculator;
7  import com.cfbenchmarks.obsi.screening.Screener;
8  import com.cfbenchmarks.obsi.screening.ScreeningResult;
9 -import org.junit.jupiter.api.Tag;                              
9  import org.junit.jupiter.api.Test;
10  import org.junit.jupiter.params.ParameterizedTest;
11  import org.junit.jupiter.params.provider.Arguments;

● Update(src/test/java/com/cfbenchmarks/obsi/deviation/SpreadBasedPedFilterPrope
rtyTest.java)
⎿  Removed 1 line
26   * Property-style tests of the spread-based PED: inputs come fr
om a {@link Random} with a fixed seed, so every run is
27   * the same. proposal.md, Approach 2: the new rule is never mor
e permissive than the 5% rule (normal cap = 5%).
28   */
29 -@Tag("wip")                                                    
29  class SpreadBasedPedFilterPropertyTest {
30  
31      private static final long SEED = 20261109L;

● Update(src/test/java/com/cfbenchmarks/obsi/deviation/SpreadBasedPedFilterPrope
rtyTest.java)
⎿  Removed 1 line
2  
3  import com.cfbenchmarks.obsi.book.ValidBook;
4  import com.cfbenchmarks.obsi.input.Snapshot;
5 -import org.junit.jupiter.api.Tag;                               
5  import org.junit.jupiter.params.ParameterizedTest;
6  import org.junit.jupiter.params.provider.MethodSource;
7

● Update(src/test/java/com/cfbenchmarks/obsi/input/UnlockEventsInputTest.java)
⎿  Removed 1 line
20   * without it the field is ignored whatever it holds (D-04). Pa
rsing the rest of the document is tested in
21   * {@link InputParserTest}.
22   */
23 -@Tag("wip")                                                    
23  class UnlockEventsInputTest {
24  
25      private static final String HEAD = "{\"calculationTime\":\"
2026-10-27T12:00:00Z\",";

● Update(src/test/java/com/cfbenchmarks/obsi/input/UnlockEventsInputTest.java)
⎿  Removed 1 line
2  
3  import com.cfbenchmarks.obsi.input.InputResult.Accepted;
4  import com.cfbenchmarks.obsi.input.InputResult.Rejected;
5 -import org.junit.jupiter.api.Tag;                               
5  import org.junit.jupiter.api.Test;
6  import org.junit.jupiter.params.ParameterizedTest;
7  import org.junit.jupiter.params.provider.Arguments;

● Update(src/test/java/com/cfbenchmarks/obsi/cli/MainTest.java)  ⎿  Removed 6 lines
203  
204      /** 25. The flag runs the spread-based rule, before or aft
er the path, once or twice. */
205      @Test
206 -    @Tag("wip")                                               
206      void newPedPublishesBeforeOrAfterThePathAndTwiceIsOnce() t
hrows IOException {
207          for (String[] args : new String[][] {
208                  {"--new-ped", "data/sample-input.json"},
...
218  
219      /** D-23: the flag no longer short-circuits, so a bad path
is the usual input error. */
220      @Test
222 -    @Tag("wip")                                               
221      void newPedWithAMissingFileIsAnInputError() {
222          assertFailure(run("--new-ped", "no-such-file.json"), 2
, null, "input error: cannot read input file");
223      }
224  
225      /** 26. On the sample, the spread-based rule gives the sam
e result as the 5% rule (proposal.md). */
226      @Test
229 -    @Tag("wip")                                               
227      void newPedOnTheSamplePublishes100Point23AndExcludesGammaA
ndDelta() throws IOException {
228          Run run = run("--new-ped", "data/sample-input.json");
229  
...
235  
236      /** 27. D-23: malformed unlockEvents is an input error wit
h the flag and is ignored (D-04) without it. */
237      @Test
241 -    @Tag("wip")                                               
238      void malformedUnlockEventsFailWithTheFlagAndAreIgnoredWith
out() throws IOException {
239          String malformed = "{\"calculationTime\":\"" + TIME +
"\",\"books\":[" + goodBook("A") + "],\"unlockEvents\":{}}";
240          String path = file(malformed).toString();
...
252       * (2.2% from the median) and the unlock band excludes. On
ly the excluded lists are pinned, not the index value.
253       */
254      @Test
259 -    @Tag("wip")                                               
255      void newPedJsonGivesDifferentResultsWithAndWithoutTheFlag(
) throws IOException {
256          Run without = run("data/new-ped.json");
257          Run with = run("--new-ped", "data/new-ped.json");
...
269  
270      /** 29. The window is what excludes Epsilon: the same file
with calculationTime after the window keeps it. */
271      @Test
277 -    @Tag("wip")                                               
272      void outsideTheWindowNewPedKeepsEpsilon() throws IOExcepti
on {
273          String text = Files.readString(Path.of("data/new-ped.j
son")).replace("2026-10-27T12:00:00Z", "2026-10-29T12:00:00Z")
;
274          Run run = run("--new-ped", file(text).toString());

● Update(src/test/java/com/cfbenchmarks/obsi/cli/MainTest.java)
⎿  Removed 1 line
4  import com.fasterxml.jackson.databind.DeserializationFeature;
5  import com.fasterxml.jackson.databind.JsonNode;
6  import com.fasterxml.jackson.databind.ObjectMapper;
7 -import org.junit.jupiter.api.Tag;                              
7  import org.junit.jupiter.api.Test;
8  import org.junit.jupiter.api.io.TempDir;
9

● Now the spec trace: N.1 to N.3, plus the stale C.2 text.

● Update(docs/spec-trace.md)
⎿  Added 3 lines, removed 3 lines
94  | O.7 | Brief Output | Failure fields: `index` = `"OBSI"`, `cal
          culationTime` same as input or null, `status` = `"failed"`, `re
          ason` | `cli/Main.fail` (key order as in the brief) | `MainTest
          ` |
95  | O.8 | Brief Output | `calculationTime` is null when the input
itself doesn't conform | `cli/Main` passes null for every inpu
t and usage error; echoes it on calculation failures and, where
it is known, on unexpected errors (D-07, D-22) | `MainTest` (n
ull: invalid JSON, missing field, missing file, directory, usag
e errors; echoed: calculation failures, injected exception) |
96  | O.9 | Brief Output | The result stays on its own stream; diag
nostics go somewhere else (D-09) | `cli/Main` (writes only the
single result JSON; no diagnostics; the catch-all prints failur
e JSON and no stack trace) | `MainTest` (success: empty stderr;
failure: empty stdout; no stack trace); `./run.sh` checked by
hand |
97 -| N.1 | Brief Task 2 | `--new-ped` runs the approach chosen in
-`proposal.md` | TBD | TBD |                                    
98 -| N.2 | Brief Task 2 | Without `--new-ped`, the original 5% rul
-e (2.12, 2.13) stays | `cli/Main` runs `result/ObsiCalculator`
-with `StaticPedFilter` (the filter is a parameter, so Task 2 on
-ly adds another) | `MainTest`, `GoldenTest`, `ObsiCalculatorTes
         -t` (custom filter) |                                           
99 -| N.3 | Brief Task 2 | New or changed input fields are document
-ed in `proposal.md` with a sample in `data/new-ped.json`; if no
-thing changes, `data/new-ped.json` is a copy of `data/sample-in
         -put.json` | TBD | TBD |                                        
97 +| N.1 | Brief Task 2 | `--new-ped` runs the approach chosen in
+`proposal.md`: relative spread `(ask − bid)/mid` per structural
+ly valid book; S = median of the spreads; limit = k·S clamped t
+o [floor, cap]; exclude if `\|m − median\| > limit·median`; k =
+ 10, normal band [1%, 5%], unlock band [0.5%, 1%], unlock windo
+w `[start, start + 48h)` (D-23, D-24) | `deviation/SpreadBasedP
         +edFilter` (`K`, `NORMAL_FLOOR`, `NORMAL_CAP`, `UNLOCK_FLOOR`, `
         +UNLOCK_CAP`, `UNLOCK_WINDOW`, each citing `proposal.md`; `sprea
         +d` is the only division, DECIMAL128; the comparison is division
+-free; the limit is never rounded); `cli/Main` picks it when `-
         +-new-ped` is given | `SpreadBasedPedFilterTest` (sample limit 3
+.99%, clamps to floor and cap, exact boundaries, window start i
+ncluded and `start + 48h` excluded, several events, `T24`, no o
+verflow near `Instant.MAX`), `SpreadBasedPedFilterPropertyTest`
+ (never more permissive than 5%; the window only excludes more;
+ limit = clamp(k·S); division oracle; order independence), `Mai
         +nTest` (flag before or after the path, sample unchanged at 100.
+23, `data/new-ped.json` differs with and without the flag); mut
+ation-checked (`>` to `>=`, window end `<` to `<=`) |          
98 +| N.2 | Brief Task 2 | Without `--new-ped`, the original 5% rul
+e (2.12, 2.13) stays, and `unlockEvents` is ignored (D-04) | `c
         +li/Main` runs `result/ObsiCalculator` with `StaticPedFilter` wh
+en the flag is absent; `input/InputParser.parse(path, false)` d
+oes not read `unlockEvents` | `MainTest`, `GoldenTest`, `ObsiCa
         +lculatorTest` (custom filter), `UnlockEventsInputTest` (malform
+ed or well-formed `unlockEvents` is ignored without the flag),
+`MainTest` (malformed `unlockEvents` publishes without the flag
+) |                                                            
99 +| N.3 | Brief Task 2 | New or changed input fields are document
+ed in `proposal.md` with a sample in `data/new-ped.json`; if no
+thing changes, `data/new-ped.json` is a copy of `data/sample-in
         +put.json` | `proposal.md` "Input change" documents the optional
+ `unlockEvents: [{"start": ...}]`; `input/InputParser.readUnloc
         +kEvents` reads and validates it with `--new-ped` only (D-23: a
+malformed value is an input error, exit 2); `data/new-ped.json`
+ = the sample + `unlockEvents` covering its `calculationTime` +
+ exchange Epsilon (kept by the 5% rule, excluded by the unlock
+band) | `UnlockEventsInputTest`, `MainTest` (`data/new-ped.json
         +` gives different results with and without the flag; outside th
+e window Epsilon is kept; malformed `unlockEvents` with the fla
+g) |

● Update(docs/spec-trace.md)
⎿  Added 1 line, removed 1 line
80  | ID | Section | Clause | Code | Test |
81  | --- | --- | --- | --- | --- |
82  | C.1 | Brief CLI | Java 25; takes one local JSON path (D-10) |
`cli/CliArgs` (`[--new-ped] <path>`, flag before or after; D-1
0, D-22), `cli/Main`; `pom.xml` (release 25, enforcer) | `CliAr
          gsTest`, `MainTest` (usage errors) |
83 -| C.2 | Brief CLI | `./run.sh data/sample-input.json` and `./ru
         -n.sh --new-ped data/new-ped.json` work offline with only Java 2
-5, after `build.sh` | `run.sh` (runs `target/obsi.jar`; exit 3
-and failure JSON when the jar is missing; D-08); the sample wor
-ks. `--new-ped` is refused until Task 2 (`usage error: ... not 
         -implemented yet`, D-22) | `RunScriptTest`, `MainTest`; `./run.s
         -h data/sample-input.json` checked by hand after `./build.sh`. `
         ---new-ped`: TBD (Task 2) |                                     
83 +| C.2 | Brief CLI | `./run.sh data/sample-input.json` and `./ru
         +n.sh --new-ped data/new-ped.json` work offline with only Java 2
+5, after `build.sh` | `run.sh` (runs `target/obsi.jar`; exit 3
+and failure JSON when the jar is missing; D-08); both work (`--
         +new-ped` runs `SpreadBasedPedFilter`; D-22's refusal is superse
+ded by D-23) | `RunScriptTest`, `MainTest`; `./run.sh data/samp
         +le-input.json` and `./run.sh --new-ped data/new-ped.json` check
+ed by hand after `./build.sh` |                                
84  | C.3 | Brief CLI | `build.sh` runs first and leaves everything
ready for `run.sh` | `build.sh` (`./mvnw -q clean verify`; the
shade plugin writes `target/obsi.jar`) | `./build.sh` passes i
n full and `./run.sh data/sample-input.json` works afterwards (
checked by hand) |
85  | I.1 | Brief Input | `calculationTime`: ISO-8601 UTC, time of
the snapshot (D-07) | `input/InputParser.isUtcInstant` (D-15) |
`InputParserTest` |
86  | I.2 | Brief Input | `exchange`: unique, nonempty (D-02, D-03)
| `input/InputParser` (blank and duplicate checks; D-16, D-18)
| `InputParserTest` |

● Now the full build.

Ran 1 shell command (ctrl+o to expand)

● The build passed. Both runs of data/new-ped.json, with exit codes:

Ran 1 shell command (ctrl+o to expand)

● --new-ped now runs the new spread-based PED rule, and ./build.sh passes in
full. Nothing is committed.

The two runs of data/new-ped.json (both exit 0, empty stderr):

./run.sh data/new-ped.json
{"index":"OBSI","calculationTime":"2026-10-27T12:00:00Z","status":"published",
"value":"100.20","metadata":{"utilizedDepth":"3","excludedExchanges":["Gamma",
"Delta"]}}

./run.sh --new-ped data/new-ped.json
{"index":"OBSI","calculationTime":"2026-10-27T12:00:00Z","status":"published",
"value":"100.23","metadata":{"utilizedDepth":"4","excludedExchanges":["Gamma",
"Delta","Epsilon"]}}

What changed
- Filter: SpreadBasedPedFilter is new, with the constants named and citing
  proposal.md. The spread is the only division (DECIMAL128), and the
  comparison has no division.
- Fixture: data/new-ped.json is the sample plus an unlock window covering its
  calculationTime and one new exchange, Epsilon. Epsilon is 2.22% from the
  median, so the 5% rule keeps it and the unlock band (1% cap) excludes it.
- Input and wiring: the "not implemented" refusal in Main is gone, and
  unlockEvents is read and validated only with the flag.
- Decisions: D-23 (malformed unlockEvents) and D-24 (no rounding, median of
  spreads) are in docs/decisions.md.
- Spec trace: N.1–N.3 are filled in. I also fixed the stale --new-ped text in
  C.2.
- GoldenTest: it follows your naming rule. X.new-ped.expected.json runs with
  the flag, so your flagged file for this fixture will be
  new-ped.new-ped.expected.json. I wrote no expected files.

Tests
- The 29 tests you approved (the test list of the approved plan) were written
  first as wip and tagged off once green. GoldenTest was changed for the
  naming rule.
- I added UnlockEventsInputTest as a separate class instead of extending
  InputParserTest.
- Two mutations (> to >= in the comparison, and the window end < to <=) were
  each caught by the tests, then reverted.
- The new-ped.json test pins only the excluded lists and that the outputs
  differ, not the index values.

Guard hook: it blocked four Bash calls this session. Two were my own slips:
sed and a grep regex with \| next to src/test, and a Python rewrite of a file,
which CLAUDE.md forbids. I used Read and Edit instead. The fourth was a
scratch python3 -c calculation with no protected path in it. I didn't retry
it, because I didn't need it.

✻ Churned for 20m 13s · done 1:56 PM

❯ Fill the three TBD rows in docs/spec-trace.md: P.1, P.2 and 1.14. Point each  
one at the code and tests that already cover it (for P.2, the stateless       
filters and their tests). If a row has no test, say so in the row rather than
writing a new test. Don't change any other row. Run ./mvnw -q verify and      
show me the three rows.

● I'll look at what already covers each of the three rows before writing them.

Searched for 1 pattern, read 1 file (ctrl+o to expand)

● Next I'll read the StaticPedFilter "no state" test, the filters' fields, and
Level's javadoc, since P.2 and 1.14 depend on them.

Read 1 file, ran 1 shell command (ctrl+o to expand)

● Now the three edits. I'm leaving every other row alone.

● Update(docs/spec-trace.md)
⎿  Added 1 line, removed 1 line
4  
5  | ID | Section | Clause | Code | Test |
6  | --- | --- | --- | --- | --- |
7 -| P.1 | Preamble | OBSI measures the USD price of XYZ at effect
-ive time T, using the books in one captured snapshot | TBD | TB
-D |                                                            
7 +| P.1 | Preamble | OBSI measures the USD price of XYZ at effect
+ive time T, using the books in one captured snapshot | `input/I
         +nputParser` turns one document into one `input/Snapshot` (T is
+its `calculationTime`, row 1.1); `result/ObsiCalculator.calcula
         +te(Snapshot, DeviationFilter)` takes that one snapshot and retu
+rns one value; `cli/Main` takes one path | `ObsiCalculatorTest`
+ (sample 100.23 and edge-cases 100.03 from one snapshot each),
+`GoldenTest`, `MainTest`. No test asserts "USD" or "XYZ" as suc
+h (see 1.14), and none checks that the books of two snapshots a
+re never mixed beyond P.2's tests |                            
8  | P.2 | Preamble | Each snapshot is evaluated independently | T
BD | TBD |
9  | P.3 | Preamble | Baseline orderbook deviation limit is 5% | `
          deviation/StaticPedFilter` (`LIMIT_PERCENT` = 5) | `StaticPedFi
          lterTest` |
10  | 1.1 | §1 | T is `calculationTime` in the input (D-07) | `inpu
          t/InputParser` (read, validated, echoed as given; D-15) | `Inpu
          tParserTest` |

● Update(docs/spec-trace.md)
⎿  Added 1 line, removed 1 line
5  | ID | Section | Clause | Code | Test |
6  | --- | --- | --- | --- | --- |
7  | P.1 | Preamble | OBSI measures the USD price of XYZ at effect
ive time T, using the books in one captured snapshot | `input/I
          nputParser` turns one document into one `input/Snapshot` (T is
its `calculationTime`, row 1.1); `result/ObsiCalculator.calcula
          te(Snapshot, DeviationFilter)` takes that one snapshot and retu
rns one value; `cli/Main` takes one path | `ObsiCalculatorTest`
(sample 100.23 and edge-cases 100.03 from one snapshot each),
`GoldenTest`, `MainTest`. No test asserts "USD" or "XYZ" as suc
h (see 1.14), and none checks that the books of two snapshots a
re never mixed beyond P.2's tests |
8 -| P.2 | Preamble | Each snapshot is evaluated independently | T
-BD | TBD |                                                     
8 +| P.2 | Preamble | Each snapshot is evaluated independently | `
         +deviation/StaticPedFilter` and `deviation/SpreadBasedPedFilter`
+ have no instance fields (constants only) and judge only the bo
+oks and the `Snapshot` they are given; `screening/Screener`, `r
         +esult/ObsiCalculator` and `cli/Main` are static and keep no sta
+te between calls. Same clause as 2.14 and 2.15 (no reentry, no
+half-limit) | `StaticPedFilterTest.theFilterKeepsNoStateBetween
         +Snapshots` (one filter, three snapshots, C out then in then out
+ again). `SpreadBasedPedFilter`: no dedicated no-state test; `S
         +preadBasedPedFilterTest` (`windowBoundaries`, `severalEvents`:
+the regime follows only the snapshot's own `calculationTime` an
+d `unlockEvents`) and `SpreadBasedPedFilterPropertyTest.theResu
         +ltDoesNotDependOnOrderAndOnlyHoldsTheGivenBooks` cover it indir
+ectly |                                                        
9  | P.3 | Preamble | Baseline orderbook deviation limit is 5% | `
          deviation/StaticPedFilter` (`LIMIT_PERCENT` = 5) | `StaticPedFi
          lterTest` |
10  | 1.1 | §1 | T is `calculationTime` in the input (D-07) | `inpu
          t/InputParser` (read, validated, echoed as given; D-15) | `Inpu
          tParserTest` |
11  | 1.2 | §1 | Spacing s = 1 XYZ | `curve/PriceVolumeCurve.SPACIN
          G` | `PriceVolumeCurveTest` (K3), `UtilizedDepthTest` |

● Update(docs/spec-trace.md)
⎿  Added 1 line, removed 1 line
20  | 1.11 | §1 | v_T is determined by Eq. 2 | `index/UtilizedDepth
          .find` (D-21) | `UtilizedDepthTest`, `IndexPropertyTest` |
21  | 1.12 | §1 | λ = 1/(0.3·v_T) | `index/ObsiIndex.weightedMean`
(D-13, D-14, D-21) | `ObsiIndexTest` (v_T = 2 closed form) |
22  | 1.13 | §1 | NF is the sum of weights through v_T | `index/Obs
          iIndex.weightedMean` (`normalizationFactor`) | `ObsiIndexTest`
(v_T = 1 and 2, constant-mid cases) |
23 -| 1.14 | §1 | Prices are USD per XYZ; sizes and depths are in X
-YZ | TBD | TBD |                                               
23 +| 1.14 | §1 | Prices are USD per XYZ; sizes and depths are in X
+YZ | A convention, not behaviour: documented in `input/Level` (
+price USD per XYZ, size XYZ), `curve/PriceVolumeCurve.SPACING`
+(s = 1 XYZ) and `index/IndexResult.Published.utilizedDepth` (v_
+T in XYZ, written by `cli/Main.published`; O.4). Values are pla
+in `BigDecimal`s with no unit type | No test: nothing in the co
+de could differ if the unit were another one. The numbers in th
+ese units are checked elsewhere (`PriceVolumeCurveTest` for siz
+es, `ObsiIndexTest` and `MainTest` for the value and `utilizedD
         +epth`) |                                                       
24  | 1.15 | §1 | A_T and B_T are the levels remaining after screen
s, consolidation and uncrossing | `book/Consolidator.consolidat
          e` on the screener's survivors (screens: rows 2.x; consolidatio
n and uncrossing: rows 3.x) | `ConsolidatorTest` (sample and ed
ge-cases fixtures end to end) |
25  | 1.16 | §1 | No order size cap applies | `book/Consolidator` (
no cap anywhere; sizes are summed and reduced exactly) | `Conso
          lidatorTest` (the consolidated sizes are the raw sums, e.g. 99.
8 → 2.3); `curve/PriceVolumeCurve` uses those sizes as they are
, no cap |
26  | 2.1 | §2 | Discard a price/size entry if either component is
not numeric (D-01, D-05) | `input/InputParser.readLevels` (impl
ements this clause fully: only JSON numbers are numeric; "stric
tly positive" is row 2.2, phase 2) | `InputParserTest` |

Ran 2 shell commands (ctrl+o to expand)

● ./mvnw -q verify exits 0 with no output. git diff --stat for
docs/spec-trace.md shows only 3 lines changed, so no other row was touched.
The three rows (Section, Clause, Code, Test):

P.1 — Preamble, "OBSI measures the USD price of XYZ at effective time T, using
the books in one captured snapshot"
- Code: input/InputParser turns one document into one input/Snapshot, and T is
  its calculationTime (row 1.1). result/ObsiCalculator.calculate(Snapshot,
  DeviationFilter) takes that one snapshot and returns one value. cli/Main
  takes one path.
- Test: ObsiCalculatorTest (the sample gives 100.23 and edge-cases gives
  100.03, one snapshot each), GoldenTest and MainTest. The row says that no
  test asserts "USD" or "XYZ" as such (see 1.14).

P.2 — Preamble, "Each snapshot is evaluated independently"
- Code: StaticPedFilter and SpreadBasedPedFilter have no instance fields
  (constants only) and judge only the books and the Snapshot they are given.
  Screener, ObsiCalculator and Main are static and keep no state between
  calls. This is the same clause as rows 2.14 and 2.15.
- Test: StaticPedFilterTest.theFilterKeepsNoStateBetweenSnapshots (one filter,
  three snapshots, book C excluded, then kept, then excluded again). The row
  says that SpreadBasedPedFilter has no dedicated no-state test. It is covered
  indirectly by SpreadBasedPedFilterTest (windowBoundaries and severalEvents,
  where the regime follows only the snapshot's own calculationTime and
  unlockEvents) and SpreadBasedPedFilterPropertyTest.theResultDoesNotDependOnO
  rderAndOnlyHoldsTheGivenBooks.

1.14 — §1, "Prices are USD per XYZ; sizes and depths are in XYZ"
- Code: this is a convention, not behaviour. It is documented in input/Level,
  PriceVolumeCurve.SPACING and IndexResult.Published.utilizedDepth (written by
  Main.published; O.4). The values are plain BigDecimals with no unit type.
- Test: the row says there is none, because nothing in the code could differ
  if the unit were another one. The numbers in those units are checked
  elsewhere (PriceVolumeCurveTest, ObsiIndexTest, MainTest).

The P.1 test cell ends with a clumsy clause ("none checks that the books of
two snapshots are never mixed beyond P.2's tests"). I can tighten or drop it
if you want.

✻ Brewed for 1m 7s · done 2:12 PM

❯ Drop the clumsy clause in P.1. For P.2, add a no-state test for               
SpreadBasedPedFilter mirroring StaticPedFilterTest's: one filter instance, a  
snapshot inside an unlock window, then one outside it, then inside again; the
middle result must use the normal band. Update the P.2 test cell to cite it.  
Run ./mvnw -q verify.

● I'll add the test to SpreadBasedPedFilterTest, drop the P.1 clause, and update
the P.2 cell.

● Update(src/test/java/com/cfbenchmarks/obsi/deviation/SpreadBasedPedFilterTest.
java)
⎿  Added 16 lines
307          assertThat(excluded(snapshot, tight("100.7"))).contain
sExactly("C");
308      }
309  
310 +    /**                                                       
311 +     * P.2, as {@code StaticPedFilterTest.theFilterKeepsNoStat
+eBetweenSnapshots}: no state between calls. One filter        
312 +     * instance sees a snapshot inside an unlock window, then
+one outside it, then inside again. The middle result uses     
313 +     * the normal band, so the unlock band left nothing behind
+, and the third is the same as the first.                     
314 +     */                                                       
315 +    @Test                                                     
316 +    void theFilterKeepsNoStateBetweenSnapshots() {            
317 +        ValidBook[] books = tight("100.7");   // C is 0.7% fro
+m the median: out at the 0.5% unlock floor, in at the 1% floor
318 +        Snapshot inside = snapshotAt("2026-10-27T12:00:00Z", "
+2026-10-26T12:00:00Z");                                       
319 +        Snapshot outside = snapshotAt("2026-10-29T12:00:00Z",
+"2026-10-26T12:00:00Z");   // start + 48h is 10-28T12:00      
320 +                                                              
321 +        assertThat(excluded(inside, books)).containsExactly("C
+");                                                           
322 +        assertThat(excluded(outside, books)).isEmpty();   // n
+ormal band: the unlock band is not carried over               
323 +        assertThat(excluded(inside, books)).containsExactly("C
+");                                                           
324 +    }                                                         
325 +                                                              
326      /** 13. T24:00:00Z is next-day midnight (D-15), and a star
t at the end of time must not overflow. */
327      @Test
328      void t24IsReadAsNextDayMidnight() {

● Update(docs/spec-trace.md)
⎿  Added 1 line, removed 1 line
4  
5  | ID | Section | Clause | Code | Test |
6  | --- | --- | --- | --- | --- |
7 -| P.1 | Preamble | OBSI measures the USD price of XYZ at effect
-ive time T, using the books in one captured snapshot | `input/I
         -nputParser` turns one document into one `input/Snapshot` (T is
-its `calculationTime`, row 1.1); `result/ObsiCalculator.calcula
         -te(Snapshot, DeviationFilter)` takes that one snapshot and retu
-rns one value; `cli/Main` takes one path | `ObsiCalculatorTest`
- (sample 100.23 and edge-cases 100.03 from one snapshot each),
-`GoldenTest`, `MainTest`. No test asserts "USD" or "XYZ" as suc
-h (see 1.14), and none checks that the books of two snapshots a
-re never mixed beyond P.2's tests |                            
7 +| P.1 | Preamble | OBSI measures the USD price of XYZ at effect
+ive time T, using the books in one captured snapshot | `input/I
         +nputParser` turns one document into one `input/Snapshot` (T is
+its `calculationTime`, row 1.1); `result/ObsiCalculator.calcula
         +te(Snapshot, DeviationFilter)` takes that one snapshot and retu
+rns one value; `cli/Main` takes one path | `ObsiCalculatorTest`
+ (sample 100.23 and edge-cases 100.03 from one snapshot each),
+`GoldenTest`, `MainTest`. No test asserts "USD" or "XYZ" as suc
+h (see 1.14) |                                                 
8  | P.2 | Preamble | Each snapshot is evaluated independently | `
          deviation/StaticPedFilter` and `deviation/SpreadBasedPedFilter`
have no instance fields (constants only) and judge only the bo
oks and the `Snapshot` they are given; `screening/Screener`, `r
          esult/ObsiCalculator` and `cli/Main` are static and keep no sta
te between calls. Same clause as 2.14 and 2.15 (no reentry, no
half-limit) | `StaticPedFilterTest.theFilterKeepsNoStateBetween
          Snapshots` (one filter, three snapshots, C out then in then out
again). `SpreadBasedPedFilter`: no dedicated no-state test; `S
          preadBasedPedFilterTest` (`windowBoundaries`, `severalEvents`:
the regime follows only the snapshot's own `calculationTime` an
d `unlockEvents`) and `SpreadBasedPedFilterPropertyTest.theResu
          ltDoesNotDependOnOrderAndOnlyHoldsTheGivenBooks` cover it indir
ectly |
9  | P.3 | Preamble | Baseline orderbook deviation limit is 5% | `
          deviation/StaticPedFilter` (`LIMIT_PERCENT` = 5) | `StaticPedFi
          lterTest` |
10  | 1.1 | §1 | T is `calculationTime` in the input (D-07) | `inpu
          t/InputParser` (read, validated, echoed as given; D-15) | `Inpu
          tParserTest` |

● Update(docs/spec-trace.md)
⎿  Added 1 line, removed 1 line
5  | ID | Section | Clause | Code | Test |
6  | --- | --- | --- | --- | --- |
7  | P.1 | Preamble | OBSI measures the USD price of XYZ at effect
ive time T, using the books in one captured snapshot | `input/I
          nputParser` turns one document into one `input/Snapshot` (T is
its `calculationTime`, row 1.1); `result/ObsiCalculator.calcula
          te(Snapshot, DeviationFilter)` takes that one snapshot and retu
rns one value; `cli/Main` takes one path | `ObsiCalculatorTest`
(sample 100.23 and edge-cases 100.03 from one snapshot each),
`GoldenTest`, `MainTest`. No test asserts "USD" or "XYZ" as suc
h (see 1.14) |
8 -| P.2 | Preamble | Each snapshot is evaluated independently | `
         -deviation/StaticPedFilter` and `deviation/SpreadBasedPedFilter`
- have no instance fields (constants only) and judge only the bo
-oks and the `Snapshot` they are given; `screening/Screener`, `r
         -esult/ObsiCalculator` and `cli/Main` are static and keep no sta
-te between calls. Same clause as 2.14 and 2.15 (no reentry, no
-half-limit) | `StaticPedFilterTest.theFilterKeepsNoStateBetween
         -Snapshots` (one filter, three snapshots, C out then in then out
- again). `SpreadBasedPedFilter`: no dedicated no-state test; `S
         -preadBasedPedFilterTest` (`windowBoundaries`, `severalEvents`:
-the regime follows only the snapshot's own `calculationTime` an
-d `unlockEvents`) and `SpreadBasedPedFilterPropertyTest.theResu
         -ltDoesNotDependOnOrderAndOnlyHoldsTheGivenBooks` cover it indir
-ectly |                                                        
8 +| P.2 | Preamble | Each snapshot is evaluated independently | `
         +deviation/StaticPedFilter` and `deviation/SpreadBasedPedFilter`
+ have no instance fields (constants only) and judge only the bo
+oks and the `Snapshot` they are given; `screening/Screener`, `r
         +esult/ObsiCalculator` and `cli/Main` are static and keep no sta
+te between calls. Same clause as 2.14 and 2.15 (no reentry, no
+half-limit) | `StaticPedFilterTest.theFilterKeepsNoStateBetween
         +Snapshots` (one filter, three snapshots, C out then in then out
+ again). `SpreadBasedPedFilterTest.theFilterKeepsNoStateBetween
         +Snapshots` (one filter, a snapshot inside an unlock window, the
+n one outside it, then inside again; the middle result uses the
+ normal band). Also `SpreadBasedPedFilterTest` (`windowBoundari
         +es`, `severalEvents`: the regime follows only the snapshot's ow
+n `calculationTime` and `unlockEvents`) and `SpreadBasedPedFilt
         +erPropertyTest.theResultDoesNotDependOnOrderAndOnlyHoldsTheGive
         +nBooks` |                                                      
9  | P.3 | Preamble | Baseline orderbook deviation limit is 5% | `
          deviation/StaticPedFilter` (`LIMIT_PERCENT` = 5) | `StaticPedFi
          lterTest` |
10  | 1.1 | §1 | T is `calculationTime` in the input (D-07) | `inpu
          t/InputParser` (read, validated, echoed as given; D-15) | `Inpu
          tParserTest` |
11  | 1.2 | §1 | Spacing s = 1 XYZ | `curve/PriceVolumeCurve.SPACIN
          G` | `PriceVolumeCurveTest` (K3), `UtilizedDepthTest` |

Ran 1 shell command (ctrl+o to expand)
● I dropped the clause from P.1, added the no-state test for
SpreadBasedPedFilter, and updated the P.2 test cell. ./mvnw -q verify exits 0.

The new test is
SpreadBasedPedFilterTest.theFilterKeepsNoStateBetweenSnapshots. One filter
instance sees the same three books each time, with the third book 0.7% from
the median:
1. Inside the window (2026-10-27T12:00Z, unlock start 2026-10-26T12:00Z), book
   C is excluded by the 0.5% unlock floor.
2. Outside it (2026-10-29T12:00Z, after the window ends at 2026-10-28T12:00Z),
   nothing is excluded. This is the normal 1% band, so the unlock band left
   nothing behind.
3. Inside again, C is excluded, the same as the first call.

I didn't run a mutation check on it. The filter has no mutable state, so the
test passes on first run and can only catch a regression that adds state.

The two rows now:
- P.1: the test cell ends at ...No test asserts "USD" or "XYZ" as such (see
  1.14), and the rest of the row is unchanged.
- P.2: the test cell now lists
  StaticPedFilterTest.theFilterKeepsNoStateBetweenSnapshots, then
  SpreadBasedPedFilterTest.theFilterKeepsNoStateBetweenSnapshots with the
  inside/outside/inside description. After those come the indirect coverage
  tests, now listed as "Also". The "no dedicated no-state test" wording is
  gone.

git diff --stat for docs/spec-trace.md still shows 3 lines changed, so no
other row was touched.

✻ Cooked for 1m 1s · done 2:15 PM