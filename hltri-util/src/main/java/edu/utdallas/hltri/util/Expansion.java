package edu.utdallas.hltri.util;

import com.google.common.base.Splitter;
import com.google.common.collect.ForwardingCollection;
import com.google.common.collect.Lists;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import edu.utdallas.hltri.logging.Logger;
import edu.utdallas.hltri.struct.Weighted;

/**
 *
 * Created by trg19 on 6/16/2017.
 */
@SuppressWarnings("unused")
public class Expansion<V> extends ForwardingCollection<V> implements Serializable {
  private static final Logger log = Logger.get(Expansion.class);
  private static final long   serialVersionUID = 1L;

  private final String          name;
  private final Collection<V> expansions;

  private Expansion(final String name, final Collection<V> expansions) {
    this.name = name;
    this.expansions = expansions;
  }

  public String getName() {
    return name;
  }

  public boolean isEmpty() {
    return expansions.isEmpty();
  }

  @Override public String toString() {
    return name + ":{" +  expansions + '}';
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    Expansion<?> expansion = (Expansion<?>) o;
    return Objects.equals(name, expansion.name) &&
        Objects.equals(expansions, expansion.expansions);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, expansions);
  }

  @Override protected Collection<V> delegate() {
    return expansions;
  }

  public Expansion<Weighted<V>> toWeightedExpansion(double weight) {
    return new Expansion<>(name, Weighted.fixed(expansions, weight));
  }

  public static <O> Expansion<O> newExpansion(
      String name, Collection<O> it) {
    return new Expansion<>(name, it);
  }

  @SuppressWarnings("WeakerAccess")
  public static <O> Expansion<Weighted<O>> newFixedWeightedExpansion(
      String name, double weight, Iterable<O> items) {
    final List<Weighted<O>> list = Lists.newArrayList();
    for (O item: items) {
      list.add(Weighted.create(weight, item));
    }
    return new Expansion<>(name, list);
  }

  @SafeVarargs
  public static <O> Expansion<Weighted<O>> newFixedWeightedExpansion(
      String name, double weight, final O... items) {
    return newFixedWeightedExpansion(name, weight, Arrays.asList(items));
  }

  public static void filterWeightedExpansions(Iterable<? extends Weighted<? extends CharSequence>> list) {
    filterWeightedExpansions(list, Splitter.on(' '));
  }

  public static void filterWeightedExpansions(Iterable<? extends Weighted<? extends CharSequence>> list,
                                              Splitter tokenizer) {
    final Map<CharSequence, List<String>> tokens = new HashMap<>();

    for (Iterator<? extends Weighted<? extends CharSequence>> it = list.iterator(); it.hasNext();) {

      final Weighted<? extends CharSequence> concept = it.next();

      boolean prune = false;
      for (Weighted<? extends CharSequence> other : list) {
        // Compare against all other elements
        if (concept == other) { continue; }
        if (Collections.indexOfSubList(
            tokens.computeIfAbsent(concept.value, tokenizer::splitToList),
            tokens.computeIfAbsent(other.value, tokenizer::splitToList)) != -1 &&
            Double.compare(concept.getWeight(), other.getWeight()) <= 0) {
          prune = true;
          log.trace("Pruning {} because of {}", concept, other);
          break;
        }
      }

      if (prune) {
        it.remove();
      }
    }
  }

  public static void reduceEntries(Iterable<String> list, Splitter tokenizer) {
    final Map<String, List<String>> tokens = new HashMap<>();

    for (Iterator<String> it = list.iterator(); it.hasNext();) {
      final String concept = it.next();

      boolean prune = false;
      for (String other : list) {
        // Compare against all other elements
        //noinspection StringEquality
        if (concept == other) { continue; }
        if (concept.equals(other) || Collections.indexOfSubList(
            tokens.computeIfAbsent(concept, tokenizer::splitToList),
            tokens.computeIfAbsent(other, tokenizer::splitToList)) != -1) {
          prune = true;
          break;
        }
      }

      if (prune) {
        it.remove();
      }
    }
  }

  public static void reduceEntries(Iterable<String> list) {
    reduceEntries(list, Splitter.on(' '));
  }

  public static <T> Expansion<T> empty(String name) {
    return new Expansion<>(name, Collections.emptyList());
  }

  public static <T> Expansion<T> singleton(String name, T item) {
    return new Expansion<T>(name, Collections.singleton(item));
  }
}
