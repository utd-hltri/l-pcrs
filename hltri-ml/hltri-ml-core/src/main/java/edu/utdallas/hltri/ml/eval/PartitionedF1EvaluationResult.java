package edu.utdallas.hltri.ml.eval;

import com.google.common.collect.*;
import edu.utdallas.hltri.ml.*;

import java.util.*;

/**
 * User: bryan
 * Date: 5/13/13
 * Time: 4:45 PM
 * Created with IntelliJ IDEA.
 */
public class PartitionedF1EvaluationResult<P,T> implements EvaluationResult<T> {
  private Map<P, F1EvaluationResult<T>> partitions = new HashMap<>();

  public double getMacroF1() {
    double sum = 0;
    for (F1EvaluationResult result : partitions.values()) {
      sum += result.getF1();
    }
//    for (P partition : partitions.keySet()) {
//      System.err.println("Partition " + partition + " f1: " + partitions.get(partition).getF1());
//      System.err.println( partitions.get(partition).countTruePositives() + " " + partitions.get(partition).countFalsePositives() + " " + partitions.get(partition).countFalseNegatives());
//    }
    assert ! Double.isNaN(sum);
    assert partitions.size() > 0;
    return sum / partitions.size();
  }

  public double getMicroF1() {
    throw new UnsupportedOperationException("Not implemented yet");
  }

  public double getOverallScore() {
    return getMicroF1();
  }

  public F1EvaluationResult<T> getPartitionResult(P partition) {
    return partitions.get(partition);
  }

  public F1EvaluationResult<T> getOrAddPartitionResult(P partition) {
    if ( ! partitions.containsKey(partition)) {
      partitions.put(partition, new F1EvaluationResult<T>());
    }
    return partitions.get(partition);
  }

  public Iterable<T> correctInstances() {
    List<Iterable<T>> subResults = new ArrayList<>();
    for (F1EvaluationResult<T> subResult : partitions.values()) {
      subResults.add(subResult.correctInstances());
    }
    return Iterables.concat(subResults);
  }

  public Iterable<T> incorrectInstances() {
    List<Iterable<T>> subResults = new ArrayList<>();
    for (F1EvaluationResult<T> subResult : partitions.values()) {
      subResults.add(subResult.incorrectInstances());
    }
    return Iterables.concat(subResults);
  }
}
