package edu.utdallas.hltri.ml;

import edu.utdallas.hltri.logging.Logger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.stream.Stream;
import scala.math.Numeric;

/**
 * This represents a feature name : feature value pair.  Multiple Features can exist with the same name.
 * User: bryan
 * Date: 12/15/12
 * Time: 11:30 PM
 */
public interface Feature<T> {
  static final Logger log = Logger.get(Feature.class);

  String name();
  T value();

  Stream<? extends Feature<?>> stream();

  NumericFeature toNumericFeature();
  StringFeature toStringFeature();
  BinaryFeature toBinaryFeature();

  static <T> Feature<T> simple(String name, T value) {
    return new SimpleFeature<T>(name, value);
  }

  static BinaryFeature binaryFeature(String name, boolean value) {
    return new BinaryFeature(name, value);
  }

  static NumericFeature numericFeature(String name, int value) {
    return new NumericFeature(name, (double)value);
  }

  static NumericFeature numericFeature(String name, double value) {
    return new NumericFeature(name, value);
  }

  static NumericFeature numericFeature(String name, float value) {
    return new NumericFeature(name, (double)value);
  }

  static NumericFeature numericFeature(String name, short value) {
    return new NumericFeature(name, (double)value);
  }

  static StringFeature stringFeature(String name, String value) {
    return new StringFeature(name, value);
  }

  @Deprecated
  static StringFeature categorical(String name, Supplier<String> value) {
    return new StringFeature(name, value.get());
  }

  static <T> Feature<Collection<T>> flatten(Collection<? extends Feature<? extends T>> features) {
    Collection<T> values = new ArrayList<>(features.size());
    String name = null;
    for (Feature<? extends T> feature : features) {
      values.add(feature.value());
      if (name != null) {
        if (!Objects.equals(name, feature.name())) {
          log.warn("Flattening features with mixed names: {}, and {}", name, feature.name());
        }
      } else {
        name = feature.name();
      }
    }
    return new MultiFeature<>(name, values);
  }
}
