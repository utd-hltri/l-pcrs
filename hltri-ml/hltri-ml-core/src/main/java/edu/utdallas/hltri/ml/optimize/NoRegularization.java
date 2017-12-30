package edu.utdallas.hltri.ml.optimize;

/**
 * User: bryan
 * Date: 12/20/12
 * Time: 5:59 PM
 * Created with IntelliJ IDEA.
 */
public class NoRegularization implements Regularization.WithGradient {

  @Override
  public double value(double[] weights) {
    return 0;
  }

  @Override
  public void gradient(double[] weights, double[] gradient) {
  }
}
