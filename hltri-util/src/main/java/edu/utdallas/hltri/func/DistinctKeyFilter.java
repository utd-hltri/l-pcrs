package edu.utdallas.hltri.func;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * Created by rmm120030 on 9/7/16.
 */
public class DistinctKeyFilter<T,K> {
  private final Map<K,Boolean> seen;
  private final Function<T,K> keyExtractor;

  public DistinctKeyFilter(Function<T,K> ke) {
    this.seen = new ConcurrentHashMap<>();
    this.keyExtractor = ke;
  }

  public boolean filter(T t) {
    return seen.putIfAbsent(keyExtractor.apply(t), Boolean.TRUE) == null;
  }
}
