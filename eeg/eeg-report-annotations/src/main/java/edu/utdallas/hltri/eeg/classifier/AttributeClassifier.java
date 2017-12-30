package edu.utdallas.hltri.eeg.classifier;

import de.bwaldvogel.liblinear.Parameter;
import de.bwaldvogel.liblinear.SolverType;
import edu.utdallas.hltri.eeg.EegNote;
import edu.utdallas.hltri.eeg.annotation.EegActivity;
import edu.utdallas.hltri.eeg.annotation.label.EventTypeLabel;
import edu.utdallas.hltri.eeg.annotation.label.ModalityLabel;
import edu.utdallas.hltri.eeg.annotation.label.PolarityLabel;
import edu.utdallas.hltri.eeg.annotators.SvmActiveLearner;
import edu.utdallas.hltri.eeg.annotators.SvmAnnotator;
import edu.utdallas.hltri.eeg.feature.FeatureUtils;
import edu.utdallas.hltri.logging.Logger;
import edu.utdallas.hltri.ml.Feature;
import edu.utdallas.hltri.ml.FeatureExtractor;
import edu.utdallas.hltri.ml.classify.LibLinearSvm;
import edu.utdallas.hltri.ml.label.BinaryLabel;
import edu.utdallas.hltri.ml.label.EnumLabel;
import edu.utdallas.hltri.ml.label.Label;
import edu.utdallas.hltri.ml.vector.FeatureVector;
import edu.utdallas.hltri.ml.vector.SparseFeatureVectorizer;
import edu.utdallas.hltri.scribe.text.Attribute;
import edu.utdallas.hltri.scribe.text.BaseDocument;
import edu.utdallas.hltri.scribe.text.Document;
import edu.utdallas.hltri.scribe.text.annotation.Annotation;
import edu.utdallas.hltri.scribe.text.annotation.AnnotationType;
import edu.utdallas.hltri.scribe.text.annotation.Event;
import edu.utdallas.hltri.struct.Pair;
import edu.utdallas.hltri.util.IntIdentifier;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * Convenience class for holding attribute-specific functions
 *
 * Created by rmm120030 on 8/22/17.
 */
@SuppressWarnings("WeakerAccess")
public class AttributeClassifier<A extends Annotation<A>> {
  private static final Logger log = Logger.get(AttributeClassifier.class);

  // converts the scribe attribute value into an enum label for this attribute
  private final Function<A, ? extends EnumLabel> value2label;
  // feature extractors
  private final Collection<FeatureExtractor<A,?>> featureExtractors;
  // sets the scribe annotation value given a double svm prediction
  private final BiConsumer<A, Double> scribeAnnotationSetter;
  // annotation type
  private final AnnotationType<A> annotationType;

  private AttributeClassifier(Function<A, ? extends EnumLabel> value2label, Collection<FeatureExtractor<A,?>> featureExtractors,
                              BiConsumer<A, Double> setter, AnnotationType<A> type) {
    this.value2label = value2label;
    this.featureExtractors = featureExtractors;
    this.scribeAnnotationSetter = setter;
    this.annotationType = type;
  }

  public static AttributeClassifier<Event> eventType(String annset) {
    return new AttributeClassifier<>(
        ev -> EventTypeLabel.valueOf(wrapNull(ev, Event.type)),
        FeatureUtils.attributeFeatureExtractors(annset),
        attributeSetter(EventTypeLabel.values(), (a, s) -> a.set(Event.type, s)),
        Event.TYPE
    );
  }

  private static AttributeClassifier<EegActivity> activityAttributeSvm(
      Function<String, ? extends EnumLabel> lfun, EnumLabel[] labels, Attribute<EegActivity, String> scribeAttr,
      Collection<FeatureExtractor<EegActivity,?>> featureExtractors) {
    return new AttributeClassifier<>(
        act -> lfun.apply(wrapNull(act, scribeAttr).toUpperCase()),
        featureExtractors,
        attributeSetter(labels, (a, s) -> a.set(scribeAttr, s)),
        EegActivity.TYPE
    );
  }

  public static AttributeClassifier<EegActivity> morphology(String actAnnset) {
    return activityAttributeSvm(EegActivity.Morphology::valueOf, EegActivity.Morphology.values(),
        EegActivity.morphology,
        FeatureUtils.attributeFeatureExtractors(actAnnset));
  }

  public static AttributeClassifier<EegActivity> band(String actAnnset) {
    return activityAttributeSvm(EegActivity.Band::valueOf, EegActivity.Band.values(), EegActivity.band,
        FeatureUtils.attributeFeatureExtractors(actAnnset, EegActivity.morphology));
  }

  public static AttributeClassifier<EegActivity> hemisphere(String actAnnset) {
    return activityAttributeSvm(EegActivity.Hemisphere::valueOf, EegActivity.Hemisphere.values(),
        EegActivity.hemisphere,
        FeatureUtils.attributeFeatureExtractors(actAnnset, EegActivity.morphology, EegActivity.band));
  }

  public static AttributeClassifier<EegActivity> dispersal(String actAnnset) {
    return activityAttributeSvm(EegActivity.Dispersal::valueOf, EegActivity.Dispersal.values(),
        EegActivity.dispersal,
        FeatureUtils.attributeFeatureExtractors(actAnnset, EegActivity.morphology, EegActivity.band, EegActivity.hemisphere));
  }

  public static AttributeClassifier<EegActivity> recurrence(String actAnnset) {
    return activityAttributeSvm(EegActivity.Recurrence::valueOf, EegActivity.Recurrence.values(),
        EegActivity.recurrence,
        FeatureUtils.attributeFeatureExtractors(actAnnset, EegActivity.morphology, EegActivity.band));
  }

  public static AttributeClassifier<EegActivity> magnitude(String actAnnset) {
    return activityAttributeSvm(EegActivity.Magnitude::valueOf, EegActivity.Magnitude.values(),
        EegActivity.magnitude,
        FeatureUtils.attributeFeatureExtractors(actAnnset, EegActivity.morphology, EegActivity.band));
  }

  public static AttributeClassifier<EegActivity> background(String actAnnset) {
    return activityAttributeSvm(EegActivity.In_Background::valueOf, EegActivity.In_Background.values(),
        EegActivity.in_background,
        FeatureUtils.attributeFeatureExtractors(actAnnset, EegActivity.morphology, EegActivity.band,
            EegActivity.hemisphere, EegActivity.dispersal, EegActivity.recurrence, EegActivity.magnitude));
  }

  public static AttributeClassifier<EegActivity> location(String location, String actAnnset) {
    return new AttributeClassifier<>(
        act -> {
          String loc = location;
          if (location.equals("POSTERIOR")) {
            loc = "OCCIPITAL";
          }
          if (act.getLocations().contains(EegActivity.Location.valueOf(loc))) {
            return BinaryLabel.TRUE;
          }
          return BinaryLabel.FALSE;
        },
        FeatureUtils.attributeFeatureExtractors(actAnnset, EegActivity.morphology, EegActivity.hemisphere, EegActivity.dispersal),
        (activity, prediction) -> {
          if (prediction.intValue() == BinaryLabel.TRUE.asInt()) {
            activity.addLocation(location);
          }
        },
        EegActivity.TYPE
    );
  }

  public static <A extends Annotation<A>> AttributeClassifier<A> modality(
      String actAnnset, Attribute<? super A, String> scribeAttr, AnnotationType<A> annotationType) {
    return new AttributeClassifier<A>(
        (A act) -> ModalityLabel.valueOf(modalityString(act.get(scribeAttr)).toUpperCase()),
        FeatureUtils.attributeFeatureExtractors(actAnnset),
        attributeSetter(ModalityLabel.values(), (a, s) -> a.set(scribeAttr, s)),
        annotationType
    );
  }

  public static <A extends Annotation<A>> AttributeClassifier<A> polarity(
      String actAnnset, Attribute<? super A, String> scribeAttr, AnnotationType<A> annotationType) {
    return new AttributeClassifier<A>(
        (A act) -> PolarityLabel.valueOf(polarityString(act.get(scribeAttr)).toUpperCase()),
        FeatureUtils.attributeFeatureExtractors(actAnnset),
        attributeSetter(PolarityLabel.values(), (a, s) -> a.set(scribeAttr, s)),
        annotationType
    );
  }

  public LibLinearSvm trainSvm(final Stream<Document<EegNote>> documents, final Path modelPath, final String name,
                               final String annset) {
    final IntIdentifier<String> iid = new IntIdentifier<>();
    final SparseFeatureVectorizer<Number> vectorizer = new SparseFeatureVectorizer<>(iid);
    final List<Pair<Label, FeatureVector<Number>>> fvs = new ArrayList<>();
    documents.forEach(doc -> {
      for (A ann : doc.get(annset, annotationType)) {
        fvs.add(new Pair<>(value2label.apply(ann),
            vectorizer.vectorize(featureExtractors.stream().flatMap(fe -> fe.apply(ann)).map(Feature::toNumericFeature))));
      }
    });
    iid.lock();
    final LibLinearSvm svm = LibLinearSvm.train(new Parameter(SolverType.MCSVM_CS, 1, 0.1), fvs)
        .save(modelPath.resolve(name + ".model"));
    iid.toFile(modelPath.resolve(name + ".tsv"));
    return svm;
  }

  public Function<A, ? extends EnumLabel> getValue2label() {
    return value2label;
  }

  public <D extends BaseDocument> SvmAnnotator<A, D> getAnnotator(LibLinearSvm svm,
                                                                  IntIdentifier<String> featureIdentifier,
                                                                  String annset) {
    return new SvmAnnotator<>(featureExtractors, svm, scribeAnnotationSetter, doc -> doc.get(annset, annotationType),
        featureIdentifier);
  }

  public <D extends BaseDocument> SvmAnnotator<A, D> getAnnotator(LibLinearSvm svm,
                                                                  IntIdentifier<String> featureIdentifier,
                                                                  String annset,
                                                                  BiConsumer<A, Double> otherSetter) {
    return new SvmAnnotator<>(featureExtractors, svm, otherSetter, doc -> doc.get(annset, annotationType),
        featureIdentifier);
  }

  public <D extends BaseDocument> SvmActiveLearner<A, D> loadActiveLearner(Path modelFile, String annset, int numLabels,
                                                                           IntIdentifier<String> featureIdentifier) {
    return new SvmActiveLearner<>(featureExtractors, LibLinearSvm.load(modelFile.toString()),
        scribeAnnotationSetter, doc -> doc.get(annset, annotationType), featureIdentifier, numLabels);
  }

  /**
   * This function will assign a fineGrainedLabel from the labels array to the value of the attribute passed in the consumed
   * annotation's featuremap.
   * The Double consumed should correspond to the numericValue() of one of the passed labels.
   * @param labels valid labels
   * @param setter sets the predicted string value in the annotation's feature map
   * @param <A> Annotation type to be labeled
   * @return the labelling function.
   */
  private static <A extends Annotation<A>> BiConsumer<A, Double> attributeSetter(final EnumLabel[] labels,
                                                                                 BiConsumer<A, String> setter) {
    return (event, prediction) -> {
      EnumLabel predictedLabel = null;
      final int predictionInt = Math.max(0, Math.min(labels.length - 1, prediction.intValue()));
      for (final EnumLabel label : labels) {
        if (label.asInt() == predictionInt) {
          predictedLabel = label;
          break;
        }
      }
      assert predictedLabel != null : String.format("No label for (%s,%s)", prediction, predictionInt);
      setter.accept(event, predictedLabel.toString());
//      event.set(attr, predictedLabel.toString());
    };
  }

  private static <A extends Annotation<A>> String wrapNull(A ann, Attribute<A, String> attr) {
    final String s = ann.get(attr);
    if (s == null || s.equalsIgnoreCase("NULL")) {
      log.warn("No {} attribute for {}:{}", attr.name, ann.getDocument().getId(), ann.toString());
      return "NA";
    }
    return s;
  }

  private static String polarityString(final String str) {
    if (str == null) {
      log.warn("null polarity. Returning POSITIVE...");
      return "POSITIVE";
    }
    switch (str) {
      case "POSITIVE":
      case "POS": return "POSITIVE";
      case "NEGATIVE":
      case "NEG": return "NEGATIVE";
      default: log.warn("Unexpected polarity: {}. Returning POSITIVE...", str);
        return "POSITIVE";
    }
  }

  private static String modalityString(final String str) {
    if (str == null) {
      log.warn("null modality. Returning FACTUAL...");
      return "FACTUAL";
    }
    return str;
  }
}
