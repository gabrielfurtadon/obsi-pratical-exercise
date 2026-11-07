# Agent rules (CLAUDE.md is a symlink to this file)

## Source of truth
- `obsi-methodology.md` is the spec and the source of truth. Never edit it (`SpecIntegrityTest` pins its SHA-256).
- Never edit anything in `src/test/resources/expected/`. Hooks and permission rules block both.
- Cite the spec section or equation in code next to the logic it implements, e.g. `// §2: PED > 5% excludes`, `// Eq. 2`. Keep `docs/spec-trace.md` up to date.
- If the spec is ambiguous or silent, add an entry to `docs/decisions.md` and stop. Don't decide it yourself and don't encode a guess in code or tests.
- I/O contract, CLI and deliverables: docs/brief.md.
- 
## Workflow
- Write tests before code. A test written ahead of its implementation gets `@Tag("wip")`. Remove the tag once it passes.
- Property-style tests: JUnit `@ParameterizedTest` with inputs from a `java.util.Random` with a fixed seed, so every run is deterministic. No property-testing library.
- Fast tests: `./mvnw -q test -DexcludedGroups=slow`. Full suite: `./build.sh`. The Stop hook runs `verify -DexcludedGroups=wip`.

## I/O
- Results go to stdout as JSON. Errors go to stderr as JSON. Nothing else goes to stdout.
- `run.sh` must work offline. It only runs `target/obsi.jar`.

## Numbers
- Use `BigDecimal` for prices and sizes, never `double`.
- Parse JSON numbers as `BigDecimal`: enable `DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS` and read with `JsonNode.decimalValue()`, so `100.2` never passes through a double.
- Do boundary checks without division:
  - PED limit (§2): `|m - med| * 100 <= 5 * med` passes.
  - midSV cutoff (Eq. 1f / Eq. 2): `midSV <= D` is `askPV <= (1 + D) * midPV`.
- Group prices with `compareTo`, using `TreeMap<BigDecimal, …>`. Never use `equals` or `HashMap`, because `100.0` and `100` are different under `equals`.
- Round the published value with `setScale(2, RoundingMode.HALF_UP)` (§5).
