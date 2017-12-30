package edu.utdallas.hltri.eeg.annotators;

import com.google.common.collect.Lists;

import edu.utdallas.hltri.conf.Config;
import edu.utdallas.hltri.eeg.annotation.EegActivity;
import edu.utdallas.hltri.eeg.annotation.EegActivity.*;
import edu.utdallas.hltri.eeg.classifier.Crf;
import edu.utdallas.hltri.eeg.classifier.Svm;
import edu.utdallas.hltri.logging.Logger;
import edu.utdallas.hltri.scribe.annotators.Annotator;
import edu.utdallas.hltri.scribe.text.BaseDocument;
import edu.utdallas.hltri.scribe.text.Document;
import edu.utdallas.hltri.scribe.text.DuplicateAnnotationException;
import edu.utdallas.hltri.scribe.text.annotation.Event;

import java.nio.file.Path;
import java.util.List;

/**
 * This annotator annotates all Medical Concepts and their attributes found in EegNotes
 * Created by rmm120030 on 3/2/16.
 */
public class EegConceptAnnotator<T extends BaseDocument> implements Annotator<T> {
  private static final Logger log = Logger.get(EegConceptAnnotator.class);

  private final CrfsEventBoundaryAnnotator<T> eventBoundaryAnnotator;
  private final CrfsEventBoundaryAnnotator<T> activityBoundaryAnnotator;
  private final TfBoundaryAnnotator<T> tfBoundaryAnnotator;
  private final SvmAnnotator<Event, T> typeAnnotator;
  private final List<SvmAnnotator<EegActivity, T>> attrAnnotators;
  private final String annset;
  private final SvmAnnotator<Event, T> evPolAnnotator;
  private final SvmAnnotator<Event, T> evModAnnotator;
  private boolean clear = false;
  private boolean useLstm;

  public static <T extends BaseDocument> EegConceptAnnotator<T> best(final String annset) {
    return new EegConceptAnnotator<>(Config.load("eeg").getPath("al.best-model"), annset);
  }

  public static <T extends BaseDocument> EegConceptAnnotator<T> load(final Path modelDir,
                                                                     final String annset) {
    return new EegConceptAnnotator<>(modelDir, annset);
  }

  public static <T extends BaseDocument> EegConceptAnnotator<T> bestWithLstm(final String annset) {
    final Config conf = Config.load("eeg");
    return new EegConceptAnnotator<>(conf.getPath("al.best-model"), annset,
        conf.getString("nn.boundary.url"), conf.getString("nn.boundary.featuremap"));
  }

  @SuppressWarnings("unused")
  public static <T extends BaseDocument> EegConceptAnnotator<T> loadWithLstm(
      final Path modelDir, final String annset, final String tfServerUrl, final String tfFeatureMapFile) {
    return new EegConceptAnnotator<>(modelDir, annset, tfServerUrl, tfFeatureMapFile);
  }



  private EegConceptAnnotator(final Path modelDir, final String annset, final String tfServerUrl,
                              final String tfFeatureMapFile) {
    this.annset = annset;
    this.useLstm = true;

    eventBoundaryAnnotator = null;
    activityBoundaryAnnotator = null;
    tfBoundaryAnnotator = new TfBoundaryAnnotator<>(tfServerUrl, tfFeatureMapFile, annset);

    typeAnnotator = Svm.loadEventTypeSvm(modelDir, annset);
    evModAnnotator = Svm.loadEventModalitySvm(modelDir, annset);
    evPolAnnotator = Svm.loadEventPolaritySvm(modelDir, annset);

    attrAnnotators = Lists.newArrayList();
    attrAnnotators.add(Svm.loadActivityAttributeSvm(modelDir, "morphology", annset));
    attrAnnotators.add(Svm.loadActivityAttributeSvm(modelDir, "band", annset));
    attrAnnotators.add(Svm.loadActivityAttributeSvm(modelDir, "hemisphere", annset));
    attrAnnotators.add(Svm.loadActivityAttributeSvm(modelDir, "dispersal", annset));
    attrAnnotators.add(Svm.loadActivityAttributeSvm(modelDir, "recurrence", annset));
    attrAnnotators.add(Svm.loadActivityAttributeSvm(modelDir, "magnitude", annset));
    attrAnnotators.add(Svm.loadActivityAttributeSvm(modelDir, "in_background", annset));
    attrAnnotators.add(Svm.loadActivityAttributeSvm(modelDir, "activity_modality", annset));
    attrAnnotators.add(Svm.loadActivityAttributeSvm(modelDir, "activity_polarity", annset));
    for (Location location : Location.values()) {
      attrAnnotators.add(Svm.loadLocationSvm(modelDir, location.toString(), annset));
    }
  }

  private EegConceptAnnotator(final Path modelDir, final String annset) {
    this.annset = annset;
    this.useLstm = false;

    eventBoundaryAnnotator = Crf.loadEventBoundaryAnnotator(annset, modelDir);
    activityBoundaryAnnotator = Crf.loadActivityBoundaryAnnotator(annset, modelDir);
    tfBoundaryAnnotator = null;

    typeAnnotator = Svm.loadEventTypeSvm(modelDir, annset);
    evModAnnotator = Svm.loadEventModalitySvm(modelDir, annset);
    evPolAnnotator = Svm.loadEventPolaritySvm(modelDir, annset);

    attrAnnotators = Lists.newArrayList();
    attrAnnotators.add(Svm.loadActivityAttributeSvm(modelDir, "morphology", annset));
    attrAnnotators.add(Svm.loadActivityAttributeSvm(modelDir, "band", annset));
    attrAnnotators.add(Svm.loadActivityAttributeSvm(modelDir, "hemisphere", annset));
    attrAnnotators.add(Svm.loadActivityAttributeSvm(modelDir, "dispersal", annset));
    attrAnnotators.add(Svm.loadActivityAttributeSvm(modelDir, "recurrence", annset));
    attrAnnotators.add(Svm.loadActivityAttributeSvm(modelDir, "magnitude", annset));
    attrAnnotators.add(Svm.loadActivityAttributeSvm(modelDir, "in_background", annset));
    attrAnnotators.add(Svm.loadActivityAttributeSvm(modelDir, "activity_modality", annset));
    attrAnnotators.add(Svm.loadActivityAttributeSvm(modelDir, "activity_polarity", annset));
    for (Location location : Location.values()) {
      attrAnnotators.add(Svm.loadLocationSvm(modelDir, location.toString(), annset));
    }
  }

  public void clear() {
    this.clear = true;
  }

  @Override
  public <B extends T> void annotate(Document<B> document) {
    if (clear || document.get(annset, Event.TYPE).isEmpty() || document.get(annset, EegActivity.TYPE).isEmpty()) {
      document.clear(annset);
      try {
        if (useLstm) {
          tfBoundaryAnnotator.annotate(document);
        } else {
          eventBoundaryAnnotator.annotate(document);
          activityBoundaryAnnotator.annotate(document);
        }
        log.debug("Annotated {} events.", document.get(annset, Event.TYPE).size());
        log.debug("Annotated {} activities.", document.get(annset, EegActivity.TYPE).size());
        typeAnnotator.annotate(document);
        log.debug("Annotated event types.");
      } catch (DuplicateAnnotationException e) {
        log.warn("Duplicate annotation exception ignored.");
      }
    }
    attrAnnotators.forEach(ann -> {
      log.debug("Annotating attr {}", attrAnnotators.indexOf(ann));
      ann.annotate(document);
    });
    log.debug("Annotating event modality...");
    evModAnnotator.annotate(document);
    log.debug("Annotating event polarity...");
    evPolAnnotator.annotate(document);
  }
}
