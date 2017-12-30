package edu.utdallas.hltri.func;

import java.io.Closeable;
import java.io.IOException;
import java.util.function.Consumer;

@FunctionalInterface
public interface CloseableConsumer<T> extends Consumer<T>, Closeable {

  @Override
  default void close() throws IOException {
    /* do nothing */
  }
}
