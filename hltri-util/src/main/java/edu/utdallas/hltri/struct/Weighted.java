package edu.utdallas.hltri.struct;

import com.google.common.collect.Lists;

import java.io.Serializable;
import java.text.DecimalFormat;
import java.util.Collection;
import java.util.function.Function;
import java.util.function.Predicate;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import edu.utdallas.hltri.logging.Logger;
import edu.utdallas.hltri.util.Unsafe;

/**
 *
 * @author travis
 */
@SuppressWarnings("unused")
public class Weighted<T> implements Serializable, Comparable<Weighted<T>> {
  private static final long   serialVersionUID = 1L;
  private static final Logger log              = Logger.get(Weighted.class);
  private static final DecimalFormat formatter = new DecimalFormat("0.##");

  public final double weight;
  public final T value;

  @Override
  public int compareTo(@Nonnull Weighted<T> o) {
    final int c = Double.compare(weight, o.weight);
    if (c == 0) {
      if (value instanceof Comparable) {
        //noinspection unchecked
        return ((Comparable<T>) value).compareTo(o.value);
      } else {
        log.warn("Comparing non-comparable values");
        return 1;
      }
    }
    else return c;
  }

  protected Weighted(double weight, T value) {
    this.weight = weight;
    this.value = value;
  }

  public static <O> Weighted<O> create(O value, double weight) {
    return new Weighted<>(weight, value);
  }

  public static <O> Weighted<O> create(double weight, O value) {
    return new Weighted<>(weight, value);
  }

  public T getValue() {
    return value;
  }

  public double getWeight() {
    return weight;
  }

  @SuppressWarnings("RedundantIfStatement")
  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    Weighted<T> weighted = Unsafe.cast(o);

    if (Double.compare(weighted.weight, weight) != 0) return false;
    if (!value.equals(weighted.value)) return false;

    return true;
  }

  @Override public int hashCode() {
    int result;
    long temp;
    temp = Double.doubleToLongBits(weight);
    result = (int) (temp ^ (temp >>> 32));
    result = 31 * result + value.hashCode();
    return result;
  }

  @Override public String toString() {
    return value.toString() + '^' + formatter.format(weight);
  }

  public static Predicate<Weighted<?>> newThresholdPredicate(final double threshold) {
    return input -> input.weight >= threshold;
  }

  public static <V extends Comparable<? super V>> Function<Weighted<V>, V> newValueExtractor() {
    return new Function<Weighted<V>, V>() {
      @Nullable
      @Override
      public V apply(@Nullable Weighted<V> input) {
        assert input != null;
        return input.value;
      }
    };
  }

  public static <O> Collection<Weighted<O>> fixed(Iterable<? extends O> items, double weight) {
    Collection<Weighted<O>> cc = Lists.newArrayList();
    for (O item : items) {
      cc.add(Weighted.create(item, weight));
    }
    return cc;
  }

  @SafeVarargs
  public static <O> Collection<Weighted<O>> fixed(double weight, O... items) {
    Collection<Weighted<O>> cc = Lists.newArrayList();
    for (O item : items) {
      cc.add(Weighted.create(item, weight));
    }
    return cc;
  }

  public Weighted<T> scale(double scale) {
    return new Weighted<>(scale * weight, value);
  }
}
