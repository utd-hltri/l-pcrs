package edu.utdallas.hltri.ml.optimize;

/**
 * User: bryan
 * Date: 12/20/12
 * Time: 5:49 PM
 * Created with IntelliJ IDEA.
 */
public interface Regularization {
  public double value(double[] weights);

  public static interface WithGradient extends Regularization {
    public void gradient(double[] weights, double[] gradient);
  }
}
