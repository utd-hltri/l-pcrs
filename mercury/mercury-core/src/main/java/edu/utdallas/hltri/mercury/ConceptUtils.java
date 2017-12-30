package edu.utdallas.hltri.mercury;

import edu.utdallas.hltri.eeg.ConceptNormalization;
import edu.utdallas.hltri.eeg.annotation.EegActivity;
import edu.utdallas.hltri.eeg.annotation.label.ModalityLabel;
import edu.utdallas.hltri.eeg.annotators.EegConceptAnnotator;
import edu.utdallas.hltri.inquire.text.Query;
import edu.utdallas.hltri.scribe.annotators.*;
import edu.utdallas.hltri.scribe.text.BaseDocument;
import edu.utdallas.hltri.scribe.text.Document;
import edu.utdallas.hltri.scribe.text.annotation.Event;
import edu.utdallas.hltri.scribe.text.annotation.Sentence;
import edu.utdallas.hltri.scribe.text.annotation.Token;
import edu.utdallas.hltri.util.Lazy;

import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Created by ramon on 5/9/17.
 */
public class ConceptUtils {

  public static final String CONC_ANNSET = "conc";
  public static final Supplier<EegConceptAnnotator<BaseDocument>> conceptAnnotator = Lazy.lazily(() -> EegConceptAnnotator.bestWithLstm(CONC_ANNSET));
  static final Supplier<GeniaAnnotator<BaseDocument>> genia = Lazy.lazily(() ->
      GeniaAnnotator.tokenAnnotator(doc -> doc.get(OpenNLPSentenceAnnotator.ANNOTATION_SET_NAME, Sentence.TYPE), false));
  static final Supplier<StanfordCoreAnnotator<BaseDocument>> stanford = Lazy.lazily(() ->
      new StanfordCoreAnnotator.Builder<>().all().clear().build());
  static final Supplier<Annotator<BaseDocument>> lingscope = Lazy.lazily(() -> new LingScopeNegationSpanAnnotator<>(
      d -> d.get("opennlp", Sentence.TYPE),
      s -> s.getContained("genia", Token.TYPE)
  ));

  public static String createActivityString(EegActivity activity) {
    return activity.get(EegActivity.morphology) + "|" +
        activity.get(EegActivity.band) + "|" +
        activity.get(EegActivity.hemisphere) + "|" +
        activity.get(EegActivity.dispersal) + "|" +
        activity.get(EegActivity.recurrence) + "|" +
        activity.get(EegActivity.in_background) + "|" +
        activity.get(EegActivity.magnitude) + "|" +
        activity.get(EegActivity.modality) + "|" +
        activity.getLocations().stream().map(EegActivity.Location::name).collect(Collectors.joining()) + "|" +
        activity.get(EegActivity.polarity);
  }

  public static String createActivityStringWithPolarity(EegActivity activity, boolean polarity) {
    return activity.get(EegActivity.morphology) + "|" +
        activity.get(EegActivity.band) + "|" +
        activity.get(EegActivity.hemisphere) + "|" +
        activity.get(EegActivity.dispersal) + "|" +
        activity.get(EegActivity.recurrence) + "|" +
        activity.get(EegActivity.in_background) + "|" +
        activity.get(EegActivity.magnitude) + "|" +
        activity.get(EegActivity.modality) + "|" +
        activity.getLocations().stream().map(EegActivity.Location::name).collect(Collectors.joining()) + "|" +
        activity.get(EegActivity.polarity);
  }

  // replaces default attribute values with wildcards
  public static String createActivityStringWithWildcards(EegActivity activity) {
//    return activity.get(EegActivity.morphology) + "|*";

    final String band = activity.get(EegActivity.band);
    final String hemisphere = activity.get(EegActivity.hemisphere);
    final String dispersal = activity.get(EegActivity.dispersal);
    final String recurrence = activity.get(EegActivity.recurrence);
    final String inBackground = activity.get(EegActivity.in_background);
    final String magnitude = activity.get(EegActivity.magnitude);
    final String modality = activity.get(EegActivity.modality);
    final String polarity = "*"; //activity.get(EegActivity.polarity);
    final String prefix = activity.get(EegActivity.morphology) + "|" +
        (EegActivity.Band.valueOf(band) == EegActivity.Band.NA ? "*" : band)+ "|" +
        (EegActivity.Hemisphere.valueOf(hemisphere) == EegActivity.Hemisphere.NA ? "*" : hemisphere) + "|" +
        (EegActivity.Dispersal.valueOf(dispersal) == EegActivity.Dispersal.NA ? "*" : dispersal) + "|" +
        (EegActivity.Recurrence.valueOf(recurrence) == EegActivity.Recurrence.NONE ? "*" : recurrence) + "|" +
        (EegActivity.In_Background.valueOf(inBackground) == EegActivity.In_Background.NO ? "*" : inBackground) + "|" +
        (EegActivity.Magnitude.valueOf(magnitude) == EegActivity.Magnitude.NORMAL ? "*" : magnitude) + "|" +
        (ModalityLabel.valueOf(modality) == ModalityLabel.FACTUAL ? "*" : modality);

    Set<EegActivity.Location> locations = activity.getLocations();
    if (locations.size() > 0) {
      final StringBuilder sb = new StringBuilder();
      for (EegActivity.Location location : locations) {
        if (sb.length() > 0) {
          sb.append(" activity:");
        }
        sb.append(prefix).append("|*").append(location).append("*|").append(polarity);
      }
      return sb.toString();
    } else {
      return prefix + "|*|" + polarity; //(PolarityLabel.valueOf(polarity) == PolarityLabel.POSITIVE ? "*" : polarity);
    }
  }

  public static Optional<String> createEventString(Event event) {
    Optional<String> op = ConceptNormalization.normalizeConcept(event);
    if (op.isPresent()) {
      return Optional.of(op.get() + "|" +
          event.get(Event.type) + "|" +
          event.get(Event.modality) + "|" +
          event.get(Event.polarity));
    } else {
      return Optional.empty();
    }
  }

  public static <Q extends Query> Document<Q> createAnnotatedQueryDocument(String rawQuery) {
    final Document<Q> doc = Document.fromString(rawQuery);
    doc.set(BaseDocument.id, "$query");
    Sentence.TYPE.create(doc, "opennlp", 0, doc.length());
    Sentence.TYPE.create(doc, "stanford", 0, doc.length());
    ConceptUtils.genia.get().annotate(doc);
    ConceptUtils.stanford.get().annotate(doc);
    ConceptUtils.lingscope.get().annotate(doc);
    conceptAnnotator.get().annotate(doc);
    return doc;
  }
}
