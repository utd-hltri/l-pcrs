package edu.utdallas.hltri.util;

import java.util.*;

/**
 * An implementation of the paper "Interval Stabbing Problems in Small Integer Ranges" by
 * Jens M. Schmidt, ISAAC 2009, LNCS 5878, pp. 163-173, 2009. Springer-Verlag Berlin Heidelberg
 * Subclass and implement the abstract methods for your particular data.
 * @author Bryan Rink (bryan@hlt.utdallas.edu)
 */
public abstract class SchmidtIntervalTree<I> {
  /** The highest possible endpoint of an interval */
  private final int maxOffset;

  private final int minOffset;

  private List<I> intervals;
  private Map<Integer,List<I>> smaller = new HashMap<Integer,List<I>>();
  private List<Node> starts = new ArrayList<Node>();
  private List<Node> starts2 = new ArrayList<Node>();
  private Map<I,Node> nodeMap = new HashMap<I,Node>();

  private Node root = new Node(null);

  public abstract int intervalStart(I interval);
  public abstract int intervalEnd(I interval);

  public boolean contains(I interval, int offset) {
    return intervalStart(interval) <= offset && intervalEnd(interval) >= offset;
  }

  public boolean intersect(I interval, int queryStart, int queryEnd) {
    return !(queryEnd < intervalStart(interval) || queryStart > intervalEnd(interval));
  }

  /**
   * @param minOffset Minimum possible end point of an interval
   * @param maxOffset Maximum possible end point of an interval
   */
  public SchmidtIntervalTree(int minOffset, int maxOffset, Iterable<I> intervals) {
    this.maxOffset = maxOffset;
    this.minOffset = minOffset;
    sortIntervals(intervals);
    preprocessing();
  }

  private void sortIntervals(Iterable<I> intervalsToSort) {
    // bucket sort as in the paper would be more efficient
    List<I> sorted = new ArrayList<I>();
    for (I interval : intervalsToSort) {
      sorted.add(interval);
    }
    Collections.sort(sorted, new Comparator<I>() {
      @Override
      public int compare(I i1, I i2) {
        int diff = intervalStart(i1) - intervalStart(i2);
        return diff != 0 ? diff : intervalEnd(i1) - intervalEnd(i2);
      }
    });
    this.intervals = sorted;
  }

  /** Algorithm 3. Preprocessing, pg. 169
   * Builds the tree.
   */
  private void preprocessing() {

    Map<Integer, List<I>> events = new HashMap<Integer,List<I>>();

    List<I> toRemove = new ArrayList<I>();
    int lastStart = maxOffset+1;
    for (int i = intervals.size()-1; i >=0; i--) {
      I interval = intervals.get(i);
      int start = intervalStart(interval);
      if (start == lastStart) {
        addToMap(smaller,start,interval);
        toRemove.add(interval);
      }
      lastStart = start;
    }


    intervals.removeAll(toRemove);
    for (I interval : intervals) {
      addToMap(events, intervalStart(interval), interval);
      addToMap(events, intervalEnd(interval), interval);
      nodeMap.put(interval, new Node(interval));
    }


    List<Node> L = new ArrayList<Node>();
    Node rightMost = null;
    for (int q = minOffset; q <= maxOffset; q++) {
      starts2.add(rightMost);
      if ( ! L.isEmpty()) {
        starts.add(L.get(L.size()-1));
      } else {
        starts.add(null);
      }
      List<I> eventParticipants = events.get(q);
      if (eventParticipants == null) { continue; }
      for (int i = eventParticipants.size()-1; i >=0 ; i--) {
        I interval = eventParticipants.get(i);
        Node node = nodeMap.get(interval);
        if (intervalStart(interval) == q) {
          rightMost = node;
          L.add(node);
          starts.set(starts.size()-1, node);
          starts2.set(starts2.size()-1, rightMost);
        } else {
          int pos = L.indexOf(node); // could be done faster by saving links as in paper
          if (pos > 0) {
            L.get(pos-1).addChild(node);
          } else {
            root.addChild(node);
          }
          L.remove(node);
        }
      }
    }
  }

  private void addToMap(Map<Integer,List<I>> map, int index, I interval) {
    if ( ! map.containsKey(index)) {
      map.put(index, new ArrayList<I>());
    }
    map.get(index).add(interval);
  }

  protected void traverse(Node v, Deque<I> stack, int q) {
    if (v != root) { stack.push(v.interval); }
    int start = v.intervalStart();
    if (smaller.containsKey(start)) {
      for (I interval : smaller.get(start)) {
        if ( ! contains(interval, q)) { break; }
        stack.push(interval);
      }
    }
    for (Node w : rightMostPath(v, q)) {
      traverse(w, stack, q);
    }
  }

  protected Deque<Node> rightMostPath(Node v, int q) {
    Deque<Node> path = new ArrayDeque<Node>();
    if (v.leftSibling == null || ! contains(v.leftSibling.interval, q)) {
      return path;
    }
    Node current = v.leftSibling;
    do {
      path.push(current);
      current = current.lastChild;
    } while (current != null && contains(current.interval, q));
    return path;
  }

//  protected Deque<Node> rightMostPathInterval(Node v, int queryStart) {
//    if (v == null) { throw new IllegalArgumentException("null node given"); }
//    Deque<Node> path = new ArrayDeque<Node>();
//    if (v.leftSibling == null || intervalEnd(v.leftSibling.interval) < queryStart) {
//      return path;
//    }
//    Node current = v.leftSibling;
//    do {
//      path.push(current);
//      current = current.lastChild;
//    } while (current != null && intervalEnd(current.interval) >= queryStart);
//    return path;
//  }


  protected void computePathToRoot(Node start, List<Node> path) {
    if (start == null) { return; }
    path.add(start);
    computePathToRoot(start.parent, path);
  }

  public Deque<I> stabbingQuery(int q) {
    Deque<I> stack = new ArrayDeque();
    if(starts.get(q-minOffset) == null) { return stack; }
    List<Node> path = new ArrayList<Node>();
    computePathToRoot(starts.get(q-minOffset), path);
    for (Node node : path) {
      traverse(node, stack, q);
    }
    return stack;
  }


  protected void traverseInterval(Node v, Deque<I> stack, int queryStart, int queryEnd) {
    if (v != root) { stack.push(v.interval); }  // root is a dummy node
    int start = v.intervalStart();
    if (smaller.containsKey(start)) {
      for (I interval : smaller.get(start)) {
        if ( ! intersect(interval, queryStart, queryEnd)) { break; }
        stack.push(interval);
      }
    }
    for (Node w : rightMostPathInterval(v, queryStart, queryEnd)) {
      traverseInterval(w, stack, queryStart, queryEnd);
    }
  }

  protected Deque<Node> rightMostPathInterval(Node v, int queryStart, int queryEnd) {
    Deque<Node> path = new ArrayDeque<Node>();
    if (v.leftSibling == null || ! intersect(v.leftSibling.interval, queryStart, queryEnd)) {
      return path;
    }
    Node current = v.leftSibling;
    do {
      path.push(current);
      current = current.lastChild;
    } while (current != null && intersect(current.interval, queryStart, queryEnd));
    return path;
  }

  public Deque<I> intersectionQuery(int queryStart, int queryEnd) {
    Deque<I> stack = new ArrayDeque();
    if (queryStart < minOffset) { queryStart = minOffset; }
    if (queryEnd > maxOffset) { queryEnd = maxOffset; }
    if (queryStart > queryEnd) { return stack; }
    //if(starts.get(queryEnd-minOffset) == null) { return stack; }
    List<Node> path = new ArrayList<Node>();
    Node t1 = starts.get(queryStart-minOffset);
    Node t2 = starts2.get(queryEnd-minOffset);
    Node t = t1;
    if (t1 == null && t2 == null) { return stack; }
    if (t1 == null) { t = t2; }
    else if (t2 == null) { t = t1; }
    else {
      if (t1.intervalStart() < t2.intervalStart()) { t = t2; }
    }
    if ( ! intersect(t.interval, queryStart, queryEnd)) { t = (t == t1 ? t2 : t1); }
    if ( t == null || ! intersect(t.interval, queryStart, queryEnd)) { return stack; }
    computePathToRoot(t, path);
    for (Node node : path) {
      traverseInterval(node, stack, queryStart, queryEnd);
    }
    return stack;
  }



//    /**
//   * Finds all intervals that intersect the query, inclusive.
//   * @param queryStart
//   * @param queryEnd
//   * @return
//   */
//  public List<I> overlappingIntervals(int queryStart, int queryEnd) {
//    List<I> results = new ArrayList<I>();
//    Deque<I> endPoints = stabbingQuery(queryEnd);
//    Deque<I> startPoints = stabbingQuery(queryStart);
//    // merge
//    while (endPoints.size() > 0 || startPoints.size() > 0) {
//      if (endPoints.isEmpty()) { results.add(startPoints.pop()); }
//      else if(startPoints.isEmpty()) { results.add(endPoints.pop()); }
//      else {
//        I i1 = startPoints.peek();
//        I i2 = endPoints.peek();
//        int start1 = intervalStart(i1);
//        int start2 = intervalStart(i2);
//        if (startPoints.peek() == endPoints.peek()) {
//          results.add(startPoints.pop());
//          endPoints.pop();
//        } else if (start1 < start2 || (start1 == start2 && intervalEnd(i1) < intervalEnd(i2))) {
//          results.add(startPoints.pop());
//        } else {
//          results.add(endPoints.pop());
//        }
//      }
//    }
//    return results;
//  }

  protected class Node {
    public Node parent;
    public Node firstChild;
    public Node lastChild;
    public Node leftSibling;
    public Node rightSibling;
    public I interval;

    public Node(I interval) {
      this.interval = interval;
    }

    public void addChild(Node child) {
      child.parent = this;
      if (firstChild == null) {
        firstChild = child;
        lastChild = child;
      } else {
        lastChild.rightSibling = child;
        child.leftSibling = lastChild;
        lastChild = child;
      }
    }

    public int intervalStart() {
      if (this == root) { return minOffset; }
      return SchmidtIntervalTree.this.intervalStart(interval);
    }

    @Override
    public String toString() {
      return String.valueOf(interval);
    }
  }

  public static class DefaultInterval {
    public int start;
    public int end;

    public DefaultInterval(int start, int end) {
      this.start = start;
      this.end = end;
    }

    @Override
    public String toString() {
      return "[" + start +","+ end + "]";
    }
  }

  public static class DefaultSchmidtTree extends SchmidtIntervalTree<DefaultInterval> {

    public DefaultSchmidtTree(int min, int max, Iterable<DefaultInterval> intervals) {
      super(min, max, intervals);
    }

    @Override
    public int intervalStart(DefaultInterval interval) {
      return interval.start;
    }

    @Override
    public int intervalEnd(DefaultInterval interval) {
      return interval.end;
    }

  }

  public static void main(String... args) {
    List<DefaultInterval> intervals = new ArrayList<DefaultInterval>();
//    intervals.add(new DefaultInterval(1, 2));
//    intervals.add(new DefaultInterval(4, 5));
//    intervals.add(new DefaultInterval(7, 8));
    intervals.add(new DefaultInterval(8, 24));
    intervals.add(new DefaultInterval(1, 23));
    intervals.add(new DefaultInterval(5, 13));
    intervals.add(new DefaultInterval(2, 10));
    intervals.add(new DefaultInterval(18, 23));
    intervals.add(new DefaultInterval(4, 8));
    intervals.add(new DefaultInterval(19, 21));
    intervals.add(new DefaultInterval(22, 23));
    intervals.add(new DefaultInterval(3, 7));
    intervals.add(new DefaultInterval(12, 17));
    intervals.add(new DefaultInterval(7, 11));
    intervals.add(new DefaultInterval(15, 17));
    intervals.add(new DefaultInterval(6, 9));
    intervals.add(new DefaultInterval(14, 16));
    intervals.add(new DefaultInterval(5, 9));
    intervals.add(new DefaultInterval(13, 15));
    intervals.add(new DefaultInterval(5, 6));
    intervals.add(new DefaultInterval(8, 8));
    intervals.add(new DefaultInterval(9, 14));
    intervals.add(new DefaultInterval(10, 13));
    intervals.add(new DefaultInterval(11, 12));

    DefaultSchmidtTree tree = new DefaultSchmidtTree(1, 24, intervals);
//    for (int q = 1; q <= 24; q++) {
//      System.err.println("--q=" + q + "------");
//      for (DefaultInterval interval : tree.stabbingQuery(q)) {
//        System.err.println(interval.start + ", " + interval.end);
//      }
//    }

    for (int start = 0; start <= 25 ; start++) {
      System.err.println(start);
      for (int end = start; end <=25; end++) {
        // Brute force
        Set<DefaultInterval> shouldGet = new HashSet<DefaultInterval>();
        for (DefaultInterval interval : intervals) {
          if ((interval.start >= start && interval.start <= end) ||
              (interval.end <= end && interval.end >= start) ||
              (interval.start <= start && interval.end >= end)) {
            shouldGet.add(interval);
          }
        }
        Deque<DefaultInterval> output = tree.intersectionQuery(start, end);
        if ( ! shouldGet.equals(new HashSet<DefaultInterval>(output))) {
          System.err.println("DISCREPANCY: " + start + "   " + end);
          System.err.println(shouldGet);
          System.err.println(output);
        }
      }
    }

//    System.err.println("INTERVAL:");
//    for (DefaultInterval interval : tree.intersectionQuery(1, 2)) {
//      System.err.println(interval);
//    }
  }

}
