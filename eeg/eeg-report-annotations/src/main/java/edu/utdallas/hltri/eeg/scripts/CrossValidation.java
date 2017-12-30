package edu.utdallas.hltri.eeg.scripts;

import com.google.common.collect.Maps;
import com.lexicalscope.jewel.cli.ArgumentValidationException;
import com.lexicalscope.jewel.cli.Cli;
import com.lexicalscope.jewel.cli.CliFactory;
import com.lexicalscope.jewel.cli.Option;
import edu.utdallas.hltri.eeg.Data;
import edu.utdallas.hltri.eeg.EegNote;
import edu.utdallas.hltri.eeg.annotators.TfBoundaryAnnotator;
import edu.utdallas.hltri.eeg.classifier.AttributeClassifier;
import edu.utdallas.hltri.eeg.annotation.EegActivity;
import edu.utdallas.hltri.eeg.annotation.EegActivity.*;
import edu.utdallas.hltri.eeg.annotation.label.EventTypeLabel;
import edu.utdallas.hltri.eeg.annotation.label.ModalityLabel;
import edu.utdallas.hltri.eeg.annotation.label.PolarityLabel;
import edu.utdallas.hltri.eeg.annotators.CrfsEventBoundaryAnnotator;
import edu.utdallas.hltri.eeg.annotators.SvmAnnotator;
import edu.utdallas.hltri.eeg.classifier.Crf;
import edu.utdallas.hltri.logging.Logger;
import edu.utdallas.hltri.ml.classify.LibLinearSvm;
import edu.utdallas.hltri.ml.eval.CrossValidationSplit;
import edu.utdallas.hltri.ml.eval.F1EvaluationResult;
import edu.utdallas.hltri.ml.eval.MulticlassF1EvalResult;
import edu.utdallas.hltri.ml.label.BinaryLabel;
import edu.utdallas.hltri.ml.label.EnumLabel;
import edu.utdallas.hltri.scribe.text.BaseDocument;
import edu.utdallas.hltri.scribe.text.Document;
import edu.utdallas.hltri.scribe.text.annotation.*;
import edu.utdallas.hltri.util.IntIdentifier;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Used for generating cross validation evaluations of the EEG Concept detection system.
 *
 * Created by rmm120030 on 9/12/16.
 */
@SuppressWarnings("ResultOfMethodCallIgnored")
public class CrossValidation {
  private static final Logger log = Logger.get(CrossValidation.class);

  interface CvOptions {
    enum Type {activity, event, all, kirk}
    @Option(longName = "type", shortName = "t", description = "activity, event, all, kirk")
    Type type();

    enum Attr {boundary, modality, polarity, type, morphology, band, recurrence, dispersal, magnitude, hemisphere, background, all}
    @Option(longName = "attr", shortName = "a", defaultValue = "all", description = "Attribute type.")
    Attr attribute();

    @Option(longName = "folds", shortName = "f", defaultValue = "5", description = "Number of folds.")
    int numFolds();

    enum CorpusType {seed, al, run}
    @Option(longName = "corpus", shortName = "c", defaultValue = "al", description = "Name of the corpus to evaluate.")
    CorpusType corpusType();

    @Option(longName = "run-number", shortName = "r", defaultValue = "10", description = "Run number")
    int runNumber();

    @Option(longName = "outfile", shortName = "o", defaultValue = "none", description = "Output file")
    String outfile();

    @Option(longName = "partial", shortName = "p", defaultValue = "false", description = "Do partial boundary match instead of exact?")
    boolean partialBoundaries();

    @Option(longName = "save", shortName = "s", defaultValue = "false", description = "Do you wanna save the results to a file?")
    boolean save();
  }

  public static void main(String... args) {
    final Cli<CvOptions> cli = CliFactory.createCli(CvOptions.class);
    try {
      final CvOptions options = cli.parseArguments(args);
      List<Document<EegNote>> documents = null;
      switch(options.corpusType()) {
        case seed: documents = Data.seed().getDocumentList();
          break;
        case al: documents = Data.activeLearning();
          break;
        case run: documents = Data.run(options.runNumber());
          break;
      }
      if (options.type() == CvOptions.Type.activity) {
        switch (options.attribute()) {
          case boundary: evaluateActivityBoundariesCV(documents, options.numFolds(), options.partialBoundaries());
            break;
          case morphology: evaluateAttributes(documents, AttributeClassifier::morphology, Morphology.values(),
              options.numFolds(), EegActivity.TYPE);
            break;
          case hemisphere: evaluateAttributes(documents, AttributeClassifier::hemisphere, Hemisphere.values(),
              options.numFolds(), EegActivity.TYPE);
            break;
          case magnitude: evaluateAttributes(documents, AttributeClassifier::magnitude, Magnitude.values(),
              options.numFolds(), EegActivity.TYPE);
            break;
          case recurrence: evaluateAttributes(documents, AttributeClassifier::recurrence, Recurrence.values(),
              options.numFolds(), EegActivity.TYPE);
            break;
          case dispersal: evaluateAttributes(documents, AttributeClassifier::dispersal, Dispersal.values(),
              options.numFolds(), EegActivity.TYPE);
            break;
          case band: evaluateAttributes(documents, AttributeClassifier::band, Band.values(), options.numFolds(),
              EegActivity.TYPE);
            break;
          case background: evaluateAttributes(documents, AttributeClassifier::background, In_Background.values(),
              options.numFolds(), EegActivity.TYPE);
            break;
          case modality: evaluateAttributes(documents, as -> AttributeClassifier.modality(as, EegActivity.modality, EegActivity.TYPE),
              ModalityLabel.values(), options.numFolds(), EegActivity.TYPE);
            break;
          case polarity: evaluateAttributes(documents, as -> AttributeClassifier.polarity(as, EegActivity.polarity, EegActivity.TYPE),
              PolarityLabel.values(), options.numFolds(), EegActivity.TYPE);
            break;
          default: throw new RuntimeException("Only valid attributes for Activities are [boundary, morphology, hemisphere," +
              " magnitude, recurrence, dispersal, band, background, modality, polarity]. "
              + options.attribute() + " was provided.");
        }
      } else if (options.type() == CvOptions.Type.event) {
        switch (options.attribute()) {
          case boundary: evaluateEventBoundariesCV(documents, options.numFolds(), options.partialBoundaries());
            break;
          case modality: evaluateAttributes(documents, as -> AttributeClassifier.modality(as, Event.modality, Event.TYPE),
              ModalityLabel.values(), options.numFolds(), Event.TYPE);
            break;
          case polarity: evaluateAttributes(documents, as -> AttributeClassifier.polarity(as, Event.polarity, Event.TYPE),
              PolarityLabel.values(), options.numFolds(), Event.TYPE);
          case type: evaluateAttributes(documents, AttributeClassifier::eventType, EventTypeLabel.values(),
              options.numFolds(), Event.TYPE);
            break;
          default: throw new RuntimeException("Only valid attributes for Events are [boundary, modality, polarity]. "
          + options.attribute() + " was provided.");
        }
      } else if (options.type() == CvOptions.Type.all) {
        final F1EvaluationResult<Event> evb = evaluateEventBoundariesCV(documents, options.numFolds(), options.partialBoundaries());
        log.info("Done event boundaries.");
        final MulticlassF1EvalResult<String, Event> evmod = evaluateAttributes(documents,
            as -> AttributeClassifier.modality(as, Event.modality, Event.TYPE), ModalityLabel.values(),
            options.numFolds(), Event.TYPE);
        log.info("Done event modalities.");
        final MulticlassF1EvalResult<String, Event> evpol = evaluateAttributes(documents,
            as -> AttributeClassifier.polarity(as, Event.polarity, Event.TYPE), PolarityLabel.values(),
            options.numFolds(), Event.TYPE);
        log.info("Done event polarities.");
        final MulticlassF1EvalResult<String, Event> evtype = evaluateAttributes(documents, AttributeClassifier::eventType,
            EventTypeLabel.values(), options.numFolds(), Event.TYPE);
        final F1EvaluationResult<EegActivity> acb = evaluateActivityBoundariesCV(documents, options.numFolds(), options.partialBoundaries());
        log.info("Done activity boundaries.");
        final MulticlassF1EvalResult<String, EegActivity> acmorph = evaluateAttributes(documents,
            AttributeClassifier::morphology, Morphology.values(), options.numFolds(), EegActivity.TYPE);
        final MulticlassF1EvalResult<String, EegActivity> achemi = evaluateAttributes(documents,
            AttributeClassifier::hemisphere, Hemisphere.values(), options.numFolds(), EegActivity.TYPE);
        final MulticlassF1EvalResult<String, EegActivity> acmag = evaluateAttributes(documents,
            AttributeClassifier::magnitude, Magnitude.values(), options.numFolds(), EegActivity.TYPE);
        final MulticlassF1EvalResult<String, EegActivity> acrec = evaluateAttributes(documents,
            AttributeClassifier::recurrence, Recurrence.values(), options.numFolds(), EegActivity.TYPE);
        final MulticlassF1EvalResult<String, EegActivity> acdis = evaluateAttributes(documents,
            AttributeClassifier::dispersal, Dispersal.values(), options.numFolds(), EegActivity.TYPE);
        final MulticlassF1EvalResult<String, EegActivity> acband = evaluateAttributes(documents,
            AttributeClassifier::band, Band.values(), options.numFolds(), EegActivity.TYPE);
        final MulticlassF1EvalResult<String, EegActivity> acbk = evaluateAttributes(documents,
            AttributeClassifier::background, In_Background.values(), options.numFolds(), EegActivity.TYPE);
        final MulticlassF1EvalResult<String, EegActivity> acmod = evaluateAttributes(documents,
            as -> AttributeClassifier.modality(as, EegActivity.modality, EegActivity.TYPE), ModalityLabel.values(),
            options.numFolds(), EegActivity.TYPE);
        final MulticlassF1EvalResult<String, EegActivity> acpol = evaluateAttributes(documents,
            as -> AttributeClassifier.polarity(as, EegActivity.polarity, EegActivity.TYPE), PolarityLabel.values(),
            options.numFolds(), EegActivity.TYPE);
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(options.outfile()))) {
          writer.append("task,tp,fp,tn,fn,precision,recall,f1,accuracy\n")
              .append("Activity Boundaries,").append(acb.toCsvString())
              .append("\nOther Concept Boundaries,").append(evb.toCsvString()).append("\n")
              .append("\nConcept Type\n").append(evtype.toCsvString())
              .append("Activity Modality\n").append(acmod.toCsvString())
              .append("Other Concept Modality\n").append(evmod.toCsvString())
              .append("Activity Polarity\n").append(acpol.toCsvString())
              .append("Other Concept Polarity\n").append(evpol.toCsvString())
              .append("Morphology\n").append(acmorph.toCsvString())
              .append("Hemisphere\n").append(achemi.toCsvString())
              .append("Magnitude\n").append(acmag.toCsvString())
              .append("Recurrence\n").append(acrec.toCsvString())
              .append("Dispersal\n").append(acdis.toCsvString())
              .append("Frequency Band\n").append(acband.toCsvString())
              .append("Background\n").append(acbk.toCsvString());
          for (Location loc : Location.values()) {
            final MulticlassF1EvalResult<String, EegActivity> acloc = evaluateAttributes(documents,
                as -> AttributeClassifier.location(loc.name(), as), BinaryLabel.values(), options.numFolds(), EegActivity.TYPE);
            writer.append("Location - ").append(loc.toString()).append("\n")
                .append(acloc.toCsvString());
          }
        } catch (IOException e) {
          throw new RuntimeException(e);
        }
      } else if (options.type() == CvOptions.Type.kirk) {
        evaluateKirkBoundariesCV(documents, options.numFolds(), options.partialBoundaries());
      }
      if (options.save()) {
        documents.forEach(Document::sync);
      }
      log.info("{} total annotated documents with {} gold events, {} gold activities, {} predicted events, and {} predicted activities", documents.size(),
          documents.stream().mapToInt(d -> d.get("gold", Event.TYPE).size()).sum(),
          documents.stream().mapToInt(d -> d.get("gold", EegActivity.TYPE).size()).sum(),
          documents.stream().mapToInt(d -> d.get("pred", Event.TYPE).size()).sum(),
          documents.stream().mapToInt(d -> d.get("pred", EegActivity.TYPE).size()).sum());
    } catch (ArgumentValidationException e) {
      System.out.println(cli.getHelpMessage());
      throw new RuntimeException(e);
    }
  }

  private static <A extends Annotation<A>> MulticlassF1EvalResult<String,A> evaluateAttributes(
      final List<Document<EegNote>> docs, Function<String,AttributeClassifier<A>> acfun, EnumLabel[] labels, int numFolds,
      AnnotationType<A> annotationType) {
    final String goldAnnset = "gold";
    final AttributeClassifier<A> ac = acfun.apply(goldAnnset);
    final CrossValidationSplit<Document<EegNote>> split = CrossValidationSplit.of(docs, numFolds);

    final MulticlassF1EvalResult<String,A> totalResult = new MulticlassF1EvalResult<>();
    final IntIdentifier<String> iid = new IntIdentifier<>();
    for (int i = 0; i < numFolds; i++) {
      final List<Document<EegNote>> train = split.getTrain(i);
      final List<Document<EegNote>> test = split.getTest(i);
      log.trace("Training split {}...", i);
      final String name = labels[0].getClass().getName().substring(labels[0].getClass().getName().indexOf('$')+1);
      final LibLinearSvm svm = ac.trainSvm(train.stream(), Paths.get("/tmp"), name + i, goldAnnset);
//          Svm.trainSvm(train.stream(), Paths.get("/tmp"), name + i, goldAnnset, labeler,
//          annotationType, FeatureUtils.attributeFeatureExtractors(goldAnnset));

//      final LibLinearSvm svm = LibLinearSvm.train(new Parameter(SolverType.L1R_L2LOSS_SVC, 1.0, 0.01),
//          vectorizer.vectorizeCorpus(train.stream(),
//              doc -> Collections.singletonList(doc.get(goldAnnset, annotationType))).collect(Collectors.toList()));
//      llsvm.save("/tmp/" + name + i + ".model");
//      vectorizer.writeFeatureMap("/tmp/" + name + i);

      final Map<A, EnumLabel> map = annotateAttrSvm(goldAnnset, test, labels, ac, svm, iid);
      final Map<String, MulticlassF1EvalResult<String,A>> docResults = doAttrEval(test.stream(), goldAnnset, map,
          ac.getValue2label(), annotationType, Arrays.stream(labels).map(EnumLabel::toString).collect(Collectors.toList()));
      docResults.values().forEach(totalResult::incorporate);
    }
    log.info("Aggregate Results:{}", totalResult.toString());
    return totalResult;
  }

  private static F1EvaluationResult<EegActivity> evaluateActivityBoundariesCV(final List<Document<EegNote>> doclist,
                                                                             final int numsplits, final boolean partial) {
    final String goldAnnset = "gold";
    final String predAnnset = "pred";
    doclist.forEach(doc -> doc.clear(predAnnset));
    final CrossValidationSplit<Document<EegNote>> split = CrossValidationSplit.of(doclist, numsplits);
    final F1EvaluationResult<EegActivity> totalResult = new F1EvaluationResult<>();
    for (int i = 0; i < numsplits; i++) {
      final String modelDir = "/tmp/split" + i;
      new File(modelDir).mkdir();
//      final List<Document<EegNote>> train = split.getTrain(i);
      final List<Document<EegNote>> test = split.getTest(i);
      log.trace("Training split {}...", i);
//      Crf.trainActivityBoundaries(train, modelDir, goldAnnset);
      final TfBoundaryAnnotator<BaseDocument> model = TfBoundaryAnnotator.loadFromConfig(predAnnset);
//      final CrfsEventBoundaryAnnotator<EegNote> model = Crf.loadActivityBoundaryAnnotator(predAnnset, Paths.get(modelDir));
      model.annotateAll(test);
      final Map<String, F1EvaluationResult<EegActivity>> docResults =
          doBoundaryEval(test.stream(), goldAnnset, predAnnset, EegActivity.TYPE, Optional.empty(), partial);
      docResults.values().forEach(totalResult::incorporate);
    }
    log.info("Aggregate Results:{}", totalResult.toString());
    return totalResult;
  }

  private static F1EvaluationResult<Event> evaluateEventBoundariesCV(final List<Document<EegNote>> doclist,
                                                                    final int numsplits, final boolean partial) {
    final String goldAnnset = "gold";
    final String predAnnset = "pred";
//    doclist.forEach(doc -> doc.clear(predAnnset));
    final CrossValidationSplit<Document<EegNote>> split = CrossValidationSplit.of(doclist, numsplits);
    final F1EvaluationResult<Event> totalResult = new F1EvaluationResult<>();
    for (int i = 0; i < numsplits; i++) {
      final String modelDir = "/tmp/split" + i;
      new File(modelDir).mkdir();
      final List<Document<EegNote>> train = split.getTrain(i);
      final List<Document<EegNote>> test = split.getTest(i);
      log.trace("Training split {}...", i);
      Crf.trainEventBoundaries(train, modelDir, goldAnnset);
      final CrfsEventBoundaryAnnotator<EegNote> model = Crf.loadEventBoundaryAnnotator(predAnnset, Paths.get(modelDir));
      model.annotateAll(test);
      final Map<String, F1EvaluationResult<Event>> docResults =
          doBoundaryEval(test.stream(), goldAnnset, predAnnset, Event.TYPE, Optional.empty(), partial);
      docResults.values().forEach(totalResult::incorporate);
    }
    log.info("Aggregate Results:{}", totalResult.toString());
    return totalResult;
  }

  @SuppressWarnings("UnusedReturnValue")
  private static F1EvaluationResult<Event> evaluateKirkBoundariesCV(final List<Document<EegNote>> doclist,
                                                                    final int numsplits, final boolean partial) {
    final String goldAnnset = "gold";
    final String predAnnset = "kirk";
    final CrossValidationSplit<Document<EegNote>> split = CrossValidationSplit.of(doclist, numsplits);
    final F1EvaluationResult<Event> totalResult = new F1EvaluationResult<>();
    for (int i = 0; i < numsplits; i++) {
      final String modelDir = "/tmp/split" + i;
      new File(modelDir).mkdir();
//      final List<Document<EegNote>> train = split.getTrain(i);
      final List<Document<EegNote>> test = split.getTest(i);
      final Map<String, F1EvaluationResult<Event>> docResults =
          doBoundaryEval(test.stream(), goldAnnset, predAnnset, Event.TYPE, Optional.of((Event e) -> {
            final String type = e.get(Event.type);
            return "PROBLEM".equals(type) || "TEST".equals(type) || "TREATMENT".equals(type);
          }), partial);
      docResults.values().forEach(totalResult::incorporate);
    }
    log.info("Aggregate Results:{}", totalResult.toString());
    log.info("Accuracy: {}", ((double)totalResult.countTruePositives() + totalResult.countTrueNegatives())/totalResult.countAll());
    return totalResult;
  }

//  public static void countSections(final String outFile) {
//    final Multiset<String> ms = HashMultiset.create();
//    Data.v060().forEachDocument(doc -> {
//      ms.addAll(doc.get("regex-eeg", Section.TYPE)
//          .stream().map(s -> s.get(Section.title).toLowerCase().trim()).collect(Collectors.toList()));
//      plog.update("processed {}", doc.getId());
//    });
//    try (final BufferedWriter writer = new BufferedWriter(new FileWriter(outFile))) {
//      final List<String> sorted = Lists.newArrayList(ms.elementSet());
//      Collections.sort(sorted, (s1, s2) -> Integer.compare(ms.count(s2), ms.count(s1)));
//      for (final String s : sorted) {
//        writer.write(s + "," + ms.count(s));
//        writer.newLine();
//      }
//      log.info("Found {} unique section titles.", ms.elementSet().size());
//    } catch (IOException e) {
//      throw new RuntimeException(e);
//    }
//  }

  @SuppressWarnings("unused")
  private static void report(final Map<String, F1EvaluationResult<Event>> docResults, final String docOut,
                             final String goldAs, final String predAs) {
    try {
      for (final String docId : docResults.keySet()) {
        final F1EvaluationResult<Event> result = docResults.get(docId);
        log.trace("{}: {} tp", docId, result.countTruePositives());
        log.trace("{}: {} fp", docId, result.countFalsePositives());
        log.trace("{}: {} fn\n", docId, result.countFalseNegatives());
        writeResult(result.falseNegatives(), Paths.get(docOut, docId, "false_negatives.txt"),
            Arrays.asList(sentenceDescriptor(), goldEventDescriptor(goldAs, predAs),
                ann -> String.format("Missed [%s].", ann.asString())));
        writeResult(result.falsePositives(), Paths.get(docOut, docId, "false_positives.txt"),
            Arrays.asList(sentenceDescriptor(), goldEventDescriptor(goldAs, predAs), annotationDescriptor()));
        writeResult(result.truePositives(), Paths.get(docOut, docId, "true_positives.txt"),
            Arrays.asList(sentenceDescriptor(), annotationDescriptor()));
        try (final BufferedWriter writer = new BufferedWriter(new FileWriter(new File(docOut + File.separator + docId, "stats.txt")))) {
          writer.write(String.format("True positives: %d\nFalse positives: %d\nFalse negatives: %d", result.countTruePositives(),
              result.countFalsePositives(), result.countFalseNegatives()));
        } catch (IOException e) {
          throw new RuntimeException(e);
        }
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private static <A extends Annotation<A>> Map<A, EnumLabel> annotateAttrSvm(
      final String goldAs, final List<Document<EegNote>> data, final EnumLabel[] labels, AttributeClassifier<A> ac,
      LibLinearSvm llsvm, IntIdentifier<String> iid) {
    // for eval purposes, just store the predicted type for each event instead of setting the Event.type
    final Map<A, EnumLabel> map = Maps.newConcurrentMap();
    final BiConsumer<A, Double> typeLabeler = (activity, prediction) -> {
      EnumLabel predictedLabel = null;
      for (final EnumLabel label : labels) {
        if (label.asInt() == prediction.intValue()) {
          predictedLabel = label;
          break;
        }
      }
      assert predictedLabel != null : "No Event Type Label for " + prediction;
      map.put(activity, predictedLabel);
    };
    final SvmAnnotator<A, EegNote> annotator = ac.getAnnotator(llsvm, iid, goldAs, typeLabeler);
//        new SvmAnnotator<>(extractors, llsvm, typeLabeler,
//        doc -> doc.get(goldAs, annotationType), iid);
    data.forEach(annotator::annotate);
    return map;
  }

  private static <A extends Annotation> void writeResult(final Iterable<A> annotations, final Path outFile,
                                                         final List<Function<A, String>> descriptors) throws IOException {
    outFile.toFile().getParentFile().mkdirs();
    final BufferedWriter writer = new BufferedWriter(new FileWriter( outFile.toFile()));
    for (final A annotation : annotations) {
      for (final Function<A, String> descriptor : descriptors) {
        writer.write(descriptor.apply(annotation));
        writer.newLine();
      }
      writer.newLine();
    }
    writer.flush();
    writer.close();
  }

  private static <A extends Annotation> Function<A, String> sentenceDescriptor() {
    return annotation -> annotation.getCovering("opennlp", Sentence.TYPE).get(0).toString();
  }

  private static <A extends Annotation> Function<A, String> annotationDescriptor() {
    return Annotation::describe;
  }

  private static <A extends Annotation> Function<A, String> goldEventDescriptor(final String goldAnnSet, final String predAnnSet) {
    return annotation -> {
      final Sentence sentence = (Sentence) annotation.getCovering("opennlp", Sentence.TYPE).get(0);
      final List<Event> goldevents = sentence.getContained(goldAnnSet, Event.TYPE);
      if (goldevents.size() > 0) {
        StringBuilder sb = new StringBuilder();
        sb.append("Gold events: ");
        for (final Event goldevent : goldevents) {
          sb.append("[").append(goldevent.toString()).append("], ");
        }
        sb = new StringBuilder(sb.substring(0, sb.length() - 2));
        sb.append("\nPredicted events: ");
        for (final Event predEvent : sentence.getContained(predAnnSet, Event.TYPE)) {
          sb.append("[").append(predEvent.toString()).append("], ");
        }
        return sb.substring(0, sb.length() - 2);
      }
      else {
        return "No events.";
      }
    };
  }

  @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
  private static <A extends Annotation<A>> Map<String, F1EvaluationResult<A>> doBoundaryEval(
      final Stream<Document<EegNote>> documents, final String goldAnnSet, final String predictedAnnSet,
      final AnnotationType<A> anntype, final Optional<Predicate<A>> filter, final boolean partial) {
    final F1EvaluationResult<A> overalResults = new F1EvaluationResult<>();
    final Map<String, F1EvaluationResult<A>> docResults = Maps.newHashMap();
    final BiPredicate<List<A>, A> isMatch = (partial) ? CrossValidation::testPartialBoundary : CrossValidation::testExactBoundary;

    documents.forEach(doc -> {
      final F1EvaluationResult<A> docResult = new F1EvaluationResult<>();
      final List<A> predictions = doc.get(predictedAnnSet, anntype);
      List<A> gold = doc.get(goldAnnSet, anntype);
//      log.info("Performing boundary eval on doc {} with {} prediced and {} gold annotations", doc.getId(),
//          predictions.size(), gold.size());
      if (filter.isPresent()) {
        gold = gold.stream().filter(filter.get()).collect(Collectors.toList());
      }
      for (final A prediction : predictions) {
        if (!filter.isPresent() || filter.get().test(prediction)) {
          if (isMatch.test(gold, prediction)) {
            overalResults.truePositive(prediction);
            docResult.truePositive(prediction);
          } else {
            overalResults.falsePositive(prediction);
            docResult.falsePositive(prediction);
          }
        }
      }
      gold.stream().filter(goldAnn -> !isMatch.test(predictions, goldAnn)).forEach(goldAnn -> {
        overalResults.falseNegative(goldAnn);
        docResult.falseNegative(goldAnn);
      });
      docResults.put(doc.get(BaseDocument.id), docResult);
    });

    log.trace(overalResults.toString());
    return docResults;
  }

  private static <A extends Annotation<A>> Map<String, MulticlassF1EvalResult<String,A>> doAttrEval(
      final Stream<Document<EegNote>> documents, final String goldAnnSet, final Map<A, EnumLabel> map,
      final Function<A, ? extends EnumLabel> labelExtractor, final AnnotationType<A> annotationType,
      final List<String> possibleLabels) {
    final MulticlassF1EvalResult<String, A> overalResults = new MulticlassF1EvalResult<>();
    final Map<String, MulticlassF1EvalResult<String,A>> docResults = Maps.newHashMap();

    documents.forEach(doc -> {
      final MulticlassF1EvalResult<String,A> docResult = new MulticlassF1EvalResult<>();
      for (final A activity : doc.get(goldAnnSet, annotationType)) {
        final String prediciton = map.get(activity).toString();
        final String label = labelExtractor.apply(activity).toString();
        if (label.equals(prediciton)) {
          overalResults.truePositive(label, activity);
          docResult.truePositive(label, activity);
        }
        else {
          overalResults.falsePositive(prediciton, activity);
          overalResults.falseNegative(label, activity);
          docResult.falsePositive(prediciton, activity);
          docResult.falseNegative(label, activity);
        }
        possibleLabels.stream().filter(l -> !l.equals(label) && !l.equals(prediciton)).forEach(l -> {
          docResult.trueNegative(l, activity);
          overalResults.trueNegative(l, activity);
        });
      }
      docResults.put(doc.get(BaseDocument.id), docResult);
    });
    log.trace(overalResults.toString());
    return docResults;
  }

  private static <A extends Annotation<A>> boolean testExactBoundary(final List<A> list, A annotation) {
    final long start = annotation.get(Annotation.StartOffset);
    final long end = annotation.get(Annotation.EndOffset);
    for (final A target : list) {
      if (target.get(Annotation.EndOffset) == end && target.get(Annotation.StartOffset) == start) {
        return true;
      }
    }
    return false;
  }

  private static <A extends Annotation<A>> boolean testPartialBoundary(final List<A> list, A annotation) {
    final long start = annotation.get(Annotation.StartOffset);
    final long end = annotation.get(Annotation.EndOffset);
    for (final A target : list) {
      final long tstart = target.get(Annotation.StartOffset);
      final long tend = target.get(Annotation.EndOffset);
      if (tstart > start) {
        if (end >= tstart) {
          return true;
        }
      } else if (tstart < start) {
        if (tend >= start) {
          return true;
        }
      }
      else {
        return true;
      }
    }
    return false;
  }
}
