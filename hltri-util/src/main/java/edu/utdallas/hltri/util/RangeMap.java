/*
 * The Broad Institute SOFTWARE COPYRIGHT NOTICE AGREEMENT This software and its documentation are copyright 2003 by
 * the Broad Institute/Massachusetts Institute of Technology. All rights are reserved. This software is supplied
 * without any warranty or guaranteed support whatsoever. Neither the Broad Institute nor MIT can be responsible for
 * its use, misuse, or functionality.
 */
package edu.utdallas.hltri.util;

import java.io.Serializable;
import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import javax.annotation.Nonnull;

/**
 * A rangeMap contains a set of intervals and maps each interval to a set of objects that exist in
 * that interval. It allows very fast lookups of range queries.
 *
 * An interval is closed, that is, the interval from 20-30 includes both 20 and 30.
 */
@SuppressWarnings("unused")
public class RangeMap<T> extends AbstractSet<T> implements Serializable {

  public static final long serialVersionUID = 1L;

  /**
   * Imagine a number line. Take the set of start and stop coordinates for each object in the
   * RangeMap and place them on the number line. This divides the number line into a series of
   * intervals. Each interval can be associated with a distinct list of objects that contain all
   * points in that interval. The RangeMap is a sorted map that keys the start of the interval to
   * this distinct list of objects. <p> Objects that fully contain the points in more than one
   * interval will be contained in multiple lists. One list for each interval they contain.
   */
  private final SortedMap<Integer, List<T>> map = new TreeMap<>();

  /**
   * Keeps track of the start and end for each object in the map so we can
   * remove them easily if necessary.
   *
   * Maps Object -> int[2]. the first element in the array is the start of
   * the key and the second element is the end of the key.
   */
  private final Map<T, int[]> objectList = new HashMap<>();

  public int size() {
    return objectList.size();
  }

  @Nonnull private Set<T> values() {
    return objectList.keySet();
  }

  @Override @Nonnull public Iterator<T> iterator() {
    return values().iterator();
  }

  /**
   * Returns the start of the first non-empty interval.  If the RangeMap has no entries, returns 0.
   */
  public int getStart() {
    return size() == 0 ? 0 : map.firstKey();
  }

  /**
   * Returns the end of the last interval
   */
  public int getStop() {
    return size() == 0 ? 0 : map.lastKey() - 1;
  }

  /**
   * An interval is a convenient data structure used to return a section of a RangeMap. It contains
   * a start, a stop, and a set of elements that occur in that range.
   */
  @SuppressWarnings("unused")
  private static class Interval<K> {
    int start;
    int stop;
    public Set<K> elements;

    Interval() {
    }

    Interval(int start, int stop, Set<K> elements) {
      this.start = start;
      this.stop = stop;
      this.elements = new HashSet<>(elements);
    }

    @Override public int hashCode() {
      int result = start;
      result = 31 * result + stop;
      result = 31 * result + elements.hashCode();
      return result;
    }

    @Override public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      Interval<?> interval = (Interval<?>) o;
      return start == interval.start &&
          stop == interval.stop &&
          Objects.equals(elements, interval.elements);
    }

    @Override @Nonnull public String toString() {
      String result = "";
      for (K element : elements) {
        result += element.toString();
      }
      return "interval " + start + " -> " + stop + " : " + result + "\n";
    }

    public int getLength() {
      return 1 + stop - start;
    }
  }

  @Nonnull public SortedMap<Integer, List<T>> getMap() {
    return Collections.unmodifiableSortedMap(map);
  }

  @Nonnull public Map<T, int[]> getObjectList() {
    return Collections.unmodifiableMap(objectList);
  }

  @Nonnull public List<Interval<T>> getDisjointRegions() {
    return getRegions(true);
  }

  @Nonnull private List<Interval<T>> getRegions() {
    return getRegions(false);
  }

  /**
   * Returns a list of Interval objects that contain overlapping elements.
   *
   * @param splitDisjointRegions if true, return disjoint regions, if false, return "old-style"
   *                             regions. Consider the following:
   *
   *                             <---------Feature A-------> <----------Feature B---------->
   *                             <---------Feature C------->
   *
   *                             - If we group these into a single region, then any two groups
   *                             returned by this method will be separated by at least one gap (ie
   *                             an area that contains no elements at all). This is more convenient.
   *                             - If we group them into two regions, then every element in a region
   *                             will overlap at least one other element in that region (if the
   *                             region contains more than one element). This is more mathematically
   *                             precise.
   *
   *                             There are good reasons to want either behavior so we support both.
   *                             Disjoint regions are more mathematically consistent but may lie
   *                             immediately adjacent to each other. Non-disjoint regions are better
   *                             separated, but "old-style" regions may contain an element that does
   *                             not overlap anything else within it.
   *
   *                             No element in a region will ever overlap an element outside the
   *                             region.
   *
   *                             This method returns a List of Interval objects. The Interval
   *                             objects returned will be in increasing order of their placement on
   *                             the RangeMap.
   * @see #getIntervals()
   */
  @Nonnull private List<Interval<T>> getRegions(boolean splitDisjointRegions) {
    List<Interval<T>> list = new ArrayList<>();
    Interval<T> currentInterval = null;
    // Iterate through the map (in order). As long as there are intervals with at least one entry, add that entry
    // to the current cluster.
    // When you hit an interval with no values, finish the interval to return.
    for (Map.Entry<Integer, List<T>> entry : map.entrySet()) {
      List<T> currentValue = entry.getValue();
      int currentKey = entry.getKey();
      if (currentValue.size() == 0) {
        assert currentInterval != null : "Multiple 0 intervals in a row were detected.";
        currentInterval.stop = currentKey - 1;
        list.add(currentInterval);
        currentInterval = null;
      } else {
        if (currentInterval != null) {
          /*
           * If two intervals are adjacent but disjoint (no element
					 * spans them) then return them as two groups, not one.
					 */
          boolean disjoint = true;
          for (T aCurrentValue : currentValue) {
            if (currentInterval.elements.contains(aCurrentValue)) {
              disjoint = false;
            }
          }
          if (disjoint && splitDisjointRegions) {
            currentInterval.stop = currentKey - 1;
            list.add(currentInterval);
            currentInterval = null;
          }
        }

        if (currentInterval == null) {
          currentInterval = new Interval<>();
          currentInterval.start = currentKey;
          currentInterval.elements = new HashSet<>();
        }
        currentInterval.elements.addAll(currentValue);
      }
    }
    assert currentInterval == null : "RangeMap did not end with a 0 entry.";
    return list;
  }

  /**
   * Returns the individual intervals in the rangeMap.
   *
   * An interval is an area of a range map where every point in that
   * area is overlapped by the same set of elements. Consider this:
   *
   * <-----Feature A----->           <---Feature C--->
   * <---------Feature B--------->
   *
   * |   1   |     2     |     3     | 4 |     5     |
   *
   * This would be represented by one region but counts as five
   * intervals: interval 1 contains A only, interval 2 contains A & B,
   * interval 3 holds B only, and so on.
   *
   * Callers should be prepared for Intervals that contain no elements.
   *
   * This method returns a List of Interval objects. The Interval objects
   * returned will be in increasing order of their placement on the RangeMap.
   *
   * @see #getRegions()
   */
  @Nonnull private List<Interval<T>> getIntervals() {
    List<Interval<T>> list = new ArrayList<>();
    Interval<T> currentInterval = null;

    for (Map.Entry<Integer, List<T>> entry : map.entrySet()) {
      List<T> currentValue = entry.getValue();
      int currentKey = entry.getKey();

      if (currentInterval != null) {
        currentInterval.stop = currentKey - 1;
        list.add(currentInterval);
      }

      currentInterval = new Interval<>();
      currentInterval.start = currentKey;
      currentInterval.elements = new HashSet<>();
      if (currentValue != null) {
        currentInterval.elements.addAll(currentValue);
      }
    }

    assert currentInterval != null;
    assert currentInterval.elements.size() == 0 : "RangeMap did not end with an empty entry";
    return list;
  }

  /**
   * Like getIntervals, but allows you to define a start and stop.  First and last interval are
   * guaranteed to start and stop at the endpoints.
   */
  @Nonnull private List<Interval<T>> getIntervals(int start, int stop) {
    List<Interval<T>> list = new ArrayList<>();
    Interval<T> currentInterval = null;

    for (Map.Entry<Integer, List<T>> entry : getSubMap(start, stop + 1).entrySet()) {
      List<T> currentValue = entry.getValue();
      int currentKey = entry.getKey();

      if (currentInterval == null && start < currentKey) {
        // We started before the beginning of the range map, add in a dummy empty interval
        currentInterval = new Interval<>();
        currentInterval.start = start;
        currentInterval.elements = Collections.emptySet();
      }

      if (currentInterval != null) {
        currentInterval.stop = currentKey - 1;
        list.add(currentInterval);
      }

      currentInterval = new Interval<>();
      currentInterval.start = currentKey > start ? currentKey : start;
      if (currentValue == null) {
        currentInterval.elements = Collections.emptySet();
      } else {
        currentInterval.elements = new HashSet<>(currentValue);
      }
    }

    if (currentInterval != null) {
      currentInterval.stop = stop;
      list.add(currentInterval);
    }
    return list;
  }

  /**
   * Returns the empty intervals.  A start and stop are required to bound the edges of the first and
   * last region.  These bounds can be within the existing region. The returned intervals are
   * closed.
   */
  @Nonnull private List<Interval<T>> getEmptyIntervals(int start, int totalSize) {
    assert start <= totalSize : "Start (" + start + ") must be less than stop (" + totalSize + ")";
    List<Interval<T>> list = new ArrayList<>();
    Interval<T> currentInterval = null;

    boolean beforeStart = true;
    for (Map.Entry<Integer, List<T>> entry : map.entrySet()) {
      List<T> currentValue = entry.getValue();
      int currentKey = entry.getKey();
      if (currentKey > totalSize)
        break;
      if (currentValue.size() == 0) {
        assert !beforeStart : "Map started with a 0 entry.";
        assert currentInterval == null : "Multiple 0 intervals in a row were detected.";
        if (currentKey < start) {
          beforeStart = true;
        } else {
          currentInterval = new Interval<>();
          currentInterval.start = currentKey;
        }
      } else {
        if (beforeStart) {
          beforeStart = false;
          if (start < currentKey) {
            currentInterval = new Interval<>();
            currentInterval.start = start;
          }
        }
        if (currentInterval != null) {
          currentInterval.stop = currentKey - 1;
          list.add(currentInterval);
          currentInterval = null;
        }
      }
    }
    if (beforeStart) {
      currentInterval = new Interval<>();
      currentInterval.start = start;
    }
    if (currentInterval != null) {
      currentInterval.stop = totalSize;
      list.add(currentInterval);
    }
    return list;
  }

  /**
   * Returns objects in the range map that overlap this range.
   *
   * query:      *-----------------*      returned
   * *------------*              returned
   * *-------------*       returned
   * *---------------------*    returned
   * *----*                        returned
   * *-*    returned
   */
  @Nonnull public Set<T> find(int argLow, int argHigh) {
    Set<T> ret = new HashSet<>();
    getSubMap(argLow, argHigh + 1).values().forEach(ret::addAll);
    return ret;
  }

  @Nonnull public Set<T> findIntersecting(int argLow, int argHigh) {
    Set<T> ret = new HashSet<T>();
    for (T o : find(argLow, argHigh)) {
      int[] bounds = objectList.get(o);
      if (bounds[0] < argLow && bounds[1] <= argHigh + 1) {
        ret.add(o);
      } else if (bounds[0] >= argLow && bounds[1] > argHigh + 1) {
        ret.add(o);
      }
    }
    return ret;
  }

  /**
   * Returns objects in the range map that are fully contained by the range.
   *
   * query:      *-----------------*      returned
   * *------------*              not returned
   * *-------------*       returned
   * *---------------------*    not returned
   * *----*                        not returned
   * *-*    not returned
   */
  @Nonnull public Set<T> findContained(int argLow, int argHigh) {
    Set<T> ret = new HashSet<>();
    for (T o : find(argLow, argHigh)) {
      int[] bounds = objectList.get(o);
      if (bounds[0] >= argLow && bounds[1] <= argHigh + 1) {
        ret.add(o);
      }
    }
    return ret;
  }

  @Nonnull public Set<T> findStrictlyContained(int argLow, int argHigh) {
    Set<T> ret = new HashSet<>();
    for (T o : find(argLow, argHigh)) {
      int[] bounds = objectList.get(o);
      if (bounds[0] > argLow && bounds[1] < argHigh + 1) {
        ret.add(o);
      }
    }
    return ret;
  }

  /**
   * Returns objects in the range map that fully contain the range.
   *
   * query:      *-----------------*      returned
   * *------------*              not returned
   * *-------------*       not returned
   * *---------------------*    returned
   * *----*                        not returned
   * *-*    not returned
   */
  @Nonnull public Set<T> findContaining(int argLow, int argHigh) {
    Set<T> ret = new HashSet<>();
    for (T o : find(argLow, argHigh)) {
      int[] bounds = objectList.get(o);
      if (bounds[0] <= argLow && bounds[1] >= argHigh + 1) {
        ret.add(o);
      }
    }
    return ret;
  }

  @Nonnull public Set<T> findStrictlyContaining(int argLow, int argHigh) {
    Set<T> ret = new HashSet<>();
    for (T o : find(argLow, argHigh)) {
      int[] bounds = objectList.get(o);
      if (bounds[0] < argLow && bounds[1] > argHigh + 1) {
        ret.add(o);
      }
    }
    return ret;
  }

  /**
   * Returns true if the map has an entry in the specified range.
   */
  public boolean hasEntry(int argLow, int argHigh) {
    Iterator it = getSubMap(argLow, argHigh + 1).values().iterator();

    // If there is no entry or a single empty entry return false.  Otherwise, return true
    return !(!it.hasNext() || ((ArrayList) it.next()).size() == 0 && !it.hasNext());
  }

  /**
   * Returns the sub-map from argLow+1 included to argHigh excluded.
   */
  @Nonnull private SortedMap<Integer, List<T>> getSubMap(int argLow, int argHigh) {
    if (argLow > argHigh)
      throw new IllegalArgumentException(
          "Low end of range (" + argLow + ") is greater than high end (" + argHigh + ")");
    int lowBound = argLow + 1;

    SortedMap<Integer, List<T>> lowMap = map.headMap(lowBound);
    if (lowMap.size() != 0)
      lowBound = lowMap.lastKey();
    return map.subMap(lowBound, argHigh);
  }

  /**
   * Adds the object o into the map with interval lo - high. The interval is inclusive, with both
   * the high and low being part of the interval.
   */
  public void add(int argLow, int argHigh, @Nonnull T o) {
    if (argLow > argHigh)
      throw new IllegalArgumentException(
          "Low end of range (" + argLow + ") is greater than high end (" + argHigh + ")");
    if (objectList.containsKey(o)) {
      throw new IllegalArgumentException("RangeMap already contains " + o);
    }
    int high = argHigh + 1;
    objectList.put(o, new int[]{argLow, high});

    makeSplitAt(argLow);
    makeSplitAt(high);

    // Take the new sub-map and add o to every element
    for (List<T> list : map.subMap(argLow, high).values()) {
      list.add(o);
    }
  }


  /**
   * Returns true if the object is in the RangeMap
   */
  @SuppressWarnings("SuspiciousMethodCalls")
  @Override public boolean contains(@Nonnull Object o) {
    return objectList.get(o) != null;
  }

  /**
   * Removes the object o from the map.
   */
  @SuppressWarnings("SuspiciousMethodCalls")
  @Override public boolean remove(@Nonnull Object o) {
    int[] bounds = objectList.get(o);
    if (bounds == null) {
      throw new IllegalArgumentException("RangeMap does not contain: " + o);
    }
    objectList.remove(o);
    // Remove the object from each interval it contained.
    boolean removed = false;
    for (List<T> l : map.subMap(bounds[0], bounds[1]).values()) {
      removed = l.remove(o) || removed;
    }
		/*
		 * Check to see if we can remove an interval. This occurs if there is no change from the
		 */
    mergeAt(bounds[0]);
    mergeAt(bounds[1]);
    return removed;
  }

  private void mergeAt(int bound) {
    List<T> startList = map.get(bound);
    SortedMap<Integer, List<T>> lowMap = map.headMap(bound);
    if (lowMap.size() == 0) {
      if (startList.size() == 0)
        map.remove(bound);
    } else {
      List<T> previousList = lowMap.get(lowMap.lastKey());
      if (previousList.equals(startList)) {
        map.remove(bound);
      }
    }
  }

  private void makeSplitAt(int bound) {
    // If there is already a split, we are done
    List<T> entry = map.get(bound);
    if (entry == null) {
      // Find the previous interval and duplicate it
      SortedMap<Integer, List<T>> headMap = map.headMap(bound);
      if (headMap.size() == 0) {
        map.put(bound, new ArrayList<>());
      } else {
        map.put(bound, new ArrayList<>(map.get(headMap.lastKey())));
      }
    }
  }

  @Override @Nonnull public String toString() {
    StringBuilder ret = new StringBuilder("Range Map (x-y:count) ");
    Integer lastKey = null;
    int lastCount = 0;
    for (Integer key : map.keySet()) {
      if (lastKey != null) {
        ret.append(lastKey).append("-").append(key - 1).append(":").append(lastCount).append(" ");
      }
      lastKey = key;
      lastCount = map.get(key).size();
    }
    ret.append(lastKey).append("-end:").append(lastCount);
    return ret.toString();
  }
}
