package edu.utdallas.hltri.func;

import java.io.Closeable;
import java.io.IOException;
import java.util.function.Predicate;

/**
 * Created by travis on 7/11/14.
 */
public interface CloseablePredicate<Source> extends Predicate<Source>, Closeable {
  @Override
  default void close() throws IOException {
    // do nothing
  }
}
