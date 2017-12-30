package edu.utdallas.hltri.io;

/**
 * Extension of {@link AutoCloseable} where {@link #close()} does not throw any exception
 * Based on https://github.com/medallia/Word2VecJava/blob/master/src/main/java/com/medallia/word2vec/util/AC.java
 */
public interface AC extends AutoCloseable {
  @Override default void close() {

  }
}
