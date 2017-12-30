package edu.utdallas.hltri.eeg;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;

import com.eclipsesource.json.WriterConfig;
import com.google.common.base.CharMatcher;
import com.google.common.base.Splitter;
import edu.utdallas.hltri.eeg.annotation.EegActivity;
import edu.utdallas.hltri.eeg.annotators.RegExEegAnnotator;
import edu.utdallas.hltri.eeg.relation.*;
import edu.utdallas.hltri.framework.Commands;
import edu.utdallas.hltri.framework.ProgressLogger;
import edu.utdallas.hltri.func.CloseableConsumer;
import edu.utdallas.hltri.io.AC;
import edu.utdallas.hltri.logging.Logger;
import edu.utdallas.hltri.scribe.annotators.GeniaAnnotator;
import edu.utdallas.hltri.scribe.annotators.OpenNLPSentenceAnnotator;
import edu.utdallas.hltri.scribe.io.Corpus;
import edu.utdallas.hltri.scribe.io.JsonCorpus;
import edu.utdallas.hltri.scribe.text.Attribute;
import edu.utdallas.hltri.scribe.text.BaseDocument;
import edu.utdallas.hltri.scribe.text.Document;
import edu.utdallas.hltri.scribe.text.annotation.*;
import edu.utdallas.hltri.scribe.text.relation.AbstractRelation;
import edu.utdallas.hltri.scribe.text.relation.Relation;
import edu.utdallas.hltri.struct.Pair;
import edu.utdallas.hltri.util.Offset;
import picocli.CommandLine;

import static edu.utdallas.hltri.eeg.ExtractTriples.SectionTitle;
import static edu.utdallas.hltri.eeg.ExtractTriples.EntityType;
import static edu.utdallas.hltri.eeg.ExtractTriples.RelationType;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Created by rmm120030 on 9/5/17.
 */
public class RelnetDataGenerator implements AC {
  private static final Logger log = Logger.get(RelnetDataGenerator.class);
  private static final String DELIMITER = ":::";
  private static final Attribute<Annotation<?>, String> normalizedForm = Attribute.typed("normalized_form", String.class);

  private final BufferedWriter writer;
  private final String conceptAnnset, sentenceAnnset, tokenAnnset;
  private final Pattern validSectionTitleRegex = Pattern.compile(Arrays.stream(SectionTitle.values())
      .map(t -> t.pattern.pattern()).collect(Collectors.joining("|")), Pattern.CASE_INSENSITIVE);

  public RelnetDataGenerator(Path outfile, String conceptAnnset, String sentenceAnnset, String tokenAnnset)
      throws IOException {
    this.writer = Files.newBufferedWriter(outfile);
    this.conceptAnnset = conceptAnnset;
    this.sentenceAnnset = sentenceAnnset;
    this.tokenAnnset = tokenAnnset;
  }

  public void writeDocuments(Stream<Document<EegNote>> documents) throws IOException {
    final JsonArray records = new JsonArray();
    documents.forEach(doc -> records.add(makeJsonNoisy(doc)));
    Json.object().add("records", records).writeTo(writer, WriterConfig.MINIMAL);
  }

  public void writeDocuments(Corpus<EegNote> corpus,
                             Function<Pair<String, String>, Optional<String>> relationLabeler) throws IOException {
    final JsonArray records = new JsonArray();
    final ProgressLogger plog = ProgressLogger.fixedSize("jsonifying", Data.V060_SIZE, 5, TimeUnit.SECONDS);
    final AtomicInteger numLabels = new AtomicInteger();
    corpus.forEachDocument(doc -> {
      final JsonObject record = makeJsonWithLabels(doc, relationLabeler);
      final int l = record.get("labels").asArray().size();
      final int total = numLabels.addAndGet(l);
      plog.update("doc {} has {} labels. {} total", doc.getId(), l, total);
      if(l > 0) {
        records.add(record);
      }
    });
    log.info("Extracted data from {} records with {} relation labels", records.size(), numLabels.get());
    Json.object().add("records", records).writeTo(writer, WriterConfig.PRETTY_PRINT);
  }

  public void writeManuallyLabeledDocuments(List<Document<EegNote>> corpus) throws IOException {
    final JsonArray records = new JsonArray();
    final ProgressLogger plog = ProgressLogger.fixedSize("jsonifying", Data.V060_SIZE, 5, TimeUnit.SECONDS);
    final AtomicInteger numLabels = new AtomicInteger();
    corpus.forEach(doc -> {
      final JsonObject record = makeJsonWithLabels(doc);
      final int l = record.get("labels").asArray().size();
      final int total = numLabels.addAndGet(l);
      plog.update("doc {} has {} labels. {} total", doc.getId(), l, total);
      if(l > 0) {
        records.add(record);
      }
      doc.close();
    });
    log.info("Extracted data from {} records with {} relation labels", records.size(), numLabels.get());
    Json.object().add("records", records).writeTo(writer, WriterConfig.PRETTY_PRINT);
  }

  private <R extends Relation<R,?,?> & EegRelation> Optional<String> contains(Set<R> relations,
                                                                              Annotation<?> gov,
                                                                              Annotation<?> dep) {
    for (R relation : relations) {
      if (relation.getGovernor().getId().equals(gov.getId()) && relation.getDependant().getId().equals(dep.getId())) {
        return Optional.of(relation.getRelationType());
      }
    }
    return Optional.empty();
  }

  private JsonObject makeJsonWithLabels(Document<EegNote> document) {
    final JsonObject record = Json.object().add("id", document.getId());
    List<Section> sections = document.get("regex-eeg", Section.TYPE);
    sections = sections.stream()
        .filter(sec -> validSectionTitleRegex.matcher(sec.get(Section.title)).matches())
        .collect(Collectors.toList());

    final Set<EEEegRelation> eeRelations = document.getRelations(conceptAnnset, EEEegRelation.TYPE);
    final Set<EAEegRelation> eaRelations = document.getRelations(conceptAnnset, EAEegRelation.TYPE);
    final Set<AEEegRelation> aeRelations = document.getRelations(conceptAnnset, AEEegRelation.TYPE);

    // Write section text to json (with concepts replaced with normalized concept tokens)
    final Map<SectionTitle, List<Annotation<?>>> section2concepts = new EnumMap<>(SectionTitle.class);
    final JsonArray jsonSections = createJsonSections(sections, section2concepts);
    record.add("sections", jsonSections);

    final JsonArray jsonLabels = new JsonArray();
    final ArrayList<SectionTitle> sectionTitles = new ArrayList<>(section2concepts.keySet());
    for (int i = 0; i < sectionTitles.size(); i++) {
      final SectionTitle st1 = sectionTitles.get(i);
      for (Annotation<?> a1: section2concepts.get(st1)) {
        final String subj = a1.get(normalizedForm);
        for (int j = i; j < sectionTitles.size(); j++) {
          final SectionTitle st2 = sectionTitles.get(j);
          for (Annotation<?> a2 : section2concepts.get(st2)) {
            final String obj = a2.get(normalizedForm);
            if (!subj.equals(obj)) {
              final AtomicBoolean hasRel = new AtomicBoolean(false);
              final AtomicBoolean hasRevRel = new AtomicBoolean(false);
              contains(eeRelations, a1, a2).ifPresent(label -> {
                hasRel.set(true);
                jsonLabels.add(createRelationLabel(subj, st1, obj, st2, label));
              });
              contains(eeRelations, a2, a1).ifPresent(label -> {
                hasRevRel.set(true);
                jsonLabels.add(createRelationLabel(obj, st2, subj, st1, label));
              });
              contains(eaRelations, a1, a2).ifPresent(label -> {
                hasRel.set(true);
                jsonLabels.add(createRelationLabel(subj, st1, obj, st2, label));
                if ("EVOKES".equals(label) && subj.startsWith("P")) {
                  jsonLabels.add(createRelationLabel(obj, st2, subj, st1, "EVIDENCES"));
                }
              });
              contains(eaRelations, a2, a1).ifPresent(label -> {
                hasRevRel.set(true);
                jsonLabels.add(createRelationLabel(obj, st2, subj, st1, label));
                if ("EVOKES".equals(label) && obj.startsWith("P")) {
                  jsonLabels.add(createRelationLabel(subj, st1, obj, st2, "EVIDENCES"));
                }
              });
              contains(aeRelations, a1, a2).ifPresent(label -> {
                hasRel.set(true);
                jsonLabels.add(createRelationLabel(subj, st1, obj, st2, label));
                if ("EVIDENCES".equals(label) && obj.startsWith("P")) {
                  jsonLabels.add(createRelationLabel(obj, st2, subj, st1, "EVOKES"));
                }
              });
              contains(aeRelations, a2, a1).ifPresent(label -> {
                hasRevRel.set(true);
                jsonLabels.add(createRelationLabel(obj, st2, subj, st1, label));
                if ("EVIDENCES".equals(label) && subj.startsWith("P")) {
                  jsonLabels.add(createRelationLabel(subj, st1, obj, st2, "EVOKES"));
                }
              });

              if (!hasRel.get()) {
                jsonLabels.add(createRelationLabel(subj, st1, obj, st2, "NONE"));
              }
            }
          }
        }
      }
    }

    record.add("labels", jsonLabels);
    return record;
  }

  /**
   *
   * @param document document
   * @param relationLabeler (subj, obj) -> Optional(relation type)
   * @return a JsonObject representation of the document
   */
  private JsonObject makeJsonWithLabels(Document<EegNote> document,
                                        Function<Pair<String, String>, Optional<String>> relationLabeler) {
    final JsonObject record = Json.object().add("id", document.getId());
    List<Section> sections = document.get("regex-eeg", Section.TYPE);
    sections = sections.stream()
        .filter(sec -> validSectionTitleRegex.matcher(sec.get(Section.title)).matches())
        .collect(Collectors.toList());

    // Write section text to json (with concepts replaced with normalized concept tokens)
    final Map<SectionTitle, List<Annotation<?>>> section2concepts = new EnumMap<>(SectionTitle.class);
    final JsonArray jsonSections = createJsonSections(sections, section2concepts);
    record.add("sections", jsonSections);

    final JsonArray jsonLabels = new JsonArray();
    final ArrayList<SectionTitle> sectionTitles = new ArrayList<>(section2concepts.keySet());
    for (int i = 0; i < sectionTitles.size(); i++) {
      final SectionTitle st1 = sectionTitles.get(i);
      for (Annotation<?> a1: section2concepts.get(st1)) {
        final String subj = a1.get(normalizedForm);
        for (int j = i; j < sectionTitles.size(); j++) {
          final SectionTitle st2 = sectionTitles.get(j);
          for (Annotation<?> a2 : section2concepts.get(st2)) {
            final String obj = a2.get(normalizedForm);
            if (!subj.equals(obj)) {
              relationLabeler.apply(Pair.of(subj, obj)).ifPresent(label ->
                  jsonLabels.add(createRelationLabel(subj, st1, obj, st2, label)));
              relationLabeler.apply(Pair.of(obj, subj)).ifPresent(label ->
                  jsonLabels.add(createRelationLabel(obj, st2, subj, st1, label)));
//              // get the rule-based relation type
//              final Optional<RelationType> relationType = ExtractTriples.getRelationType(st1, st2,
//                  EntityType.getType(subj), EntityType.getType(obj));
//              if (relationType.isPresent()) {
//                final Optional<String> labelOp = relationLabeler.apply(Pair.of(subj, obj));
//                labelOp.ifPresent(label -> {
//                  jsonLabels.add(createRelationLabel(subj, st1, obj, st2, label));
//                });
//              }
              if (st1 == SectionTitle.IMPRESSION && subj.startsWith("A_") &&
                  st2 == SectionTitle.CORRELATION && (obj.startsWith("P_"))) {
                jsonLabels.add(createRelationLabel(obj, st2, subj, st1, "EVOKES"));
              }
              else if (st2 == SectionTitle.IMPRESSION && obj.startsWith("A_") &&
                  st1 == SectionTitle.CORRELATION && (subj.startsWith("P_"))) {
                jsonLabels.add(createRelationLabel(subj, st1, obj, st2, "EVOKES"));
              }
            }
          }
        }
      }
    }

    record.add("labels", jsonLabels);
    return record;
  }

  private JsonObject createRelationLabel(String subj, SectionTitle subjSt, String obj, SectionTitle objSt, String label) {
    final JsonObject jsonLabel = new JsonObject();
    jsonLabel.add("section1", subjSt.name());
    jsonLabel.add("subject", subj);
    jsonLabel.add("section2", objSt.name());
    jsonLabel.add("object", obj);
    jsonLabel.add("label", label);
    return jsonLabel;
  }

  private JsonObject makeJsonNoisy(Document<EegNote> document) {
    final JsonObject record = Json.object().add("id", document.getId());
    List<Section> sections = document.get("regex-eeg", Section.TYPE);
    sections = sections.stream()
        .filter(sec -> validSectionTitleRegex.matcher(sec.get(Section.title)).matches())
        .collect(Collectors.toList());

    // Write section text to json (with concepts replaced with normalized concept tokens)
    final Map<SectionTitle, List<Annotation<?>>> section2concepts = new EnumMap<>(SectionTitle.class);
    final JsonArray jsonSections = createJsonSections(sections, section2concepts);
    record.add("sections", jsonSections);

    final JsonArray jsonLabels = new JsonArray();
    final ArrayList<SectionTitle> sectionTitles = new ArrayList<>(section2concepts.keySet());
    for (int i = 0; i < sectionTitles.size(); i++) {
      final SectionTitle st1 = sectionTitles.get(i);
      for (Annotation<?> a1: section2concepts.get(st1)) {
        final String subj = a1.get(normalizedForm);
        for (int j = i; j < sectionTitles.size(); j++) {
          final SectionTitle st2 = sectionTitles.get(j);
          for (Annotation<?> a2 : section2concepts.get(st2)) {
            final String obj = a2.get(normalizedForm);
            if (!subj.equals(obj)) {
              // get the rule-based relation type
              final Optional<RelationType> relationType = ExtractTriples.getRelationType(st1, st2,
                  EntityType.getType(subj), EntityType.getType(obj));
              final JsonObject jsonLabel = new JsonObject();
              jsonLabel.add("section1", st1.name());
              jsonLabel.add("subject", subj);
              jsonLabel.add("section2", st2.name());
              jsonLabel.add("object", obj);
              jsonLabel.add("label", relationType.map(RelationType::name).orElse("NONE"));
              jsonLabels.add(jsonLabel);
            }
          }
        }
      }
    }

    record.add("labels", jsonLabels);
    return record;
  }

  private JsonArray createJsonSections(Collection<Section> sections, Map<SectionTitle, List<Annotation<?>>> section2concepts) {
    final JsonArray jsonSections = new JsonArray();
    for (Section section : sections) {
      final JsonObject jsonSection = Json.object();
      final SectionTitle title = SectionTitle.fromString(section.get(Section.title));
      final Set<Annotation<?>> sectionConcepts = new HashSet<>();
      final JsonArray jsonSentences = new JsonArray();
      for (Sentence sentence : section.getContained(sentenceAnnset, Sentence.TYPE)) {
        final List<Token> tokens = sentence.getContained(tokenAnnset, Token.TYPE);
        final ArrayList<Annotation<?>> concepts = new ArrayList<>(sentence.getContained(conceptAnnset, EegActivity.TYPE));
        concepts.addAll(sentence.getContained(conceptAnnset, Event.TYPE));
        final List<Annotation<?>> merged = Offset.mergeSequencesWithSubsumption(tokens, concepts);
        final String sentenceString = merged.stream().map(ann -> {
          if (ann instanceof Token) {
            return ann.toString().toLowerCase();
          } else {
            final Optional<String> normalized = ConceptNormalization.normalizeConceptWithAttributes(ann, DELIMITER);
            normalized.ifPresent(nf -> {
              ann.set(normalizedForm, nf);
              sectionConcepts.add(ann);
            });
            return normalized.orElse(ann.toString().toLowerCase());
          }
        }).collect(Collectors.joining(" "));
        jsonSentences.add(sentenceString);
      }
      // add name
      jsonSection.add("name", title.name());
      // add sentences
      jsonSection.add("sentences", jsonSentences);
      final JsonArray jsonConcepts = new JsonArray();
      sectionConcepts.stream().map(a -> a.get(normalizedForm)).forEach(jsonConcepts::add);
      // add concepts
      jsonSection.add("concepts", jsonConcepts);

      // add the filled out section
      jsonSections.add(jsonSection);
      // save the extracted/normalized concepts so we don't have to extract/normalize them again
      section2concepts.put(title, new ArrayList<>(sectionConcepts));
    }
    return jsonSections;
  }

  @Override
  public void close() {
    try {
      writer.close();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private static class Arguments {
    @CommandLine.Option(names = { "-o", "--outfile" }, description = "full path to json output file")
    private Path outfile = Paths.get("/home/rmm120030/working/eeg/relnet/data.json");

    @CommandLine.Option(names = { "-c", "--correct-relations" }, description = "full path to correct relations file")
    private Path correctRelations = Paths.get("/home/rmm120030/working/eeg/relnet/good_triples/correct.tsv");

    @CommandLine.Option(names = { "-i", "--incorrect-relations" }, description = "full path to incorrect relations file")
    private Path incorrectRelations = Paths.get("/home/rmm120030/working/eeg/relnet/good_triples/incorrect.tsv");

    @CommandLine.Option(names = { "-b", "--brat-corpus" }, description = "full path to incorrect relations file")
    private String bratCorpus = "/home/rmm120030/working/eeg/brat/al/seed";
  }

  private static void addRelationsToMap(Map<Pair<String, String>, String> labelMap, Path infile, boolean correctLabels) throws IOException {
    final Splitter splitter = Splitter.on(CharMatcher.whitespace()).omitEmptyStrings();
    Files.readAllLines(infile).stream().map(splitter::splitToList).forEach(list -> {
      final Pair<String, String> args = Pair.of(list.get(1), list.get(3));
      if (correctLabels) {
        labelMap.put(args, list.get(2));
      } else {
        labelMap.put(args, "NONE");
      }
    });
  }

  public static void main(String... args) {
    final Arguments arguments = Commands.createArguments(Arguments::new, args);
    final String concAnnset = "gold";
    try (final RelnetDataGenerator rdg = new RelnetDataGenerator(arguments.outfile, concAnnset, "opennlp", "genia")) {
//      final JsonCorpus<EegNote> corpus = Data.v060("best", "opennlp", "genia", "regex-eeg");
//
//      final Map<Pair<String, String>, String> labelMap = new HashMap<>();
//      addRelationsToMap(labelMap, arguments.correctRelations, true);
//      addRelationsToMap(labelMap, arguments.incorrectRelations, false);
//
//      // Ignore attributes
//      final Function<Pair<String, String>, Optional<String>> tripleLabeler = pair ->
//          Optional.ofNullable(labelMap.get(Pair.of(
//              pair.first().substring(0, pair.first().indexOf(DELIMITER)),
//              pair.second().substring(0, pair.second().indexOf(DELIMITER)))
//          ));
//      rdg.writeDocuments(corpus, tripleLabeler);

      final RelationBratCorpus bratCorpus = RelationBratCorpus.at(arguments.bratCorpus, concAnnset);
      final OpenNLPSentenceAnnotator<BaseDocument> onlp = new OpenNLPSentenceAnnotator<>();
      final GeniaAnnotator<BaseDocument> genia = GeniaAnnotator.tokenAnnotator(doc -> doc.get("opennlp", Sentence.TYPE), false);
      final RegExEegAnnotator section = RegExEegAnnotator.sectionAnnotator("opennlp");
      final List<Document<EegNote>> docs = new ArrayList<>();
      bratCorpus.getDocumentList().forEach(bdoc -> {
        final int numRels = bdoc.getRelations(concAnnset, EEEegRelation.TYPE).size() +
            bdoc.getRelations(concAnnset, EAEegRelation.TYPE).size() +
            bdoc.getRelations(concAnnset, AEEegRelation.TYPE).size();
        if (numRels > 0) {
          onlp.annotate(bdoc);
          genia.annotate(bdoc);
          section.annotate(bdoc);
          docs.add(bdoc);
          log.info("Brat doc {} has {} relations!", bdoc.getId(), numRels);
        } else {
          log.debug("Brat doc {} has no relations!", bdoc.getId());
          bdoc.close();
        }
      });
      rdg.writeManuallyLabeledDocuments(docs);

    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
