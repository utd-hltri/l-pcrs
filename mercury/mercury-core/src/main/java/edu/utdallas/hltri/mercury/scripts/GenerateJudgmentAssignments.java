package edu.utdallas.hltri.mercury.scripts;

import com.google.common.base.Splitter;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import edu.utdallas.hltri.logging.Logger;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import picocli.CommandLine;
import picocli.CommandLine.Help.Ansi;
import picocli.CommandLine.Parameters;

/**
 * Created by travis on 10/7/16.
 */
public class GenerateJudgmentAssignments implements Runnable {

  private static final Logger log = Logger.get(GenerateJudgmentAssignments.class);

  @Parameters(paramLabel = "JUDGMENT-SET-FILE",
      index = "0",
      description = "Judgment set file")
  private Path judgmentSetFile;

  @Parameters(paramLabel = "USER-QUERY-ASSIGNMENTS-FILE",
      index = "1",
      description = "File indicating QIDs assigned to each user"
          + "(Format: one line for each user consisting of: "
          + "(1) the user's name, (2) a tab ('\\t'), and "
          + "(3) a space-separated list of QIDs assigned to that user")
  private Path userQueryAssignmentsFile;

  @Parameters(paramLabel = "USER-QUERY-ASSIGNMENTS-FILE",
      index = "2",
      description = "Name/path of resultant CSV file")
  private Path userReportAssignmentCsvFile;

  public void run() {
    final ListMultimap<String, String> judgmentSets = ArrayListMultimap.create();
    final Splitter splitter = Splitter.on(' ').omitEmptyStrings();
    try {
      for (final String line : Files.readAllLines(judgmentSetFile)) {
        final int delim = line.indexOf('\t');
        final String qid = line.substring(0, delim);
        final Iterable<String> rids = splitter.split(line.substring(delim + 1));
        judgmentSets.putAll(qid, rids);
      }
      log.info("Read {} judgment sets for {} users", judgmentSets.size(), judgmentSets.keySet().size());
    } catch (IOException e) {
      throw new RuntimeException("Failed to read judgment sets file", e);
    }

    try (BufferedWriter writer = Files.newBufferedWriter(userReportAssignmentCsvFile)) {
      writer.append("USER").append(',')
          .append("QID").append(',')
          .append("DID").append(',')
          .append("RID");
      writer.newLine();
      for (final String line : Files.readAllLines(userQueryAssignmentsFile)) {
        final int delim = line.indexOf('\t');
        final String user = line.substring(0, delim);
        final Iterable<String> qids = splitter.split(line.substring(delim + 1));
        log.debug("Writing query assignments for {}...",  user);
        for (String qid : qids) {
          for (String rid : judgmentSets.get(qid)) {
            writer.append(user).append(',')
                .append(qid).append(',')
                .append(rid).append(',')
                .append("-1");
            writer.newLine();
          }
        }
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  // usage: <judgmentSetFile> <qRelsFile> <Output CSV>
  public static void main(String... args) throws IOException {
    final GenerateJudgmentAssignments self = new GenerateJudgmentAssignments();
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
