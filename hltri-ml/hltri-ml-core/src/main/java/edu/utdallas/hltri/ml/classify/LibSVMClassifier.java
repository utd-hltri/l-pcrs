//package edu.utdallas.hltri.ml.classify;
//
//import cc.mallet.types.*;
//import edu.utdallas.hltri.ml.*;
//import libsvm.*;
//
///**
// * User: bryan
// * Date: 5/14/13
// * Time: 1:13 PM
// * Created with IntelliJ IDEA.
// */
//public class LibSVMClassifier<I,C> implements Classifier<I,C> {
//  private final svm_model model;
//  private final ClassificationMetadata<I,C> classificationData;
//
//  /** If svm_predict returns i, it actually means the class at classes[i] */
//  private final int[] classes;
//
//  public LibSVMClassifier(svm_model model, ClassificationMetadata<I, C> classificationData) {
//    this.model = model;
//    this.classificationData = classificationData;
//    this.classes = new int[classificationData.classIDs().size()];
//    svm.svm_get_labels(model, classes);
//  }
//
//  @Override
//  public C classify(I instance) {
//
//    SparseVector vector = classificationData.getVector(instance, false);
//    svm_node[] nodes = LibSVMTrainer.createNodes(vector);
//
//    double prediction = svm.svm_predict(model, nodes);
//
//    return classificationData.classIDs().get(classes[(int) prediction]);
//  }
//}
