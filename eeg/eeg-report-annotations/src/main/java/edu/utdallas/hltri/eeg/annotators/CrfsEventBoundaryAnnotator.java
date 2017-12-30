package edu.utdallas.hltri.eeg.annotators;

import com.google.common.collect.Lists;
import edu.utdallas.hltri.eeg.al.ActiveLearner;
import edu.utdallas.hltri.logging.Logger;
import edu.utdallas.hltri.ml.Feature;
import edu.utdallas.hltri.ml.FeatureExtractor;
import edu.utdallas.hltri.ml.vector.SparseFeatureVector;
import edu.utdallas.hltri.ml.vector.SparseFeatureVectorizer;
import edu.utdallas.hltri.ml.classify.CrfsFeatureVector;
import edu.utdallas.hltri.ml.classify.CrfsWrapper;
import edu.utdallas.hltri.ml.label.IobLabel;
import edu.utdallas.hltri.scribe.annotators.Annotator;
import edu.utdallas.hltri.scribe.text.BaseDocument;
import edu.utdallas.hltri.scribe.text.Document;
import edu.utdallas.hltri.scribe.text.DuplicateAnnotationException;
import edu.utdallas.hltri.scribe.text.annotation.*;
import edu.utdallas.hltri.struct.Pair;
import edu.utdallas.hltri.util.IntIdentifier;
import edu.utdallas.hltri.ml.classify.IobSequenceChunker;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Created by ramon on 2/14/16.
 */
public class CrfsEventBoundaryAnnotator<D extends BaseDocument> implements ActiveLearner<Token,D> {
  private static final Logger log = Logger.get(CrfsEventBoundaryAnnotator.class);

  private final String annset;
  private final String tokenAnnSet;
  private final String sentenceAnnSet, modelPath;
  private Path outDir;
  private Collection<FeatureExtractor<Token,?>> featureExtractors;
  private SparseFeatureVectorizer<Number> vectorizer;
  private final AnnotationType<? extends Annotation<?>> annotationType;
  private final Function<Document<? extends D>, List<List<Token>>> tokenSupplier;

  private CrfsEventBoundaryAnnotator(final Builder builder) {
    this.annset = builder.annset;
    this.tokenAnnSet = builder.tokenAnnSet;
    this.sentenceAnnSet = builder.sentenceAnnSet;
    this.annotationType = builder.annotationType;
    this.modelPath = builder.modelPath;

    tokenSupplier = doc -> doc.get(sentenceAnnSet, Sentence.TYPE).stream()
        .map(s -> s.getContained(tokenAnnSet, Token.TYPE)).collect(Collectors.toList());

    final IntIdentifier<String> identifier = IntIdentifier.fromFile(builder.featureMapFile).lock();
    this.featureExtractors = builder.featureExtractors;
    try {
      outDir = (builder.outDir == null) ? Files.createTempDirectory(annset) : Paths.get(builder.outDir);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    assert outDir != null : "outDir was not assigned.";

    this.vectorizer = new SparseFeatureVectorizer<>(identifier);

    log.info("Loaded CRFS model from {} with feature map at {}.", builder.modelPath, builder.featureMapFile);
  }

  public static class Builder<D extends BaseDocument> extends Annotator.Builder<D,Builder<D>> {
    private String tokenAnnSet = "genia";
    private String sentenceAnnSet = "opennlp";
    private String modelPath;
    private String outDir = null;
    private String annset = "crfs-event";
    private Collection<FeatureExtractor<Token,?>> featureExtractors;
    private String featureMapFile;
    private AnnotationType<? extends Annotation<?>> annotationType = Event.TYPE;

    public Builder<D> featureMapFile(final String featureMapFile) {
      this.featureMapFile = featureMapFile;
      return self();
    }

    public Builder<D> featureExtractors(final Collection<FeatureExtractor<Token,?>> extractors) {
      if (featureExtractors == null) {
        featureExtractors = extractors;
      }
      else {
        featureExtractors.addAll(extractors);
      }
      return self();
    }

    public Builder<D> featureExtractor(final FeatureExtractor<Token,?> extractor) {
      if (featureExtractors == null) {
        featureExtractors = Arrays.asList(extractor);
      }
      else {
        featureExtractors.add(extractor);
      }
      return self();
    }

    public Builder<D> annset(final String annset) {
      this.annset = annset;
      return self();
    }

    public Builder<D> saveOutput(final String outDir) {
      this.outDir = outDir;
      return self();
    }

    public Builder<D> model(final String modelPath) {
      this.modelPath = modelPath;
      return self();
    }

    public Builder<D> tokens(final String tokenAnnSet) {
      this.tokenAnnSet = tokenAnnSet;
      return self();
    }

    public Builder<D> sentences(final String sentenceAnnSet) {
      this.sentenceAnnSet = sentenceAnnSet;
      return self();
    }

    public Builder<D> type(final AnnotationType<? extends Annotation<?>> type) {
      this.annotationType = type;
      return self();
    }

    @Override
    protected Builder<D> self() {
      return this;
    }

    @Override
    public CrfsEventBoundaryAnnotator<D> build() {
      assert modelPath != null : "Model path not set.";
      assert Files.exists(Paths.get(modelPath)) : "No CRFS model at " + modelPath;
      assert featureMapFile != null : "No feature map file provided. Provide the feature map file of the data the CRFS model was trained on.";
      assert Files.exists(Paths.get(featureMapFile)) : "No feature map file at " + featureMapFile;
      assert featureExtractors != null : "No feature extractors added.";

      return new CrfsEventBoundaryAnnotator<>(self());
    }
  }

  @Override
  public <B extends D> void annotate(Document<B> document) {
    annotateWithConfidence(document);
  }

  @Override
  public <B extends D> List<Pair<Token, double[]>> annotateWithConfidence(Document<B> document) {
    final List<Pair<Token, double[]>> tokensWithConfidence = Lists.newArrayList();
    final List<List<Token>> sentences = tokenSupplier.apply(document);
    final List<List<CrfsFeatureVector>> sequences = sentences.stream().map(sequence ->
        sequence.stream().map(token -> {
          final SparseFeatureVector<Number> vector = vectorizer.vectorize(featureExtractors.stream()
              .flatMap(fe -> fe.apply(token)).map(Feature::toNumericFeature));
          return new CrfsFeatureVector(IobLabel.O, vector);
        }).collect(Collectors.toList())).collect(Collectors.toList());
    final List<List<Pair<String, Double>>> predictions = CrfsWrapper.predict(sequences, modelPath, outDir, document.getId());

    assert predictions.size() == sentences.size() : "Different number of input/ouput sequences to/from CRF Suite Wrapper";
    final AtomicInteger count = new AtomicInteger();
//    boolean inEvent = false;
    final IobSequenceChunker<Token> slc = new IobSequenceChunker<Token>() {
      @Override
      public void combine(int start, int end, String label) {
        try {
          final Annotation<?> a = annotationType.create(document, annset, start, end);
          count.incrementAndGet();
          log.trace("Created {}: {}", annotationType.getName(), a);
        } catch (DuplicateAnnotationException e) {
          log.warn("Ignoring duplicate annotation exception...");
        }
      }

      @Override
      public int getStart(Token token) {
        return token.get(Token.StartOffset).intValue();
      }

      @Override
      public int getEnd(Token token) {
        return token.get(Token.EndOffset).intValue();
      }
    };

    // for each sentence
    for (int i = 0; i < predictions.size(); i++) {
      final List<Token> sentence = sentences.get(i);
      final List<Pair<String, Double>> labeledSentence = predictions.get(i);
      assert sentence.size() == labeledSentence.size() : "Different number of tokens in sentence " + i;
      for (int j = 0; j < sentence.size(); j++) {
        final Token token = sentence.get(j);
        final Pair<String, Double> labeledToken = labeledSentence.get(j);
        final Double labelProb = labeledToken.second();
        switch (labeledToken.first()) {
          case "B":
            slc.processToken(token, IobLabel.B);
            tokensWithConfidence.add(Pair.of(token, new double[]{labelProb, (1 - labelProb) / 2, (1 - labelProb) / 2}));
            break;
          case "I":
            slc.processToken(token, IobLabel.I);
            tokensWithConfidence.add(Pair.of(token, new double[]{(1 - labelProb) / 2, labelProb, (1 - labelProb) / 2}));
            break;
          case "O":
            slc.processToken(token, IobLabel.O);
            tokensWithConfidence.add(Pair.of(token, new double[]{1 - labelProb, 0.0, labelProb}));
            break;
          default:
            throw new RuntimeException("Unexpected fineGrainedLabel: " + labeledToken.first());
        }
      }
      slc.close();
    }
    log.trace("Found {} {}s in doc {}", count.get(), annotationType.getName(), document.getId());
    return tokensWithConfidence;
  }

  public String annset() {
    return annset;
  }
}
