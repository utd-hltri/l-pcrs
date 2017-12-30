package edu.utdallas.hltri.util;

import com.google.common.base.Suppliers;
import java.util.function.Supplier;

/**
 * Created by travis on 1/26/16.
 *
 * The semantics of Lazy.lazily(supplier) really don't make any sense.
 * Better to just call Suppliers.memoize() from Guava
 */
@Deprecated
public class Lazy {
  public static <Z> Supplier<Z> lazily(Supplier<Z> supplier) {
    return Suppliers.memoize(supplier::get);
  }
}
