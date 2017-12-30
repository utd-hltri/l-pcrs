package edu.utdallas.hltri.eeg;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import edu.utdallas.hltri.eeg.annotation.EegActivity;
import edu.utdallas.hltri.framework.ProgressLogger;
import edu.utdallas.hltri.io.TextFiles;
import edu.utdallas.hltri.logging.Logger;
import edu.utdallas.hltri.scribe.io.JsonCorpus;
import edu.utdallas.hltri.scribe.text.annotation.Annotation;
import edu.utdallas.hltri.scribe.text.annotation.Event;
import edu.utdallas.hltri.scribe.text.annotation.Section;
import edu.utdallas.hltri.scribe.text.relation.OpenRelation;
import edu.utdallas.hltri.struct.Pair;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Extracts relation triples for the Medical Knowledge Embedding system.
 *
 * Created by rmm120030 on 2/13/17.
 */
public class ExtractTriples {
  private static final Logger log = Logger.get(ExtractTriples.class);
  private static final String CONCEPT_ANNSET = "lstm";

  enum SectionTitle {
    HISTORY(".*hist.*"),
    MEDICATIONS(".*med.*"),
    DESCRIPTION(".*desc.*"),
    IMPRESSION(".*impr.*|.*sion.*"),
    CORRELATION(".*corr.*|.*clin.*");

    final Pattern pattern;
    SectionTitle(String uncompiledRegex) {
      this.pattern = Pattern.compile(uncompiledRegex, Pattern.CASE_INSENSITIVE);
    }

    public boolean matches(String s) {
      return pattern.matcher(s).matches();
    }

    static SectionTitle fromString(String title) {
      return Arrays.stream(SectionTitle.values()).filter(t -> t.matches(title)).findAny()
          .orElseGet(() -> {throw new RuntimeException("invalid title: " + title);});
    }
  }

  enum EntityType {
    PROBLEM, TEST, TREATMENT, EVENT, ACTIVITY;

    @SuppressWarnings("unused")
    public static EntityType getType(Annotation<?> entity) {
      if (entity instanceof Event) {
        switch (((Event) entity).get(Event.type)) {
          case "EEG_EVENT": return EVENT;
          case "PROBLEM": return PROBLEM;
          case "TEST": return TEST;
          case "TREATMENT": return TREATMENT;
          default: throw new RuntimeException("Invalid type: " + ((Event) entity).get(Event.type));
        }
      } else {
        assert entity instanceof EegActivity : "entity must be either an Event of EegActivity";
        return ACTIVITY;
      }
    }

    public static EntityType getType(String entity) {
      if (entity.startsWith("Tr")) {
        return TREATMENT;
      } else if (entity.startsWith("P")) {
        return PROBLEM;
      } else if (entity.startsWith("Ev")) {
        return EVENT;
      } else if (entity.startsWith("A")) {
        return ACTIVITY;
      } else if (entity.startsWith("Te")) {
        return TEST;
      } else {
        throw new RuntimeException("Invalid type: " + entity);
      }
    }
  }

  public enum RelationType {
    EVOKES, EVIDENCES, TREATMENT_FOR, OCCURS_WITH, NONE
  }

  public static void main(String... args) {
//    System.out.println(Arrays.stream(EegActivity.Morphology.values())
//            .map(m -> m.asString() + ":" + m.asInt()).collect(Collectors.joining("\n")));
//    generateTriples(args[0], args[1]);
//    generateSignalTriples(args[0], args[1]);
    generateOpenIeTriples(Paths.get(args[0]));
  }

  private static <A extends Annotation<A>> Optional<String> conceptString(A ann) {
    final List<EegActivity> activities = ann.getOverlapping(CONCEPT_ANNSET, EegActivity.TYPE);
    if (!activities.isEmpty()) {
      return ConceptNormalization.normalizeConcept(activities.get(0));
    }
    final List<Event> events = ann.getOverlapping(CONCEPT_ANNSET, Event.TYPE);
    if (!events.isEmpty()) {
      return ConceptNormalization.normalizeConcept(events.get(0));
    }
    return Optional.empty();
  }

  private static void generateOpenIeTriples(final Path outDir) {
    final JsonCorpus<EegNote> corpus = Data.v060("stanford", CONCEPT_ANNSET);
    final List<String> triples = new ArrayList<>();
    final Multiset<String> relations = HashMultiset.create(), arguments = HashMultiset.create();
    final ProgressLogger plog = ProgressLogger.fixedSize("processing", Data.V060_SIZE, 5L, TimeUnit.SECONDS);
    corpus.forEachDocument(doc ->
      doc.getRelations("stanford", OpenRelation.TYPE).forEach(relation -> {
        plog.update(doc.getId());
        final Optional<String> govOp = conceptString(relation.getGovernor());
        final Optional<String> depOp = conceptString(relation.getDependant());
        if (govOp.isPresent() && depOp.isPresent()) {
          final String gov = govOp.get();
          final String dep = depOp.get();
          final String rel = relation.get(OpenRelation.relation).replaceAll("\\s", "_");
          relations.add(rel);
          arguments.add(gov);
          arguments.add(dep);
          triples.add(gov + "\t" + rel + "\t" + dep);
        }
      })
    );
    try {
      Files.write(outDir.resolve("triples.tsv"), triples);
      TextFiles.writeMultisetSortedDescending(relations, outDir.resolve("relations.tsv"));
      TextFiles.writeMultisetSortedDescending(arguments, outDir.resolve("arguments.tsv"));
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @SuppressWarnings("unused")
  private static void generateAllOpenIeTriples(final Path outDir) {
    final JsonCorpus<EegNote> corpus = Data.v060("stanford", CONCEPT_ANNSET);
    final List<String> triples = new ArrayList<>();
    final Multiset<String> relations = HashMultiset.create(), arguments = HashMultiset.create();
    corpus.forEachDocument(doc ->
      doc.getRelations("stanford", OpenRelation.TYPE).forEach(relation -> {
        final String gov = relation.getGovernor().asString().replaceAll("\\s", "_");
        final String dep = relation.getDependant().asString().replaceAll("\\s", "_");
        final String rel = relation.get(OpenRelation.relation).replaceAll("\\s", "_");
        relations.add(rel);
        arguments.add(gov);
        arguments.add(dep);
        triples.add(gov + "\t" + rel + "\t" + dep);
      })
    );
    try {
      Files.write(outDir.resolve("all_triples.tsv"), triples);
      TextFiles.writeMultisetSortedDescending(relations, outDir.resolve("all_relations.tsv"));
      TextFiles.writeMultisetSortedDescending(arguments, outDir.resolve("all_arguments.tsv"));
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @SuppressWarnings({"unused", "ConstantConditions"})
  private static void generateSignalTriples(final String outFile, final String annset) {
    final JsonCorpus<EegNote> corpus = Data.v060(annset);
    List<String> triples = new ArrayList<>();
    corpus.forEachDocument(doc -> {
      doc.get(annset, EegActivity.TYPE).stream().map(ConceptNormalization::normalizeConcept)
          .filter(Optional::isPresent).forEach(op -> triples.add(op.get() + "\tHAS_SIGNAL\t" + doc.getId()));
      doc.get(annset, Event.TYPE).stream().map(ConceptNormalization::normalizeConcept)
          .filter(Optional::isPresent).forEach(op -> triples.add(op.get() + "\tHAS_SIGNAL\t" + doc.getId()));
    });
    log.info("Extracted {} triples.", triples.size());
    try {
      Files.write(Paths.get(outFile), triples);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @SuppressWarnings({"unused", "ConstantConditions"})
  private static void generateTriples(final String outFile, final String annset) {
    final List<String> triples = new ArrayList<>();
    final Pattern validSectionTitleRegex = Pattern.compile(Arrays.stream(SectionTitle.values())
        .map(t -> t.pattern.pattern()).collect(Collectors.joining("|")));
    final ProgressLogger plog = ProgressLogger.fixedSize("extracting", Data.V060_SIZE, 5L, TimeUnit.SECONDS);
    final JsonCorpus<EegNote> corpus = Data.v060(annset, "regex-eeg");
//    final AtomicInteger dc = new AtomicInteger(0);
//    corpus.getIdStream().filter(id -> dc.incrementAndGet() < 100).forEach(id -> {
//      try (final Document<EegNote> doc = corpus.load(id)) {
    corpus.forEachDocument(doc -> {
      int count = triples.size();
      final List<Section> sections = doc.get("regex-eeg", Section.TYPE).stream()
          .filter(sec -> validSectionTitleRegex.matcher(sec.get(Section.title).toLowerCase()).matches())
          .collect(Collectors.toList());

      final Set<String> docTripleSet = new HashSet<>();
      for (Section subjSection : sections) {
        final SectionTitle subjSt = SectionTitle.fromString(subjSection.get(Section.title));
        final List<Event> subjEvents = subjSection.getContained(annset, Event.TYPE).stream()
            .filter(ExtractTriples::isValidEvent).collect(Collectors.toList());
        final List<EegActivity> subjActivities = subjSection.getContained(annset, EegActivity.TYPE).stream()
            .filter(ExtractTriples::isValidActivity).collect(Collectors.toList());
        if ("ABNORMAL".equals(doc.get(EegNote.interpretation))) {
          subjEvents.stream().map(ConceptNormalization::normalizeConcept)
              .filter(Optional::isPresent)
              .forEach(op -> docTripleSet.add(op.get() + "\tEVIDENCES\tP_Cerebral_Disfunction"));
          subjActivities.stream().map(ConceptNormalization::normalizeConcept)
              .filter(Optional::isPresent)
              .forEach(op -> docTripleSet.add(op.get() + "\tEVIDENCES\tP_Cerebral_Disfunction"));
        }
        for (Section objSection : sections) {
          final SectionTitle objSt = SectionTitle.fromString(objSection.get(Section.title));
          final List<Event> objEvents = objSection.getContained(annset, Event.TYPE);
          final List<EegActivity> objActivities = objSection.getContained(annset, EegActivity.TYPE);
          for (EegActivity objActivity : objActivities) {
            if (isValidActivity(objActivity)) {
              // we only want one example of each triple here per document to account for repeated events
              // so add to set
              // A -> A
              subjActivities.stream()
//                  .filter(a -> !a.get(EegActivity.polarity).equals("NEGATIVE"))
                  .map(sa -> generateTriple(subjSt, objSt, sa, objActivity))
                  .filter(Optional::isPresent).map(Optional::get).forEach(docTripleSet::add);
              // E -> A
              subjEvents.stream()
//                  .filter(e -> !e.get(Event.polarity).equals("NEGATIVE"))
                  .map(se -> generateTriple(subjSt, objSt, se, objActivity))
                  .filter(Optional::isPresent).map(Optional::get).forEach(docTripleSet::add);
            }
          }
          for (Event objEvent : objEvents) {
            if (isValidEvent(objEvent)) {
              // E -> A
              subjActivities.stream()
//                  .filter(a -> !a.get(EegActivity.polarity).equals("NEGATIVE"))
                  .map(sa -> generateTriple(subjSt, objSt, sa, objEvent))
                  .filter(Optional::isPresent).map(Optional::get).forEach(docTripleSet::add);
              // E -> E
              subjEvents.stream()
//                  .filter(e -> !e.get(Event.polarity).equals("NEGATIVE"))
                  .map(se -> generateTriple(subjSt, objSt, se, objEvent))
                  .filter(Optional::isPresent).map(Optional::get).forEach(t -> {
                if (t.contains("OCCURS_WITH")) {
                  docTripleSet.add(t);
                } else {
                  triples.add(t);
                }
              });
            }
          }
        }
      }
      triples.addAll(docTripleSet);
      plog.update("doc {} had {} relations. {} total", doc.getId(), triples.size() - count, triples.size());
//      }
    });
    log.info("Extracted {} triples.", triples.size());
    try {
      Files.write(Paths.get(outFile), triples);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  static boolean isValidEvent(Event e) {
    return !e.get(Event.type).equals("TEST") && !e.get(Event.polarity).equals("NEGATIVE");
  }

  static boolean isValidActivity(EegActivity e) {
    return !e.get(EegActivity.polarity).equals("NEGATIVE");
  }

  @SuppressWarnings("unused")
  private static void generateTriplesInSeparateFiles(final String outDirName, final String annset) {
    final Map<Pair<SectionTitle,SectionTitle>,List<String>> sectionRelations = new HashMap<>();
    final List<String> docRelations = new ArrayList<>();
    final Pattern validSectionTitleRegex = Pattern.compile(Arrays.stream(SectionTitle.values())
        .map(t -> t.pattern.pattern()).collect(Collectors.joining("|")));
    final ProgressLogger plog = ProgressLogger.fixedSize("extracting", Data.V060_SIZE, 5L, TimeUnit.SECONDS);
    Data.v060(annset, "regex-eeg").forEachDocument(doc -> {
      int scount = 0, dcount = 0;
      final List<Section> sections = doc.get("regex-eeg", Section.TYPE).stream()
          .filter(sec -> validSectionTitleRegex.matcher(sec.get(Section.title).toLowerCase()).matches()).collect(Collectors.toList());
      for (int i = 0; i < sections.size() - 1; i++) {
        for (int j = i+1; j < sections.size(); j++) {
          final Section sec1 = sections.get(i);
          final SectionTitle st1 = SectionTitle.fromString(sec1.get(Section.title));
          final Section sec2 = sections.get(j);
          final SectionTitle st2 = SectionTitle.fromString(sec2.get(Section.title));
          final Pair<SectionTitle, SectionTitle> key = Pair.of(st1, st2);

          final List<String> list = (sectionRelations.containsKey(key)) ? sectionRelations.get(key) :
              new ArrayList<>();
          for (EegActivity activity : sec1.getContained(annset, EegActivity.TYPE)) {
            for (Event event : sec2.getContained(annset, Event.TYPE)) {
              final Optional<String> normalizedEvent = normalizeEvent(event);
              normalizedEvent.ifPresent(s -> list.add(activity.get(EegActivity.morphology) + "\t" + s));
            }
          }
          for (Event event : sec1.getContained(annset, Event.TYPE)) {
            final Optional<String> normalizedEvent = normalizeEvent(event);
            if (normalizedEvent.isPresent()) {
              for (EegActivity activity : sec2.getContained(annset, EegActivity.TYPE)) {
                list.add(normalizedEvent.get() + "\t" + activity.get(EegActivity.morphology));
              }
            }
          }
          for (Event e1 : sec1.getContained(annset, Event.TYPE)) {
            final Optional<String> ne1 = normalizeEvent(e1);
            if (ne1.isPresent()) {
              for (Event e2 : sec2.getContained(annset, Event.TYPE)) {
                final Optional<String> ne2 = normalizeEvent(e2);
                ne2.ifPresent(s -> list.add(ne1.get() + "\t" + s));
              }
            }
          }
          scount += list.size();
          sectionRelations.put(key, list);
        }
      }
      final List<EegActivity> activities = doc.get(annset, EegActivity.TYPE);
      final List<Event> events = doc.get(annset, Event.TYPE);
      for (int i = 0; i < events.size(); i++) {
        final Event e1 = events.get(i);
        final Optional<String> normalizedEvent = normalizeEvent(e1);
        if (normalizedEvent.isPresent()) {
          for (EegActivity activity : activities) {
            docRelations.add(activity.get(EegActivity.morphology) + "\t"+normalizedEvent.get());
            dcount++;
          }
          if (i + 1 < events.size()) {
            for (int j = i + 1; j < events.size(); j++) {
              final Event e2 = events.get(j);
              if (!e2.get(Event.type).equals(e1.get(Event.type))) {
                final Optional<String> ne2 = normalizeEvent(e2);
                if (ne2.isPresent()) {
                  docRelations.add(normalizedEvent.get() + "\t"+ne2.get());
                  dcount++;
                }
              }
            }
          }
        }
      }
      plog.update("doc {} had {} section relations and {} doc relations", doc.getId(), scount, dcount);
    });
    log.info("Extracted {} document triples and {} section triples.", docRelations.size(),
        sectionRelations.values().stream().mapToInt(List::size).sum());
    final Path path = Paths.get(outDirName);
    try {
      for (Pair<SectionTitle, SectionTitle> pair : sectionRelations.keySet()) {
        Files.write(path.resolve(pair.first().name() + "_" + pair.second().name() + ".tsv"), sectionRelations.get(pair));
      }
      Files.write(path.resolve("doc_relations.tsv"), docRelations);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private static Optional<String> normalizeEvent(Event event) {
    return ConceptNormalization.normalizeConcept(event);
  }

  private static <A1 extends Annotation<A1>, A2 extends Annotation<A2>> Optional<String> generateTriple(
      SectionTitle subjSt, SectionTitle objSt, A1 subj, A2 obj) {
    // we don't want any X -> X relations
    if (subj.equals(obj)) return Optional.empty();
    final Optional<String> normalizedSubj = ConceptNormalization.normalizeConcept(subj);
    // if subject can be successfully normalized
    if (normalizedSubj.isPresent()) {
      final Optional<String> normalizedObj = ConceptNormalization.normalizeConcept(obj);
      // if object can be successfully normalized
      if (normalizedObj.isPresent()) {
        if (!normalizedObj.get().equals(normalizedSubj.get())) {
          final Optional<RelationType> relOp = getRelationType(subjSt, objSt, EntityType.getType(normalizedSubj.get()),
              EntityType.getType(normalizedObj.get()));
          if (relOp.isPresent()) {
            return Optional.of(normalizedSubj.get() + "\t" + relOp.get() + "\t" + normalizedObj.get());
          }
        }
      }
    }

//    final Optional<RelationType> relOp = getRelationType(subjSt, objSt, EntityType.getType(subj), EntityType.getType(obj));
//    // if is valid relation type
//    if (relOp.isPresent()) {
//      final Optional<String> normalizedSubj = EntityNormalization.normalizeConcept(subj);
//      // if subject can be successfully normalized
//      if (normalizedSubj.isPresent()) {
//        final Optional<String> normalizedObj = EntityNormalization.normalizeConcept(obj);
//        // if object can be successfully normalized
//        if (normalizedObj.isPresent()) {
//          // we don't want any X -> X relations
//          if (!normalizedObj.get().equals(normalizedSubj.get())) {
//            return Optional.of(normalizedSubj.get() + "\t" + relOp.get() + "\t" + normalizedObj.get());
//          }
//        }
//      }
//    }
    return Optional.empty();
  }

  static Optional<RelationType> getRelationType(SectionTitle subjSt, SectionTitle objSt, EntityType subjType,
                                                EntityType objType) {
    if (subjType == EntityType.TEST || objType == EntityType.TEST) {
      return Optional.empty();
    }
    RelationType type = null;
    if (subjSt == objSt && objType == subjType) {
      // subj = obj from same section
      type = RelationType.OCCURS_WITH;
    }
    else if (objType == EntityType.ACTIVITY) {
      if (subjType == EntityType.TREATMENT || subjType == EntityType.PROBLEM) {
        if (objSt != SectionTitle.HISTORY && (subjSt == SectionTitle.HISTORY || subjSt == SectionTitle.DESCRIPTION)) {
          // subj = treatment or problem from history or description
          // obj = non-historical activity
          type = RelationType.EVOKES;
        }
      } else if (subjType == EntityType.EVENT) {
        // subj = event
        // obj  = activity
        type = RelationType.EVOKES;
      }
    } else if (objType == EntityType.PROBLEM) {
      if (objSt == SectionTitle.CORRELATION) {
        // subj = anything from anywhere
        // obj  = problem from correlation
        type = RelationType.EVIDENCES;
      } else if (objSt == SectionTitle.HISTORY && subjType == EntityType.TREATMENT) {
        // subj = treatment
        // obj = problem from history
        type = RelationType.TREATMENT_FOR;
      }
    }
    return Optional.ofNullable(type);
  }

  @SuppressWarnings("unused")
  private static Optional<RelationType> getRelationTypeOld(SectionTitle subjSt, SectionTitle objSt,
                                                           EntityType subjType, EntityType objType) {
    if (subjType == EntityType.TEST || objType == EntityType.TEST) {
      return Optional.empty();
    }
    RelationType type = null;
    if (objType == EntityType.ACTIVITY) {
      if (subjType == EntityType.TREATMENT || subjType == EntityType.PROBLEM) {
        if (subjSt == SectionTitle.HISTORY || subjSt == SectionTitle.DESCRIPTION) {
          type = RelationType.EVOKES;
        }
      } else if (subjType == EntityType.EVENT) {
        type = RelationType.EVOKES;
      } else if (subjType == EntityType.ACTIVITY) {
        type = RelationType.OCCURS_WITH;
      }
    } else if (objType == EntityType.PROBLEM) {
      if (objSt == SectionTitle.CORRELATION && subjSt != SectionTitle.CORRELATION) {
        type = RelationType.EVIDENCES;
      } else if (objSt == SectionTitle.HISTORY && subjType == EntityType.TREATMENT) {
        type = RelationType.TREATMENT_FOR;
      } else if (subjType == EntityType.PROBLEM) {
        type = RelationType.OCCURS_WITH;
      }
    } else if (objType == subjType) {
      type = RelationType.OCCURS_WITH;
    }
    return Optional.ofNullable(type);
  }

  @SuppressWarnings("unused")
  private static void reversePolarities() {
    final Multiset<String> ms = HashMultiset.create();
    final String annset = "run9";
    Function<String, String> polInverter = s -> {
      switch(s) {
        case "POSITIVE": return "NEGATIVE";
        case "NEGATIVE": return "POSITIVE";
        default: throw new RuntimeException("Unrecognized polarity: |" + s + "|");
      }
    };
    Data.v060(annset).forEachDocument(doc -> {
      doc.get(annset, Event.TYPE).forEach(e -> ms.add(e.get(Event.polarity)));
      doc.get(annset, EegActivity.TYPE).forEach(e -> ms.add(e.get(EegActivity.polarity)));
    });
//    Data.v060(annset).forEachDocument(doc -> {
//      doc.get(annset, Event.TYPE).forEach(e -> e.set(Event.polarity, polInverter.apply(e.get(Event.polarity))));
//      doc.get(annset, EegActivity.TYPE).forEach(e -> e.set(EegActivity.polarity, polInverter.apply(e.get(EegActivity.polarity))));
//      doc.sync();
//    });
    System.out.println(ms.toString());
  }
}
