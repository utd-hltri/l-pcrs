//package edu.utdallas.hltri.ml.clda;
//
//import DirichletDistribution;
//import MultinomialDistribution;
//import org.apache.commons.math3.distribution.IntegerDistribution;
//import org.apache.commons.math3.distribution.MultivariateNormalDistribution;
//import org.apache.commons.math3.distribution.MultivariateRealDistribution;
//import org.apache.commons.math3.random.MersenneTwister;
//import org.apache.commons.math3.random.RandomGenerator;
//
///**
// * Created by travis on 2/2/15.
// */
//public class DistributionalLda {
//  final static RandomGenerator rng = new MersenneTwister(1337);
//
//  /* Model Variables */
//  final double[] alpha;       // Dirichlet hyper-parameter for prior topic distribution
//  double[] theta_d[];         // Multinomial parameter for distribution of topics for document d
//  int      z_d_n[][];         // Assigned topic for word n of document d
//  final double w_d_n_i[][][];     // Value of i-th element in distribution vector for word n in document d
//
//  final double[]   mean_k[];        // Sum of i-th element for distribution vectors assigned to topic k
//  final double[][] covariance_k[];  // Covariance of i-th and j-th elements for distribution vectors assigned to topic k
//
//  double[]   mu_k[];              // Parameter for gaussian of topic_k
//  double[][] sigma_k[];           // Parameter for gaussian of topic_k
//
//  final double psi[][];       // Hyper-parameter for Inverse-Wishart distribution
//  final double v;             // Hyper-parameter for Inverse-Wishart distribution
//
//  /* Count variables */
//  final long n_k[];           // Number of documents with topic k
//
//
//  /* Model parameters */
//  final int K;
//  final int D;
//  final int N_d[];
//  final int I;
//
//
//  final MultivariateRealDistribution prior_w_d_n;
//
//  final double[]   mean;
//  final double[][] covariance;
//
//
//  final int N;
//
//  public DistributionalLda(final double[][][] w_d_n_i, int K, int D, int N_d[], int I, double[] alpha, double[][] psi, double v) {
//    this.K = K;
//    this.D = D;
//    {
//      final int R = N_d.length;
//      this.N_d = new int[N_d.length];
//      System.arraycopy(N_d, 0, this.N_d, 0, R);
//    }
//    this.theta_d = new double[K][D];
//
//    this.I = I;
//
//    this.alpha = new double[alpha.length];
//    System.arraycopy(alpha, 0, this.alpha, 0, alpha.length);
//
//
//    {
//      final int R = psi.length;
//      this.psi = new double[R][R];
//      for (int i = 0; i < R; i++) {
//        System.arraycopy(psi[i], 0, this.psi[i], 0, R);
//      }
//    }
//
//    this.w_d_n_i = new double[ D ][][];
//    for (int d = 0; d < D; d++) {
//      this.w_d_n_i[d] = new double[N_d[d]][I];
//      for (int n = 0; n < N_d[d]; n++) {
//        System.arraycopy(w_d_n_i[d][n], 0, this.w_d_n_i[d][n], 0, I);
//      }
//    }
//
//    this.mean = new double[ I ];
//    int N = 0;
//    for (int d = 0; d < D; d++) {
//      for (int n = 0; n < N_d[d]; n++) {
//        N += 1;
//        for (int i = 0; i < I; i++) {
//          mean[i] += w_d_n_i[d][n][i];
//        }
//      }
//    }
//    this.N = N;
//    for (int i = 0; i < I; i++) {
//      mean[i] /= N;
//    }
//
//    this.covariance = new double[I][I];
//    for (int d = 0; d < D; d++) {
//      for (int n = 0; n < N_d[d]; n++) {
//        for (int i = 0; i < I; i++) {
//          double iDev = w_d_n_i[d][n][i] - mean[i];
//          for (int j = 0; j < I; j++) {
//            double jDev = w_d_n_i[d][n][j] - mean[j];
//            covariance[i][j] += (iDev * jDev - 0.0) / N;
//          }
//        }
//      }
//    }
//
//    prior_w_d_n = new MultivariateNormalDistribution(rng, this.mean, this.covariance);
//
//    this.v = v;
//
//    this.mu_k = new double[K][I];
//    this.mean_k = new double[K][I];
//    this.sigma_k = new double[K][I][I];
//    this.covariance_k = new double[K][I][I];
//    this.n_k = new long [K];
//  }
//
//  public void initialize() {
//    // Initialize counts
//    for (int k = 0; k < K; k++) {
//      mu_k[k] = 0.0;      // Sample from NIW(psi, v)
//      sigma_k[k] = 0.0;   // Sample from NIW(psi, v)
//      n_k[k] = 0;
//    }
//
//    // Initialize assignment(s)
//    for (int d = 0; d < D; d++) {
//      final MultivariateRealDistribution p_theta_d = new DirichletDistribution(rng, alpha);
//      theta_d[d] = p_theta_d.sample();
//      for (int n = 0; n < N_d[d]; n++) {
//        final IntegerDistribution p_z_d_n_theta_d = new MultinomialDistribution(rng, theta_d[d]);
//        final int k = z_d_n[d][n] = p_z_d_n_theta_d.sample(); // Sample from Multinomial(theta_d);
//        n_k[k] += 1;
//
//        for (int i = 0; i < I; i++) {
//          mean_k[k][i] += w_d_n_i[d][n][i];
//        }
//      }
//    }
//
//    for (int i = 0; i < I; i++) {
//      for (int k = 0; k < K; k++) {
//        mean_k[k][i] /= n_k[k];
//      }
//    }
//
//    for (int d = 0; d < D; d++) {
//      for (int n = 0; n < N_d[d]; n++) {
//        final int k = z_d_n[d][n];
//        for (int i = 0; i < I; i++) {
//          double iDev = w_d_n_i[d][n][i] - mean[i];
//          for (int j = 0; j < I; j++) {
//            double jDev = w_d_n_i[d][n][j] - mean[j];
//            covariance_k[k][i][j] += (iDev * jDev - 0.0) / n_k[k];
//          }
//        }
//      }
//    }
//  }
//
//  private void increment(int d, int n) {
//    final int k = z_d_n[d][n];
//    n_k[k] ++;
//    for (int i = 0; i < I; i++) {
//      double iDev_k = w_d_n_i[d][n][i] - mean_k[k][i];
//      double iDev = w_d_n_i[d][n][i] - mean[i];
//      for (int j = 0; j < I; j++) {
//        double jDev_k = w_d_n_i[d][n][i] - mean_k[k][j];
//        double jDev = w_d_n_i[d][n][i] - mean[j];
//        covariance_k[k][i][j] += (n_k[k] - 1) / n_k[k] * iDev_k * jDev_k;
//        covariance[i][j] += (N - 1) / N * iDev * jDev;
//      }
//    }
//    for (int i = 0; i < I; i++) {
//      mean_k[k][i] += (w_d_n_i[d][n][i] - mean_k[k][i]) / n_k[k];
//      mean[i] += (w_d_n_i[d][n][i] - mean[i]) / N;
//    }
//  }
//
//  private void decrement(int d, int n) {
//    final int k = z_d_n[d][n];
//    n_k[k]--;
//    for (int i = 0; i < I; i++) {
//      double iDev_k = w_d_n_i[d][n][i] - mean_k[k][i];
//      double iDev = w_d_n_i[d][n][i] - mean[i];
//      for (int j = 0; j < I; j++) {
//        double jDev_k = w_d_n_i[d][n][i] - mean_k[k][j];
//        double jDev = w_d_n_i[d][n][i] - mean[j];
//        covariance_k[k][i][j] -= (n_k[k] - 1) / n_k[k] * iDev_k * jDev_k;
//        covariance[i][j] -= (N - 1) / N * iDev * jDev;
//      }
//    }
//    for (int i = 0; i < I; i++) {
//      mean_k[k][i] -= (w_d_n_i[d][n][i] - mean_k[k][i]) / n_k[k];
//      mean[i] -= (w_d_n_i[d][n][i] - mean[i]) / N;
//    }
//  }
//
//  public void fit() {
//    // Re-initialize
//    initialize();
//
//    // Gibbs sampling!
//    for (int t = 1; t < T; t++) {
//      for (int d = 0; d < D; d++) {
//        for (int n = 0; n < N_d[d]; n++) {
//          decrement(d, n);
//
//          // Sample new topic k for d
//          double[] likelihood_k = new double[K];
//          for (int k = 0; k < K; k++) {
//            final MultivariateRealDistribution p_k_w = new MultivariateNormalDistribution(rng, mu_k[k], sigma_k[k]);
//            likelihood_k[k] = p_k_w.density(w_d_n_i[d][n]) * theta_d[d][k] / prior_w_d_n.density(w_d_n_i[d][n]);
//          }
//          z_d_n[d][n] = new MultinomialDistribution(rng, likelihood_k).sample();
//
//          increment(d, n);
//        }
//      }
//
//      mu_k =
//
//
//    }
//  }
//
//
//}
