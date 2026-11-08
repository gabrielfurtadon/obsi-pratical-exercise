/**
 * The constituent book as a domain object: {@link com.cfbenchmarks.obsi.book.ValidBook} (a book that
 * passed the §2 structural screens) and §3 consolidation and uncrossing
 * ({@link com.cfbenchmarks.obsi.book.Consolidator}).
 *
 * <p>Spec: §3 "Consolidation and uncrossing": sum sizes at identical prices per side, order bids
 * descending and asks ascending, then repeatedly remove the smaller size from the best bid and
 * best ask while best bid &gt;= best ask; fail if either side is exhausted. Books that passed §2 can
 * never exhaust a side (D-20); the failure is still implemented as the spec requires.
 *
 * <p>This package depends only on {@code input}.
 */
package com.cfbenchmarks.obsi.book;
