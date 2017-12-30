package edu.utdallas.hltri.ml;

import java.io.Serializable;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * Created by ramon on 10/20/2016.
 */
public abstract class AbstractFeature<T> implements Feature<T>, Serializable {
  private static final long serialVersionUID = 1L;

  protected final String name;
  protected final T value;

  protected AbstractFeature(String name, T value) {
    this.name = name;
    this.value = value;
  }

  @Override
  public String toString() {
    return name() + ":" + Objects.toString(value());
  }

  @Override
  public String name() {
    return name;
  }

  @Override
  public T value() {
    return value;
  }

  @Override
  public Stream<? extends Feature<?>> stream() {
    return Stream.of(this);
  }
}
