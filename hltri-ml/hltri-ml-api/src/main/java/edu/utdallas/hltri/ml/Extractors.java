package edu.utdallas.hltri.ml;

import com.google.common.collect.Lists;
import com.google.common.collect.Multiset;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.ToDoubleBiFunction;
import java.util.function.ToDoubleFunction;
import java.util.function.ToIntFunction;
import java.util.function.ToLongFunction;

import edu.utdallas.hltri.ml.Feature;
import edu.utdallas.hltri.ml.NumericFeature;

/**
 * Created by travis on 8/7/14.
 */
public class Extractors {

  public static <I, O> Function<I, Collection<? extends Feature<? extends O>>> single(String name, Function<I, O> function) {
    return (I i) -> Collections.singleton(Feature.simple(name, function.apply(i)));
  }

  public static <I> Function<I, Collection<? extends Feature<Number>>> singleInt(String name, ToIntFunction<I> function) {
    return (I i) -> Collections.singleton(Feature.numericFeature(name, function.applyAsInt(i)));
  }

  public static <I> Function<I, Collection<? extends Feature<Number>>> singleDouble(String name, ToDoubleFunction<I> function) {
    return (I i) -> Collections.singleton(Feature.simple(name, function.applyAsDouble(i)));
  }

  public static <I,J> BiFunction<I, J,  Collection<? extends Feature<Number>>> singleDouble(String name, ToDoubleBiFunction<I,J> function) {
    return (I i, J j) -> Collections.singleton(Feature.simple(name, function.applyAsDouble(i, j)));
  }


  public static <I> Function<I, Collection<? extends Feature<Number>>> singleLong(String name, ToLongFunction<I> function) {
    return (I i) -> Collections.singleton(Feature.simple(name, function.applyAsLong(i)));
  }

  public static <I> Function<I, Collection<? extends Feature<Number>>> multinomial(String prefix, Function<I, Multiset<String>> function) {
    return (I i) -> {
      final Multiset<String> multiset =function.apply(i);
      Collection<Feature<Number>> features = Lists.newArrayList();
      for (Multiset.Entry<String> entry : multiset.entrySet()) {
        features.add(Feature.numericFeature(prefix + "." + entry.getElement(), entry.getCount()));
      }
      return features;
    };
  }

  public static <I> Function<I, Collection<? extends Feature<Number>>> multinomialFromMap(String prefix, Function<I, Map<String, Integer>> function) {
    return (I i) -> {
      final Map<String, Integer> multiset =function.apply(i);
      Collection<Feature<Number>> features = Lists.newArrayList();
      for (Map.Entry<String, Integer> entry : multiset.entrySet()) {
        features.add(Feature.numericFeature(prefix + "." + entry.getKey(), entry.getValue()));
      }
      return features;
    };
  }
}
