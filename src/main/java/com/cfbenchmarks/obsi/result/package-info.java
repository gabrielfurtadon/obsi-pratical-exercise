/**
 * Calculation outcome: a published index value or a failure.
 *
 * <p>Spec: §6 "Calculation failures": malformed required input, no surviving book, uncrossing
 * exhausting a side, or an empty Eq. 2 set. No index value is published for a failed snapshot.
 */
package com.cfbenchmarks.obsi.result;
