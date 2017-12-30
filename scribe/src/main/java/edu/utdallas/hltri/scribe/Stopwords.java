package edu.utdallas.hltri.scribe;

import com.google.common.collect.ForwardingSet;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;

public abstract class Stopwords extends ForwardingSet<String> implements Predicate<CharSequence> {
  final protected Set<String> delegate = new HashSet<>();

  @Override protected Set<String> delegate() {
    return delegate;
  }

  @Override public boolean test(CharSequence obj) {
    return !delegate.contains(obj.toString());
  }
}
