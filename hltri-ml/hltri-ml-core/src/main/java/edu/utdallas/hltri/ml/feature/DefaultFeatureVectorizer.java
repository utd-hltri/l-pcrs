//package edu.utdallas.hltri.ml.feature;
//
//import cc.mallet.types.SparseVector;
//import com.google.common.collect.HashMultimap;
//import com.google.common.collect.Multimap;
//import edu.utdallas.hltri.ml.Feature;
//import edu.utdallas.hltri.ml.FeatureVectorizer;
//import edu.utdallas.hltri.ml.NumericFeature;
//import edu.utdallas.hltri.ml.StringFeature;
//import edu.utdallas.hltri.util.IntIdentifier;
//
//import java.util.Iterator;
//
///**
// * User: bryan
// * Date: 12/20/12
// * Time: 9:06 AM
// * Created with IntelliJ IDEA.
// */
//public class DefaultFeatureVectorizer implements FeatureVectorizer {
//
//  /** Maps every unique feature name/value combination to a unique, dense, index */
//  private IntIdentifier<String> featureValueIDs = new IntIdentifier<>();
//
//  /** Whether to add an "always on" feature to every instance */
//  private boolean addBias = false;
//
//  /** Make sure someone doesn't change the state of this object */
//  private boolean shouldBeImmutable = false;
//
//  /** Adds an "always on" (=1.0) feature to every instance */
//  public DefaultFeatureVectorizer addBias() {
//    if (shouldBeImmutable) throw new IllegalStateException("Vectors have already been generated");
//    this.addBias = true;
//    return this;
//  }
//
//  /**
//   *
//   * @param features the feature values to be represented as a vector
//   * @param growVectors If true, previously unseen feature values will get a new element assigned to the them.  This
//   *                    should generally be set to true during training, and false during testing.
//   * @return
//   */
//  public SparseVector getVector(Iterable<? extends Feature<?>> features, boolean growVectors) {
//    shouldBeImmutable = true;
//    Multimap<String,Feature<?>> uniqueFeatures = groupFeatures(features);
//    int[] indexes = new int[uniqueFeatures.keySet().size()+(addBias ? 1 : 0)];
//    double[] values = new double[uniqueFeatures.keySet().size()+(addBias ? 1 : 0)];
//    int f = 0;
//    for (String featRepr: uniqueFeatures.keySet()) {
//      int featID = featureValueIDs.getID(featRepr, growVectors);
//      if (featID >= 0) { // it will be negative for out of vocabulary feature values. ignore them.
//        indexes[f] = featID;
//        Iterator<Feature<?>> featIterator = uniqueFeatures.get(featRepr).iterator();
//        Feature<?> feature = featIterator.next();
//        if (feature.value() instanceof Number) {
//          values[f] = ((Number) feature.value()).doubleValue(); // TODO: what if more than one numeric feature have the same name?
//        } else {
//          values[f] = 1.0; // TODO: allow bag of words representation for duplicated features
//        }
//        if (featIterator.hasNext()) {
//          throw new IllegalStateException("Bag of words and numeric feature aggregation aren't properly supported");
//        }
//        f++;
//      }
//    }
//    if (addBias) {
//      indexes[f] = featureValueIDs.size();
//      values[f] = 1.0;
//    }
//    return new SparseVector(indexes, values);
//  }
//
//  /** Number of dimensions in the feature vectors */
//  @Override
//  public int numDimensions() {
//    return featureValueIDs.size() + (addBias ? 1 : 0);
//  }
//
////  /** Ensures that vectors will have elements for the given features. */
////  public void addFeatureIDs(Iterable<? extends Feature<?>> features) {
////    for (NameValuePair uniqueFeat : groupFeatures(features).elementSet()) {
////      featureValueIDs.getIDOrAdd(uniqueFeat);
////    }
////  }
//
//  @Override
//  public String getFeatureRepresentation(int featID) {
//    if (addBias && featID == featureValueIDs.size()) { return "__BIAS__"; }
//    return featureValueIDs.get(featID);
//  }
//
//  /** Map from a feature name (numeric feats) or feature name & value to the corresponding features*/
//  private Multimap<String,Feature<?>> groupFeatures(Iterable<? extends Feature<?>>  features) {
//    Multimap<String,Feature<?>> uniqueFeats = HashMultimap.create();
//    for (Feature<?> feat : features) {
//      uniqueFeats.put(getRepr(feat), feat);
//    }
//
//    return uniqueFeats;
//  }
//
//  private String getRepr(Feature<?> feat) {
//    if (feat instanceof NumericFeature) {
//      return feat.name();
//    } else if (feat instanceof StringFeature) {
//      return feat.name() + "_::_" + ((StringFeature) feat).value();
//    } else {
//      throw new IllegalArgumentException("Unknown feature type: " + feat.getClass());
//    }
//  }
//}
