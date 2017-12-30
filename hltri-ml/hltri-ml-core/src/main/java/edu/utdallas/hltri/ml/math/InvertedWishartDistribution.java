package edu.utdallas.hltri.ml.math;

import org.apache.commons.math3.distribution.ChiSquaredDistribution;
import org.apache.commons.math3.distribution.NormalDistribution;
import org.apache.commons.math3.distribution.RealDistribution;
import org.apache.commons.math3.linear.*;
import org.apache.commons.math3.random.RandomGenerator;

/**
 * Created by travis on 2/3/15.
 */
public class InvertedWishartDistribution {
  final RandomGenerator rng;
  final double     v;
  final RealMatrix psi;
  final RealDistribution randn;
  final int D;

  /**
   * Based on python code from: http://www.mit.edu/~mattjj/released-code/hsmm/stats_util.py
   * @param rng
   * @param psi
   * @param v
   */
  public InvertedWishartDistribution(RandomGenerator rng, double[][] psi, double v) {
    this.D = psi.length;
    this.psi = new Array2DRowRealMatrix(psi);
    this.v = v;
    this.rng = rng;
    this.randn = new NormalDistribution(rng, 0.0, 1);
  }

  public double[][] sample() {
    RealMatrix x;
    if (v <= 81 + D && (v == (int) v)) {
      double[][] xArray = new double[(int) v][];
      for (int d = 0; d < xArray.length; d++) {
        xArray[d] = randn.sample(D);
      }
      x = new BlockRealMatrix(xArray);
    } else {
      double[] t = new double[D];
      for (int i = 0; i < D; i++) {
        t[i] = Math.sqrt(new ChiSquaredDistribution(rng, v - i).getNumericalVariance());
      }
      x = new DiagonalMatrix(t);
      for (int i = 0; i < D; i++) {
        for (int j = i + 1; j < D; j++) {
          x.setEntry(i, j, randn.sample());
        }
      }
    }
    final QRDecomposition solver = new QRDecomposition(x);
    RealMatrix T = solver.getSolver().solve(new CholeskyDecomposition(psi).getL());
    return T.multiply(T.transpose()).getData();
  }
}
