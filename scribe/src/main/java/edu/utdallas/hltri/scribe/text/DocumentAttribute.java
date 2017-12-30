package edu.utdallas.hltri.scribe.text;

import com.google.common.reflect.TypeToken;
import edu.utdallas.hltri.util.Unsafe;

import java.io.Serializable;
import java.util.function.BiConsumer;

/**
 * Created by travis on 8/14/14.
 */
public class DocumentAttribute<S, T> implements Serializable {
  protected final static long serialVersionUID = 1L;
  public final String   name;
  public final Class<T> type;
  public final BiConsumer<Document<? extends BaseDocument>, T> setter;

  protected DocumentAttribute(String name, Class<T> type) {
    this(name, type, (x, y) -> {});
  }

  protected DocumentAttribute(String name, Class<T> type, BiConsumer<Document<? extends BaseDocument>, T> setter) {
    this.name = name;
    this.type = type;
    this.setter = setter;
  }

  public static <S, T> DocumentAttribute<S, T> typed(String name, Class<T> type) {
    return new DocumentAttribute<>(name, type);
  }

  public static <S, T> DocumentAttribute<S, T> inferred(String name) {
    TypeToken<T> token = new TypeToken<T>(DocumentAttribute.class) {
    };
    return new DocumentAttribute<>(name, Unsafe.cast(token.getRawType()));
  }

  public static <S, T> DocumentAttribute<S, T> specialized(String name, Class<T> type, BiConsumer<Document<? extends BaseDocument>, T> setter) {
    return new DocumentAttribute<S, T>(name, type, setter);
  }

  @Override
  public String toString() {
    return name + ": " + type.getName();
  }
}

