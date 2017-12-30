package edu.utdallas.hltri.eeg.classifier;

import com.google.common.collect.Lists;
import edu.utdallas.hltri.eeg.EegNote;
import edu.utdallas.hltri.eeg.annotation.EegActivity;
import edu.utdallas.hltri.eeg.annotation.label.EventTypeLabel;
import edu.utdallas.hltri.eeg.annotation.label.ModalityLabel;
import edu.utdallas.hltri.eeg.annotation.label.PolarityLabel;
import edu.utdallas.hltri.eeg.annotators.SvmActiveLearner;
import edu.utdallas.hltri.eeg.annotators.SvmAnnotator;
import edu.utdallas.hltri.logging.Logger;
import edu.utdallas.hltri.ml.classify.LibLinearSvm;
import edu.utdallas.hltri.ml.label.BinaryLabel;
import edu.utdallas.hltri.ml.label.EnumLabel;
import edu.utdallas.hltri.scribe.text.BaseDocument;
import edu.utdallas.hltri.scribe.text.Document;
import edu.utdallas.hltri.scribe.text.annotation.Annotation;
import edu.utdallas.hltri.scribe.text.annotation.Event;
import edu.utdallas.hltri.util.ImmutibleIntIdenitifer;
import edu.utdallas.hltri.util.IntIdentifier;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * Utilities for loading/training LibLinear backed SVMs for clinical concept attribute classification
 * Created by rmm120030 on 9/7/16.
 */
@SuppressWarnings({"deprecation", "RedundantTypeArguments", "UnusedReturnValue"})
public class Svm {
  private static final Logger log = Logger.get(Svm.class);
//  private static final Parameter defaultMulticlassParameters = new Parameter(SolverType.MCSVM_CS, 1, 0.1);
//  private static final Parameter defaultBinaryParameters = new Parameter(SolverType.L2R_L2LOSS_SVC_DUAL, 1, 0.1);

  public static <T extends BaseDocument> SvmActiveLearner<Event, T> loadEventPolaritySvm(Path modelDir, String annset) {
    return AttributeClassifier.modality(annset, Event.polarity, Event.TYPE).loadActiveLearner(
        modelDir.resolve("polarity.model"),
        annset,
        PolarityLabel.POSITIVE.numLabels(),
        IntIdentifier.fromFile(modelDir.resolve("polarity.tsv").toString()).lock()
    );
  }

  public static <T extends BaseDocument> SvmActiveLearner<Event, T> loadEventModalitySvm(Path modelDir, String annset) {
    return AttributeClassifier.modality(annset, Event.modality, Event.TYPE).loadActiveLearner(
        modelDir.resolve("modality.model"),
        annset,
        ModalityLabel.FACTUAL.numLabels(),
        IntIdentifier.fromFile(modelDir.resolve("modality.tsv").toString()).lock()
    );
  }

//  public static <T extends BaseDocument> SvmActiveLearner<Event, T> loadEventAttributeSvm(
//      Path modelDir, String attr, String annset, final BiConsumer<Event, Double> attributeSetter, final int numLabels) {
//    return new SvmActiveLearner<>(FeatureUtils.<Event>attributeFeatureExtractors(annset),
//        LibLinearSvm.load(modelDir.resolve(attr + ".model").toString()),
//        attributeSetter,
//        doc -> doc.get(annset, Event.TYPE),
//        IntIdentifier.fromFile(modelDir.resolve(attr + ".tsv").toString()).lock(),
//        numLabels);
//  }

  public static <T extends BaseDocument> SvmActiveLearner<Event, T> loadEventTypeSvm(Path modelPath, String conceptAnnset) {
    final ImmutibleIntIdenitifer<String> iid = IntIdentifier.fromFile(modelPath.resolve("type.tsv").toString()).lock();
    System.out.println("id(in_span=and): " + iid.getID("in_span=and"));
    System.out.println("id(in_span='and'): " + iid.getID("in_span='and'"));
    System.out.println(iid.getItems());
    return AttributeClassifier.eventType(conceptAnnset).loadActiveLearner(modelPath.resolve("type.model"), conceptAnnset,
        EventTypeLabel.TEST.numLabels(), IntIdentifier.fromFile(modelPath.resolve("type.tsv").toString()).lock());
  }

  public static <T extends BaseDocument> SvmActiveLearner<EegActivity, T> loadActivityAttributeSvm(Path modelDir,
                                                                                                   String attr,
                                                                                                   String annset) {
    switch (attr) {
      case "morphology": return AttributeClassifier.morphology(annset).loadActiveLearner(
          modelDir.resolve("attr").resolve("morphology.model"),
          annset,
          EegActivity.Morphology.ABNORMAL.numLabels(),
          IntIdentifier.fromFile(modelDir.resolve("attr").resolve("morphology.tsv").toString()).lock()
      );
      case "band": return AttributeClassifier.band(annset).loadActiveLearner(
          modelDir.resolve("attr").resolve("band.model"),
          annset,
          EegActivity.Band.NA.numLabels(),
          IntIdentifier.fromFile(modelDir.resolve("attr").resolve("band.tsv").toString()).lock()
      );
      case "hemisphere": return AttributeClassifier.hemisphere(annset).loadActiveLearner(
          modelDir.resolve("attr").resolve("hemisphere.model"),
          annset,
          EegActivity.Hemisphere.NA.numLabels(),
          IntIdentifier.fromFile(modelDir.resolve("attr").resolve("hemisphere.tsv").toString()).lock()
      );
      case "dispersal": return AttributeClassifier.dispersal(annset).loadActiveLearner(
          modelDir.resolve("attr").resolve("dispersal.model"),
          annset,
          EegActivity.Dispersal.NA.numLabels(),
          IntIdentifier.fromFile(modelDir.resolve("attr").resolve("dispersal.tsv").toString()).lock()
      );
      case "recurrence": return AttributeClassifier.recurrence(annset).loadActiveLearner(
          modelDir.resolve("attr").resolve("recurrence.model"),
          annset,
          EegActivity.Recurrence.NONE.numLabels(),
          IntIdentifier.fromFile(modelDir.resolve("attr").resolve("recurrence.tsv").toString()).lock()
      );
      case "magnitude": return AttributeClassifier.magnitude(annset).loadActiveLearner(
          modelDir.resolve("attr").resolve("magnitude.model"),
          annset,
          EegActivity.Magnitude.NORMAL.numLabels(),
          IntIdentifier.fromFile(modelDir.resolve("attr").resolve("magnitude.tsv").toString()).lock()
      );
      case "in_background": return AttributeClassifier.background(annset).loadActiveLearner(
          modelDir.resolve("attr").resolve("in_background.model"),
          annset,
          EegActivity.In_Background.NO.numLabels(),
          IntIdentifier.fromFile(modelDir.resolve("attr").resolve("in_background.tsv").toString()).lock()
      );
      default: throw new RuntimeException("Unrecognized attribute type: " + attr);
    }
//    return new SvmActiveLearner<>(FeatureUtils.<EegActivity>attributeFeatureExtractors(annset),
//        LibLinearSvm.load(modelDir.resolve("attr").resolve(attr + ".model").toString()),
//        attributeSetter,
//        doc -> doc.get(annset, EegActivity.TYPE),
//        IntIdentifier.fromFile(modelDir.resolve("attr").resolve(attr + ".tsv").toString()).lock(),
//        numLabels);
  }

  public static <T extends BaseDocument> SvmActiveLearner<EegActivity, T> loadLocationSvm(Path modelDir,
                                                                                          String location,
                                                                                          String annset) {
    return AttributeClassifier.location(location, annset).loadActiveLearner(
        modelDir.resolve("attr").resolve("loc").resolve(location + ".model"),
        annset,
        2,
        IntIdentifier.fromFile(modelDir.resolve("attr").resolve("loc").resolve(location + ".tsv").toString()).lock()
    );
//    return new SvmActiveLearner<>(FeatureUtils.<EegActivity>attributeFeatureExtractors(annset),
//        LibLinearSvm.load(modelDir.resolve("attr").resolve("loc").resolve(location + ".model").toString()),
//        locationSetter(location),
//        doc -> doc.get(annset, EegActivity.TYPE),
//        IntIdentifier.fromFile(modelDir.resolve("attr").resolve("loc").resolve(location + ".tsv").toString()).lock(),
//        2);
  }

  public static LibLinearSvm trainTypeSvm(final List<Document<EegNote>> train, final String modelDir, final String annset) {
    log.info("Training type classifier...");
    return AttributeClassifier.eventType(annset).trainSvm(train.stream(), Paths.get(modelDir), "type", annset);

//    final LibLinearSvm svm = trainSvm(train.stream(),
//        Paths.get(modelDir), "type", annset, typeLabeler,
//        Event.TYPE, FeatureUtils.<Event>attributeFeatureExtractors(annset));
//    log.info("Done.");
//    return svm;
  }

  public static LibLinearSvm trainEventModalitySvm(final List<Document<EegNote>> train, final String modelDir,
                                                   final String annset) {
    log.info("Training modality classifier...");
    return AttributeClassifier.modality(annset, Event.modality, Event.TYPE)
        .trainSvm(train.stream(), Paths.get(modelDir), "modality", annset);

//    final LibLinearSvm svm = trainSvm(train.stream(),
//        Paths.get(modelDir), "modality", annset, modalityLabeler,
//        Event.TYPE, FeatureUtils.<Event>attributeFeatureExtractors(annset));
//    log.info("Done.");
//    return svm;
  }

  public static LibLinearSvm trainEventPolaritySvm(final List<Document<EegNote>> train, final String modelDir,
                                                   final String annset) {
    log.info("Training polarity classifier...");
    return AttributeClassifier.polarity(annset, Event.polarity, Event.TYPE)
        .trainSvm(train.stream(), Paths.get(modelDir), "polarity", annset);

//    final LibLinearSvm svm = trainSvm(train.stream(),
//        Paths.get(modelDir), "polarity", annset, polarityLabeler,
//        Event.TYPE, FeatureUtils.<Event>attributeFeatureExtractors(annset));
//    log.info("Done.");
//    return svm;
  }

  public static List<SvmAnnotator<EegActivity, EegNote>> trainSvmAllAttrs(final List<Document<EegNote>> documents,
                                                                          String modelDir, final String annset) {
    final Path modelPath = Paths.get(modelDir, "attr");
    //noinspection ResultOfMethodCallIgnored
    modelPath.toFile().mkdir();
    final List<SvmAnnotator<EegActivity, EegNote>> svmAnnotators = Lists.newArrayList();
    final List<String> names = new ArrayList<>();
    final List<AttributeClassifier<EegActivity>> asvms = new ArrayList<>();
    names.add("morphology");
    asvms.add(AttributeClassifier.morphology(annset));
    names.add("band");
    asvms.add(AttributeClassifier.band(annset));
    names.add("hemisphere");
    asvms.add(AttributeClassifier.hemisphere(annset));
    names.add("dispersal");
    asvms.add(AttributeClassifier.dispersal(annset));
    names.add("recurrence");
    asvms.add(AttributeClassifier.recurrence(annset));
    names.add("magnitude");
    asvms.add(AttributeClassifier.magnitude(annset));
    names.add("in_background");
    asvms.add(AttributeClassifier.background(annset));
    names.add("activity_modality");
    asvms.add(AttributeClassifier.modality(annset, EegActivity.modality, EegActivity.TYPE));
    names.add("activity_polarity");
    asvms.add(AttributeClassifier.polarity(annset, EegActivity.polarity, EegActivity.TYPE));

    for (EegActivity.Location location : EegActivity.Location.values()) {
      names.add(location.name());
      AttributeClassifier.location(location.name(), annset);
    }

    for (int i = 0; i < names.size(); i++) {
      final String name = names.get(i);
      log.info("Training {} classifier...", name);
      final AttributeClassifier<EegActivity> asvm = asvms.get(i);
      final LibLinearSvm svm = asvm.trainSvm(documents.stream(), modelPath, name, annset);
      svmAnnotators.add(asvm.getAnnotator(svm, IntIdentifier.fromFile(modelPath.resolve(name + ".tsv")), annset));
      log.info("done.");
    }

    return svmAnnotators;
  }

  public static <A extends Annotation<A>> BiConsumer<A, Double> modalitySetter() {
    return (event, prediction) -> {
      EnumLabel predictedLabel = null;
      final int predictionInt = Math.max(0, Math.min(3, prediction.intValue()));
      for (final EnumLabel label : ModalityLabel.values()) {
        if (label.numericValue().intValue() == predictionInt) {
          predictedLabel = label;
          break;
        }
      }
      assert predictedLabel != null : String.format("No event label for (%s,%s)", prediction, predictionInt);
      if (event instanceof Event)
        ((Event) event).set(Event.modality, predictedLabel.toString());
      else if (event instanceof EegActivity)
        ((EegActivity) event).set(EegActivity.modality, predictedLabel.toString());
      else {
        throw new RuntimeException("Not an event or activity: " + event.describe());
      }
    };
  }

  public static <A extends Annotation<A>> BiConsumer<A, Double> polaritySetter() {
    return (event, prediction) -> {
      EnumLabel predictedLabel = null;
      final int predictionInt = Math.max(0, Math.min(1, prediction.intValue()));
      for (final EnumLabel label : PolarityLabel.values()) {
        if (label.asInt() == predictionInt) {
          predictedLabel = label;
          break;
        }
      }
      assert predictedLabel != null : String.format("No event label for (%s,%s)", prediction, predictionInt);
      if (event instanceof Event)
        ((Event) event).set(Event.polarity, predictedLabel.toString());
      else if (event instanceof EegActivity)
        ((EegActivity) event).set(EegActivity.polarity, predictedLabel.toString());
      else {
        throw new RuntimeException("Not an event or activity: " + event.describe());
      }
    };
  }

  public static Function<EegActivity, BinaryLabel> locationLabler(String location) {
    return act -> {
      String loc = location;
      if (location.equals("POSTERIOR")) {
        loc = "OCCIPITAL";
      }
      if (act.getLocations().contains(EegActivity.Location.valueOf(loc))) {
        return BinaryLabel.TRUE;
      }
      return BinaryLabel.FALSE;
    };
  }
}
