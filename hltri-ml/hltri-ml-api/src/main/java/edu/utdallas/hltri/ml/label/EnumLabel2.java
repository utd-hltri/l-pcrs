package edu.utdallas.hltri.ml.label;

import java.io.Serializable;
import java.lang.reflect.Array;
import java.util.EnumSet;
import java.util.Iterator;

/**
 * Created by rmm120030 on 4/20/17.
 */
public class EnumLabel2<T extends Enum<T>> implements Serializable, Label {
  private final int ordinal, numLabels;
  private final String name;

  private EnumLabel2(int ordinal, String name, int numLabels) {
    this.ordinal = ordinal;
    this.name = name;
    this.numLabels = numLabels;
  }

  public static <T extends Enum<T>> EnumLabel2<T>[] create(Class<T> clazz) {
    final EnumSet<T> eset = EnumSet.allOf(clazz);
    @SuppressWarnings("unchecked")
    final EnumLabel2<T>[] arr = (EnumLabel2<T>[]) new Object[eset.size()];
    final Iterator<T> it = eset.iterator();
    for (int i = 0; it.hasNext(); i++) {
      T t = it.next();
      arr[i] = new EnumLabel2<T>(t.ordinal(), t.name(), eset.size());
    }

    return arr;
  }

  @Override
  public int asInt() {
    return ordinal();
  }

  public int ordinal() {
    return ordinal;
  }

  @Override
  public String asString() {
    return name();
  }

  public String name() {
    return name;
  }

  public int numLabels() {
    return numLabels;
  }
}
