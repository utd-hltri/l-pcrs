package edu.utdallas.hltri.util;

import java.util.Collection;

import edu.utdallas.hltri.conf.Config;
import edu.utdallas.hltri.struct.Weighted;

/**
 * Created by travis on 7/28/14.
 */
public abstract class AbstractFixedWeightExpander<I, O> extends AbstractExpander<I, Weighted<O>> {
  protected final double weight;

  protected AbstractFixedWeightExpander(String name) {
    super(name);
    final Config conf = Config.load("inquire.expansion");
    this.weight = conf.getDouble("weight");
  }

  protected AbstractFixedWeightExpander(String name, double weight) {
    super(name);
    this.weight = weight;
  }

  protected abstract Collection<O> getUnweightedExpansions(I item);

  @Override protected Collection<Weighted<O>> getExpansions(I item) {
    return Weighted.fixed(getUnweightedExpansions(item), weight);
  }
}
