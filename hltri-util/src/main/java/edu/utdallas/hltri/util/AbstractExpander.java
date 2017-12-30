package edu.utdallas.hltri.util;

import java.util.Collection;

/**
 * Created by trg19 on 6/16/2017.
 */
public abstract class AbstractExpander<I, O> implements Expander<I, O> {
  protected final String name;

  protected AbstractExpander(String name) {
    this.name = name;
  }

  protected abstract Collection<O> getExpansions(I item);

  @Override public Expansion<O> expand(I item) {
    return Expansion.newExpansion(name, getExpansions(item));
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public Expansion<O> empty() {
    return Expansion.empty(name);
  }
}
