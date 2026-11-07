# Order Book Snapshot Index (OBSI) methodology

The Order Book Snapshot Index (OBSI) measures the price of token XYZ in USD at an effective time \(T\), using the constituent exchange order books supplied in one captured snapshot. This document is the calculation specification for the exercise. Each snapshot is evaluated independently. The baseline orderbook deviation limit is 5%.

## 1. Parameters and notation

| Symbol | Meaning | Exercise value |
| --- | --- | --- |
| \(T\) | Effective calculation time | `calculationTime` in the input |
| \(s\) | Price-volume curve spacing | \(1\) XYZ |
| \(D\) | Mid-spread cutoff | \(0.005\) (0.5%) |
| \(v\) | Cumulative token XYZ volume | Multiples of \(s\) |
| \(e\) | A constituent exchange | One book in the snapshot |
| \(b_{T,e}^{\mathrm{best}}\), \(a_{T,e}^{\mathrm{best}}\) | Best bid and best ask of exchange \(e\) | Prices in USD per XYZ |
| \(m_{T,e}\) | Top-of-book mid-price of exchange \(e\) | Determined before consolidation |
| \(\widetilde m_T\) | Median of structurally valid book mid-prices | Mean of the middle two when the count is even |
| \(\mathrm{PED}_{T,e}\) | Percentage deviation of exchange \(e\) from \(\widetilde m_T\) | Compared with the 5% limit |
| \(A_T=\{(ap_{T,i},as_{T,i})\}\) | Consolidated ask price and size levels, ordered by ascending price | Raw, uncapped sizes |
| \(B_T=\{(bp_{T,i},bs_{T,i})\}\) | Consolidated bid price and size levels, ordered by descending price | Raw, uncapped sizes |
| \(v_T\) | Utilized depth | Determined by Eq. 2 |
| \(\lambda\) | Exponential weighting parameter | \(1/(0.3v_T)\) |
| \(NF\) | Normalization factor | Sum of weights through \(v_T\) |

Prices are USD per XYZ and sizes and depths are in XYZ. The book levels in \(A_T\) and \(B_T\) are those remaining after the screens, consolidation, and uncrossing below. No order size cap applies.

## 2. Constituent order book screens

For a parsed constituent exchange book, discard a price/size entry if either component is not numeric or is not strictly positive. Exclude a book if its structure cannot be read, either side is empty after entry screening, or its best bid is greater than or equal to its best ask. Apply these checks to each book before combining exchanges. Malformed top-level input and missing required top-level fields are input errors.

For each structurally valid book, calculate its top-of-book mid-price:

$$
m_{T,e}=\frac{b_{T,e}^{\mathrm{best}}+a_{T,e}^{\mathrm{best}}}{2}.
$$

Let \(\widetilde m_T\) be the median of those mid-prices, averaging the middle two for an even number of books. The orderbook percentage deviation (PED) of book \(e\) is

$$
\mathrm{PED}_{T,e}=100\,\frac{|m_{T,e}-\widetilde m_T|}{\widetilde m_T}\%.
$$

Exclude a book only if \(\mathrm{PED}_{T,e}>5\%\). A book at exactly 5% passes. Perform this deviation check independently for every snapshot using only the books in that snapshot; there is no reentry state or half-limit test.

## 3. Consolidation and uncrossing

Combine the surviving books into one order book. Sum sizes at identical bid prices and, separately, at identical ask prices. Order consolidated bids from highest to lowest price and asks from lowest to highest price.

While the best bid is greater than or equal to the best ask, subtract the smaller of their sizes from both levels and remove every level whose residual size becomes zero. Repeat until the best bid is below the best ask. The calculation fails if either side is exhausted.

## 4. Price-volume curves

The unspaced ask and bid price-volume curves return the price level reached by cumulative volume \(v\). For \(j\geq0\), with an empty sum equal to zero:

$$
\widehat{\operatorname{askPV}}_T(v)=ap_{T,j+1}
\quad\text{where}\quad
\sum_{i=1}^{j}as_{T,i}<v\ \land
\sum_{i=1}^{j+1}as_{T,i}\geq v. \tag{1a}
$$

$$
\widehat{\operatorname{bidPV}}_T(v)=bp_{T,j+1}
\quad\text{where}\quad
\sum_{i=1}^{j}bs_{T,i}<v\ \land
\sum_{i=1}^{j+1}bs_{T,i}\geq v. \tag{1b}
$$

At spacing \(s=1\) XYZ, the spaced curves, mid-price curve, and mid-spread curve are:

$$
\operatorname{askPV}_T(v)=\widehat{\operatorname{askPV}}_T\!\left(s\left\lceil\frac{v}{s}\right\rceil\right), \tag{1c}
$$

$$
\operatorname{bidPV}_T(v)=\widehat{\operatorname{bidPV}}_T\!\left(s\left\lceil\frac{v}{s}\right\rceil\right), \tag{1d}
$$

$$
\operatorname{midPV}_T(v)=\frac{\operatorname{askPV}_T(v)+\operatorname{bidPV}_T(v)}{2}, \tag{1e}
$$

$$
\operatorname{midSV}_T(v)=\frac{\operatorname{askPV}_T(v)}{\operatorname{midPV}_T(v)}-1. \tag{1f}
$$

## 5. Utilized depth and index value

The utilized depth is the maximum sampled volume satisfying the threshold-crossing condition below. Evaluate each candidate only when both curves are defined at \(v_i\) and \(v_{i+1}\):

$$
v_T=\max\left(\left\{v_i:\operatorname{midSV}_T(v_i)\leq D\ \land
\operatorname{midSV}_T(v_{i+1})>D\right\}\right),
\qquad v_i=is,\ i=1,2,\ldots. \tag{2}
$$

Set \(\lambda=1/(0.3v_T)\). Normalize the exponential weights over the sampled volumes through \(v_T\):

$$
NF=\sum_{v\in\{s,2s,\ldots,v_T\}}\lambda e^{-\lambda v}.
$$

The index value is the weighted mean of the mid-price curve:

$$
\mathrm{OBSI}_T=\sum_{v\in\{s,2s,\ldots,v_T\}}
\operatorname{midPV}_T(v)\frac{1}{NF}\lambda e^{-\lambda v}. \tag{3}
$$

Round the published value to two decimal places using Java `HALF_UP` rounding.

## 6. Calculation failures

The calculation fails for malformed required input, if no constituent book survives screening, or if uncrossing exhausts either side. It also fails when the set in Eq. 2 is empty: this includes insufficient depth to evaluate consecutive samples and no mid-spread cutoff crossing before either curve ends. Do not publish an index value for a failed snapshot.

This exercise specifies one offline calculation from one supplied snapshot. It does not require collection or publication infrastructure.

## 7. Fixed parameters for this exercise

| Parameter | Value |
| --- | --- |
| Orderbook percentage deviation limit | 5% |
| Mid-spread cutoff, \(D\) | \(0.005\) (0.5%) |
| Price-volume curve spacing, \(s\) | \(1\) XYZ |
| Exponential weighting parameter, \(\lambda\) | \(1/(0.3v_T)\) |
| Published value precision | Two decimal places, Java `HALF_UP` |

## Reference

Adapted for this fictional exercise from the [CME CF Cryptocurrency Real Time Indices Methodology Guide, version 17.0](https://docs.cfbenchmarks.com/CME%20CF%20Real%20Time%20Indices%20Methodology.pdf). The fixed exercise parameters and independent per-snapshot deviation check above govern this exercise.

