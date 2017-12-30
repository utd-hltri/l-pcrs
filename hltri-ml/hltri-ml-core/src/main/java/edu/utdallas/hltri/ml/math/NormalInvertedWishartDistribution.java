package edu.utdallas.hltri.ml.math;

import org.apache.commons.math3.distribution.MultivariateNormalDistribution;
import org.apache.commons.math3.random.RandomGenerator;

/**
 * Created by travis on 2/3/15.
 */
public class NormalInvertedWishartDistribution {
  final RandomGenerator rng;
  final InvertedWishartDistribution iwd;
//  final MultivariateNormalDistribution mnd;
  final double lambda;
  final double[] mu_0;


  /**
   * Based on python code from: http://www.mit.edu/~mattjj/released-code/hsmm/stats_util.py
   * @param rng
   * @param psi
   * @param v
   */
  public NormalInvertedWishartDistribution(RandomGenerator rng, double[] mu_0, double lambda, double[][] psi, double v) {
    this.rng = rng;
    this.iwd = new InvertedWishartDistribution(rng, psi, v);
    this.lambda = lambda;
    this.mu_0 = mu_0;
  }

  public MultivariateNormalDistribution sample() {
    double[][] sigma = iwd.sample();
    double[][] psi = new double[sigma.length][];
    for (int i = 0; i < sigma.length; i++) {
      psi[i] = new double[sigma[i].length];
      for (int j = 0; j < sigma[i].length; j++) {
        psi[i][j] = sigma[i][j] / lambda;
      }
    }
    double[] mu = new MultivariateNormalDistribution(rng, mu_0, psi).sample();
    return new MultivariateNormalDistribution(rng, mu, sigma);
  }
}
