package edu.utdallas.hltri.util;

import com.google.common.base.Throwables;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Created by travis on 7/15/14.
 * Based on edu.stanford.nlp.util.ErasureUtils from Stanford's CoreNLP
 */
public class Unsafe {
  private Unsafe() {
  }

  @FunctionalInterface
  public interface CheckedFunction<F,V>  {
    V apply(F from) throws Exception;
  }

  @FunctionalInterface
  public interface CheckedSupplier<V>  {
    V get() throws Exception;
  }

  @FunctionalInterface
  public interface CheckedConsumer<F> {
    void accept(F source) throws Exception;
  }

  @FunctionalInterface
  public interface CheckedRunnable  {
    void run() throws Exception;
  }

    public static <F, V> Function<F, V> function(CheckedFunction<F, V> f) {
      return (e -> {
        try {
          return f.apply(e);
        } catch (Exception x) {
          throw new RuntimeException(x);
        }
      });
    }

    public static <V> Supplier<V> supplier(CheckedSupplier<V> f) {
      return (() -> {
        try {
          return f.get();
        } catch (Exception x) {
          throw new RuntimeException(x);
        }
      });
    }

    public static <F> Consumer<F> consumer(CheckedConsumer<F> c) {
      return (e -> {
        try {
          c.accept(e);
        } catch (Exception x) {
          throw new RuntimeException(x);
        }
      });
    }


    public static Runnable runnable(CheckedRunnable f) {
      return (() -> {
        try {
          f.run();
        } catch (Exception x) {
          throw new RuntimeException(x);
        }
      });
    }

  /**
   * Casts an Object to a T
   *
   * @param <T>
   */
  @SuppressWarnings("unchecked")
  public static <T> T cast(Object o) {
    return (T) o;
  }

  /**
   * Makes an array based on clazz, but casts it to be of type T[]. This is a very
   * unsafe operation and should be used carefully. Namely, you should ensure that
   * clazz is a subtype of T, or that clazz is a supertype of T *and* that the array
   * will not escape the generic constant *and* that clazz is the same as the erasure
   * of T.
   *
   * @param <T>
   */
  @SuppressWarnings("unchecked")
  public static <T> T[] mkTArray(Class<?> clazz, int size) {
    return (T[]) (Array.newInstance(clazz, size));

  }

  @SuppressWarnings("unchecked")
  public static <T> T[][] mkT2DArray(Class<?> clazz, int[] dim) {
    if (dim.length != 2)
      throw new RuntimeException("dim should be an array of size 2.");
    return (T[][]) (Array.newInstance(clazz, dim));
  }

  @SuppressWarnings("unchecked")
  public static <T> List<T> tryToSort(Collection<T> collection) {
    List<T> result = new ArrayList<T>(collection);
    try {
      Collections.sort((List) result);
    } catch (ClassCastException e) {
      // unable to sort, just return the copy
    }
    return result;
  }
}
