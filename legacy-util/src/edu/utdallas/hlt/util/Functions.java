package edu.utdallas.hlt.util;

import com.google.common.base.Function;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;

/**
 *
 * @author bryan
 */
public class Functions {

  public static <F,T> Function<F,T> cached(final  Function<F,T> func) {
    return CacheBuilder.newBuilder().build(new CacheLoader<F, T>() {
      @Override
      public T load(F k) throws Exception {
        return func.apply(k);
      }
    });
  }
}
