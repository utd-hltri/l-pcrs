package edu.utdallas.hltri.func;

import java.io.IOException;
import java.util.Objects;

import javax.annotation.Nullable;

import edu.utdallas.hltri.logging.Logger;

/**
 * Created by travis on 7/22/14.
 */
public abstract class Filters {
  private static final Logger log = Logger.get(Filters.class);

  private Filters() {
  }

  public static <I, O> CloseablePredicate<I> isNot(final CloseableFunction<I, O> extractor, final O value) {
    return new CloseablePredicate<I>() {
      @Override public boolean test(@Nullable I input) {
        final O output = extractor.apply(input);
        if (Objects.equals(value, output)) {
          log.trace("Extracted {} == {}", output, value);
          return false;
        }
        return true;
      }

      @Override public void close() { }

      @Override public String toString() {
        return "IsNot(" + extractor.getClass().getSimpleName() + ", " + value + ")";
      }
    };
  }

  public static <I, O> CloseablePredicate<I> is(final CloseableFunction<I, O> extractor, final O value) {
    return new CloseablePredicate<I>() {
      @Override public boolean test(@Nullable I input) {
        final O output = extractor.apply(input);
        if (!Objects.equals(value, output)) {
          log.trace("Extracted {} != {}", output, value);
          return false;
        }
        return true;
      }

      @Override public void close() { }

      @Override public String toString() {
        return "Is(" + extractor.getClass().getSimpleName() + ", " + value + ")";
      }
    };
  }

  @SafeVarargs public static <I> CloseablePredicate<I> and(final CloseablePredicate<? super I>... filters) {
    return new CloseablePredicate<I>() {
      @Override public boolean test(@Nullable I input) {
        for (CloseablePredicate<? super I> filter : filters) {
          if (!filter.test(input)) {
            return false;
          }
        }
        return true;
      }

      //TODO: ramon added the try catch block
      @Override public void close() {
        for (CloseablePredicate<? super I> filter: filters) {
          try {
            filter.close();
          } catch (IOException e) {
            throw new RuntimeException(e);
          }
        }
      }

      @Override public String toString() {
        final StringBuilder sb = new StringBuilder("Joint(");
        boolean first = true;
        for (CloseablePredicate<? super I> filter : filters) {
          if (first) { first = false; }
          else { sb.append(' '); }
          sb.append(filter.toString());
        }
        sb.append(')');
        return sb.toString();
      }
    };
  }
}
