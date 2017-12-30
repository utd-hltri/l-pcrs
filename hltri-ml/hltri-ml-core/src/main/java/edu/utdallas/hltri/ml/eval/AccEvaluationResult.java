package edu.utdallas.hltri.ml.eval;

import edu.utdallas.hltri.ml.EvaluationResult;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by ramon on 2/26/16.
 */
public class AccEvaluationResult<T> implements EvaluationResult<T> {
  private List<T> correct = new ArrayList<>();
  private List<T> incorrect = new ArrayList<>();

  public void correct(T instance) {
    correct.add(instance);
  }

  public void incorrect(T instance) {
    incorrect.add(instance);
  }

  public void incorporate(AccEvaluationResult<T> other) {
    other.correctInstances().forEach(this::correct);
    other.incorrectInstances().forEach(this::incorrect);
  }

  public int countCorrect() {
    return correct.size();
  }

  public int countIncorrect() {
    return incorrect.size();
  }

  @Override
  public double getOverallScore() {
    return getAccuracy();
  }

  public Iterable<T> correctInstances() {
    return correct;
  }

  public Iterable<T> incorrectInstances() {
    return incorrect;
  }

  public double getAccuracy() {
    return EvalUtils.accuracy(countCorrect(), countIncorrect());
  }

  @Override
  public String toString() {
    return
        "\n----------------------------------" +
        "\n| Correct | Incorrect | Accuracy |" +
        String.format("\n| %5d   |  %5d    | %.5f  |", countCorrect(), countIncorrect(), getAccuracy()) +
        "\n----------------------------------";
  }
}