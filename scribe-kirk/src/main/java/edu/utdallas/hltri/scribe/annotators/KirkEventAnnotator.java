package edu.utdallas.hltri.scribe.annotators;

import com.google.common.base.CharMatcher;
import edu.utdallas.hlt.i2b2.MetaMapPhrase;
import edu.utdallas.hlt.kiwi.i2b2.I2B2Documents;
import edu.utdallas.hlt.kiwi.i2b2.event.*;
import edu.utdallas.hlt.metamap_wrapper.MetaMapWrapper;
import edu.utdallas.hlt.util.Config;
import edu.utdallas.hltri.logging.Logger;
import edu.utdallas.hltri.scribe.io.JsonCorpus;
import edu.utdallas.hltri.scribe.kirk.EventClassifiers;
import edu.utdallas.hltri.scribe.kirk.KirkDocument;
import edu.utdallas.hltri.scribe.text.BaseDocument;
import edu.utdallas.hltri.scribe.text.Document;
import edu.utdallas.hltri.scribe.text.annotation.Event;
import edu.utdallas.hltri.scribe.text.annotation.Sentence;
import edu.utdallas.hltri.scribe.text.annotation.Token;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by rmm120030 on 6/30/15.
 */
public class KirkEventAnnotator<D extends BaseDocument> extends KirkAnnotator<D> {
  private static final Logger log = Logger.get(KirkConceptAnnotator.class);

  public final String annotationSetName;
  private final MachineEventRecognizer er;
  private final MachineEventTypeClassifier etc;
  private final MachineEventModalityClassifier emc;
  private final MachineEventPolarityClassifier epc;

  private KirkEventAnnotator(Builder<D> builder) {
    super(builder);
    this.annotationSetName = builder.annotationSetName;
    er = EventClassifiers.boundaryClassifier();
    er.loadModel(builder.boundaryModel);
    etc = EventClassifiers.typeClassifier();
    etc.loadModel(builder.typeModel);
    emc = EventClassifiers.modalityClassifier();
    emc.loadModel(builder.modalityModel);
    epc = EventClassifiers.polarityClassifier();
    epc.loadModel(builder.polarityModel);

    try {
      Config.init("edu.utdallas.hltri.scribe.kirk");
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public static class Builder<D extends BaseDocument> extends KirkAnnotator.Builder<D,Builder<D>> {
    private boolean modelsSet = false;
    private String boundaryModel = null;
    private String typeModel = null;
    private String modalityModel = null;
    private String polarityModel = null;
    private String annotationSetName = "i2b2";

    public Builder() {}

    @Override
    public Builder<D> self() {
      return this;
    }

    public Builder<D> boundaryModel(final String model) {
      this.boundaryModel = model;
      return self();
    }

    public Builder<D> typeModel(final String model) {
      this.typeModel = model;
      return self();
    }

    public Builder<D> modalityModel(final String model) {
      this.modalityModel = model;
      return self();
    }

    public Builder<D> polarityModel(final String model) {
      this.polarityModel = model;
      return self();
    }

    /**
     * Sets all the model files to those in the passed directory named "boudary.model", "type.model", "polarity.model",
     * and "modality.model".
     *
     * Does NOT override previously set model files. This enables loading a subset of the model files from a directory
     * while still setting other individual model files manually using boundaryModel(), typeModel(), etc.
     * @param modelDir directory containing correctly named model files.
     * @return self
     */
    public Builder<D> modelDir(String modelDir) {
      this.boundaryModel = (boundaryModel == null) ? modelDir + File.separator + "boundary.model" : boundaryModel;
      this.typeModel = (typeModel == null) ? modelDir + File.separator + "type.model": typeModel;
      this.polarityModel = (polarityModel == null) ? modelDir + File.separator + "polarity.model" : polarityModel;
      this.modalityModel = (modalityModel == null) ? modelDir + File.separator + "modality.model" : modalityModel;
      modelsSet = true;
      return self();
    }

    public Builder<D> annotationSetName(String annotationSetName) {
      this.annotationSetName = annotationSetName;
      return self();
    }

    @Override
    public KirkEventAnnotator<D> build() {
      if (tokenProvider == null) {
        throw new IllegalStateException("No token function provided");
      }
      if (sentenceProvider == null) {
        throw new IllegalStateException("No sentence function provided");
      }
      if (!modelsSet) {
        log.warn("No models given. Defaulting to i2b2...");
        final edu.utdallas.hltri.conf.Config config = edu.utdallas.hltri.conf.Config.load("annotators.event");
        modelDir(config.getString("i2b2"));
      }

      return new KirkEventAnnotator<>(self());
    }
  }

  @Override
  public <B extends D> void annotate(Document<B> document) {
    if (!document.getAnnotationSets().contains(annotationSetName)) {
      final edu.utdallas.hlt.text.Document kirkDoc = new KirkDocument.ToKirkBuilder<D>(document)
          .tokens(tokenProvider)
          .sentences(sentenceProvider)
          .buildWithGeniaFeatures();

      log.debug("Extracting events...");
      List<edu.utdallas.hlt.text.Event> kirkEvents = er.findEvents(kirkDoc);
      log.debug("Found {} events.", kirkEvents.size());

      log.debug("Labeling event types, modalities, and polarities...");
      for (edu.utdallas.hlt.text.Event ke : kirkEvents) {
        if (ke.getEndCharOffset() < document.length()) {
          final int endOffset =  (!CharMatcher.JAVA_LETTER_OR_DIGIT.matches(document.charAt(ke.getEndCharOffset()))) ?
              ke.getEndCharOffset() : ke.getEndCharOffset()-1;
          final Event event = Event.TYPE.create(document, annotationSetName, ke.getStartCharOffset(), endOffset);
          event.set(Event.type, etc.classifyType(ke));
          event.set(Event.modality, emc.classifyModality(ke));
          event.set(Event.polarity, epc.classifyPolarity(ke));
        }
      }
    }
  }

  @Override
  public void close() {}

  /////////////////////////////////////////////////////////////////////////////////
  // For retraining kirk's classifier models
  /////////////////////////////////////////////////////////////////////////////////
  /**
   * Retrain the SVM event type classifier
   * @param modelFile where the model will be saved
   * @param docs training documents
   */
  public static void retrainEventTypeClassifier(final String modelFile, final List<edu.utdallas.hlt.text.Document> docs) {
    final SVMEventTypeClassifier svm = EventClassifiers.typeClassifier();
    log.info("Training...");
    svm.setModelFile(modelFile);
    svm.train(docs);
    log.info("Done.");
  }

  /**
   * Retrain the SVM event modality classifier
   * @param modelFile where the model will be saved
   * @param docs training documents
   */
  public static void retrainEventModalityClassifier(final String modelFile, final List<edu.utdallas.hlt.text.Document> docs) {
    final SVMEventModalityClassifier svm = EventClassifiers.modalityClassifier();
    log.info("Training...");
    svm.setModelFile(modelFile);
    svm.train(docs);
    log.info("Done.");
  }

  /**
   * Retrain the SVM event polarity classifier
   * @param modelFile where the model will be saved
   * @param docs training documents
   */
  public static void retrainEventPolarityClassifier(final String modelFile, final List<edu.utdallas.hlt.text.Document> docs) {
    final SVMEventPolarityClassifier svm = EventClassifiers.polarityClassifier();
    log.info("Training...");
    svm.setModelFile(modelFile);
    svm.train(docs);
    log.info("Done.");
  }

  /**
   * Retrain the CRF event boundary classifier
   * @param modelFile where the model will be saved
   * @param docs training documents
   */
  public static void retrainEventBoundaryClassifier(final String modelFile, final List<edu.utdallas.hlt.text.Document> docs) {
    final MachineEventRecognizer crf = EventClassifiers.boundaryClassifier();
    log.info("Training...");
    crf.setModelFile(modelFile);
    crf.train(docs);
    log.info("Done.");
  }

  public static void metamap(String... args) {
    try {
      Config.init("edu.utdallas.hltri.scribe.kirk");

      final String host = "localhost";
      final int port = 8066;
      final String options = "-y -a --prune 10";

      log.info("MM Host Name: {}", host);
      log.info("MM Port: {}", port);
      final JsonCorpus<BaseDocument> corpus = JsonCorpus.at(args[0]).tiered().annotationSets("genia", "brat-gold").tiered().build();
      final MetaMapWrapper mmw = new MetaMapWrapper("bigmem06", 8066, "-y -a --prune 10");
      corpus.getIdStream("brat-gold")
          .map(corpus::load)
          .map(doc -> new KirkDocument.ToKirkBuilder<BaseDocument>(doc)
              .tokens(s -> s.getContained("genia", Token.TYPE))
              .sentences(d -> d.get("genia", Sentence.TYPE))
              .buildWithGeniaFeatures())
          .forEach(doc -> {
            log.info("Annotating {}", doc.getDocumentID());
            mmw.annotateDocument(doc);
            log.info("{}: found {} metamap annotations", doc.getDocumentID(), doc.getAnnotations(MetaMapPhrase.class));
          });
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public static void main(String... args) {
    final JsonCorpus<BaseDocument> corpus = JsonCorpus.at(args[0]).annotationSets("genia", "brat-gold").tiered().build();
    try {
      Config.init("edu.utdallas.hltri.scribe.kirk");
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    final List<edu.utdallas.hlt.text.Document> docs = corpus.getIdStream("brat-gold")
        .map(did -> {
          try (Document<BaseDocument> doc = corpus.load(did)){
//          for (final Event event : doc.get("brat-gold", Event.TYPE)) {
//            if (!event.get(Event.type).equals("PATIENT_STATE") && event.getOverlapping("brat-gold", Event.TYPE).size() > 1) {
//              log.info("Doc {}: Overlapping events: [{}] and {}", doc.get(BaseDocument.id), event.describe(), event.getOverlapping("brat-gold", Event.TYPE));
//              throw new RuntimeException();
//            }
//          }
            return KirkDocument.asKirk(doc, "genia", "genia", "brat-gold");
          }
        })
        .collect(Collectors.toList());

    docs.addAll(I2B2Documents.trainDocs());
    log.info("Training {} documents", docs.size());

//    retrainEventBoundaryClassifier(args[1], docs);
    retrainEventTypeClassifier(args[1], docs);
    retrainEventModalityClassifier(args[2], docs);
    retrainEventPolarityClassifier(args[3], docs);
  }
}
