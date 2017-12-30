package edu.utdallas.hltri.combinatorics;

/**
 *
 * @author travis
 */
public interface CooccurrenceMeasure {
  double measure(long x, long y, long xy, long n);
}
