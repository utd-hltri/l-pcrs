package edu.utdallas.hltri.ml.vector;

import java.util.stream.Stream;

import edu.utdallas.hltri.logging.Logger;
import edu.utdallas.hltri.ml.Feature;
import edu.utdallas.hltri.util.IntIdentifier;

/**
 * Created by ramon on 10/20/2016.
 */
public class SparseFeatureVectorizer<K> implements FeatureVectorizer<K> {
  private static final Logger log = Logger.get(SparseFeatureVectorizer.class);
  private final IntIdentifier<String> featureIndentifier;

  public SparseFeatureVectorizer(IntIdentifier<String> featureIndentifier) {
    this.featureIndentifier = featureIndentifier;
    log.debug("Initialized feature vectorizer with {} pre-existing features.", featureIndentifier.size());
  }

  @Override
  public SparseFeatureVector<K> vectorize(Stream<Feature<K>> features) {
    final SparseFeatureVector<K> sfv = new SparseFeatureVectorImpl<>(featureIndentifier);
    features.flatMap(Feature::stream)
        // check if this feature should be ignored
        .filter(f -> !featureIndentifier.isLocked() || featureIndentifier.getID(f.name()) > -1)
        // add it to the sparse feature vector
        .forEach(f -> sfv.addByName(f.name(), (K) f.value()));
    return sfv;
  }

  public SparseFeatureVector<K> emptyVector(int id, K value) {
    final SparseFeatureVector<K> sfv = new SparseFeatureVectorImpl<>(featureIndentifier);
    sfv.addById(id, value);
    return sfv;
  }

  public static <K> SparseFeatureVectorizer<K> verbose(IntIdentifier<String> featureIndentifier) {
    return new SparseFeatureVectorizer<K>(featureIndentifier) {
      @Override
      public SparseFeatureVector<K> vectorize(Stream<Feature<K>> features) {
        final SparseFeatureVector<K> sfv = new SparseFeatureVectorImpl<>(featureIndentifier);
        features.flatMap(Feature::stream)
            .map(f -> {
              System.out.format("%s (id=%d). Passes filter? %b\n", f.toString(), featureIndentifier.getID(f.name()), !featureIndentifier.isLocked() || featureIndentifier.getID(f.name()) > -1);
              return f;
            })
            // check if this feature should be ignored
            .filter(f -> !featureIndentifier.isLocked() || featureIndentifier.getID(f.name()) > -1)
            // add it to the sparse feature vector
            .forEach(f -> sfv.addByName(f.name(), (K) f.value()));
        return sfv;
      }
    };
  }
}
