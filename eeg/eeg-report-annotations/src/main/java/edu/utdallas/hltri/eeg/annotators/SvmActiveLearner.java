package edu.utdallas.hltri.eeg.annotators;

import com.google.common.collect.Lists;

import edu.utdallas.hltri.eeg.al.ActiveLearner;
import edu.utdallas.hltri.logging.Logger;
import edu.utdallas.hltri.ml.Feature;
import edu.utdallas.hltri.ml.FeatureExtractor;
import edu.utdallas.hltri.ml.classify.LibLinearSvm;
import edu.utdallas.hltri.ml.vector.SparseFeatureVector;
import edu.utdallas.hltri.scribe.text.BaseDocument;
import edu.utdallas.hltri.scribe.text.Document;
import edu.utdallas.hltri.struct.Pair;
import edu.utdallas.hltri.util.IntIdentifier;

import java.util.Collection;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * Created by rmm120030 on 7/26/16.
 */
public class SvmActiveLearner<A, D extends BaseDocument> extends SvmAnnotator<A,D> implements ActiveLearner<A,D> {
  private static final Logger log = Logger.get(SvmActiveLearner.class);

  private final int numLabels;

  public SvmActiveLearner(final Collection<FeatureExtractor<A,?>> featureExtractors,
                          final LibLinearSvm svm,
                          final BiConsumer<A, Double> labeler,
                          final Function<Document<? extends D>, List<A>> annotationSupplier,
                          final IntIdentifier<String> featureIdentifier,
                          final int numLabels) {
    super(featureExtractors, svm, labeler, annotationSupplier, featureIdentifier);
    this.numLabels = numLabels;
  }

  public <B extends D> List<Pair<A,double[]>> annotateWithConfidence(final Document<B> document) {
    final List<Pair<A,double[]>> confidences = Lists.newArrayList();
    for (final A ann : annotationSupplier.apply(document)) {
      SparseFeatureVector<Number> fv = vectorizer.vectorize(
          extractors.stream().flatMap(fe -> fe.apply(ann)).map(Feature::toNumericFeature));
      if (fv.ids().length < 1) {
        fv = vectorizer.emptyVector(0, 0);
      }
      labeler.accept(ann, svm.classify(fv));
      confidences.add(new Pair<>(ann, svm.labelProbabilities(fv, numLabels)));
    }
    return confidences;
  }
}
