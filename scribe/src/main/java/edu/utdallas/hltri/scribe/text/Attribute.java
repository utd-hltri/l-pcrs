package edu.utdallas.hltri.scribe.text;

import com.google.common.reflect.TypeToken;
import edu.utdallas.hltri.util.Unsafe;

import java.io.Serializable;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * Created by travis on 8/14/14.
 */
public class Attribute<S, T> {
  public final String   name;
  public final Class<T> type;

  protected Attribute(String name, Class<T> type) {
    this.name = name;
    this.type = type;
  }

  public static <S, T> Attribute<S, T> typed(String name, Class<T> type) {
    return new Attribute<>(name, type);
  }

  public static <S, T> Attribute<S, T> inferred(String name) {
    TypeToken<T> token = new TypeToken<T>(Attribute.class) {
    };
    return new Attribute<>(name, Unsafe.cast(token.getRawType()));
  }

  @Override
  public String toString() {
    return name + ": " + type.getName();
  }
}

