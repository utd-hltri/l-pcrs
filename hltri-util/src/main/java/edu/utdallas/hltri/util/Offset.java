package edu.utdallas.hltri.util;

import com.google.common.collect.Iterators;
import com.google.common.collect.PeekingIterator;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

/**
 * Created by rmm120030 on 9/7/17.
 */
public interface Offset {
  long getStart();

  long getEnd();

  /**
   * Merges two sequences of Offsets, subsuming items from the lowPriority sequence with overlapping items from the
   * highPriority sequence.
   * For example:
   * Low Priority = --l1-- --l2-- --l3-- --l4-- --l5-- --l6--
   * High Priority=        --h1--        -----h2------
   * Return       = --l1-- --h1-- --l3-- -----h2------ --l6--
   * @param lowPriority less important sequence (subsumed by highPriority)
   * @param highPriority more important sequence (subsumes lowPriority)
   * @return a new list of non-overlapping offsets, with every item from highPriority and any item from lowPriority
   *         that does not overlap with an item from highPriority, sorted by offset
   */
  static <A extends Offset, L extends A, H extends A> List<A> mergeSequencesWithSubsumption(List<L> lowPriority, List<H> highPriority) {
    // sorts are O(n) if lists are already sorted
    final ArrayList<L> lpSorted = new ArrayList<>(lowPriority);
    lpSorted.sort(Comparator.comparingLong(Offset::getStart));
    final ArrayList<H> hpSorted = new ArrayList<>(highPriority);
    hpSorted.sort(Comparator.comparingLong(Offset::getStart));
    final Iterator<L> lowIt = lpSorted.iterator();
    final PeekingIterator<H> highIt = Iterators.peekingIterator(hpSorted.iterator());
    final List<A> merged = new ArrayList<>(lpSorted.size() + hpSorted.size());
    while(lowIt.hasNext() || highIt.hasNext()) {
      if (!lowIt.hasNext()) {
        merged.add(highIt.next());
      } else if (!highIt.hasNext()) {
        merged.add(lowIt.next());
      } else {
        final A nextLow = lowIt.next();
        final long lowStart = nextLow.getStart();
        final long lowEnd = nextLow.getEnd();
        final A nextHigh = highIt.peek();
        final long highStart = nextHigh.getStart();
        final long highEnd = nextHigh.getEnd();
        if (lowEnd <= highStart) {
          merged.add(nextLow);
        } else if (lowEnd == highEnd) {
          merged.add(highIt.next());
        } else if (lowEnd > highEnd) {
          merged.add(highIt.next());
          if (lowStart >= highEnd) {
            merged.add(nextLow);
          }
        }
      }
    }

    return merged;
  }
}
