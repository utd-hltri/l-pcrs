package edu.utdallas.hltri.eeg;

import com.google.common.collect.*;
import edu.utdallas.hltri.eeg.annotation.EegActivity;
import edu.utdallas.hltri.eeg.annotation.label.EventTypeLabel;
import edu.utdallas.hltri.eeg.annotation.label.ModalityLabel;
import edu.utdallas.hltri.eeg.annotation.label.PolarityLabel;
import edu.utdallas.hltri.eeg.annotators.AttributeNetworkActiveLearner.*;
import edu.utdallas.hltri.eeg.feature.*;
import edu.utdallas.hltri.logging.Logger;
import edu.utdallas.hltri.ml.Feature;
import edu.utdallas.hltri.ml.label.EnumLabel;
import edu.utdallas.hltri.ml.feature.AnnotationVectorizer;
import edu.utdallas.hltri.ml.label.IoLabel;
import edu.utdallas.hltri.ml.label.Label;
import edu.utdallas.hltri.ml.vector.CountingSparseFeatureVectorizer;
import edu.utdallas.hltri.ml.vector.SparseFeatureVector;
import edu.utdallas.hltri.ml.vector.SparseFeatureVectorizer;
import edu.utdallas.hltri.ml.vector.VectorUtils;
import edu.utdallas.hltri.scribe.text.BaseDocument;
import edu.utdallas.hltri.scribe.text.Document;
import edu.utdallas.hltri.scribe.text.annotation.Event;
import edu.utdallas.hltri.scribe.text.annotation.Sentence;
import edu.utdallas.hltri.scribe.text.annotation.Token;
import edu.utdallas.hltri.struct.Pair;
import edu.utdallas.hltri.util.IntIdentifier;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by rmm120030 on 10/12/16.
 */
public class TensorflowUtils {
  private static Logger log = Logger.get(TensorflowUtils.class);

  public static <D extends BaseDocument> void writeActivityVectorsOneLocation(final List<Document<D>> documents,
                                                                              final String outDir, final String annset) {
    final AnnotationVectorizer<EegActivity> vectorizer = new AnnotationVectorizer<EegActivity>(
        FeatureUtils.attributeFeatureExtractors(annset),
        a -> EnumLabel.NULL, new IntIdentifier<>());
    final List<String> vectors = Lists.newArrayList();
    final Multimap<String, String> labelMap = HashMultimap.create();
    documents.parallelStream().flatMap(doc -> doc.get(annset, EegActivity.TYPE).stream()).forEach(act -> {
      vectors.add(act.getId() + " " + vectorizer.vectorizeAnnotation(act).noLabel());
      labelMap.put("MORPHOLOGY", act.getId() + " " + EegActivity.Morphology.valueOf(act.get(EegActivity.morphology)).numericValue().intValue());
      labelMap.put("FREQUENCY_BAND", act.getId() + " " + EegActivity.Band.valueOf(act.get(EegActivity.band)).numericValue().intValue());
      labelMap.put("HEMISPHERE", act.getId() + " " + EegActivity.Hemisphere.valueOf(act.get(EegActivity.hemisphere)).numericValue().intValue());
      labelMap.put("DISPERSAL", act.getId() + " " + EegActivity.Dispersal.valueOf(act.get(EegActivity.dispersal)).numericValue().intValue());
      labelMap.put("RECURRENCE", act.getId() + " " + EegActivity.Recurrence.valueOf(act.get(EegActivity.recurrence)).numericValue().intValue());
      labelMap.put("BACKGROUND", act.getId() + " " + EegActivity.In_Background.valueOf(act.get(EegActivity.in_background)).numericValue().intValue());
      labelMap.put("MAGNITUDE", act.getId() + " " + EegActivity.Magnitude.valueOf(act.get(EegActivity.magnitude)).numericValue().intValue());
      labelMap.put("LOCATION", act.getId() + " " + act.getLocations().stream().map(l -> l.numericValue().toString()).reduce("", (l1, l2) -> l1 + l2 + " "));
      labelMap.put("MODALITY", act.getId() + " " + ModalityLabel.valueOf(act.get(EegActivity.modality)).numericValue().intValue());
      labelMap.put("POLARITY", act.getId() + " " + PolarityLabel.fromString(act.get(EegActivity.polarity)).numericValue().intValue());
    });
    try {
      final Path path = Paths.get(outDir).resolve("activity");
      path.toFile().mkdir();
      Files.write(path.resolve("activity_attr.svml"), vectors);
      for (final String lbl : labelMap.keySet()) {
        Files.write(path.resolve(lbl + ".lbl"), labelMap.get(lbl));
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public static <D extends BaseDocument> void writeActivityVectors(final List<Document<D>> documents,
                                                                   final String outDir, final String annset) {
    final AnnotationVectorizer<EegActivity> vectorizer = new AnnotationVectorizer<EegActivity>(
        Arrays.asList(new GoldFeatureExtractor(), new ConceptSpanFeatureExtractor<>(),
            new ContextFeatureExtractor<>(annset), new SectionFeatureExtractor<>()),
        a -> EnumLabel.NULL, new IntIdentifier<>());
    final List<String> vectors = Lists.newArrayList();
    final Multimap<String, String> labelMap = HashMultimap.create();
    documents.parallelStream().flatMap(doc -> doc.get(annset, EegActivity.TYPE).stream()).forEach(act -> {
      vectors.add(act.getId() + " " + vectorizer.vectorizeAnnotation(act).noLabel());
      labelMap.put("MORPHOLOGY", act.getId() + " " + EegActivity.Morphology.valueOf(act.get(EegActivity.morphology)).numericValue().intValue());
      labelMap.put("FREQUENCY_BAND", act.getId() + " " + EegActivity.Band.valueOf(act.get(EegActivity.band)).numericValue().intValue());
      labelMap.put("HEMISPHERE", act.getId() + " " + EegActivity.Hemisphere.valueOf(act.get(EegActivity.hemisphere)).numericValue().intValue());
      labelMap.put("DISPERSAL", act.getId() + " " + EegActivity.Dispersal.valueOf(act.get(EegActivity.dispersal)).numericValue().intValue());
      labelMap.put("RECURRENCE", act.getId() + " " + EegActivity.Recurrence.valueOf(act.get(EegActivity.recurrence)).numericValue().intValue());
      labelMap.put("BACKGROUND", act.getId() + " " + EegActivity.In_Background.valueOf(act.get(EegActivity.in_background)).numericValue().intValue());
      labelMap.put("MAGNITUDE", act.getId() + " " + EegActivity.Magnitude.valueOf(act.get(EegActivity.magnitude)).numericValue().intValue());
      act.getLocations().forEach(loc -> labelMap.put(loc.toString(), act.getId() + " " + 1.0));
      labelMap.put("MODALITY", act.getId() + " " + ModalityLabel.valueOf(act.get(EegActivity.modality)).numericValue().intValue());
      labelMap.put("POLARITY", act.getId() + " " + PolarityLabel.fromString(act.get(EegActivity.polarity)).numericValue().intValue());
    });
    try {
      final Path path = Paths.get(outDir).resolve("activity");
      path.toFile().mkdir();
      Files.write(path.resolve("activity_attr.svml"), vectors);
      for (final String lbl : labelMap.keySet()) {
        Files.write(path.resolve(lbl + ".lbl"), labelMap.get(lbl));
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public static <D extends BaseDocument> void writeEventVectors(final List<Document<D>> documents, final String outDir,
                                                                final String annset) {
    final AnnotationVectorizer<Event> vectorizer = new AnnotationVectorizer<Event>(FeatureUtils.attributeFeatureExtractors(annset),
        a -> EnumLabel.NULL, new IntIdentifier<>());
    final List<String> vectors = Lists.newArrayList();
    final Multimap<String, String> labelMap = HashMultimap.create();
    documents.parallelStream().flatMap(doc -> doc.get(annset, Event.TYPE).stream()).forEach(act -> {
      vectors.add(act.getId() + " " + vectorizer.vectorizeAnnotation(act).noLabel());
      labelMap.put("TYPE", act.getId() + " " + EventTypeLabel.valueOf(act.get(Event.type)).numericValue().intValue());
      labelMap.put("MODALITY", act.getId() + " " + ModalityLabel.valueOf(act.get(Event.modality)).numericValue().intValue());
      labelMap.put("POLARITY", act.getId() + " " + PolarityLabel.fromString(act.get(Event.polarity)).numericValue().intValue());
    });
    try {
      final Path path = Paths.get(outDir).resolve("event");
      path.toFile().mkdir();
      Files.write(path.resolve("activity_attr.svml"), vectors);
      for (final String lbl : labelMap.keySet()) {
        Files.write(path.resolve(lbl + ".lbl"), labelMap.get(lbl));
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public static List<AttributeConfidence> readActivityConfidences(final Path path) {
    try {
      final List<String> lines = Files.readAllLines(path);
      return lines.stream().map(AttributeConfidence::new).collect(Collectors.toList());
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public static <D extends BaseDocument> void writeBoundaryVectors(final List<Document<D>> documents,
                                                                   final String outDir, final String annset) {
    final CountingSparseFeatureVectorizer<Number> vectorizer = new CountingSparseFeatureVectorizer<>();
//    final AnnotationVectorizer<Token> avectorizer = new AnnotationVectorizer<>(
//        FeatureUtils.boundaryDetectionFeatureExtractors(),
//        FeatureUtils.boundaryLabeler(annset, EegActivity.TYPE), new IntIdentifier<>());
//    final AnnotationVectorizer<Token> evectorizer = new AnnotationVectorizer<>(
//        FeatureUtils.boundaryDetectionFeatureExtractors(),
//        FeatureUtils.boundaryLabeler(annset, Event.TYPE), new IntIdentifier<>());
    final List<List<Pair<Label, SparseFeatureVector<Number>>>> as = Lists.newArrayList(), es = Lists.newArrayList();
    final String sas = "opennlp", tas = "genia";
    final Multiset<Integer> sizes = HashMultiset.create();
    documents.forEach(doc -> {
      for (Sentence sentence : doc.get(sas, Sentence.TYPE)) {
        final List<Pair<Label, SparseFeatureVector<Number>>> alist = Lists.newArrayList(), elist = Lists.newArrayList();
        int size = 0;
        for (Token token : sentence.getContained(tas, Token.TYPE)) {
          final SparseFeatureVector<Number> fv = vectorizer.vectorize(FeatureUtils.boundaryDetectionFeatureExtractors().stream()
              .flatMap(fe -> fe.apply(token))
              .map(Feature::toNumericFeature));
          alist.add(new Pair<>(FeatureUtils.ioBoundaryLabeler(annset, EegActivity.TYPE).apply(token), fv));
          elist.add(new Pair<>(FeatureUtils.ioBoundaryLabeler(annset, Event.TYPE).apply(token), fv));
          size++;
        }
        sizes.add(size);
        as.add(alist);
        es.add(elist);
      }
      doc.close();
    });
    log.info("num sentences: {}", sizes.size());
    final int gt20 = (int)sizes.stream().filter(s -> s > 38).mapToInt(s -> s).count();
    final int gt35 = (int)sizes.stream().filter(s -> s > 39).mapToInt(s -> s).count();
    log.info("num sentences with length > 38: {}, {}%", gt20, 100.0 * gt20 / sizes.size());
    log.info("num sentences with length > 39: {}, {}%", gt35, 100.0 * gt35 / sizes.size());

    vectorizer.lockAndRemoveUncommonFeatures(2);

    try {
      final Path apath = Paths.get(outDir).resolve("activity");
      apath.toFile().mkdir();
      Files.write(apath.resolve("boundary.svml"), getVectorStrings(as, vectorizer));
      vectorizer.getFeatureIdentifier().toFile(apath.resolve("boundary.tsv"));
      final Path epath = Paths.get(outDir).resolve("event");
      epath.toFile().mkdir();
      Files.write(epath.resolve("boundary.svml"), getVectorStrings(es, vectorizer));
      vectorizer.getFeatureIdentifier().toFile(epath.resolve("boundary.tsv"));
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public static <D extends BaseDocument> List<List<String>> generateUnlabeledBoundaryVectors(
      final List<Document<D>> documents, final IntIdentifier<String> iid) {
    final SparseFeatureVectorizer<Number> vectorizer = new SparseFeatureVectorizer<>(iid);
    final String sas = "opennlp", tas = "genia";
    final List<List<String>> vectorStrings = new ArrayList<>();
    documents.forEach(doc -> {
      for (Sentence sentence : doc.get(sas, Sentence.TYPE)) {
        final List<String> sequence = new ArrayList<>();
        for (Token token : sentence.getContained(tas, Token.TYPE)) {
          final SparseFeatureVector<Number> fv = vectorizer.vectorize(FeatureUtils.boundaryDetectionFeatureExtractors().stream()
              .flatMap(fe -> fe.apply(token))
              .map(Feature::toNumericFeature));
          sequence.add(IoLabel.O + " " + VectorUtils.toZeroIndexedSvmlWithId(fv,
              doc.getId() + "|" + sentence.getId() + "|" + token.getId()));
        }
        vectorStrings.add(sequence);
      }
    });

    return vectorStrings;
  }

  public static <D extends BaseDocument> void writeUnlabeledBoundaryVectors(final List<Document<D>> documents,
                                                                            final String outDir,
                                                                            final String iidFile,
                                                                            final String outfileName) {
    final List<String> vectorStrings = generateUnlabeledBoundaryVectors(documents,
        IntIdentifier.fromFile(iidFile).lock()).stream()
        .reduce((l1, l2) -> {l1.add(""); l1.addAll(l2); return l1;}).get();

    try {
      Files.write(Paths.get(outDir).resolve(outfileName), vectorStrings);
      log.info("Wrote {} lines to {}", vectorStrings.size(), Paths.get(outDir).resolve(outfileName));
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private static List<String> getVectorStrings(List<List<Pair<Label, SparseFeatureVector<Number>>>> labeledVectorLists,
                                               CountingSparseFeatureVectorizer<Number> vectorizer) {
    final List<String> strings = new ArrayList<>();
    for (List<Pair<Label, SparseFeatureVector<Number>>> list : labeledVectorLists) {
      for (Pair<Label, SparseFeatureVector<Number>> pair : list) {
        strings.add(pair.first() + " " + VectorUtils.toZeroIndexedSvml(vectorizer.removeUncommon(pair.second())));
      }
      strings.add("");
    }
    return strings;
  }
}
