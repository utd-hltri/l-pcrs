//package edu.utdallas.hltri.ml.classify;
//
//import cc.mallet.optimize.*;
//import cc.mallet.types.*;
//import com.google.common.base.Function;
//import com.google.common.collect.Iterables;
//import com.google.common.collect.Lists;
//import edu.utdallas.hltri.ml.*;
//import edu.utdallas.hltri.ml.NumericFeature;
//import edu.utdallas.hltri.util.IntIdentifier;
//
//import java.util.*;
//
//
///**
// * Performs logistic regression.
// *
// * Helpful reference: http://justindomke.wordpress.com/logistic-regression/
// * Created with IntelliJ IDEA.
// * User: bryan
// * Date: 12/17/12
// * Time: 10:56 AM
// */
//public class LogisticRegressionVector<I,C> {
//  private Iterable<? extends I> trainingData;
//  private Function<? super I,? extends C> targetFunc;
//  private Iterable<? extends FeatureExtractor<? super I, ?>> featureExtractors;
//  private MLContext mlContext = new MLContext() {
//    @Override
//    public FeatureCache featureCache() {
//      return null;
//    }
//
//    @Override
//    public MLConfig config() {
//      return null;
//    }
//  };
//
//  private boolean addBias = true;
//  private Map<C, DenseVector> classToWeights = new HashMap<>();
//  private Map<I, SparseVector> instanceVectors = new HashMap<>();
//  private IntIdentifier<String> featureValueIDs = new IntIdentifier<>();
//  private IntIdentifier<C> classIDs = new IntIdentifier<>();
//
//
//  public LogisticRegressionVector(Iterable<? extends I> trainingData, Function<? super I, ? extends C> targetFunc,
//                                  Iterable<? extends FeatureExtractor<? super I, ?>> featureExtractors) {
//    this.trainingData = trainingData;
//    this.targetFunc = targetFunc;
//    this.featureExtractors = featureExtractors;
//  }
//
//  double lambda = 0.1;
//
//
//
//  private double instanceClassDotProd(I instance, C clazz) {
//    return classToWeights.get(clazz).dotProduct(getInstanceVector(instance));
//  }
//
//  private SparseVector getInstanceVector(I instance) {
//    SparseVector vec = instanceVectors.get(instance);
//    if (vec == null) {
//      vec = createInstanceVector(instance); // maybe it would help to cache for a little while?
//    }
//    return vec;
//  }
//
//  private double instanceDenominator(I instance) {
//    double denom = 0;
//    for (C altClass : classIDs.getItems()) {
//      denom += Math.exp(instanceClassDotProd(instance, altClass));
//    }
//    return denom;
//  }
//
//  private double computeLogLikelihood() {
//    System.err.println("Classes: " + classIDs.size());
//    System.err.println("Total dims: " + (classIDs.size()*(featureValueIDs.size()+1)));
//    System.err.println("Training size: " + Iterables.size(trainingData));
//    double ll = 0; // log likelihood
//    for (I instance : trainingData) {
//      C goldClass = targetFunc.apply(instance);
//      ll += instanceClassDotProd(instance, goldClass);
//      ll -= Math.log(instanceDenominator(instance));
//    }
//    MalletOptimizable opt = new MalletOptimizable();
//    double[] params = new double[opt.getNumParameters()];
//    opt.getParameters(params);
//    ll += lambda * MatrixOps.oneNorm(params);
//    return ll;
//  }
//
//  private double[] computeLogLikelihoodGradient() {
//    double gradSum = 0;
//    int indexSum =0;
//    double[] gradient = new double[classIDs.size() * (featureValueIDs.size()+(addBias ? 1 : 0))];
//    for (I instance : trainingData) {
//      C goldClass = targetFunc.apply(instance);
//      SparseVector instVec = getInstanceVector(instance);
//      int[] indexes = instVec.getIndices();
//      double[] values = instVec.getValues();
//      indexSum += indexes.length;
//      for (C altClass : classIDs.getItems()) {
//        int altClassID = classIDs.getID(altClass);
//        double probClass = probClassGivenInstance(altClass, instance);
//        double delta = (altClass.equals(goldClass) ? 1 : 0) - probClass;
//        for (int i = 0; i < indexes.length; i++) {
//          double gradVal = values[i] * delta;
//          gradSum += gradVal;
//          gradient[altClassID * (featureValueIDs.size() + (addBias ? 1 : 0)) + indexes[i]] += gradVal;
//        }
//      }
//    }
////    for (int i = 0; i < gradient.length; i++) {
////      int clazz = i / (featureValueIDs.size()+1);
////      int featID = i - (clazz*(featureValueIDs.size()+1));
////      String rep = null;
////      if (featID==featureValueIDs.size()) { rep = "__BIAS__"; }
////      else { rep =  featureValueIDs.get(featID); }
////      System.err.println("GRAD: " + gradient[i] + "  " + classIDs.get(clazz) + "  " + rep);
////    }
////    System.exit(0);
////    System.err.println("indexSum: " + indexSum);
////    System.err.println("Gradsum: " + gradSum);
////    System.err.println("2norm: " + new DenseVector(gradient).twoNorm());
//    MalletOptimizable opt = new MalletOptimizable();
//    for (int i = 0; i < gradient.length; i++) {
//      if (opt.getParameter(i) >= 0) {
//        gradient[i] = gradient[i] - lambda; // Derivative of l1 norm
//      } else {
//        gradient[i] = gradient[i] + lambda; // Derivative of l1 norm
//      }
//    }
//
//    return gradient;
//  }
//
//  private double probClassGivenInstance(C clazz, I instance) {
//    return Math.exp(instanceClassDotProd(instance, clazz)) / instanceDenominator(instance);
//  }
//
//  public void train() {
//    initializeWeights();
//    createInstanceVectors();
//    MalletOptimizable optimizable = new MalletOptimizable();
//
//    //new GradientAscent(optimizable).optimize();
//    new LimitedMemoryBFGS(optimizable).optimize();
//    //new ConjugateGradient(optimizable).optimize();
//    //new OrthantWiseLimitedMemoryBFGS(optimizable).optimize();
//    System.err.println("ll: "+ computeLogLikelihood());
//  }
//
//  private void createInstanceVectors() {
//    for (I instance : trainingData) {
//      instanceVectors.put(instance, createInstanceVector(instance));
//    }
//  }
//
//  private SparseVector createInstanceVector(I instance) {
//    List<? extends Feature<?>> uniqueFeatures = Lists.newArrayList(getUniqueFeatures(instance));
//    int[] indexes = new int[uniqueFeatures.size()+(addBias ? 1 : 0)];
//    double[] values = new double[uniqueFeatures.size()+(addBias ? 1 : 0)];
//    int f = 0;
//    for (Feature<?> feature : uniqueFeatures) {
//      int featID = featureValueIDs.getID(getFeatureRepr(feature));
//      if (featID >= 0) { // it will be negative for out of vocabulary feature values
//        indexes[f] = featID;
//        if (feature instanceof NumericFeature) {
//          values[f] = ((Number) feature.value()).doubleValue();
//        } else {
//          values[f] = 1.0; // TODO: allow bag of words representation for duplicated features
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
//  private Iterable<? extends Feature<?>> getUniqueFeatures(I instance) {
//    Map<String,Feature<?>> uniqueFeats = new HashMap<>();
//    for (FeatureExtractor<? super I, ?> extractor : featureExtractors) {
//      for (Feature<?> feat : extractor.extract(instance)) {
//        uniqueFeats.put(getFeatureRepr(feat), feat);
//      }
//    }
//    return uniqueFeats.values();
//  }
//
//  private String getFeatureRepr(Feature<?> feat) {
//    return feat.name() + "::" + feat.value().toString();
//  }
//
//  private void initializeWeights() {
//    // might be able to lazy compute these later
//    for (I instance : trainingData) {
//      for (Feature<?> feat : getUniqueFeatures(instance)) {
//        featureValueIDs.getIDOrAdd(getFeatureRepr(feat));
//      }
//      classIDs.getIDOrAdd(targetFunc.apply(instance));
//    }
//
//    Random rand = new Random(1337);
//    for (C clazz : classIDs.getItems()) {
//      double[] weights = new double[featureValueIDs.size()+(addBias ? 1 : 0)];
//      for (int w = 0; w < weights.length; w++) {
//        //weights[w] = rand.nextGaussian();
//        //weights[w] = rand.nextDouble() * 6 - 3;
//        weights[w] = 0;
//      }
//      classToWeights.put(clazz, new DenseVector(weights, false));
//    }
//  }
//
//  public void printWeights() {
//    for (C clazz : classIDs.getItems()) {
//      for (String featVal : featureValueIDs.getItems()) {
//        int classID = classIDs.getID(clazz);
//        int featID = featureValueIDs.getID(featVal);
//        double weight = classToWeights.get(clazz).value(featID);
//        System.err.printf("WEIGHT: %20s   %20s   %f\n", clazz, featVal,  weight);
//      }
//    }
//  }
//
//  private class MalletOptimizable implements Optimizable.ByGradientValue {
//
//    @Override
//    public void getValueGradient(double[] buffer) {
//      double[] grad = computeLogLikelihoodGradient();
//      System.arraycopy(grad, 0, buffer, 0, buffer.length);
//    }
//
//    @Override
//    public double getValue() {
//      return  computeLogLikelihood();
//    }
//
//    @Override
//    public int getNumParameters() {
//      return (featureValueIDs.size()+(addBias ? 1 : 0)) * classIDs.size();
//    }
//
//    @Override
//    public void getParameters(double[] buffer) {
//      assert buffer.length == (featureValueIDs.size()+(addBias ? 1 : 0 ))*classIDs.size() :
//        "buffer: " + buffer.length + "  expected: " + (featureValueIDs.size()+(addBias ? 1 : 0 ))*classIDs.size() +
//            "  classes: " + classIDs.size();
//      for (int classID = 0; classID < classIDs.size(); classID++) {
//        classToWeights.get(classIDs.get(classID)).arrayCopyInto(buffer,
//            (featureValueIDs.size()+(addBias ? 1 : 0)) * classID);
//      }
//    }
//
//    @Override
//    public double getParameter(int index) {
//      int vecSize = (featureValueIDs.size()+(addBias ? 1 : 0));
//      int classID = index / vecSize;
//      return classToWeights.get(classIDs.get(classID)).value(index-classID*vecSize);
//    }
//
//    @Override
//    public void setParameters(double[] params) {
//      for (int p = 0; p < params.length; p++) {
//        setParameter(p, params[p]);
//      }
//    }
//
//    @Override
//    public void setParameter(int index, double value) {
//      int vecSize = (featureValueIDs.size()+(addBias ? 1 : 0));
//      int classID = index / vecSize;
//      int instIndex = index-classID*vecSize;
//      assert instIndex >= 0 && instIndex < vecSize : "instIndex: " + instIndex + "  vecSize: " + vecSize + "  orig index: " + index;
//      classToWeights.get(classIDs.get(classID)).setSingleValue(instIndex, value);
//    }
//  }
//
//  public C classify(I instance) {
//    C bestClass= null;
//    double bestProb = Double.NEGATIVE_INFINITY;
//    for (C candClass : classIDs.getItems()) {
//      double prob = probClassGivenInstance(candClass, instance);
//      if (prob > bestProb) {
//        bestProb = prob;
//        bestClass = candClass;
//      }
//    }
//    return bestClass;
//  }
//
//
//}
