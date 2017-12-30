package edu.utdallas.hltri.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.function.Function;

import edu.utdallas.hltri.struct.Weighted;

/**
 * Utility for expanding an object of type I to a collection of weighted objects of type O
 * Typically used for query expansion between CharSequence and Strings
 * Created by travis on 7/28/14.
 */
@SuppressWarnings("unused")
public interface Expander<I, O> {
  Expansion<O> expand(I item);

  default Expansion<O> empty() {
    return Expansion.empty(getName());
  }

  String getName();

  @SuppressWarnings("SameParameterValue")
  default Expander<I, Weighted<O>> withFixedWeight(double weight) {
    return new AbstractFixedWeightExpander<I, O>(getName(), weight) {
      @Override
      protected Collection<O> getUnweightedExpansions(I item) {
        return Expander.this.expand(item);
      }
    };
  }

  default Expansion<O> expandAll(Iterable<? extends I> items) {
    final Iterator<? extends I> it = items.iterator();
    if (it.hasNext()) {
      final Expansion<O> expansions = expand(it.next());
      while (it.hasNext()) {
        expansions.addAll(expand(it.next()));
      }
      return expansions;
    } else {
      return empty();
    }
  }

  default Iterable<Expansion<O>> expandEach(Iterable<? extends I> input) {
    final Collection<Expansion<O>> expansions = new ArrayList<>();
    for (final I item : input) {
      expansions.add(expand(item));
    }
    return expansions;
  }

  static <I, O> Expander<I, O> fromFunction(String name, Function<I, ? extends Collection<O>> func) {
    return new AbstractExpander<I, O>(name) {
      @Override
      protected Collection<O> getExpansions(I item) {
        return func.apply(item);
      }
    };
  }
}
