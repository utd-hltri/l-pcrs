package edu.utdallas.hltri.ml.vector;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import edu.utdallas.hltri.logging.Logger;
import edu.utdallas.hltri.ml.Feature;
import edu.utdallas.hltri.util.IntIdentifier;

import java.util.Arrays;
import java.util.stream.Stream;

/**
 * This is a SparseFeatureVectorizer that keeps counts of how often each feature is seen in any Stream<Feature>
 *   processed by its .vectorize() method.
 * CountingSparseFeatureVectorizer can be used to remove uncommon features.
 */
public class CountingSparseFeatureVectorizer<K> extends SparseFeatureVectorizer<K> {
  private static final Logger log = Logger.get(SparseFeatureVectorizer.class);
  private IntIdentifier<String> featureIndentifier;
  private final Multiset<String> counts = HashMultiset.create();
  private boolean locked = false;

  public CountingSparseFeatureVectorizer() {
    super(new IntIdentifier<>());
    this.featureIndentifier = new IntIdentifier<>();
  }

  public IntIdentifier<String> getFeatureIdentifier() {
    return featureIndentifier;
  }

  @Override
  public SparseFeatureVector<K> vectorize(Stream<Feature<K>> features) {
    final SparseFeatureVector<K> sfv = new SparseFeatureVectorImpl<>(featureIndentifier);
    features.flatMap(Feature::stream)
        // check if this feature should be ignored
        .filter(f -> !featureIndentifier.isLocked() || featureIndentifier.getID(f.name()) > -1)
        // add it to the sparse feature vector
        .forEach(f -> {
          counts.add(f.name());
          sfv.addByName(f.name(), (K)f.value());
        });
    return sfv;
  }

  public void lockAndRemoveUncommonFeatures(int n) {
    this.featureIndentifier = removeUncommonFeatures(n, featureIndentifier, counts);
    locked = true;
  }

  public static IntIdentifier<String> removeUncommonFeatures(int n, IntIdentifier<String> iid, Multiset<String> counts) {
    final IntIdentifier<String> newIdentifier = new IntIdentifier<>();
    iid.getItems().stream().filter(f -> counts.count(f) > n).forEach(newIdentifier::getIDOrAdd);
    newIdentifier.lock();
    log.info("Trimmed {} of {} features for a new total of {}", iid.size() - newIdentifier.size(),
        iid.size(), newIdentifier.size());
    return newIdentifier;
  }

  public SparseFeatureVector<K> removeUncommon(SparseFeatureVector<K> old) {
    assert locked : "Call to removeUncommon on unlocked CountingSparseFeatureVectorizer.";
    return removeUncommon(old, featureIndentifier);
  }

  public static <K> SparseFeatureVector<K> removeUncommon(SparseFeatureVector<K> old, IntIdentifier<String> iid) {
    final SparseFeatureVector<K> nevv = new SparseFeatureVectorImpl<>(iid);
    Arrays.stream(old.names()).filter(n -> iid.getID(n) >= 0).forEach(n -> nevv.addByName(n, old.getByName(n)));
    return nevv;
  }

  public static <K> SparseFeatureVectorizer<K> verbose(IntIdentifier<String> featureIndentifier) {
    return new SparseFeatureVectorizer<K>(featureIndentifier) {
      @Override
      public SparseFeatureVector<K> vectorize(Stream<Feature<K>> features) {
        final SparseFeatureVector<K> sfv = new SparseFeatureVectorImpl<>(featureIndentifier);
        features.flatMap(Feature::stream)
            .map(f -> {
              System.out.format("%s (id=%d). Passes filter? %b\n", f.toString(), featureIndentifier.getID(f.name()),
                  !featureIndentifier.isLocked() || featureIndentifier.getID(f.name()) > -1);
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
