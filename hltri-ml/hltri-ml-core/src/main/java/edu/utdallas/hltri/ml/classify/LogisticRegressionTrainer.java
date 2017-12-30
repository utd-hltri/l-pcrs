//package edu.utdallas.hltri.ml.classify;
//
//import cc.mallet.optimize.*;
//import cc.mallet.types.*;
//import com.google.common.base.Function;
//import edu.utdallas.hltri.ml.*;
//import DefaultFeatureVectorizer;
//import NoRegularization;
//import OptimizerFactory;
//import Regularization;
//import edu.utdallas.hltri.util.IntIdentifier;
//
//import java.util.*;
//
///**
// * Helpful reference: http://justindomke.wordpress.com/logistic-regression/
// * User: bryan
// * Date: 12/20/12
// * Time: 8:39 AM
// * Created with IntelliJ IDEA.
// */
//public class LogisticRegressionTrainer<I,C> implements ClassifierTrainer<I,C> {
//
//  /** Optimizes the log-likelihood function for us */
//  private OptimizerFactory optimizers;
//
//  private ClassificationMetadata<I,C> representations;
//
//  private LogisticRegressionClassifier<I, C> classifier;
//
//  private Map<I,SparseVector> instVecs;
//
//  private Function<? super I, ? extends C> targetFunc;
//
//  private Regularization.WithGradient regularization = new NoRegularization();
//
//  public LogisticRegressionTrainer(OptimizerFactory optimizers) {
//    this.optimizers = optimizers;
//  }
//
//
//  @Override
//  public LogisticRegressionClassifier<I, C> train(Iterable<? extends I> trainingSet,
//                                Function<? super I, ? extends C> targetFunc,
//                                Iterable<? extends FeatureExtractor<? super I, ?>> featureExtractors,
//                                MLConfig config) {
//
//    this.targetFunc = targetFunc;
//
//    this.representations = new ClassificationMetadata<>(featureExtractors,
//        new DefaultFeatureVectorizer().addBias(), new IntIdentifier<C>());
//
//    this.classifier = initialClassifier(trainingSet, targetFunc);
//
//    MalletOptimizable optimizable = new MalletOptimizable();
//
//    try {
//      optimizers.createOptimizer(optimizable).optimize();
//    } catch (cc.mallet.optimize.OptimizationException oe) {
//      oe.printStackTrace();
//      System.err.println("Continuing...");
//    }
////    System.err.println("ll: " + optimizable.getValue());
//
//    return classifier;
//  }
//
//  public LogisticRegressionTrainer<I,C> regularization(Regularization.WithGradient regularization) {
//    this.regularization = regularization;
//    return this;
//  }
//
//  private LogisticRegressionClassifier<I, C> initialClassifier(Iterable<? extends I> trainingData,
//                                                               Function<? super I, ? extends C> targetFunc) {
//    this.instVecs = representations.getVectors(trainingData, targetFunc);
//
//    Random rand = new Random(1337);
//    double[] weights = new double[representations.classIDs().size() * representations.vectorizer().numDimensions()];
//    for (int w = 0; w < weights.length; w++) {
//      //weights[w] = rand.nextGaussian();
//      weights[w] = rand.nextDouble() * 6 - 3;
////      weights[w] = 0;
//    }
//    return new LogisticRegressionClassifier<>(weights, representations);
//  }
//
//  private void setClassifier(LogisticRegressionClassifier<I,C> newClassifier) {
//    this.classifier = newClassifier;
//  }
//
//  /////////// Optimizable ///////////////////////
//
//  private class MalletOptimizable implements Optimizable.ByGradientValue {
//
//    @Override
//    public void getValueGradient(double[] gradient) {
//      Arrays.fill(gradient, 0);
//      //System.err.println("Num dims: " + representations.vectorizer().numDimensions());
//      //assert gradient.length == representations.vectorizer().numDimensions();
//      double gradSum =0;
//      int indexSum = 0;
//      for (I instance : instVecs.keySet()) {
//        C goldClass = targetFunc.apply(instance);
//        SparseVector instVec = instVecs.get(instance);
//        int[] indexes = instVec.getIndices();
//        indexSum += indexes.length;
//        for (C altClass : representations.classIDs().getItems()) {
//          int altClassID = representations.classIDs().getID(altClass);
//          double probClass = classifier.probClassGivenInstance(altClass, instVec);
//          double delta = (altClass.equals(goldClass) ? 1 : 0) - probClass;
//          for (int index : indexes) {
//            double gradVal = instVec.value(index) * delta;
//            gradSum += gradVal;
//            gradient[altClassID * representations.vectorizer().numDimensions() + index] += gradVal;
//          }
//        }
//      }
//
//      regularization.gradient(classifier.getFeatureWeights(), gradient);
////      for (int i = 0; i < gradient.length; i++) {
////        int clazz = i / (representations.vectorizer().numDimensions());
////        int featID = i - (clazz*(representations.vectorizer().numDimensions()));
////        System.err.println("GRAD: " + gradient[i] + "  " + representations.classIDs().get(clazz) + "  " +
////            representations.vectorizer().getFeatureRepresentation(featID));
////      }
////      System.exit(0);
////      System.err.println("indexSum: " + indexSum);
////      System.err.println("2norm: " + new DenseVector(gradient).twoNorm());
////      System.err.println("gradSum = " + gradSum);
//    }
//
//    /** Log likelihood of training set */
//    @Override
//    public double getValue() {
////      System.err.println("Classes: " + representations.classIDs().size());
////      System.err.println("Total dims: " + classifier.getFeatureWeights().length);
////      System.err.println("Training size: " + instVecs.keySet().size());
//      double ll = 0; // log likelihood
//      for (I instance : instVecs.keySet()) {
//        SparseVector instVec = instVecs.get(instance);
//        C goldClass = targetFunc.apply(instance);
//        ll += classifier.instanceClassDotProd(instVec, goldClass);
//        ll -= Math.log(classifier.instanceDenominator(instVec));
//      }
//
//      ll += regularization.value(classifier.getFeatureWeights());
//
//      return ll;
//    }
//
//    @Override
//    public int getNumParameters() {
//      return representations.vectorizer().numDimensions() * representations.classIDs().size();
//    }
//
//    @Override
//    public void getParameters(double[] buffer) {
//      System.arraycopy(classifier.getFeatureWeights(), 0, buffer, 0, buffer.length);
//    }
//
//    @Override
//    public double getParameter(int index) {
//      return classifier.getFeatureWeights()[index];
//    }
//
//    @Override
//    public void setParameters(double[] params) {
//      double[] newWeights = new double[params.length]; // TODO: probably don't need to copy
//      System.arraycopy(params, 0, newWeights, 0, params.length);
//      setClassifier(new LogisticRegressionClassifier<>(newWeights, representations));
//    }
//
//    @Override
//    public void setParameter(int index, double value) {
//      throw new UnsupportedOperationException();
//    }
//  }
//}
