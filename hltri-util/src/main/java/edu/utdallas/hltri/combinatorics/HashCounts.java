package edu.utdallas.hltri.combinatorics;

import gnu.trove.map.hash.TObjectLongHashMap;
import gnu.trove.procedure.TLongProcedure;


/**
 * Counts items in-memory, backed by a Trove TObjectLongHashMap.
 * @author bryan
 */
public class HashCounts<T> implements Counts<T>, Counter<T> {
  private TObjectLongHashMap<T> counts;
  private long total = 0;
  private boolean totalCalculated = false;
  
  public HashCounts(TObjectLongHashMap<T> counts) {
    this.counts = counts;
  }
  
  public HashCounts() {
    this.counts = new TObjectLongHashMap<T>();
  }

  @Override
  public long getCount(T item) {
    return counts.get(item);
  }

  @Override
  public void add(T item, long amount) {
    counts.adjustOrPutValue(item, amount, amount);
    if (totalCalculated) { total += amount; }
  }
  
  public long numItems() {
    return counts.size();
  }
  
  public long total() {
    if (totalCalculated) return total;
    final long[] total = {0};
    counts.forEachValue(new TLongProcedure() {
      @Override
      public boolean execute(long l) {
        total[0] += l;
        return true;
      }
    });
    this.total = total[0];
    this.totalCalculated = true;
    return total[0];
  }
  
}
