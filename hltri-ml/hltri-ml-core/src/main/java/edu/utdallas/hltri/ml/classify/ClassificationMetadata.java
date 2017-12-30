//package edu.utdallas.hltri.ml.classify;
//
//import cc.mallet.types.SparseVector;
//import com.google.common.base.Function;
//import edu.utdallas.hltri.ml.*;
//import edu.utdallas.hltri.ml.feature.*;
//import edu.utdallas.hltri.util.IntIdentifier;
//
//import java.util.*;
//
///**
// * Holds information common across classification tasks, such as the feature extractors to use, and mappings from
// * classes to indexes as well as from features to feature vectors.
// * Class parameter I: The type of object representing a training or testing instance.
// * Class parameter C: The type of labels to classify (Often {@link String})
// * User: bryan
// * Date: 12/20/12
// * Time: 11:04 AM
// * Created with IntelliJ IDEA.
// */
//public class ClassificationMetadata<I,C> {
//  /** The feature extractors whose features this classifier will use to classify */
//  private final Iterable<? extends FeatureExtractor<? super I, ?>> featureExtractors;
//
//  /** Converts a util of feature values into a vector representing the same information */
//  private final FeatureVectorizer vectorizer;
//
//  /** Maps every unique label to a unique, dense, index */
//  private final IntIdentifier<C> classIDs;
//
//  public ClassificationMetadata(Iterable<? extends FeatureExtractor<? super I, ?>> featureExtractors,
//                                FeatureVectorizer vectorizer,
//                                IntIdentifier<C> classIDs) {
//    this.featureExtractors = featureExtractors;
//    this.vectorizer = vectorizer;
//    this.classIDs = classIDs;
//  }
//
//  public FeatureVectorizer vectorizer() {
//    return vectorizer;
//  }
//
//  public SparseVector getVector(I instance, boolean growVector) {
//    return vectorizer().getVector(FeatureUtils.extractFeatures(instance, featureExtractors), growVector);
//  }
//
//  public IntIdentifier<C> classIDs() {
//    return classIDs;
//  }
//
//  public Map<I,SparseVector> getVectors(Iterable<? extends I> trainingSet, Function<? super I,? extends C> targetFunc) {
//    Map<I,SparseVector> vecs = new HashMap<>();
//    for (I instance : trainingSet) {
//      vecs.put(instance, vectorizer.getVector(FeatureUtils.extractFeatures(instance, featureExtractors), true));
//      classIDs.getIDOrAdd(targetFunc.apply(instance));
//    }
//    return vecs;
//  }
//}
