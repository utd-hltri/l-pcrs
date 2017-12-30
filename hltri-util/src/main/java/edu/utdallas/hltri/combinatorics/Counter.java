package edu.utdallas.hltri.combinatorics;

/**
 * Interface for classes that count items
 * @author bryan
 */
public interface Counter<T> {

  public void add(T item, long amount);
}
