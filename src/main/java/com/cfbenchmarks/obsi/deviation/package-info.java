/**
 * The median of the top-of-book mid-prices and the percentage-deviation (PED) screen, behind the
 * {@link com.cfbenchmarks.obsi.deviation.DeviationFilter} interface. The mid itself is
 * {@link com.cfbenchmarks.obsi.book.ValidBook#mid()}.
 *
 * <p>Spec: §2 "Constituent order book screens", mid-price formula, median (mean of the middle
 * two for an even count), PED formula, and "Exclude a book only if PED &gt; 5%. A book at
 * exactly 5% passes." No reentry state or half-limit test.
 */
package com.cfbenchmarks.obsi.deviation;
