package edu.utdallas.hltri.util;

import java.util.Collection;
import java.util.Optional;

/**
 * Created by travis on 8/13/14.
 */
public abstract class Optionals {
  public static <T extends CharSequence> Optional<T> fromCharSequence(T data) {
    if (data == null || data.length() == 0) {
      return Optional.empty();
    } else {
      return Optional.of(data);
    }
  }

  public static <T extends Collection<?>> Optional<T> fromCollection(T data) {
    if (data == null || data.isEmpty()) {
      return Optional.empty();
    } else {
      return Optional.of(data);
    }
  }
}
