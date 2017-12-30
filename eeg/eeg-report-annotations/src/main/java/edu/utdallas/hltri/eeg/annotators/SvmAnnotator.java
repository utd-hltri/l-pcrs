package edu.utdallas.hltri.eeg.annotators;

import edu.utdallas.hltri.logging.Logger;
import edu.utdallas.hltri.ml.Feature;
import edu.utdallas.hltri.ml.FeatureExtractor;
import edu.utdallas.hltri.ml.classify.LibLinearSvm;
import edu.utdallas.hltri.ml.vector.SparseFeatureVector;
import edu.utdallas.hltri.ml.vector.SparseFeatureVectorizer;
import edu.utdallas.hltri.scribe.annotators.Annotator;
import edu.utdallas.hltri.scribe.text.BaseDocument;
import edu.utdallas.hltri.scribe.text.Document;
import edu.utdallas.hltri.util.IntIdentifier;

import java.util.Collection;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * Created by ramon on 2/25/16.
 */
public class SvmAnnotator<A, D extends BaseDocument> implements Annotator<D> {
  private static final Logger log = Logger.get(SvmAnnotator.class);

  protected LibLinearSvm svm;
  protected final Function<Document<? extends D>, List<A>> annotationSupplier;
  protected final Collection<FeatureExtractor<A,?>> extractors;
  protected final BiConsumer<A, Double> labeler;
  protected final SparseFeatureVectorizer<Number> vectorizer;

  public SvmAnnotator(final Collection<FeatureExtractor<A,?>> extractors,
                      final LibLinearSvm svm,
                      final BiConsumer<A, Double> labeler,
                      final Function<Document<? extends D>, List<A>> annotationSupplier,
                      final IntIdentifier<String> featureIdentifier) {
    this.extractors = extractors;
    this.svm = svm;
    this.labeler = labeler;
    this.annotationSupplier = annotationSupplier;
    vectorizer = new SparseFeatureVectorizer<>(featureIdentifier);
  }

  @Override
  public <B extends D> void annotate(final Document<B> document) {
    for (final A ann : annotationSupplier.apply(document)) {
      SparseFeatureVector<Number> fv = vectorizer.vectorize(
          extractors.stream().flatMap(fe -> fe.apply(ann)).map(Feature::toNumericFeature));
      if (fv.ids().length < 1) {
        fv = vectorizer.emptyVector(0, 0);
      }
      log.trace("|{}| has feature vector: {}", ann.toString(), fv);
      labeler.accept(ann, svm.classify(fv));
    }
  }
}
