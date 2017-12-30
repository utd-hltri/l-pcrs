package edu.utdallas.hltri.eeg.al;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.MinMaxPriorityQueue;
import com.lexicalscope.jewel.cli.ArgumentValidationException;
import com.lexicalscope.jewel.cli.Cli;
import com.lexicalscope.jewel.cli.CliFactory;
import com.lexicalscope.jewel.cli.Option;
import edu.utdallas.hltri.conf.Config;
import edu.utdallas.hltri.eeg.Data;
import edu.utdallas.hltri.eeg.EegNote;
import edu.utdallas.hltri.eeg.annotation.EegActivity;
import edu.utdallas.hltri.eeg.annotation.EegActivity.Location;
import edu.utdallas.hltri.eeg.annotators.CrfsEventBoundaryAnnotator;
import edu.utdallas.hltri.eeg.annotators.SvmActiveLearner;
import edu.utdallas.hltri.eeg.classifier.Crf;
import edu.utdallas.hltri.eeg.classifier.Svm;
import edu.utdallas.hltri.eeg.io.EegEventBratCorpus;
import edu.utdallas.hltri.eeg.io.EegJsonCorpus;
import edu.utdallas.hltri.framework.ProgressLogger;
import edu.utdallas.hltri.func.DistinctKeyFilter;
import edu.utdallas.hltri.io.TextFiles;
import edu.utdallas.hltri.logging.Logger;
import edu.utdallas.hltri.scribe.io.JsonCorpus;
import edu.utdallas.hltri.scribe.text.Document;
import edu.utdallas.hltri.scribe.text.annotation.Annotation;
import edu.utdallas.hltri.scribe.text.annotation.Event;
import edu.utdallas.hltri.scribe.text.annotation.Sentence;
import edu.utdallas.hltri.scribe.text.annotation.Token;
import edu.utdallas.hltri.struct.Pair;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

import static edu.utdallas.hltri.eeg.al.ActiveLearning.ClassifierType.*;

/**
 * Learns actively
 * Created by rmm120030 on 7/26/16.
 */
public class ActiveLearning {
  private static final Logger log = Logger.get(ActiveLearning.class);
  private static final ProgressLogger plog = ProgressLogger.fixedSize("Active Learning",
      Data.V060_SIZE, 5, TimeUnit.SECONDS);

  private static final Config conf = Config.load("eeg.al");
  private static Map<ClassifierType, SvmActiveLearner<EegActivity, EegNote>> activityClassifiers;
  private static CrfsEventBoundaryAnnotator<EegNote> eventBoundaryAnnotator;
  private static CrfsEventBoundaryAnnotator<EegNote> activityBoundaryAnnotator;
  private static SvmActiveLearner<Event, EegNote> type;
  private static SvmActiveLearner<Event, EegNote> evModality;
  private static SvmActiveLearner<Event, EegNote> evPolarity;

  interface ActiveLearningOptions {
    enum Mode {output, retrain, test, brat2json}
    @Option(longName = "mode", description = "Active learning mode. Use 'output' to output the next n documents to the" +
        "user. Use 'retrain' to retrain models based on new annotations.")
    Mode getMode();

    @Option(longName = "run-name", description = "The name of the run. Models will be stored in/read from " +
        "<model_dir>/run-name and brat corpora will be written to/read from <brat_dir>/run-name")
    String getRunName();

    @Option(longName = "model-name", defaultValue = "seed", description = "The name of the last completed run." +
        "We will use the models created from this run. Only used if mode=output. Default is 'seed'.")
    String getModelName();

    @Option(longName = "n", defaultValue = "5", description = "Number of documents to output.")
    int getN();

    @Option(longName = "brat-dir", defaultValue = "none", description = "Only for brat2json mode.")
    String getBratDir();

    @Option(longName = "json-dir", defaultValue = "none", description = "Only for brat2json mode.")
    String getJsonDir();

    @Option(longName = "debug", defaultValue = "false", description = "Enter debug mode.")
    boolean debug();

    enum UncertaintyFunction {entropy, margin}
    @Option(longName = "uncertainty-function", defaultValue = "entropy", description = "Uncertainty function: [entropy, " +
        "margin].")
    UncertaintyFunction getUncertaintyFunction();

//    @Option(longName = "new-brat-dirs", shortName = "brat", description = "Newly annotated brat dirs")
//    String[] getNewBratDirs();
  }

  public static void main(String... args) {
    final Cli<ActiveLearningOptions> cli = CliFactory.createCli(ActiveLearningOptions.class);
    try {
      final ActiveLearningOptions options = cli.parseArguments(args);
      if (options.getMode() == ActiveLearningOptions.Mode.output) {
        final int n = options.getN();
        final Function<double[], Double> scoreFun = options.getUncertaintyFunction() == ActiveLearningOptions.UncertaintyFunction.entropy
            ? ActiveLearning::entropy : ActiveLearning::margin;
        final String runName = options.getRunName();
        final String modelName = options.getModelName();
        outputNextNExamples(n, scoreFun, runName, modelName, Data.v060(Data.getDefaultAnnSets(runName)), options.debug());
      }
      else if (options.getMode() == ActiveLearningOptions.Mode.retrain) {
        final String runName = options.getRunName();
        updateModels(runName);
      }
      else if (options.getMode() == ActiveLearningOptions.Mode.test) {
        final String runName = options.getRunName();
        loadModels(runName + "2", conf.getPath("model").resolve(runName));
        final Document<EegNote> doc = Data.v060().load("00004166_s02");
        eventBoundaryAnnotator.annotate(doc);
        type.annotate(doc);
        activityBoundaryAnnotator.annotate(doc);
        final List<Pair<EegActivity, double[]>> pairs = activityClassifiers.get(BACKGROUND).annotateWithConfidence(doc);
        log.info(pairs.stream().map(Pair::second).map(Arrays::toString).reduce("", (a1, a2) -> a1 + a2 + ", "));
        log.info("Found {} events and {} activities", doc.get(runName + "2", Event.TYPE).size(), doc.get(runName + "2", EegActivity.TYPE).size());
//        Data.loadBrat("/home/rmm120030/working/eeg/brat/al/test", runName + 2).save(doc);
      }
      else if (options.getMode() == ActiveLearningOptions.Mode.brat2json) {
        final String annset = conf.getString("gold-annset");
        assert !options.getBratDir().equals("none") : "Must provide brat-dir";
        assert !options.getJsonDir().equals("none") : "Must provide json-dir";
        bratToJson(Data.loadJson(options.getJsonDir(), false, Data.getDefaultAnnSets(annset)), options.getBratDir(), annset);
      }
    } catch (ArgumentValidationException e) {
      System.out.println(cli.getHelpMessage());
      throw new RuntimeException(e);
    }
  }

  enum ClassifierType {
    EVENT_BOUNDARY,
    EVENT_POLARITY,
    EVENT_MODALITY,
    ACTIVITY_BOUNDARY,
    ACTIVITY_POLARITY,
    ACTIVITY_MODALITY,
    TYPE,
    MORPHOLOGY,
    FREQUENCY_BAND,
    MAGNITUDE,
    RECURRENCE,
    DISPERSAL,
    HEMISPHERE,
    BACKGROUND,
    FRONTAL,
    OCCIPITAL,
    TEMPORAL,
    CENTRAL,
    PARIETAL,
    FRONTOCENTRAL,
    FRONTOTEMPORAL,
    CENTROPARIETAL,
    PARIETO_OCCIPITAL

//    public ClassifierType fromString(String s) {
//      final ClassifierType type = ClassifierType.valueOf(s);
//      if (type == null) {
//
//      }
//      return type;
//    }
  }

  /**
   * Loads the classifiers.
   * @param annset the annotation set the annotators will assign new annotations to
   * @param modelIn the root model directory for this run
   */
  private static void loadModels(String annset, Path modelIn) {
    eventBoundaryAnnotator = Crf.loadEventBoundaryAnnotator(annset, modelIn);
    activityBoundaryAnnotator = Crf.loadActivityBoundaryAnnotator(annset, modelIn);
    type = Svm.loadEventTypeSvm(modelIn, annset);
    evModality = Svm.loadEventModalitySvm(modelIn, annset);
    evPolarity = Svm.loadEventPolaritySvm(modelIn, annset);

    activityClassifiers = new EnumMap<>(ClassifierType.class);
    activityClassifiers.put(MORPHOLOGY, Svm.loadActivityAttributeSvm(modelIn, "morphology", annset));
    activityClassifiers.put(FREQUENCY_BAND, Svm.loadActivityAttributeSvm(modelIn, "band", annset));
    activityClassifiers.put(HEMISPHERE, Svm.loadActivityAttributeSvm(modelIn, "hemisphere", annset));
    activityClassifiers.put(DISPERSAL, Svm.loadActivityAttributeSvm(modelIn, "dispersal", annset));
    activityClassifiers.put(RECURRENCE, Svm.loadActivityAttributeSvm(modelIn, "recurrence", annset));
    activityClassifiers.put(MAGNITUDE, Svm.loadActivityAttributeSvm(modelIn, "magnitude", annset));
    activityClassifiers.put(BACKGROUND, Svm.loadActivityAttributeSvm(modelIn, "in_background", annset));
    activityClassifiers.put(ACTIVITY_MODALITY, Svm.loadActivityAttributeSvm(modelIn, "activity_modality", annset));
    activityClassifiers.put(ACTIVITY_POLARITY, Svm.loadActivityAttributeSvm(modelIn, "activity_polarity", annset));
    for (Location location : Location.values()) {
      activityClassifiers.put(ClassifierType.valueOf(location.toString()),
          Svm.loadLocationSvm(modelIn, location.toString(), annset));
    }
  }

  private static void outputNextNExamples(int n, Function<double[], Double> scoreFun, String runName, String modelName,
                                         final JsonCorpus<EegNote> fullCorpus, boolean debug) {
    if (debug) {
      log.warn("DEBUG MODE");
    }
    else if (fullCorpus.getIdStream(runName).findAny().isPresent()) {
      throw new RuntimeException("There are already (" + runName + ") annotations in the v060 corpus. Either use a " +
          "different run-name or delete/move the existing v0606 (" + runName + ") annotations.");
    }

    // load the list of already annotated documents to exclude from the pool of unannotated documents
    final Set<String> alreadyAnnotated = TextFiles.loadSet(conf.getString("annotated-doc-list"));

    // load models
    loadModels(runName, conf.getPath("model").resolve(modelName));

    final Map<ClassifierType, List<Pair<String, Double>>> rankMap = new EnumMap<>(ClassifierType.class);
    rankMap.put(EVENT_BOUNDARY, Lists.newArrayListWithExpectedSize(17000));
    rankMap.put(ACTIVITY_BOUNDARY, Lists.newArrayListWithExpectedSize(17000));
    rankMap.put(EVENT_POLARITY, Lists.newArrayListWithExpectedSize(17000));
    rankMap.put(ACTIVITY_POLARITY, Lists.newArrayListWithExpectedSize(17000));
    rankMap.put(EVENT_MODALITY, Lists.newArrayListWithExpectedSize(17000));
    rankMap.put(ACTIVITY_MODALITY, Lists.newArrayListWithExpectedSize(17000));
    rankMap.put(MORPHOLOGY, Lists.newArrayListWithExpectedSize(17000));
    rankMap.put(FREQUENCY_BAND, Lists.newArrayListWithExpectedSize(17000));
    rankMap.put(HEMISPHERE, Lists.newArrayListWithExpectedSize(17000));
    rankMap.put(RECURRENCE, Lists.newArrayListWithExpectedSize(17000));
    rankMap.put(MAGNITUDE, Lists.newArrayListWithExpectedSize(17000));
    rankMap.put(BACKGROUND, Lists.newArrayListWithExpectedSize(17000));
    rankMap.put(DISPERSAL, Lists.newArrayListWithExpectedSize(17000));
    rankMap.put(TYPE, Lists.newArrayListWithExpectedSize(17000));
    for (Location l : Location.values()) {
      rankMap.put(ClassifierType.valueOf(l.toString()), Lists.newArrayListWithExpectedSize(17000));
    }

    log.info("Begin Annotation...");
    final List<String> dids = Lists.newArrayList();
    fullCorpus.getIdStream()
        // don't bother loading already manually annotated documents
        .filter(id -> !alreadyAnnotated.contains(id))
        .map(fullCorpus::load)
        .forEach(doc -> {
      if (debug) doc.clear(runName);
      final String did = doc.getId();
      if (doc.get("opennlp", Sentence.TYPE).size() > 0 && doc.get("genia", Token.TYPE).size() > 0) {
        final List<Pair<Token, double[]>> eventTokens = eventBoundaryAnnotator.annotateWithConfidence(doc);
        assert eventTokens.size() > 0 : "No token-confidence pairs returned by event boundary annotator. " +
            doc.get("genia", Token.TYPE).size() + " genia tokens.";
        final double eventBoundaryUncertainty = eventTokens.stream()
            .map(Pair::second).mapToDouble(scoreFun::apply).average().orElse(0d);
        addDocScoreToMap(rankMap, EVENT_BOUNDARY, did, eventBoundaryUncertainty);
        final int numEvents = doc.get(runName, Event.TYPE).size();
        if (numEvents > 0) {
          addDocScoreToMap(rankMap, TYPE, did, type.annotateWithConfidence(doc).stream()
              .map(Pair::second).mapToDouble(scoreFun::apply).average().orElse(0d));
          addDocScoreToMap(rankMap, EVENT_MODALITY, did, evModality.annotateWithConfidence(doc).stream()
              .map(Pair::second).mapToDouble(scoreFun::apply).average().orElse(0d));
          addDocScoreToMap(rankMap, EVENT_POLARITY, did, evPolarity.annotateWithConfidence(doc).stream()
              .map(Pair::second).mapToDouble(scoreFun::apply).average().orElse(0d));
        }
        else {
          log.warn("Doc {} has no events and {} tokens.", did, doc.get("genia", Token.TYPE).size());
        }

        final List<Pair<Token, double[]>> activityTokens = activityBoundaryAnnotator.annotateWithConfidence(doc);
        assert activityTokens.size() > 0 : "No token-confidence pairs returned by activity boundary annotator.";
        final double activityBoundaryUncertainty = activityTokens.stream()
            .map(Pair::second).mapToDouble(scoreFun::apply).average().orElse(0d);
        addDocScoreToMap(rankMap, ACTIVITY_BOUNDARY, did, activityBoundaryUncertainty);
        final int numActs = doc.get(runName, EegActivity.TYPE).size();
        if (numActs > 0) {
          activityClassifiers.keySet().forEach(type ->
              addDocScoreToMap(rankMap, type, did, activityClassifiers.get(type).annotateWithConfidence(doc)
              .stream().map(Pair::second).mapToDouble(scoreFun::apply).average().orElse(0d)));
          log.info("Done attribute classification on doc {}", did);

          plog.update("annotated doc {} with {} activities and {} events.{}\n", did, numActs, numEvents,
              getRankReport(rankMap, did));
        } else {
          log.warn("Doc {} has no activities but {} tokens!", did, doc.get("genia", Token.TYPE).size());
        }
      }
      else {
        log.warn("Doc {} has {} genia tokens and {} onlp sentences.", did, doc.get("genia", Token.TYPE).size(),
            doc.get("opennlp", Sentence.TYPE).size());
      }
      dids.add(doc.getId());
      fullCorpus.save(doc);
      doc.close();
    });

    // Lists of doc ids sorted by confidence
    final Map<ClassifierType, RankedList> sortedLists = Maps.newHashMap();
    rankMap.keySet().forEach(type -> sortedLists.put(type, new RankedList(rankMap.get(type))));
    // We want the n lowest ranked document where an document's rank is defined as the sum of each of its ranks
    // from each classifier.
    // Lower values come first in the ordering
    final MinMaxPriorityQueue<String> committeeRanks = MinMaxPriorityQueue.<String>orderedBy(
        Comparator.comparingInt(d -> getRank(sortedLists, d))).create();
    committeeRanks.addAll(dids);

    // Create the output Brat Corpus
    final File bratDirFile = conf.getPath("brat-root").resolve(runName).toFile();
    if (!bratDirFile.exists()) {
      //noinspection ResultOfMethodCallIgnored
      bratDirFile.mkdirs();
    }
    final EegEventBratCorpus brat = EegEventBratCorpus.at(bratDirFile, runName);
    for (int i = 0; i < n; i++) {
      try (final Document<EegNote> doc = fullCorpus.load(committeeRanks.poll())) {
        brat.save(doc);
        final String did = doc.getId();
        log.info("Saving doc {} with rank {}", did, getRank(sortedLists, did));
      }
    }
    log.info("Output least confident {} examples to {}", n, bratDirFile.toString());
  }

  private static String getRankReport(final Map<ClassifierType, List<Pair<String,Double>>> rankMap, String did) {
    final StringBuilder sb = new StringBuilder();
    for (final ClassifierType type : rankMap.keySet()) {
      sb.append("\n").append(type.toString()).append(" Uncertainty: ");
      final List<Pair<String, Double>> pairs = rankMap.get(type);
      if (pairs.size() > 0 && pairs.get(pairs.size() - 1).first().equals(did)) {
        sb.append(pairs.get(pairs.size() - 1).second());
      }
      else {
        sb.append("n/a");
      }
    }
    return sb.toString();
  }

  private static int getRank(final Map<ClassifierType, RankedList> map, final String did) {
    return map.keySet().stream().mapToInt(type -> map.get(type).rank(did)).sum();
  }

  private static void addDocScoreToMap(Map<ClassifierType, List<Pair<String, Double>>> rankMap, ClassifierType type,
                                       String docName, double score) {
    final List<Pair<String, Double>> scores = rankMap.get(type);
    scores.add(Pair.of(docName, score));
    rankMap.put(type, scores);
  }

  @SuppressWarnings("ResultOfMethodCallIgnored")
  private static void updateModels(String runName) {
    List<Document<EegNote>> documentList = Data.seed().getDocumentList();
    log.info("Loaded {} documents from the seed corpus for retraining.", documentList.size());
    final String goldAnnset = conf.getString("gold-annset");
    final String[] annsets = Data.getDefaultAnnSets(runName, goldAnnset);

    // load corpora from previous rounds of annotation
    final Path cachePath = conf.getPath("cache");
    final Set<String> cachedJsonDirs = TextFiles.loadSet(cachePath.resolve("cache_list.txt").toString());
    for (String cachedJsonDir : cachedJsonDirs) {
      final Path path = Paths.get(cachedJsonDir);
      final JsonCorpus<EegNote> json = Data.loadJsonWithv060Text(annsets, path);
      final List<Document<EegNote>> docs = json.getIdStream(goldAnnset).map(json::load).collect(Collectors.toList());
      log.info("Loaded {} documents for retraining from the cached json corpus at {}.", docs.size(), cachedJsonDir);
      documentList.addAll(docs);
    }

    // add new brat annotations to the json cache
    final Path jsonPath = cachePath.resolve(runName);
    log.info("Caching annotations from run {} to json corpus at {}", runName, jsonPath);
    final JsonCorpus<EegNote> json = Data.loadJsonWithv060Text(annsets, jsonPath);
    bratToJson(json, conf.getPath("brat-root").resolve(runName).toString(), goldAnnset);
    documentList.addAll(json.getIdStream(goldAnnset).map(json::load).collect(Collectors.toList()));
    cachedJsonDirs.add(jsonPath.toString());

    log.info("Updating cached JsonCorpus list...");
    // update json cache list
    TextFiles.saveSet(cachedJsonDirs, cachePath.resolve("cache_list.txt").toString());

    log.info("Updating annotated documents list...");
    // update list of annotated documents
    TextFiles.saveSet(documentList.stream().map(Document::getId).collect(Collectors.toSet()), conf.getString("annotated-doc-list"));

    // retrain models
    log.info("Begin retraining models...");
    final Path modelPath = conf.getPath("model").resolve(runName);
    if (Files.exists(modelPath)) {
      throw new RuntimeException("There are already models saved at " + modelPath + ". Manually rename or delete that directory.");
    } else {
      log.info("Writing models to {}", modelPath);
      modelPath.toFile().mkdir();
    }
    // ensure no duplicate documents
    documentList = documentList.stream()
        .filter(new DistinctKeyFilter<Document<?>, String>(Document::getId)::filter)
        .collect(Collectors.toList());
    documentList.forEach(EegJsonCorpus::preprocess);
    log.info("Training documents: {}", documentList.stream().map(d -> "(" + d.getId() + "," +
        d.get(goldAnnset, Event.TYPE).size() + "," + d.get(goldAnnset, EegActivity.TYPE).size() + ")")
        .reduce("", (s1, s2) -> s1 + s2 + ", "));
    Crf.trainEventBoundaries(documentList, modelPath.toString(), goldAnnset);
    Crf.trainActivityBoundaries(documentList, modelPath.toString(), goldAnnset);
    Svm.trainSvmAllAttrs(documentList, modelPath.toString(), goldAnnset);
    Svm.trainTypeSvm(documentList, modelPath.toString(), goldAnnset);
    Svm.trainEventModalitySvm(documentList, modelPath.toString(), goldAnnset);
    Svm.trainEventPolaritySvm(documentList, modelPath.toString(), goldAnnset);
  }

  private static void bratToJson(JsonCorpus<EegNote> json, String bratDir, String annset) {
    final JsonCorpus<EegNote> v060 = Data.v060(Data.getDefaultAnnSets(annset));
    Data.loadBrat(bratDir, annset).forEachDocument(bratDoc -> {
      // add the new annotations to the fully annotated version of this document in the v060 json corpus, then save
      // that
      if (v060.canLoad(bratDoc.getId())) {
        final Document<EegNote> fulldoc = v060.load(bratDoc.getId());
        fulldoc.clear(annset);
        bratDoc.get(annset, EegActivity.TYPE).forEach(a -> EegActivity.duplicateActivity(a, annset, fulldoc));
        bratDoc.get(annset, Event.TYPE).forEach(e -> {
          final String modality = e.get(Event.modality) != null ? e.get(Event.modality) : "FACTUAL";
          final String polarity = e.get(Event.polarity) != null ? e.get(Event.polarity) : "POSITIVE";
          Event.TYPE.create(fulldoc, annset, e.get(Annotation.StartOffset), e.get(Annotation.EndOffset))
              .set(Event.type, e.get(Event.type))
              .set(Event.modality, modality)
              .set(Event.polarity, polarity);
        });
        json.save(fulldoc);
      }
      else {
        log.warn("Doc {} was not found in v060. Preprocessing...");
        EegJsonCorpus.preprocess(bratDoc);
        json.save(bratDoc);
      }
    });
  }

  private static double entropy(double[] q) {
    double entropy = 0.0;
    for (double q_c : q) {
      entropy -= (q_c == 0.0) ? 0 : q_c * Math.log(q_c);
    }
    return entropy;
  }

  private static double margin(double[] q) {
    double l1 = Double.MIN_VALUE, l2 = Double.MIN_VALUE;
    for (double q_c : q) {
      if (q_c > l1) {
        l2 = l1;
        l1 = q_c;
      }
      else if (q_c > l2) {
        l2 = q_c;
      }
    }
    return l1 - l2;
  }

  private static class RankedList {
    private final Map<String, Integer> rankMap = Maps.newHashMap();

    RankedList(List<Pair<String,Double>> list) {
      list.sort((p1, p2) -> Double.compare(p2.second(), p1.second()));
      int rank = 0;
      double prevScore = list.get(0).second();
      rankMap.put(list.get(0).first(), rank);
      for (int i = 1; i < list.size(); i++) {
        final Pair<String, Double> pair = list.get(i);
        if (prevScore < pair.second()) {
          rank++;
        }
        rankMap.put(pair.first(), rank);
      }
    }

    int rank(String did) {
      final Integer rank = rankMap.get(did);
      if (rank == null) {
        return rankMap.size() + 1;
      }
      return rank;
    }
  }
}
