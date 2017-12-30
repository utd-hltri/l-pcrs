package edu.utdallas.hltri.mercury.scripts;

import com.lexicalscope.jewel.cli.ArgumentValidationException;
import com.lexicalscope.jewel.cli.Cli;
import com.lexicalscope.jewel.cli.CliFactory;
import com.lexicalscope.jewel.cli.Option;
import edu.utdallas.hltri.conf.Config;
import edu.utdallas.hltri.eeg.Data;
import edu.utdallas.hltri.inquire.eval.TrecRunWriter;
import edu.utdallas.hltri.logging.Logger;
import edu.utdallas.hltri.mercury.CohortQueryParser;
import edu.utdallas.hltri.mercury.ConceptCohortQueryParser;
import edu.utdallas.hltri.mercury.MercuryQuery;
import edu.utdallas.hltri.mercury.MercurySearchEngine;
import edu.utdallas.hltri.mercury.ParsedQueryLoader;
import edu.utdallas.hltri.mercury.SolrResult;
import edu.utdallas.hltri.mercury.relevance.ConceptKgRelevanceScorer;
import edu.utdallas.hltri.mercury.relevance.KgRelevanceScorer;
import edu.utdallas.hltri.mercury.relevance.RelevanceScorer;
import edu.utdallas.hltri.mercury.relevance.SignalRelevanceScorer;
import edu.utdallas.hltri.mercury.relevance.SolrRelevanceScorer;
import edu.utdallas.hltri.scribe.text.Document;
import edu.utdallas.hltri.struct.Pair;
import edu.utdallas.hltri.struct.Weighted;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.common.SolrDocument;

/**
 * Created by ramon on 5/10/17.
 */
public class MakeRunsJamia {
  private static final Logger log = Logger.get(MakeRunsJamia.class);
  private static final Config mercuryConfig = Config.load("eeg.mercury");

  interface MercurySearchOptions {
    @Option(longName = "run-name", defaultValue = "default", description = "Run name.")
    String getRunName();

    @Option(longName = "k", defaultValue = "3", description = "K to use during k-means signal relevance.")
    int getK();

    @Option(longName = "query-file", defaultValue = "/home/travis/work/eegs/mercury/general_queries.csv",
        description = "Query csv file")
    String getQueries();

    @Option(longName = "outdir", defaultValue = "/home/rmm120030/working/eeg/mercury",
        description = "Directory to write experiments to")
    String getOutDir();

    @Option(longName = "kg-emb-file", defaultValue = "default", description = "KG embedding file")
    String getKgEmbFile();

    @Option(longName = "signal-emb-file", defaultValue = "default", description = "Signal embedding file")
    String getSignalEmbFile();

    @Option(longName = "solr-weight", defaultValue = "0.34", description = "Weight of solr relevance model.")
    double solrWeight();

    @Option(longName = "kg-weight", defaultValue = "0.33", description = "Weight of kg relevance model.")
    double kgWeight();

    @Option(longName = "signal-weight", defaultValue = "0.33", description = "Weight of signal relevance model.")
    double signalWeight();

    @Option(longName = "use-old-query-parser", description = "Use the old query parser?")
    boolean useOldQueryParser();

    @Option(longName = "use-umls", description = "Use umls for query expansion?")
    boolean useUmls();

    @Option(longName = "use-kg-qe", description = "Use kg for query expansion?")
    boolean useKgQe();

    @Option(longName = "use-concept-sim",
        description = "Use concept similarity for KG relevance instead of relation plausibility?")
    boolean useConceptSimilarity();
  }

  public static void main(String... args) throws IOException, SolrServerException {
    final Cli<MercurySearchOptions> cli = CliFactory.createCli(MercurySearchOptions.class);
    try {
      final MercurySearchOptions options = cli.parseArguments(args);
      final Path outfile = createExperimentFolder(options);
      final MercurySearchEngine searchEngine = createSearchEngine(options);
      try (final TrecRunWriter qrels = new TrecRunWriter(outfile, "MERCuRY")) {
        for (final Map.Entry<String, String> query : ParsedQueryLoader.INSTANCE.loadUnparsed(Paths.get(options.getQueries())).entrySet()) {
          final List<SolrResult> finalResults = searchEngine.search(query.getValue(), 1000);

          int rank = 1;
          if (finalResults.size() < 100) {
            log.warn("Returned only {} results for query {}", finalResults.size(), query.getKey());
          }
          for (final SolrResult result : finalResults) {
            qrels.writeResult(query.getKey(), result.getId(), rank, result.getScore());
            rank++;
            if (rank > 100) break;
          }
        }
      }
    } catch (ArgumentValidationException e) {
      System.out.println(cli.getHelpMessage());
      throw new RuntimeException(e);
    }
  }

  private static MercurySearchEngine createSearchEngine(MercurySearchOptions options) {
    final CohortQueryParser parser = (options.useOldQueryParser()) ?
        new CohortQueryParser(options.useUmls(), "opennlp", "opennlp") :
        new ConceptCohortQueryParser(options.useUmls(), options.useKgQe());
    final Collection<Weighted<RelevanceScorer<Document<MercuryQuery>,SolrDocument>>> scorers = new ArrayList<>();
    if (options.solrWeight() > 0) {
      scorers.add(Weighted.create(options.solrWeight(), new SolrRelevanceScorer()));
    }
    if (options.kgWeight() > 0) {
      final String conceptAnnset = mercuryConfig.getString("eeg-concept-annset");
      final Function<Path, KgRelevanceScorer> kgRelevanceConstructor = path -> (options.useConceptSimilarity()) ?
          new ConceptKgRelevanceScorer(path, mercuryConfig.getPath("concept-cache"), Data.v060(conceptAnnset), conceptAnnset) :
          new KgRelevanceScorer(path, mercuryConfig.getPath("concept-cache"), Data.v060(conceptAnnset), conceptAnnset);

      final Path kgEmbeddingFile = ("default".equals(options.getKgEmbFile())) ?
          mercuryConfig.getPath("concept-embedding-index") :
          Paths.get(options.getKgEmbFile());
      scorers.add(Weighted.create(options.kgWeight(), kgRelevanceConstructor.apply(kgEmbeddingFile)));
    }
    if (options.signalWeight() > 0) {
      final String signalEmbeddingFile = ("default".equals(options.getSignalEmbFile())) ?
          mercuryConfig.getString("signal-index") :
          options.getSignalEmbFile();
      scorers.add(Weighted.create(options.signalWeight(), new SignalRelevanceScorer(signalEmbeddingFile, options.getK())));
    }
    return new MercurySearchEngine(parser, scorers);
  }

  private static Path createExperimentFolder(MercurySearchOptions options) throws IOException {
    final Path path = Paths.get(options.getOutDir()).resolve(options.getRunName());
    if (path.toFile().exists() || path.toFile().mkdir()) {
      final List<String> parameters = new ArrayList<>();
      parameters.add("queries: " + options.getQueries());
      parameters.add("use old query parser: " + options.useOldQueryParser());
      parameters.add("use umls query expansion: " + options.useUmls());
      parameters.add("use kg query expansion: " + options.useKgQe());
      parameters.add("solr weight: " + options.solrWeight());
      parameters.add("kg weight: " + options.kgWeight());
      parameters.add("use concept kg relevance: " + options.useConceptSimilarity());
      parameters.add("signal weight: " + options.signalWeight());
      parameters.add("kg embeddings: " + options.getKgEmbFile());
      parameters.add("signal embeddings: " + options.getSignalEmbFile());
      Files.write(path.resolve("parameters.txt"), parameters);
      return path.resolve("run.qrel");
    } else {
      throw new RuntimeException("Could not make dir at " + path);
    }
  }
}
