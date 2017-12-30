package edu.utdallas.hltri.ml;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import edu.utdallas.hltri.logging.Logger;

/**
 * Feature that contains multiple values
 */
@SuppressWarnings({"WeakerAccess", "unused"})
public class MultiFeature<K> implements Feature<Collection<K>> {
  private static final Logger log = Logger.get(MultiFeature.class);

  private final String name;
  private final Collection<K> values;

  public MultiFeature(String name, Collection<K> values) {
    this.name = name;
    this.values = values;
  }

  /**
   * Flatten a collection of features into a single multi-feature
   * NOTE: every feature must have the same name!
   * @param features collection of features to flatten -- all features must have the same name()
   * @param <K> type of feature
   * @return resultant MultiFeature
   */
  public static <K> MultiFeature<K> flatten(Collection<? extends Feature<K>> features) {
    assert !features.isEmpty() : "Cannot flatten empty collection";
    final String name = features.iterator().next().name();
    final Collection<K> values = new ArrayList<>();
    for (Feature<? extends K> feature : features) {
      assert feature.name().equals(name) : "Cannot flatten features with different names";
      values.add(feature.value());
    }
    log.trace("Generated {}={} by flattening {}", name, values, features);
    return new MultiFeature<>(name, values);
  }

  /**
   * Flatten a collection of features into a single multi-feature with the given name
   * Features in the given collection can have different names
   * @param features collection of features to flatten
   * @param name name of resultant multi-feature
   * @param <K> type of feature
   * @return resultant MultiFeature
   */
  public static <K> MultiFeature<K> flattenAs(Collection<? extends Feature<K>> features, String name) {
    assert !features.isEmpty() : "Cannot flatten empty collection";
    final Collection<K> values = features.stream().map(Feature::value).collect(Collectors.toList());
    return new MultiFeature<>(name, values);
  }

  public void addValue(K value) {
    this.values.add(value);
  }

  @Override
  public String name() {
    return name;
  }

  @Override
  public Collection<K> value() {
    return values;
  }

  @Override
  public Stream<? extends Feature<?>> stream() {
    return values.stream().map(v -> new AbstractFeature<K>(name, v) {
      @Override
      public NumericFeature toNumericFeature() {
        return (value instanceof Number) ? Feature.numericFeature(name, ((Number) value).doubleValue())
        : Feature.numericFeature(name + "=" + Objects.toString(value), 1.0);
      }

      @Override
      public StringFeature toStringFeature() {
        return Feature.stringFeature(name, Objects.toString(value));
      }

      @Override
      public BinaryFeature toBinaryFeature() {
        return Feature.binaryFeature(name + "=" + Objects.toString(value), true);
      }
    });
  }

  @Override
  public NumericFeature toNumericFeature() {
    throw new UnsupportedOperationException("MultiFeatures must be flattened using .stream().");
  }

  @Override
  public StringFeature toStringFeature() {
    throw new UnsupportedOperationException("MultiFeatures must be flattened using .stream().");
  }

  @Override
  public BinaryFeature toBinaryFeature() {
    throw new UnsupportedOperationException("MultiFeatures must be flattened using .stream().");
  }
}
