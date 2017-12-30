package edu.utdallas.hltri.eeg;

import com.google.common.collect.ObjectArrays;
import com.google.common.io.Files;
import edu.utdallas.hltri.conf.Config;
import edu.utdallas.hltri.eeg.io.EegEventBratCorpus;
import edu.utdallas.hltri.framework.ProgressLogger;
import edu.utdallas.hltri.io.TextFiles;
import edu.utdallas.hltri.logging.Logger;
import edu.utdallas.hltri.scribe.io.JsonCorpus;
import edu.utdallas.hltri.scribe.text.BaseDocument;
import edu.utdallas.hltri.scribe.text.Document;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Created by rmm120030 on 6/29/15.
 */
@SuppressWarnings("unused")
public class Data {
  private static final Logger log = Logger.get(Data.class);
  private static final ProgressLogger plog = ProgressLogger.indeterminateSize("EEG", 1, TimeUnit.SECONDS);

  public static final int V060_SIZE = 16342;
  /**
   * genia: tokens (sentences, pos, lemmas, phrase chunks)
   * opennlp: sentences
   * regex-eeg: sections
   * umls: our internal UMLS concept annotator
   * gold: stuart-annotated EEG activities/events + medical problems/tests/treatments
   * stanford: dependencies/sentences
   */
  public static final String[] defaultAnnSets = new String[]{"genia", "opennlp", "regex-eeg", "umls", "gold", "stanford"};

  public static final Config config = Config.load("eeg");

  private static final Function<String[], JsonCorpus<EegNote>> goldCorpusLoader = a ->
      loadJson(config.getString("corpus.gold.json-path"), false, a);

  private static final Function<String[], JsonCorpus<EegNote>> seedCorpusLoader = a ->
      loadJson(config.getString("corpus.seed.json-path"), false, a);

//  private static final Function<String[], JsonCorpus<EegNote>> oldCorpusLoader = a ->
//      loadJson(config.getString("corpus.gold.old-json-path"), true, a);

  private static final Function<String[], JsonCorpus<EegNote>> testingCorpusLoader = a ->
      loadJson(config.getString("corpus.v060.testing"), true, a);

  private static final Function<String, EegEventBratCorpus> bratCorpusLoader = a ->
      loadBrat(config.getString("corpus.gold.brat-path"), a);

  public static JsonCorpus<EegNote> gold(final String... goldAnnSets) {
    return goldCorpusLoader.apply(goldAnnSets);
  }

  public static JsonCorpus<EegNote> seed(final String... goldAnnSets) {
    return seedCorpusLoader.apply(goldAnnSets);
  }

  public static JsonCorpus<EegNote> oldGold(final String... goldAnnSets) {
    return loadJson(config.getString("corpus.gold.old-json-path"), true, goldAnnSets);
  }

  public static JsonCorpus<EegNote> v060(final String... annSets) {
    return loadJson(config.getString("corpus.v060.json-path"), true, annSets);
  }

  public static List<Document<EegNote>> activeLearning(final String... otherAnnsets) {
    final String[] annsets = Data.getDefaultAnnSets(otherAnnsets);
    final List<Document<EegNote>> documentList = Data.seed(annsets).getDocumentList();
    log.info("Loaded {} documents from the seed corpus for retraining.", documentList.size());

    // load corpora from previous rounds of annotation
    final Path cachePath = config.getPath("al.cache");
    final Set<String> cachedJsonDirs = TextFiles.loadSet(cachePath.resolve("cache_list.txt").toString());
    for (String cachedJsonDir : cachedJsonDirs) {
      final Path path = Paths.get(cachedJsonDir);
      final JsonCorpus<EegNote> json = loadJsonWithv060Text(annsets, path);
      final List<Document<EegNote>> docs = json.getIdStream(config.getString("al.gold-annset")).map(json::load).collect(Collectors.toList());
      log.info("Loaded {} documents for retraining from the cached json corpus at {}.", docs.size(), cachedJsonDir);
      documentList.addAll(docs);
    }
    return documentList;
  }

  public static List<Document<EegNote>> run(final int run, final String... otherAnnsets) {
    List<Document<EegNote>> documentList = Data.seed().getDocumentList();
    log.info("Loaded {} documents from the seed corpus for retraining.", documentList.size());
    final String[] annsets = Data.getDefaultAnnSets(otherAnnsets);

    // load corpora from previous rounds of annotation
    final Path cachePath = config.getPath("al.cache");
    final Set<String> cachedJsonDirs = TextFiles.loadSet(cachePath.resolve("cache_list.txt").toString());
    for (String cachedJsonDir : cachedJsonDirs) {
      int cachedRunNum;
      if (cachedJsonDir.charAt(cachedJsonDir.length()-2) == 'n') {
        cachedRunNum = Integer.parseInt(cachedJsonDir.substring(cachedJsonDir.length() - 1));
      } else {
        cachedRunNum = Integer.parseInt(cachedJsonDir.substring(cachedJsonDir.length() - 2));
      }
      if (run >= cachedRunNum) {
        final Path path = Paths.get(cachedJsonDir);
        final JsonCorpus<EegNote> json = loadJsonWithv060Text(annsets, path);
        final List<Document<EegNote>> docs = json.getIdStream(config.getString("al.gold-annset")).map(json::load).collect(Collectors.toList());
        log.info("Loaded {} documents for retraining from the cached json corpus at {}.", docs.size(), cachedJsonDir);
        documentList.addAll(docs);
      }
    }
    return documentList;
  }

  public static JsonCorpus<EegNote> loadJsonWithv060Text(String[] annsets, Path path) {
    return JsonCorpus.<EegNote>builder(
        Data.config.getPath("corpus.v060.json-path").resolve("text").toString())
        .annotationSets(annsets)
        .tiered()
        .annPath(path.resolve("anns").toString())
        .featPath(path.resolve("feats").toString())
        .build();
  }

  public static String[] getDefaultAnnSets(final String... otherAnnsets) {
    return ObjectArrays.concat(defaultAnnSets, otherAnnsets, String.class);
  }

  public static JsonCorpus<EegNote> testing(final String... otherAnnSets) {
    return testingCorpusLoader.apply(otherAnnSets);
  }

  public static EegEventBratCorpus brat(final String annset) {
    return bratCorpusLoader.apply(annset);
  }

  public static JsonCorpus<EegNote> loadJson(final String jsonPath, final boolean tiered, final String... annsets) {
    final JsonCorpus.Builder<EegNote> builder = JsonCorpus.<EegNote>at(jsonPath);
    if (tiered) builder.tiered();
    if (annsets.length > 0) {
      builder.annotationSets(annsets);
    } else {
      builder.annotationSets(defaultAnnSets);
    }
    return builder.build();
  }

  public static EegEventBratCorpus loadBrat(final String path, final String annset) {
    return EegEventBratCorpus.at(path, annset);
  }

  /* Assumes directory structure: root/patientId/visitId/visitFile */
  @SuppressWarnings("ConstantConditions")
  public static void importEegs(final String rawDir, final String jsonDir) {
    final JsonCorpus<EegNote> json = JsonCorpus.<EegNote>at(jsonDir).tiered().build();
    for (final File rootDir : new File(rawDir).listFiles()) {
      for (final File patientId : rootDir.listFiles()) {
        for (final File visitId : patientId.listFiles()) {
          for (final File visitFile : visitId.listFiles()) {
            if (visitFile.getName().endsWith(".txt")) {
              final Document<EegNote> doc;
              try {
                final String id = visitFile.getName().substring(0, visitFile.getName().length() - 4);
                doc = Document.fromString(Files.toString(visitFile, Charset.defaultCharset()));
                doc.set(BaseDocument.id, id);
                doc.set(BaseDocument.path, visitFile.getAbsolutePath());
                doc.set(EegNote.patientId, patientId.getName());
                doc.set(EegNote.date, visitId.getName().substring(4));
                plog.update("Saving doc {}", id);
                json.save(doc);
              } catch (IOException e) {
                throw new RuntimeException(e);
              }
            }
          }
        }
      }
    }
  }
}
