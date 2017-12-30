package edu.utdallas.hltri;

import edu.utdallas.hltri.logging.Logger;

import java.util.Arrays;

/**
 * Created by travis on 9/9/14.
 */
public class Assert {

  public static void nonNull(Object o) {
    nonNull(o, "{} was null", o);
  }
  public static void nonNull(Object o, String message, Object... args) {
    if (null == o) {
      throw new IllegalArgumentException(Logger.format(message, args));
    }
  }

  public static void nonZero(double value) {
    nonZero(value, "{} was zero", value);
  }

  public static void nonZero(double value, String message, Object... args) {
    if (Double.compare(value, 0d) == 0) {
      throw new IllegalArgumentException(Logger.format(message, args));
    }
  }

  public static void nonNegative(double value) {
    nonNegative(value, "{} was negative", value);
  }

  public static void nonNegative(double value, String message, Object... args) {
    if (value < 0d) {
      throw new IllegalArgumentException(Logger.format(message, args));
    }
  }


  public static void positiveDefinite(int[] ints) {
    positiveDefinite(ints, "{} had value {}", Arrays.toString(ints));
  }

  public static void positiveDefinite(double[] doubles) {
    positiveDefinite(doubles, "{} had value {}", Arrays.toString(doubles));
  }


  public static void positiveDefinite(int[] ints, String message, Object... args) {
    Assert.nonNull(ints);
    for (int i = 0; i < ints.length; i++) {
      if (ints[i] < 0) {
        final Object[] nargs = Arrays.copyOf(args, args.length + 1);
        nargs[nargs.length - 1] = ints[i];
        throw new IllegalArgumentException(Logger.format(message, nargs));
      }
    }
  }

  public static void positiveDefinite(double[] doubles, String message, Object... args) {
    Assert.nonNull(doubles);
    for (int i = 0; i < doubles.length; i++) {
      if (Double.isNaN(doubles[i]) || doubles[i] < 0d) {
        final Object[] nargs = Arrays.copyOf(args, args.length + 1);
        nargs[nargs.length - 1] = doubles[i];
        throw new IllegalArgumentException(Logger.format(message, nargs));
      }
    }
  }
}
