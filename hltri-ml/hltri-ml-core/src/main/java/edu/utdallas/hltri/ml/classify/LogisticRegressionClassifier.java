//package edu.utdallas.hltri.ml.classify;
//
//import cc.mallet.types.SparseVector;
//import edu.utdallas.hltri.ml.Classifier;
//
///**
// * User: bryan
// * Date: 12/20/12
// * Time: 8:32 AM
// * Created with IntelliJ IDEA.
// */
//public class LogisticRegressionClassifier<I,C> implements Classifier<I,C> {
//
//  /** The weights of this model, used to estimate the probability of a class given an instance */
//  private double[] featureWeights;
//
//  /** Holds ids and vectors for the classification */
//  private final ClassificationMetadata<I,C> representations;
//
//  public LogisticRegressionClassifier(double[] featureWeights, ClassificationMetadata<I, C> representations) {
//    this.featureWeights = featureWeights;
//    this.representations = representations;
//  }
//
//  /** Classify an instance by calculating the estimated probability of each class and returning the highest */
//  @Override
//  public C classify(I instance) {
//    C bestClass= null;
//    double bestProb = Double.NEGATIVE_INFINITY;
//    SparseVector instanceVec = representations.getVector(instance, false);
//    for (C candClass : representations.classIDs().getItems()) {
//      double prob = probClassGivenInstance(candClass, instanceVec);
//      if (prob > bestProb) {
//        bestProb = prob;
//        bestClass = candClass;
//      }
//    }
//    return bestClass;
//  }
//
//  double probClassGivenInstance(C clazz, SparseVector instanceVec) {
//    return Math.exp(instanceClassDotProd(instanceVec, clazz)) / instanceDenominator(instanceVec);
//  }
//
//  double instanceClassDotProd(SparseVector instanceVec, C clazz) {
//    int classID = representations.classIDs().getID(clazz);
//    int weightOffset = classID * representations.vectorizer().numDimensions();
//    double dotProd = 0;
//    for (int instIdx : instanceVec.getIndices()) {
//      dotProd += instanceVec.value(instIdx) * featureWeights[weightOffset + instIdx];
//    }
//    return dotProd;
//  }
//
//  double instanceDenominator(SparseVector instanceVec) {
//    double denom = 0;
//    for (C altClass : representations.classIDs().getItems()) {
//      denom += Math.exp(instanceClassDotProd(instanceVec, altClass));
//    }
//    return denom;
//  }
//
//  public void printWeights() {
//    for (C clazz : representations.classIDs().getItems()) {
//      int classID = representations.classIDs().getID(clazz);
//      for (int featID = 0; featID < representations.vectorizer().numDimensions(); featID++) {
//        double weight = featureWeights[classID * representations.vectorizer().numDimensions() + featID];
//        String featRepr = representations.vectorizer().getFeatureRepresentation(featID);
//        System.err.printf("WEIGHT: %20s   %20s   %f\n", clazz, featRepr,  weight);
//      }
//    }
//  }
//
//  double[] getFeatureWeights() {
//    return featureWeights;
//  }
//
//}
