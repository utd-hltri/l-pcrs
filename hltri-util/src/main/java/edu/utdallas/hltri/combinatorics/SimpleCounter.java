package edu.utdallas.hltri.combinatorics;

/**
 * Created by ramon on 3/16/16.
 */
public class SimpleCounter {
  private int count;

  public SimpleCounter() {
    count = 0;
  }

  public synchronized void increment() {
    count++;
  }

  public synchronized void decrement() {
    count--;
  }

  public int count() {
    return count;
  }

  @Override
  public String toString() {
    return Integer.toString(count);
  }
}
