// TODO: pls fix me, ramon, pls (´・ω・`)
// package edu.utdallas.hltri.eeg.annotators;
//
//import edu.utdallas.hltri.eeg.*;
//import edu.utdallas.hltri.eeg.annotation.EegActivity;
//import edu.utdallas.hltri.eeg.annotation.EegActivity.*;
//import edu.utdallas.hltri.eeg.annotation.label.EventTypeLabel;
//import edu.utdallas.hltri.logging.Logger;
//import ml.classify.LibLinearSvm;
//import edu.utdallas.hltri.ml.Label;
//import ml.feature.AnnotationVectorizer;
//import edu.utdallas.hltri.scribe.annotators.Annotator;
//import edu.utdallas.hltri.scribe.text.Document;
//import edu.utdallas.hltri.scribe.text.DuplicateAnnotationException;
//import edu.utdallas.hltri.scribe.text.annotation.Event;
//import edu.utdallas.hltri.util.IntIdentifier;
//import edu.utdallas.hltri.eeg.Annotate;
//import edu.utdallas.hltri.eeg.Features;
//
//import java.nio.file.Path;
//import java.nio.file.Paths;
//import java.util.ArrayList;
//import java.util.List;
//
///**
// * Created by rmm120030 on 3/2/16.
// */
//public class EegEventAnnotator implements Annotator<EegNote> {
//  private static final Logger log = Logger.get(EegEventAnnotator.class);
//
//  private final CrfsEventBoundaryAnnotator<EegNote> eventBoundaryAnnotator;
//  private final CrfsEventBoundaryAnnotator<EegNote> activityBoundaryAnnotator;
//  private final SvmAnnotator<Event, EegNote> typeAnnotator;
//  private final List<SvmAnnotator<EegActivity, EegNote>> attrAnnotators;
//  private final String annset;
//  private final SvmAnnotator<EegActivity, EegNote> actPolAnnotator;
//  private final SvmAnnotator<Event, EegNote> evPolAnnotator;
//  private final SvmAnnotator<EegActivity, EegNote> actModAnnotator;
//  private final SvmAnnotator<Event, EegNote> evModAnnotator;
//
//  public EegEventAnnotator(final String modelDir, final String annset) {
//    this.annset = annset;
//    final Path modelPath = Paths.get(modelDir);
//    this.eventBoundaryAnnotator = new CrfsEventBoundaryAnnotator.Builder<EegNote>()
//        .annset(annset)
//        .type(Event.TYPE)
//        .featureExtractors(Features.tokenFeatureExtractors())
//        .featureMapFile(modelPath.resolve("event-boundary.tsv").toString())
//        .model(modelPath.resolve("event-boundary.model").toString())
//        .sentences("opennlp")
//        .tokens("genia")
//        .build();
//
//    this.activityBoundaryAnnotator = new CrfsEventBoundaryAnnotator.Builder<EegNote>()
//        .annset(annset)
//        .type(EegActivity.TYPE)
//        .featureExtractors(Features.tokenFeatureExtractors())
//        .featureMapFile(modelPath.resolve("activity-boundary.tsv").toString())
//        .model(modelPath.resolve("activity-boundary.model").toString())
//        .sentences("opennlp")
//        .tokens("genia")
//        .build();
//
//    this.typeAnnotator =
//        new SvmAnnotator<>(new AnnotationVectorizer<>(Features.<Event>attributeFeatureExtractors(),
//          e -> EventTypeLabel.NULL,
//          IntIdentifier.fromFile(modelPath.resolve("type.tsv").toString())),
//        LibLinearSvm.load(modelPath.resolve("type.model").toString()),
//        Annotate.typeLabeler(EventTypeLabel.values(), Event.type),
//        doc -> doc.get(annset, Event.TYPE));
//
//    this.attrAnnotators = new ArrayList<>();
//    attrAnnotators.add(Annotate.loadActivityAttributeSvm(modelPath, "morphology", annset, a -> Morphology.NULL,
//        Annotate.typeLabeler(Morphology.values(), EegActivity.morphology), Morphology.RHYTHM.numLabels()));
//    attrAnnotators.add(Annotate.loadActivityAttributeSvm(modelPath, "band", annset, a -> Band.NULL,
//        Annotate.typeLabeler(Band.values(), EegActivity.band), Band.NA.numLabels()));
////    attrAnnotators.add(Annotate.loadActivityAttributeSvm(modelPath, "location", annset, a -> Location.NULL,
////        Annotate.attributeSetter(Location.values(), EegActivity.location), Location.NA.numLabels()));
//    attrAnnotators.add(Annotate.loadActivityAttributeSvm(modelPath, "hemisphere", annset, a -> Label.NULL,
//        Annotate.typeLabeler(Hemisphere.values(), EegActivity.hemisphere), Hemisphere.NA.numLabels()));
//    attrAnnotators.add(Annotate.loadActivityAttributeSvm(modelPath, "recurrence", annset, a -> Label.NULL,
//        Annotate.typeLabeler(Recurrence.values(), EegActivity.recurrence), Recurrence.NONE.numLabels()));
//    attrAnnotators.add(Annotate.loadActivityAttributeSvm(modelPath, "dispersal", annset, a -> Label.NULL,
//        Annotate.typeLabeler(Dispersal.values(), EegActivity.dispersal), Dispersal.NA.numLabels()));
//    attrAnnotators.add(Annotate.loadActivityAttributeSvm(modelPath, "magnitude", annset, a -> Label.NULL,
//        Annotate.typeLabeler(Magnitude.values(), EegActivity.magnitude), Magnitude.NORMAL.numLabels()));
//    attrAnnotators.add(Annotate.loadActivityAttributeSvm(modelPath, "in_background", annset, a -> Label.NULL,
//        Annotate.typeLabeler(In_Background.values(), EegActivity.in_background), In_Background.NO.numLabels()));
//
//
//    this.actPolAnnotator = new SvmAnnotator<>(new AnnotationVectorizer<>(Features.<EegActivity>attributeFeatureExtractors(),
//          a -> Label.NULL,
//          IntIdentifier.fromFile(modelPath.resolve("polarity.tsv").toString())),
//        LibLinearSvm.load(modelPath.resolve("polarity.model").toString()),
//        Annotate.polarityLabeler(),
//        doc -> doc.get(annset, EegActivity.TYPE));
//
//    this.evPolAnnotator = new SvmAnnotator<>(new AnnotationVectorizer<>(Features.<Event>attributeFeatureExtractors(),
//          a -> Label.NULL,
//          IntIdentifier.fromFile(modelPath.resolve("polarity.tsv").toString())),
//        LibLinearSvm.load(modelPath.resolve("polarity.model").toString()),
//        Annotate.polarityLabeler(),
//        doc -> doc.get(annset, Event.TYPE));
//
//    this.actModAnnotator = new SvmAnnotator<>(new AnnotationVectorizer<>(Features.<EegActivity>attributeFeatureExtractors(),
//          a -> Label.NULL,
//          IntIdentifier.fromFile(modelPath.resolve("modality.tsv").toString())),
//        LibLinearSvm.load(modelPath.resolve("modality.model").toString()),
//        Annotate.modalityLabeler(),
//        doc -> doc.get(annset, EegActivity.TYPE));
//
//    this.evModAnnotator = new SvmAnnotator<>(new AnnotationVectorizer<>(Features.<Event>attributeFeatureExtractors(),
//          a -> Label.NULL,
//          IntIdentifier.fromFile(modelPath.resolve("modality.tsv").toString())),
//        LibLinearSvm.load(modelPath.resolve("modality.model").toString()),
//        Annotate.modalityLabeler(),
//        doc -> doc.get(annset, Event.TYPE));
//  }
//
//  @Override
//  public <B extends EegNote> void annotate(Document<B> document) {
//    if (true || document.get(annset, Event.TYPE).isEmpty() || document.get(annset, EegActivity.TYPE).isEmpty()) {
//      document.clear(annset);
//      try {
//        eventBoundaryAnnotator.annotate(document);
////    log.info("Added {} events.", document.get(annset, Event.TYPE).size());
//        activityBoundaryAnnotator.annotate(document);
////    log.info("Added {} activities.", document.get(annset, EegActivity.TYPE).size());
//        typeAnnotator.annotate(document);
//      } catch (DuplicateAnnotationException e) {
//        log.warn("Duplicate annotation exception ignored.");
//      }
//    }
//    attrAnnotators.forEach(ann -> ann.annotate(document));
//    actModAnnotator.annotate(document);
//    evModAnnotator.annotate(document);
//    actPolAnnotator.annotate(document);
//    evPolAnnotator.annotate(document);
//  }
//}
