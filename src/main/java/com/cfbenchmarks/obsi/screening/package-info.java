/**
 * Per-book entry and structural screens.
 *
 * <p>Spec: §2 "Constituent order book screens", first paragraph: discard an entry whose price or
 * size is not numeric or not strictly positive; exclude a book whose structure cannot be read,
 * with an empty side after entry screening, or whose best bid is greater than or equal to its
 * best ask. Applied to each book before combining exchanges.
 */
package com.cfbenchmarks.obsi.screening;
