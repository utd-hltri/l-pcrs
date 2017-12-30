package edu.utdallas.hltri.math;

import edu.utdallas.hltri.Assert;
import edu.utdallas.hltri.logging.Logger;

import java.util.Arrays;
import java.util.Random;

/**
 * Created by travis on 8/7/14.
 */
public final class Statistics {
  private static final Logger log = Logger.get(Statistics.class);

  private Statistics() { /* */ }

  public final static double LOG_ONE = 0d;
  public final static double LOG_ZERO = Double.NEGATIVE_INFINITY;

  public static double logAdd(double logX, double logY) {
    // 1. make X the max
    if (logY > logX) {
      double temp = logX;
      logX = logY;
      logY = temp;
    }

    // 2. now X is bigger
    if (logX == Double.NEGATIVE_INFINITY) {
      return logX;
    }

    // 3. otherwise use some nice algebra to stay in the log domain
    //    (except for negDiff)
    double negDiff = logY - logX;
    return logX + Math.log1p(Math.exp(negDiff));
  }

  public static double logAddFast(double logX, double logY, double epsilon) {
    // 1. make X the max
    if (logY > logX) {
      double temp = logX;
      logX = logY;
      logY = temp;
    }

    // 2. now X is bigger
    if (logX == Double.NEGATIVE_INFINITY) {
      return logX;
    }

    // 3. how far "down" (think decibels) is logY from logX?
    //    if it's really small (20 orders of magnitude smaller), then ignore
    double negDiff = logY - logX;
    if (negDiff < epsilon) {
      return logX;
    }

    // 4. otherwise use some nice algebra to stay in the log domain
    //    (except for negDiff)
    return logX + Math.log1p(Math.exp(negDiff));
  }


  public static double logSubtract(double logX, double logY) {
    if (logX < logY) {
      log.error("Log-space subtraction of {} from {} requires computing the log of a negative number!", logY, logX);
      return Double.NaN;
    } else if (logX == logY) {
      return LOG_ZERO;
    }
      // error!! computing the log of a negative number

    if (logY == Double.NEGATIVE_INFINITY) {
      return logX;
    }

    double negDiff = logY - logX;
//    if (negDiff < -20) {
//      return logX;
//    }

    return logX + Math.log1p(-Math.exp(negDiff));
  }

  public static double logSubtractFast(double logX, double logY, double epsilon) {
    if (logX < logY) {
      log.error("Log-space subtraction of {} from {} requires computing the log of a negative number!", logY, logX);
      return Double.NaN;
    } else if (logX == logY) {
      return LOG_ZERO;
    }

    if (logY == Double.NEGATIVE_INFINITY)
      return logX;

    double negDiff = logY - logX;
    if (negDiff < -epsilon) {
      return logX;
    }

    return logX + Math.log1p(-Math.exp(negDiff));
  }

  public static double getNormalProbability(double x, double mean, double variance) {
    final double deviation = x - mean;
    final double p = Math.exp(-(deviation * deviation) / (2 * variance)) / Math.sqrt(2 * variance * Math.PI);
    log.info("Normal prob of {} given mean={} and variance={} is {}", x, mean, variance, p);
    return p;
  }

  // Based on "Gaussian" smoothing from Yahoo's "Graphical Models for the Internet" presentation
  // http://alex.smola.org/drafts/www11-1.pdf
  public static double getSmoothedNormalProbability(double x, double mean, double variance, int n, int smoother) {
    final double smoothedMean = mean * n / ((double) n + smoother);
    final double smoothedVariance = variance * n / ((double) n + smoother) + (smoother) / (n + smoother) - (smoothedMean * smoothedMean);
    final double p = getNormalProbability(x, smoothedMean, smoothedVariance);
    log.info("Smoothed normal prob of {} given mean={} and variance={} is {}", x, mean, variance, p);
    return p;
  }



  public static void validate(double value) {
    Assert.nonNegative(value);
    if (value > 1d) {
      throw new IllegalArgumentException(value + " was not a valid probability");
    }
  }

  public static void validate(double... values) {
    Assert.nonNull(values);
    Assert.positiveDefinite(values);
    double sum = sum(values);
    if (Math.abs(sum - 1d) > 1E-7) {
      throw new IllegalArgumentException(Arrays.toString(values) + " was not a valid probability distribution");
    }
  }

  public static int sum(int[] array) {
    Assert.nonNull(array);
    int sum = 0;
    for (int i = 0; i < array.length; i++) {
      sum += array[i];
    }
    return sum;
  }

  public static double sum(double[] array) {
    Assert.nonNull(array);
    double sum = 0.0;
    for (int i = 0; i < array.length; i++) {
      sum += array[i];
    }
    return sum;
  }

  public static void inormalize(double[] array) {
//    Assert.positiveDefinite(array);
    double sum = sum(array);
    Assert.nonZero(sum);
    for (int i = 0; i < array.length; i++) {
      array[i] /= sum;
      validate(array[i]);
    }
  }

  public static double[] normalizeFromLogs(double[] array) {
    double sum = array[0];
    for (int i = 1; i < array.length; i++) {
      sum = Statistics.logAdd(sum, array[i]);
    }

    double[] normalized = new double[array.length];
    for (int i = 0; i < array.length; i++) {
      normalized[i] = Math.exp(array[i] - sum);
    }

    return normalized;
  }

  public static double[] normalize(double[] array) {
    Assert.positiveDefinite(array);
    double sum = sum(array);
    Assert.nonZero(sum);
    double[] normalized = new double[array.length];
    for (int i = 0; i < array.length; i++) {
      normalized[i] = array[i] / sum;
      validate(normalized[i]);
    }
    return normalized;
  }

  public static double[] normalize(int[] array) {
//    Assert.positiveDefinite(array);
    int sum = sum(array);
    Assert.nonZero(sum);
    double[] normalized = new double[ array.length ];
    for (int i = 0; i < array.length; i++) {
      normalized[i] = array[i] / (double) sum;
      validate(normalized[i]);
    }
    return normalized;
  }

  public static int sampleIndexUnnormalized(final Random random, final double[] values, final double Z) {
    final double key = random.nextDouble() * Z;
    double sum = 0.0;
    int i;
    for (i = 0; i < values.length; i++) {
      sum += values[i];
      if (sum >= key) break;
    }
    assert (sum >= key) : "failed to sample from " + Arrays.toString(values) + " (sum was " + sum + "; key was " + key + ")";
    return i;
  }

  public static int sampleIndexMultinomial(final Random random, final double[] values) {
    validate(values);
    final double key = random.nextDouble();
    double sum = 0.0;
    int i;
    for (i = 0; i < values.length; i++) {
      sum += values[i];
      if (sum >= key) break;
    }
    assert (sum >= key) : "failed to sample from " + Arrays.toString(values) + " (sum was " + sum + "; key was " + key + ")";
    return i;
  }

  public static Number min(Iterable<? extends Number> it) {
    double min = Double.POSITIVE_INFINITY;
    double val;
    for (Number item: it) {
      val = item.doubleValue();
      if (val < min) { min = val; }
    }
    return min;
  }


  public static double min(double... it) {
    double min = Double.POSITIVE_INFINITY;
    for (double item : it) {
      if (item < min) {
        min = item;
      }
    }
    return min;
  }

  public static int maxIndex(double[] values) {
    double max = Double.NEGATIVE_INFINITY;
    int maxIndex = -1;
    for (int i = 0; i < values.length; i++) {
      if (values[i] > max) {
        max = values[i];
        maxIndex = i;
      }
    }
    assert maxIndex > -1 : "Failed to get arg-max for " + Arrays.toString(values);
    return maxIndex;
  }

  public static int maxIndex(int[] values) {
    int max = Integer.MIN_VALUE;
    int maxIndex = -1;
    for (int i = 0; i < values.length; i++) {
      if (values[i] > max) {
        max = values[i];
        maxIndex = i;
      }
    }
    assert maxIndex > -1 : "Failed to get arg-max for " + Arrays.toString(values);
    return maxIndex;
  }

  public static double max(double... values) {
    double max = Double.NEGATIVE_INFINITY;
    for (int i = 0; i < values.length; i++) {
      if (values[i] > max) {
        max = values[i];
      }
    }
    return max;
  }

  public static Number max(Iterable<? extends Number> it) {
    double max = Double.NEGATIVE_INFINITY;
    double val;
    for (Number item : it) {
      val = item.doubleValue();
      if (val > max) {
        max = val;
      }
    }
    return max;
  }

  public static double mean(double... array) {
    return sum(array) / array.length;
  }


  public static Number mean(Iterable<? extends Number> it) {
    double sum = 0.0;
    int n = 0;
    for (Number item : it) {
      sum += item.doubleValue();
    }
    return sum / n;
  }

  public static Number variance (Iterable<? extends Number> it) {
    double sum = 0.0, sumSq = 0.0, value;
    int n = 0;
    for (Number item : it) {
      value = item.doubleValue();
      sum += value;
      sumSq += value * value;
    }
    double mean = sum / n;
    return sumSq / n - mean * mean;
  }

  public static Number sum(Iterable<? extends Number> it) {
    double sum = 0.0;
    for (Number item : it) {
      sum += item.doubleValue();
    }
    return sum;
  }
}
