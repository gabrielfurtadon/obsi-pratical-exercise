# Proposal: a liquidity-adaptive PED with an unlock regime

## Context

Today an exchange is excluded when its mid-price deviates from the median by more than 5%, a limit set wide to cope with thinner weekend liquidity. The PM raised three concerns: that limit is too wide on weekdays; some new exchanges hold a persistent premium that looks like market structure, not bad data; and PED should be much tighter during the roughly 48 hours of higher liquidity after an announced unlock. The PM prefers something grounded in financial principles and easy to explain to clients, and not every concern has to be solved.

## Current practice

The [CME CF Real Time Indices methodology](https://docs.cfbenchmarks.com/CME%20CF%20Real%20Time%20Indices%20Methodology.pdf) already sets the PED parameter per asset (5% for the most liquid assets, 10% for less liquid ones). It readmits an excluded exchange only once its deviation falls below half the limit ([announcement](https://blog.cfbenchmarks.com/implementation-change-to-the-potentially-erroneous-data-parameter-for-all-cme-cf-real-time-indices/)), and it reviews the parameter at least annually with the Oversight Committee, which looked at lower thresholds in 2025 and kept the existing one ([June 2025 minutes](https://docs.cfbenchmarks.com/CME%20CF%20Oversight%20Committee%20Meeting%20Minutes%20June%202025.pdf)). It also computes the order size cap and curve spacing dynamically from the consolidated book. [CoinDesk](https://downloads.coindesk.com/cd3/CDI/CCMR-Methodology.pdf) tiers its band by the number of markets. Both approaches below keep that pattern: deterministic rules, reproducible from the data, with committee-governed parameters.

## Approach 1: a regime calendar

The first approach sets a fixed limit per regime, for example 2% on weekdays, 5% at weekends and 1% during an unlock window, chosen from `calculationTime` and a list of announced unlocks in the input. It is simple, predictable, easy to audit, and meets the unlock request with certainty. But the weekday and weekend limits are hard to justify without historical data, and the weekend boundary is arbitrary.

## Approach 2: a spread-based limit with an unlock regime (selected)

The second approach recomputes the limit for every snapshot from the liquidity it observes. For each structurally valid book it takes the relative spread, `(best ask − best bid) / mid`. The median of those spreads, `S`, measures how liquid the market is at that moment. The limit is `k × S`, clamped between a floor and a cap. If `calculationTime` falls within 48 hours of the start of an announced unlock, the unlock regime's floor and cap apply. The comparison itself is unchanged and still avoids division: `|m − median| × 100 ≤ limit × median`.

The rationale is the cost of arbitrage. A deviation between exchanges is only suspicious when it is larger than what it would cost to close it by buying on one venue and selling on the other, which includes the spread, trading fees, moving the asset between venues and price risk while doing so. The spread is the part of that cost visible in the snapshot, and `k` stands for the rest. With illustrative values of `k = 10`, a 1% to 5% band in the normal regime and a 0.5% to 1% band during an unlock, a liquid market gets a tight limit and an illiquid one falls back to today's 5%.

The key property is that, with the normal cap equal to today's 5%, the new rule is never more permissive than the current one. It only tightens when liquidity allows, which makes the migration safe: weekends behave as they do today, and liquid weekdays get the stricter limit the PM asked for.

On `data/sample-input.json`, the spreads of Alpha, Beta and Delta are 0.40%, 0.10% and 1.33%. Their median is 0.40%, which gives a limit of 3.99%. Delta, 49.6% from the median, is still excluded, and the index stays at 100.23. An exchange 2.7% from the median would pass under both 5% and 3.99%, but would be excluded on a more liquid weekday, with a typical spread of 0.2% and a limit of 2%.

## Comparison

Both approaches address the first and third concerns, leave the second as future work, need the same unlock calendar and guarantee a tighter limit during the window. They differ on weekends versus weekdays. The calendar ties fixed, hard-to-justify limits to the day of the week and cannot react to what it does not foresee, such as a weekday holiday or an unusually busy weekend. The spread-based rule observes actual liquidity, so it is right in those cases too, and it rests on an economic argument rather than an operational convention, in line with how CME CF already treats the order size cap and spacing. In exchange it has more parameters to calibrate, which is why it is designed never to be looser than the current rule.

## Input change

The document gains an optional top-level field, `"unlockEvents": [{ "start": "<ISO-8601 UTC>" }]`. The window runs from `start` (inclusive) to `start + 48h` (exclusive). Without the field, the normal regime applies. `data/new-ped.json` contains an example whose window covers its `calculationTime`. Without the `--new-ped` flag the field is ignored and the original 5% rule applies.

## Persistent premium: future work

This proposal does not address the second concern. Correcting a premium requires estimating it per exchange from historical data, which conflicts with evaluating each snapshot independently. Giving new exchanges a wider limit was considered and rejected: being new does not cause the premium, a wider limit would hide real errors instead of correcting the offset, and it would reduce scrutiny on the exchanges with the least history. The right treatment is a per-exchange premium, approved by the committee and subtracted before PED, which fits the same structure without changing it.

## Risks and limitations

The values of `k`, the floors and the caps are illustrative; in production they would be calibrated by backtesting historical snapshots for wrongful exclusions and detected incidents, then approved by the committee. The median spread stops a single exchange from widening the limit through its own spread, and the cap bounds any distortion. The half-limit readmission rule is out of scope because each snapshot is evaluated independently, but it is a natural extension in a stateful version.