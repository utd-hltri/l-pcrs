package edu.utdallas.hltri.eeg;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import com.google.common.collect.Sets;
import edu.utdallas.hlt.medbase.metamaplite.MetaMapLiteWrapper;
import edu.utdallas.hltri.conf.Config;
import edu.utdallas.hltri.eeg.annotation.EegActivity;
import edu.utdallas.hltri.ml.label.EnumLabel;
import edu.utdallas.hltri.scribe.text.annotation.Annotation;
import edu.utdallas.hltri.scribe.text.annotation.Event;
import edu.utdallas.hltri.scribe.text.annotation.attributes.HasModality;
import edu.utdallas.hltri.scribe.text.annotation.attributes.HasPolarity;
import gov.nih.nlm.nls.metamap.lite.types.ConceptInfo;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by rmm120030 on 2/12/17.
 */
public class ConceptNormalization {
  private static MetaMapLiteWrapper mml = null;
  private static volatile boolean init = false;
  private static Set<String> validProblems = null, validTreatments = null;
  private static Config conf = Config.load("eeg.kg");

  private static Set<String> problemSemTypes = Sets.newHashSet("sosy", "clna", "dsyn", "mobd", "neop", "patf", "comd",
      "emod", "inpo");
  private static Set<String> treatmentSemTypes = Sets.newHashSet("phsu", "antb", "bacs", "bodm", "bacs", "horm", "enzy",
      "vita", "imft", "orch", "nnon", "aapp", "hlca", "topp");

  private static void eventNormalizationHelper(String infile, String outdir) {
    try {
      final Multiset<String> unnormalizedCounts  = HashMultiset.create();
      final Multiset<String> normalizedCounts  = HashMultiset.create();
      final Set<String> noClassSet = new HashSet<>();
      Files.readAllLines(Paths.get(infile)).forEach(line -> {
        final String[] split = line.split("\\t");
        String event = split[0];
        int count = -1;
        if (split.length > 2) {
          for (int i = 1; i < split.length-1; i++) {
            event = event + " " + split[i];
          }
          count = Integer.parseInt(split[split.length-1]);
        } else {
          count = Integer.parseInt(split[1]);
        }
        unnormalizedCounts.add(event, count);
        normalizedCounts.add(normalizeEvent(event, noClassSet), count);
      });
      final ArrayList<String> sorted = new ArrayList<>(normalizedCounts.elementSet());
      Collections.sort(sorted, (s1, s2) -> Integer.compare(normalizedCounts.count(s2), normalizedCounts.count(s1)));
      Files.write(Paths.get(outdir).resolve("normalized_events.tsv"),
          sorted.stream().map(s -> s + "\t" + normalizedCounts.count(s)).collect(Collectors.toList()));
      Files.write(Paths.get(outdir).resolve("no_class_events.txt"), noClassSet);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  static synchronized void init() {
    // After waiting for our turn to init(), check to make sure no one did it before us
    if (!init) {
      mml = MetaMapLiteWrapper.getInstance();
      try {
        validProblems = new HashSet<>(Files.readAllLines(conf.getPath("prob_cuis")));
        validTreatments = new HashSet<>(Files.readAllLines(conf.getPath("tr_cuis")));
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
      init = true;
    }
  }

  static Optional<String> normalizeProbTestTr(String probTestTr, String expectedType) {
    // Check if initialized
    if (!init) {
      // Try to initialize (synchronized)
      init();
    }
    if (Objects.isNull(expectedType)) {
      return Optional.empty();
    }
    probTestTr = probTestTr.trim().toLowerCase();
    final Optional<ConceptInfo> cOp = mml.getBestConcept(probTestTr, "Tr_".equals(expectedType) ? treatmentSemTypes :
        ("P_".equals(expectedType) ? problemSemTypes : new HashSet<>()));
    if (cOp.isPresent()) {
      final ConceptInfo conc = cOp.get();
      if (validTreatments.contains(conc.getCUI()) || validProblems.contains(conc.getCUI())) {
        final String normalized = conc.getPreferredName().replaceAll("\\s", "_");
        if (conc.getSemanticTypeSet().stream().anyMatch(treatmentSemTypes::contains)) {
          if (conc.getSemanticTypeSet().stream().anyMatch(problemSemTypes::contains)) {
            return Optional.of(expectedType + normalized);
          } else {
            return Optional.of("Tr_" + normalized);
          }
        } else if (conc.getSemanticTypeSet().stream().anyMatch(problemSemTypes::contains)) {
          return Optional.of("P_" + normalized);
        }
      }
    }
    return Optional.empty();
  }

  public static Optional<String> normalizeConcept(Annotation<?> concept) {
    if (!init) {
      init();
    }

    if (concept instanceof Event) {
      return normalizeProbTestTr(concept.toString(), getEventPrefix((Event) concept));
    } else {
      assert concept instanceof EegActivity : "entity must be either an Event of EegActivity";
      return Optional.of("A_" + ((EegActivity) concept).get(EegActivity.morphology));
    }
  }

  public static <A extends Annotation<?>> Optional<String> normalizeConceptWithAttributes(
      A concept, String delimiter) {
    if (!init) {
      init();
    }
    if (concept instanceof Event) {
      final Optional<String> normalizedName = normalizeProbTestTr(concept.toString(), getEventPrefix((Event) concept));
      return normalizedName.map(name ->
          name + delimiter +
          "POL=" + ((Event) concept).get(HasPolarity.polarity) + delimiter +
          "MOD=" + ((Event) concept).get(HasModality.modality));
    } else {
      assert concept instanceof EegActivity : "entity must be either an Event of EegActivity";
      final EegActivity act = ((EegActivity) concept);
      return Optional.of("A_" + act.get(EegActivity.morphology) + delimiter +
          "POL=" + act.get(EegActivity.polarity) + delimiter +
          "MOD=" + act.get(EegActivity.modality) + delimiter +
          "FB=" + act.get(EegActivity.band) + delimiter +
          "DISP=" + act.get(EegActivity.dispersal) + delimiter +
          "HEMI=" + act.get(EegActivity.hemisphere) + delimiter +
          "REC=" + act.get(EegActivity.recurrence) + delimiter +
          "MAG=" + act.get(EegActivity.magnitude) + delimiter +
          "BK=" + act.get(EegActivity.in_background) + delimiter +
          act.getLocations().stream().map(EnumLabel::asString).collect(Collectors.joining(delimiter))
      );
    }
  }

  private static String getEventPrefix(Event event) {
    switch (event.get(Event.type)) {
      case "EEG_EVENT":
        final String eventClass = normalizeEvent(event.toString(), null);
        return (eventClass.equals("NO_CLASS")) ? null : "Ev_" + eventClass;
      case "PROBLEM": return "P_";
      case "TEST": return "Te_";
      case "TREATMENT": return "Tr_";
      default: throw new RuntimeException("Invalid type: " + event.get(Event.type));
    }
  }

  public static Optional<String> normalizeConcept(String concept) {
    return normalizeProbTestTr(concept, "");
  }

  static String getConceptTypePrefix(Annotation<?> entity) {
    if (entity instanceof Event) {
      switch (((Event) entity).get(Event.type)) {
        case "EEG_EVENT": return "Ev";
        case "PROBLEM": return "P";
        case "TEST": return "Te";
        case "TREATMENT": return "Tr";
        default: throw new RuntimeException("Invalid type: " + ((Event) entity).get(Event.type));
      }
    } else {
      assert entity instanceof EegActivity : "entity must be either an Event of EegActivity";
      return "A";
    }
  }

  private static String normalizeEvent(String event, Set<String> noClassSet) {
    event = event.trim().toLowerCase();
    if (event.contains("photic") || event.contains("photo stimulation")) {
      return "PHOTIC_STIMULATION";
    }
    else if (event.contains("eye") || event.contains("photoelectric")) {
      return "EYE_MOVEMENT_ARTIFACT";
    }
    else if (event.contains("ventil") || (event.contains("hyp") && event.contains("tion"))) {
      return "HYPERVENTILATION_ARTIFACT";
    }
    else if (event.contains("tactile") | event.contains("facial stimulation")) {
      return "TACTILE_STIMULATION";
    }
    else if (event.contains("verbal") || event.contains("oral")) {
      return "VERBAL_STIMULATION";
    }
//    else if (event.contains("anesthesia") || event.contains("sedation") || event.contains("haldol")
//        || event.contains("fentanyl")) {
//      return "SEDATION_ARTIFACT";
//    }
    else if (event.contains("stimulation") || event.contains("simulation") || event.contains("activat")) {
      return "MISC_ACTIVATION_PROCEDURE";
    }
    else if (event.contains("seizure") || event.contains("siezure") || event.contains("stroke")
        || event.contains("epilepti")) {
      return "SEIZURE";
    }
    else if (event.contains("rocking") || event.contains("head rock") || event.contains("muscle") || event.contains("twitch")
        || event.contains("movement") || event.contains("chew") || event.contains("tremor") || event.contains("arous")
        || event.contains("jerk") || event.contains("cough") || event.contains("shak") || event.contains("trembl")
        || event.contains("blink") || event.contains("facial") || event.contains("agitation") || event.contains("head")
        || event.contains("flex")|| event.contains("spasm")|| event.contains("tremulousness")) {
      return "MOVEMENT_ARTIFACT";
    }
    else if (event.contains("agitation") || event.contains("anxi") || event.contains("anxio")
        || event.contains("discomfort") || event.contains("panic") || event.contains("nervous")) {
      return "ANXIETY_ARTIFACT";
    }
//    else if (event.contains("ekg") || event.contains("emg") || event.contains("electrode")
//        || event.contains("ventilator") || event.contains("eeg") || event.contains("detector")) {
//      return "INSTRUMENT_ARTIFACT";
//    }
//    else if (event.contains("medic")) {
//      return "MEDICATION_ARTIFACT";
//    }
    else if (event.contains("button")) {
      return "BUTTON_PRESS";
    }
//    else if (event.contains("snor") || event.contains("sleep")) {
//      return "SLEEP_ARTIFACT";
//    }
//    else if (event.contains("artifact")) {
//      return "MISC_ARTIFACT";
//    }
    else {
//      final List<Entity> entities = mml.getEntities(event);
//      if (entities.stream().flatMap(e -> e.getEvList().stream())
//          .flatMap(ev -> ev.getConceptInfo().getSemanticTypeSet().stream())
//          .anyMatch(t -> t.equals("phsu") || t.equals("clnd") || t.equals("orch") || t.equals("phsu"))) {
//        return "MEDICATION_ARTIFACT";
//      }
//      else {
        if (noClassSet != null) {
          System.out.printf("No class for: |%s|\n", event);
          noClassSet.add(event);
        }
        return "NO_CLASS";
//      }
    }
  }
}
