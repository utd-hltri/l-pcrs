package edu.utdallas.hltri.ml.eval;

/**
 * User: pretty much bryan
 * Date: 5/13/13
 * Time: 4:24 PM
 * Created with IntelliJ IDEA.
 *
 * Version of F1EvaluationResult that doesn't hold on to the things evaluates, just counts.
 */
public class F1Computer {
  private int truePositives;
  private int trueNegatives;
  private int falsePositives;
  private int falseNegatives;

  public F1Computer() {
    truePositives = 0;
    trueNegatives = 0;
    falsePositives = 0;
    falseNegatives = 0;
  }

  public void truePositive() { truePositives++; }
  public void trueNegative() { trueNegatives++; }
  public void falsePositive() { falsePositives++; }
  public void falseNegative() { falseNegatives++; }

  public int truePositives() { return truePositives; }

  public int trueNegatives() { return trueNegatives; }

  public int falsePositives() { return falsePositives; }

  public int falseNegatives() { return falseNegatives;  }

  public double getF1() {
    return EvalUtils.fMeasure(precision(), recall());
  }

  public double getOverallScore() { return getF1(); }

  public double precision() {
    return EvalUtils.precision(truePositives(), falsePositives());
  }

  public double recall() {
    return EvalUtils.recall(truePositives(), falseNegatives());
  }
}
