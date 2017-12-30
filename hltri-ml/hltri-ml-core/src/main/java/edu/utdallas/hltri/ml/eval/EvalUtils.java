package edu.utdallas.hltri.ml.eval;

/**
 * http://en.wikipedia.org/wiki/F1_score
 * User: bryan
 * Date: 12/18/12
 * Time: 3:34 PM
 * Created with IntelliJ IDEA.
 */
public class EvalUtils {
  public static double fMeasure(double precision, double recall) {
    assert !Double.isNaN(precision);
    assert !Double.isNaN(recall);
    if (precision == 0 || recall == 0) return 0;
    return 2.0 * (precision * recall) / (precision + recall);
  }

  public static double fMeasure(double truePositive, double falseNegative, double falsePositive) {
    return 2.0 * truePositive / (2.0 * truePositive + 1.0 * falseNegative + falsePositive);
  }

  public static double precision(double truePositive, double falsePositive) {
    if (truePositive == 0) return 0;
    return truePositive / (truePositive + falsePositive);
  }

  public static double recall(double truePositive, double falseNegative) {
    if (truePositive == 0) return 0;
    return truePositive / (truePositive + falseNegative);
  }

  public static double accuracy(double correct, double incorrect) {
    if (correct == 0) return 0;
    return correct / (correct + incorrect);
  }
}
