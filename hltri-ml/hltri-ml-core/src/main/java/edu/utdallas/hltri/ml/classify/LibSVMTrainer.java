//package edu.utdallas.hltri.ml.classify;
//
//import cc.mallet.types.*;
//import com.google.common.base.*;
//import com.google.common.collect.*;
//import edu.utdallas.hltri.ml.*;
//import edu.utdallas.hltri.ml.feature.*;
//import edu.utdallas.hltri.util.*;
//import libsvm.*;
//
///**
// * User: bryan
// * Date: 5/14/13
// * Time: 1:15 PM
// * Created with IntelliJ IDEA.
// */
//public class LibSVMTrainer<I,C> implements ClassifierTrainer<I,C> {
//
//  private svm_parameter param = new svm_parameter();
//
//  public LibSVMTrainer() {
//    createParameters();
//  }
//
//  private svm_parameter createParameters() {
//    param.svm_type = param.C_SVC;
//    param.kernel_type = param.LINEAR;
//    param.degree = 3;
//    param.cache_size = 100;
//    param.C = 1;
//    param.nu = 0.5;
//    param.p = 0.1;
//    param.eps = .001;
//    param.gamma = 0; //1.0/valueIDs.size();
//    param.shrinking = 1;
//    param.coef0 = 0;
//    param.probability = 0;
//    return param;
//  }
//
//  public svm_parameter getParams() {
//    return param;
//  }
//
//  private svm_problem createProblem(Iterable<? extends I> trainingSet, Function<? super I, ? extends C> targetFunc,
//                                    ClassificationMetadata<I,C> classificationData) {
//    svm_node[][] nodes = new svm_node[Iterables.size(trainingSet)][];
//    double[] y = new double[nodes.length];
//
//    int instIdx = 0;
//    for (I instance : trainingSet) {
//      nodes[instIdx] = createNodes(classificationData.getVector(instance, true));
//      y[instIdx++] = classificationData.classIDs().getIDOrAdd(targetFunc.apply(instance));
//    }
//
//    svm_problem problem = new svm_problem();
//    problem.x = nodes;
//    problem.y = y;
//    problem.l = nodes.length;
//    return problem;
//  }
//
//  public static svm_node[] createNodes(SparseVector vector) {
//    svm_node[] nodes = new svm_node[vector.numLocations()];
//    for (int i = 0; i < nodes.length; i++) {
//      svm_node node = new svm_node();
//      node.index = vector.indexAtLocation(i);
//      node.value = vector.valueAtLocation(i);
//      nodes[i] = node;
//    }
//    return nodes;
//  }
//
//  @Override
//  public LibSVMClassifier<I, C> train(Iterable<? extends I> trainingSet, Function<? super I, ? extends C> targetFunc,
//                                Iterable<? extends FeatureExtractor<? super I, ?>> featureExtractors, MLConfig config) {
//
//    ClassificationMetadata<I,C> classificationData = new ClassificationMetadata<>(featureExtractors,
//        new DefaultFeatureVectorizer().addBias(), new IntIdentifier<C>());
//
//    svm_problem problem = createProblem(trainingSet, targetFunc, classificationData);
//
//    param.gamma = 1.0 / classificationData.vectorizer().numDimensions();
//
//    String checkResult = svm.svm_check_parameter(problem, param);
//    if (checkResult != null) System.err.println("LibSVMTrainer:\n" + checkResult);
//
//
//    svm_model model = svm.svm_train(problem, param);
//
//    return new LibSVMClassifier<I, C>(model,classificationData);
//  }
//}
