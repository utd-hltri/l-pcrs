package edu.utdallas.hltri.ml;

import edu.utdallas.hltri.logging.Logger;
import java.util.Arrays;
import java.util.Collection;

/**
 * Created with IntelliJ IDEA.
 * User: bryan
 * Date: 12/16/12
 * Time: 12:31 AM
 */
public class NumericFeature extends AbstractFeature<Number> {
  private static final Logger log = Logger.get(NumericFeature.class);

  NumericFeature(String name, Number value) {
    super(name, value);
  }

  @Override
  public NumericFeature toNumericFeature() {
    return this;
  }

  @Override
  public StringFeature toStringFeature() {
    return Feature.stringFeature(name, value.toString());
  }

  @Override
  public BinaryFeature toBinaryFeature() {
    return Feature.binaryFeature(name + "=" + value.toString(), true);
  }

  /**
   * Calculates sum, min, max, average, variance
   * @param collectionFeature Feature containing list to gather statistics about
   * @return Collection of features of the form (collectionFeature.name.statistic -> statistic)
   */
  public static Collection<Feature<Number>> getStatistics(
      Feature<? extends Collection<Number>> collectionFeature) {
    final Collection<? extends Number> numbers = collectionFeature.value();
//    log.trace("Calculating statistics for {}: {}", numbers, collectionFeature.name());

    double mean = 0, sum = 0, min = 0, max = 0, var = 0;
    if (!numbers.isEmpty()) {
      min = Double.POSITIVE_INFINITY;
      max = Double.NEGATIVE_INFINITY;

      final double K = numbers.iterator().next().doubleValue();

      double n = 0;
      double sumSq = 0;
      for (Number number : numbers) {
        final double x = number.doubleValue();
        n += 1;
        sum += x - K;
        sumSq += (x - K) * (x - K);
        if (x < min) {
          min = x;
        }
        if (x > max) {
          max = x;
        }
      }
      var = (sumSq - (sum * sum)/ n) / n;
      mean = sum / n + K;
    }

    final String name = collectionFeature.name();
//    log.trace("{}: AVG={} SUM={} MIN={} MAX={} VAR={}", name, mean, sum, min, max, var);

    return Arrays.asList(
        Feature.numericFeature(name + ".avg", mean),
        Feature.numericFeature(name + ".sum", sum),
        Feature.numericFeature(name + ".min", min),
        Feature.numericFeature(name + ".max", max),
        Feature.numericFeature(name + ".var", var)
    );
  }
}
