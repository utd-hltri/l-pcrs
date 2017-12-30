package edu.utdallas.hltri.ml.math;

import org.apache.commons.math3.distribution.EnumeratedIntegerDistribution;
import org.apache.commons.math3.exception.*;
import org.apache.commons.math3.random.RandomGenerator;

/**
 * Created by travis on 2/2/15.
 */
public class MultinomialDistribution extends EnumeratedIntegerDistribution {
  /**
   * Create a discrete distribution using the given random number generator
   * and probability mass function definition.
   *
   * @param rng           random number generator.
   * @param probabilities array of probabilities.
   * @throws org.apache.commons.math3.exception.DimensionMismatchException if
   *                                                                       {@code singletons.length != probabilities.length}
   * @throws org.apache.commons.math3.exception.NotPositiveException       if any of the probabilities are negative.
   * @throws org.apache.commons.math3.exception.NotFiniteNumberException   if any of the probabilities are infinite.
   * @throws org.apache.commons.math3.exception.NotANumberException        if any of the probabilities are NaN.
   * @throws org.apache.commons.math3.exception.MathArithmeticException    all of the probabilities are 0.
   */
  public MultinomialDistribution(RandomGenerator rng, double[] probabilities) throws DimensionMismatchException, NotPositiveException, MathArithmeticException, NotFiniteNumberException, NotANumberException {
    super(rng, enumerate(probabilities), probabilities);
  }

  private static int[] enumerate(double[] probabilities) {
    int[] labels = new int[probabilities.length];
    for (int i = 0; i < labels.length; i++) {
      labels[i] = i + 1;
    }
    return labels;
  }
}
