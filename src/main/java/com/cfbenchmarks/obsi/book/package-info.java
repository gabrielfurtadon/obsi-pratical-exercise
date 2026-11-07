/**
 * Consolidation of surviving books into one book, and uncrossing.
 *
 * <p>Spec: §3 "Consolidation and uncrossing": sum sizes at identical prices per side, order bids
 * descending and asks ascending, then repeatedly remove the smaller size from the best bid and
 * best ask while best bid &gt;= best ask; fail if either side is exhausted.
 */
package com.cfbenchmarks.obsi.book;
