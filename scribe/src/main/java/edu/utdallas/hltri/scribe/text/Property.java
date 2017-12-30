package edu.utdallas.hltri.scribe.text;

import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * Created by travis on 8/15/14.
 */
public class Property<S, T> {
  private final Function<S, T>   getter;
  private final BiConsumer<S, T> setter;

  protected Property(Function<S, T> getter, BiConsumer<S, T> setter) {
    this.getter = getter;
    this.setter = setter;
  }

  public static <S, T> Property<S, T> create(Function<S, T> getter, BiConsumer<S, T> setter) {
    return new Property<S, T>(getter, setter);
  }

  public static <S, T> Property<S, T> readOnly(Function<S, T> getter) {
    return new Property<S, T>(getter, (s, t) -> { throw new UnsupportedOperationException("Immutable property"); });
  }

  public static <S, T> Property<S, T> attributeProperty(String name, Class<T> type, Function<S, T> getter, BiConsumer<S, T> setter) {
    return new Property<S, T>(getter, setter);
  }


  public void set(S source, T value) {
    setter.accept(source, value);
  }

  public T get(S source) {
    return getter.apply(source);
  }
}
