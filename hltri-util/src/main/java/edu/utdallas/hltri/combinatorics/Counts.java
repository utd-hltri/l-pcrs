package edu.utdallas.hltri.combinatorics;

/**
 * Interface for classes that can return a number for an item.
 * @author bryan
 */
public interface Counts<T> {
  public long getCount(T item);
}
