package edu.utdallas.hltri.ml;

/**
 * User: bryan
 * Date: 12/18/12
 * Time: 9:02 AM
 * Created with IntelliJ IDEA.
 */
public interface Classifier<T,C> {
  // Classifier(MLConfig config, Iterable<FeatureExtractor> extractors)
  public C classify(T item);
}
