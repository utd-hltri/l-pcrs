package edu.utdallas.hltri.mercury.scripts;

import edu.utdallas.hltri.inquire.SearchResult;
import edu.utdallas.hltri.inquire.eval.TrecRunReader;
import edu.utdallas.hltri.logging.Logger;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import picocli.CommandLine;
import picocli.CommandLine.Help.Ansi;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

/**
 * Created by travis on 10/7/16.
 */
public class GenerateJudgmentSets implements Runnable {

  private static final Logger log = Logger.get(GenerateJudgmentSets.class);

  @Option(names = {"-l", "--limit"},
          description = "max number of articles to judge for each query")
  private int limit = 1_000;

  @Option(names = {"-r", "--max-relevant"},
          description = "maximum number of relevant documents to judge for each query")
  private int maxRelevant = 10;

  @Option(names = {"-s", "--max-samples"},
          description = "maximum number of sampled documents to judge for each query")
  private int maxSampled = 10;

  @Option(names = {"-S", "--sample-start"},
          description = "upper bound (rank) for sampling documents")
  private int sampleStart = maxRelevant + 1;

  @Option(names = {"-E", "--sample-end"},
          description = "lower bound (rank) for sampling documents")
  private int sampleEnd = 100;

  @Parameters(paramLabel = "TREC-RUN-FILE",
              index = "0",
              description = "TREC-style run file to judge")
  private Path runFile;

  @Parameters(paramLabel = "DEST-FILE",
              index = "1",
              description = "Destination of resultant judgment set file")
  private Path judgmentSetFile;

  @Override
  public void run() {
    log.debug("Running {}", this.toString());
    final ThreadLocalRandom random = ThreadLocalRandom.current();
    final TrecRunReader reader = new TrecRunReader(runFile);
    try (BufferedWriter writer = Files.newBufferedWriter(judgmentSetFile)) {
      for (String qid : reader.keySet()) {
        final List<String> judgmentSet = new ArrayList<>();

        final List<SearchResult<String>> reports = reader.get(qid);

        final int relevantLimit = Math.min(maxRelevant, reports.size());
        reports.stream()
            .limit(relevantLimit)
            .map(SearchResult::getValue)
            .forEach(judgmentSet::add);

        final int sampleStart = Math.min(this.sampleStart, reports.size() - relevantLimit);
        final int sampleEnd = Math.min(this.sampleEnd, reports.size() - relevantLimit);
        final int numSampled = Math.min(this.maxSampled, reports.size() - relevantLimit);
        if (sampleStart >= sampleEnd) {
          log.error("Attempted to sample between ranks {} and {} for list of size {} (adjusted to {} after skipping {} relevant articles)",
              sampleStart, sampleEnd, reports.size(), reports.size() - relevantLimit, relevantLimit);
        }
        random.ints(sampleStart, sampleEnd)
            .distinct()
            .limit(numSampled)
            .mapToObj(reports::get)
            .map(SearchResult::getValue)
            .forEach(judgmentSet::add);

        writer.append(qid).append('\t');
        final int limit = Math.min(this.limit, judgmentSet.size()) ;
        for (int r = 0; r < limit; r++) {
          if (r > 0) {
            writer.append(' ');
          }
          writer.append(judgmentSet.get(r));
        }
        writer.newLine();
      }
    } catch (IOException e) {
    throw new RuntimeException(e);
    }
  }

  @Override
  public String toString() {
    return "GenerateJudgmentSets{" +
        "limit=" + limit +
        ", maxRelevant=" + maxRelevant +
        ", maxSampled=" + maxSampled +
        ", sampleStart=" + sampleStart +
        ", sampleEnd=" + sampleEnd +
        ", runFile=" + runFile +
        ", judgmentSetFile=" + judgmentSetFile +
        '}';
  }

  //usage: <qRels file> <output CSV>
  public static void main(String... args) throws IOException {
    final GenerateJudgmentSets self = new GenerateJudgmentSets();
    final CommandLine cmd = new CommandLine(self).registerConverter(Path.class, Paths::get);
    try {
      cmd.parse(args);
    } catch (Exception ex) {
      System.err.println(ex.getMessage());
      cmd.usage(System.err, Ansi.AUTO);
      return;
    }
    self.run();
  }
}
