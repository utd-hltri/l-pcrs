package edu.utdallas.hltri.ml;

import java.util.Objects;
import java.util.stream.Stream;

/**
 * String feature implementation
 */
@SuppressWarnings("WeakerAccess")
public class SimpleFeature<O> implements Feature<O> {
  private final String name;
  private final O value;

  SimpleFeature(String name, O value) {
    this.name = name;
    this.value = value;
  }

  @Override
  public String name() {
    return name;
  }

  @Override
  public O value() {
    return value;
  }

  @Override
  public Stream<? extends Feature<?>> stream() {
    return Stream.of(this);
  }

  @Override
  public NumericFeature toNumericFeature() {
    if (value instanceof Number) {
      return Feature.numericFeature(name, ((Number) value).doubleValue());
    } else {
      return Feature.numericFeature(name + '=' + Objects.toString(value), 1d);
    }
  }

  @Override
  public StringFeature toStringFeature() {
    return new StringFeature(name, Objects.toString(value));
  }

  @Override
  public BinaryFeature toBinaryFeature() {
    return new BinaryFeature(name + '=' + Objects.toString(value), true);
  }
}
