//package edu.utdallas.hltri.ml.feature;
//
//import edu.utdallas.hltri.ml.Feature;
//import edu.utdallas.hltri.ml.FeatureExtractor;
//
//import java.util.*;
//
///**
// * User: bryan
// * Date: 12/20/12
// * Time: 9:59 AM
// * Created with IntelliJ IDEA.
// */
//public class FeatureUtils {
//
//  /** Extracts all the features for an instance */
//  public static <I> List<Feature<?>> extractFeatures(I instance,
//                                               Iterable<? extends FeatureExtractor<? super I, ?>> featureExtractors) {
//    List<Feature<?>> features = new ArrayList<>();
//    features.addAll(features);
//    for (FeatureExtractor<? super I, ?> extractor : featureExtractors) {
//      features.addAll(extractor.extract(instance));
//    }
//    return features;
//  }
//
//}
