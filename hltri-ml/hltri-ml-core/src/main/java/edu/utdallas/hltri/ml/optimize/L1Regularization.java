package edu.utdallas.hltri.ml.optimize;

/**
 * User: bryan
 * Date: 12/20/12
 * Time: 5:52 PM
 * Created with IntelliJ IDEA.
 */
public class L1Regularization implements Regularization.WithGradient {


  private final double lambda;

  public L1Regularization(double lambda) {
    this.lambda = lambda;
  }

  @Override
  public double value(double[] weights) {
    double sum = 0;
    for (double weight : weights) {
      sum += Math.abs(weight);
    }
    return -lambda * sum;
  }

  @Override
  public void gradient(double[] weights, double[] gradient) {
    for (int i = 0; i < weights.length; i++) {
      gradient[i] += lambda * (weights[i] > 0 ? -1 : weights[i] == 0 ? 0 : 1);
    }
  }
}
