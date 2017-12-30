package edu.utdallas.hltri.ml.vector;

import edu.utdallas.hltri.ml.Feature;

import java.util.stream.Stream;

/**
 *
 */
public interface FeatureVectorizer<K> {
  FeatureVector<K> vectorize(Stream<Feature<K>> features);
}
