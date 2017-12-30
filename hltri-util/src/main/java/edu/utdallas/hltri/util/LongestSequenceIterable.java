
package edu.utdallas.hltri.util;

import java.util.Iterator;
import java.util.List;

/**
 *
 * @author travis
 */
public class LongestSequenceIterable<T> implements Iterable<List<T>>{
  private final List<T> original;
  private final int start;
  private final int end;


  /**
   * Creates a new LongestSequenceIterable from the given List
   * This allows a list to be iterated as a series of longest
   * possible sub-sequences from right-to-left.
   *
   * <p>
   * For example, given the sequence A B C D, the following ordering
   * would be produced:
   * </p>
   * <pre>
   *  A B C D
   *    B C D
   *      C D
   *        D
   *  A B C
   *    B C
   *      C
   *  A B
   *    B
   *  A
   * </pre>
   * @param original List of elements
   */
  protected LongestSequenceIterable(List<T> original, int start, int end) {
    this.original = original;
    this.start = start;
    this.end = end;
  }

  public static <T> LongestSequenceIterable<T> of(final List<T> original) {
    return new LongestSequenceIterable<>(original, 0, original.size());
  }

  public static <T> LongestSequenceIterable<T> of(final List<T> original, final int start, final int end) {
    return new LongestSequenceIterable<>(original, start, end);
  }


  /**
   * @inheritDoc
   */
  @Override public Iterator<List<T>> iterator() {
    return new LongestSequenceIterator<>(original, start, end);
  }


  private static class LongestSequenceIterator<T> implements Iterator<List<T>> {
    private final List<T> original;

    private int start;
    private int end;

    private LongestSequenceIterator(final List<T> original, final int start, final int end) {
      this.original = original;
      this.start = start;
      this.end = end;
    }

    @Override public boolean hasNext() {
      return end > 0;
    }

    @Override public List<T> next() {
      final List<T> value = original.subList(start, end);
      if (start < end - 1) {
        start++;
      } else {
        start = 0;
        end--;
      }
      return value;
    }

    @Override public void remove() {
      throw new UnsupportedOperationException("Not supported.");
    }
  }
}
