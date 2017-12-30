package edu.utdallas.hltri.eeg.classifier;

import edu.utdallas.hltri.eeg.EegNote;
import edu.utdallas.hltri.eeg.feature.FeatureUtils;
import edu.utdallas.hltri.eeg.annotation.EegActivity;
import edu.utdallas.hltri.eeg.annotators.CrfsEventBoundaryAnnotator;
import edu.utdallas.hltri.logging.Logger;
import edu.utdallas.hltri.ml.*;
import edu.utdallas.hltri.ml.classify.CrfsFeatureVector;
import edu.utdallas.hltri.ml.classify.CrfsWrapper;
import edu.utdallas.hltri.ml.label.IobLabel;
import edu.utdallas.hltri.ml.vector.SparseFeatureVector;
import edu.utdallas.hltri.ml.vector.SparseFeatureVectorizer;
import edu.utdallas.hltri.scribe.text.BaseDocument;
import edu.utdallas.hltri.scribe.text.Document;
import edu.utdallas.hltri.scribe.text.annotation.AnnotationType;
import edu.utdallas.hltri.scribe.text.annotation.Event;
import edu.utdallas.hltri.scribe.text.annotation.Sentence;
import edu.utdallas.hltri.scribe.text.annotation.Token;
import edu.utdallas.hltri.util.IntIdentifier;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Utilities for loading/training CRF Suite backed CRFs for clinical concept classification
 * Created by rmm120030 on 9/7/16.
 */
public class Crf {
  private static final Logger log = Logger.get(Crf.class);

  public static void trainEventBoundaries(final List<Document<EegNote>> train, final String modelDir, final String annset) {
    log.info("Training Event Boundaries...");
    trainBoundaries(train, modelDir, "event", annset, Event.TYPE);
  }

  public static void trainActivityBoundaries(final List<Document<EegNote>> train, final String modelDir, final String annset) {
    log.info("Training Activity Boundaries...");
    trainBoundaries(train, modelDir, "activity", annset, EegActivity.TYPE);
  }

  private static void trainBoundaries(final List<Document<EegNote>> train, final String modelDir, final String boundaryType,
                                     final String annset, final AnnotationType<?> type) {
    final IntIdentifier<String> featureIdentifier = new IntIdentifier<>();
    final SparseFeatureVectorizer<Number> vectorizer = new SparseFeatureVectorizer<>(featureIdentifier);
    final Collection<FeatureExtractor<Token, ?>> featureExtractors = FeatureUtils.boundaryDetectionFeatureExtractors();
    final List<List<CrfsFeatureVector>> vectors = train.stream().flatMap(document -> document.get("opennlp", Sentence.TYPE).stream())
        .map(s -> {
          final List<CrfsFeatureVector> fvs = new ArrayList<>();
          for (Token token : s.getContained("genia", Token.TYPE)) {
            final IobLabel label = FeatureUtils.boundaryLabeler(annset, type).apply(token);
            final SparseFeatureVector<Number> fv = vectorizer.vectorize(featureExtractors.stream().flatMap(fe -> fe.apply(token))
                .map(Feature::toNumericFeature));
            fvs.add(new CrfsFeatureVector(label, fv));
          }
          return fvs;
        }).collect(Collectors.toList());

    CrfsWrapper.train(modelDir + File.separator + boundaryType + "-boundary.model",
        modelDir + File.separator + boundaryType + "-boundary.tsv",
        featureIdentifier,
        vectors,
        false);
  }

  public static <T extends BaseDocument> CrfsEventBoundaryAnnotator<T> loadActivityBoundaryAnnotator(String annset, Path modelPath) {
    return new CrfsEventBoundaryAnnotator.Builder<T>()
        .annset(annset)
        .type(EegActivity.TYPE)
        .featureExtractors(FeatureUtils.boundaryDetectionFeatureExtractors())
        .featureMapFile(modelPath.resolve("activity-boundary.tsv").toString())
        .model(modelPath.resolve("activity-boundary.model").toString())
        .sentences("opennlp")
        .tokens("genia")
        .build();
  }

  public static <T extends BaseDocument> CrfsEventBoundaryAnnotator<T> loadEventBoundaryAnnotator(String annset, Path modelPath) {
    return new CrfsEventBoundaryAnnotator.Builder<T>()
        .annset(annset)
        .type(Event.TYPE)
        .featureExtractors(FeatureUtils.boundaryDetectionFeatureExtractors())
        .featureMapFile(modelPath.resolve("event-boundary.tsv").toString())
        .model(modelPath.resolve("event-boundary.model").toString())
        .sentences("opennlp")
        .tokens("genia")
        .build();
  }
}
