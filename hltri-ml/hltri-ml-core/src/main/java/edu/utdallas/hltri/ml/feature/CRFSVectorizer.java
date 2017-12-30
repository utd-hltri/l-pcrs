//package edu.utdallas.hltri.ml.feature;
//
//import edu.utdallas.hltri.logging.Logger;
//import edu.utdallas.hltri.ml.Feature;
//import edu.utdallas.hltri.util.IntIdentifier;
//import edu.utdallas.hltri.ml.label.EnumLabel;
//
//import java.util.Collection;
//import java.util.function.Function;
//
///**
// * Created by rmm120030 on 1/29/16.
// */
//public class CRFSVectorizer<T> extends AnnotationVectorizer<T> {
//  private static final Logger log = Logger.get(CRFSVectorizer.class);
//
//  public CRFSVectorizer(final Collection<Function<T, Collection<? extends Feature<?>>>> featureExtractors,
//                        final Function<T, ? extends EnumLabel> labelExtractor, final IntIdentifier<String> identifier) {
//    super(featureExtractors, labelExtractor, identifier);
//    this.stringFun = OldFeatureVector::toCrfSuiteString;
//    this.featureExtension = ".vec";
//    this.mappingExtension = ".tsv";
//  }
//}
