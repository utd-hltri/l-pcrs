//package edu.utdallas.hltri.ml.feature.extractors;
//
//import com.google.common.collect.*;
//import edu.utdallas.hltri.ml.*;
//
//import java.util.*;
//
///**
// * User: bryan
// * Date: 5/22/13
// * Time: 10:03 AM
// * Created with IntelliJ IDEA.
// */
//public class ExcludingExtractor<T> implements FeatureExtractor<T> {
//  private Set<String> excludes;
//  private Iterable<FeatureExtractor<? super T>> extractors;
//
//  public ExcludingExtractor(Iterable<FeatureExtractor<? super T>> extractors, Iterable<String> excludedFeatures) {
//    excludes = Sets.newHashSet(excludedFeatures);
//    this.extractors = extractors;
//  }
//
//  @Override
//  public Collection<? extends Feature<?>> extract(T instance, MLContext context) {
//    Collection<Feature<?>> features = new ArrayList<>();
//    for (FeatureExtractor<? super T> extractor : extractors) {
//      if ( ! excludes.contains())
//      features.addAll(extractor.extract(instance, context));
//    }
//    return features;
//  }
//}
