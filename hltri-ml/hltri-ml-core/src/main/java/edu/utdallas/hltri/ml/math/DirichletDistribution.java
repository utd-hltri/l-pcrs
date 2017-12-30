package edu.utdallas.hltri.ml.math;

import org.apache.commons.math3.distribution.AbstractMultivariateRealDistribution;
import org.apache.commons.math3.distribution.GammaDistribution;
import org.apache.commons.math3.distribution.RealDistribution;
import org.apache.commons.math3.random.RandomGenerator;
import org.apache.commons.math3.special.Gamma;

/**
 * Created by travis on 2/2/15.
 */
public class DirichletDistribution extends AbstractMultivariateRealDistribution {
  double[] alpha;
  RealDistribution[] gammas;

  public DirichletDistribution(RandomGenerator rng, double[] alpha) {
    super(rng, alpha.length);
    this.alpha = alpha;
  }

  /**
   * {@inheritDoc}
   */
  @Override public double[] sample() {
    initialize();
    double[] sample = new double[ alpha.length ];
    double sum = 0.0;
    for (int i = 0; i < sample.length; i++) {
      sample[i] = gammas[i].sample();
      sum += sample[i];
    }
    for (int i = 0; i < sample.length; i++) {
      sample[i] /= sum;
    }
    return sample;
  }

  /**
   * Returns the probability density function (PDF) of this distribution
   * evaluated at the specified point {@code x}. In general, the PDF is the
   * derivative of the cumulative distribution function. If the derivative
   * does not exist at {@code x}, then an appropriate replacement should be
   * returned, e.g. {@code Double.POSITIVE_INFINITY}, {@code Double.NaN}, or
   * the limit inferior or limit superior of the difference quotient.
   *
   * @param x Point at which the PDF is evaluated.
   * @return the value of the probability density function at point {@code x}.
   */
  @Override public double density(double[] x) {
    double prod = 1.0;
    for (int i = 0; i < x.length; i++) {
      prod *= Math.pow(x[i], alpha[i]);
    }
    return prod * multivariateBeta(x);
  }

  public static double multivariateBeta(double[] x) {
    double prod = 1.0;
    double sum = 0.0;
    for (int i = 0; i < x.length; i++) {
      prod *= Gamma.gamma(x[i]);
      sum += x[i];
    }
    return prod / Gamma.gamma(sum);
  }

  private void initialize() {
    if (this.gammas == null) {
      this.gammas = new RealDistribution[alpha.length];
      for (int i = 0; i < alpha.length; i++) {
        new GammaDistribution(random, alpha[i], 1);
      }
    }
  }
}
