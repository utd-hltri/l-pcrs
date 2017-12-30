package edu.utdallas.hltri.scribe.text;

import edu.utdallas.hltri.util.Unsafe;
import gate.FeatureMap;

import java.util.Optional;

/**
 * Created by rmm120030 on 8/10/15.
 */
public interface HasFeatureMap {
  FeatureMap getFeatureMap();

  default <T> Optional<T> get(String key) {
    return Optional.ofNullable(Unsafe.<T>cast(getFeatureMap().get(key)));
  }

  default <T> void set(String key, T value) {
    getFeatureMap().put(key, value);
  }
}
