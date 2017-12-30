package edu.utdallas.hltri.ml.eval;

import com.google.common.collect.*;
import edu.utdallas.hltri.ml.*;

import java.util.*;

/**
 * User: bryan
 * Date: 5/13/13
 * Time: 4:24 PM
 * Created with IntelliJ IDEA.
 *
 * Abstracts an F1 evalutaion, keeping and exposing the instances of true positives, false negatives, etc for
 * inspection.
 */
public class F1EvaluationResult<T> implements EvaluationResult<T> {
  private List<T> truePositives = new ArrayList<>();
  private List<T> trueNegatives = new ArrayList<>();
  private List<T> falsePositives = new ArrayList<>();
  private List<T> falseNegatives = new ArrayList<>();

  public void truePositive(T instance) { truePositives.add(instance); }
  public void trueNegative(T instance) { trueNegatives.add(instance); }
  public void falsePositive(T instance) { falsePositives.add(instance); }
  public void falseNegative(T instance) { falseNegatives.add(instance); }
  public void incorporate(F1EvaluationResult<T> other) {
    other.truePositives().forEach(this::truePositive);
    other.trueNegatives().forEach(this::trueNegative);
    other.falsePositives().forEach(this::falsePositive);
    other.falseNegatives().forEach(this::falseNegative);
  }

  public int countTruePositives() { return truePositives.size(); }

  public int countTrueNegatives() { return trueNegatives.size(); }

  public int countFalsePositives() { return falsePositives.size(); }

  public int countFalseNegatives() { return falseNegatives.size();  }

  public int countAll() {
    return truePositives.size() + trueNegatives.size() + falseNegatives.size() + falsePositives.size();
  }

  public int countSupport() {
    return countTruePositives() + countFalseNegatives();
  }

  public int countPredicted() {
    return countTruePositives() + countFalsePositives();
  }

  public List<T> truePositives() { return truePositives; }

  public List<T> trueNegatives() { return trueNegatives; }

  public List<T> falsePositives() { return falsePositives;  }

  public List<T> falseNegatives() { return falseNegatives; }

  public double getF1() {
    return EvalUtils.fMeasure(precision(), recall());
  }

  public double getOverallScore() { return getF1(); }

  public double precision() {
    return EvalUtils.precision(countTruePositives(), countFalsePositives());
  }

  public double recall() {
    return EvalUtils.recall(countTruePositives(), countFalseNegatives());
  }

  public Iterable<T> correctInstances() {
    return Iterables.concat(truePositives(), trueNegatives());
  }

  public Iterable<T> incorrectInstances() {
    return Iterables.concat(falsePositives(), falseNegatives());
  }

  @Override
  public String toString() {
    return
        "\n------------------------------------------------" +
        "\n|   F1   | Precision | Recall | Support | Pred |" +
        String.format("\n| %.5f|  %.5f  | %.5f|  %5d  | %4d |", getF1(), precision(), recall(), countSupport(), countPredicted()) +
        "\n------------------------------------------------";
  }

  public String toCsvString() {
    return String.valueOf(countTruePositives()) +
        "," + countFalsePositives() +
        "," + countTrueNegatives() +
        "," + countFalseNegatives() +
        "," + precision() +
        "," + recall() +
        "," + getF1();
  }
}
