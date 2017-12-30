package edu.utdallas.hltri.func;

import java.io.Closeable;
import java.io.IOException;
import java.util.function.Function;

/**
 * Created by travis on 7/11/14.
 */
@FunctionalInterface
public interface CloseableFunction<I, O> extends Function<I, O>, Closeable {
  @Override
  default void close() throws IOException {
    // do nothing
  }
}
