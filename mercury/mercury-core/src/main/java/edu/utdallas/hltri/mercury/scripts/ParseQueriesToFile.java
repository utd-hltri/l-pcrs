package edu.utdallas.hltri.mercury.scripts;

import edu.utdallas.hltri.mercury.ConceptCohortQueryParser;
import edu.utdallas.hltri.mercury.ParsedQueryLoader;
import java.nio.file.Files;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Map;

/**
 * Created by ramon on 5/27/17.
 */
public class ParseQueriesToFile {

  public static void main(String... args) {
    final Map<String, String> queries = ParsedQueryLoader.INSTANCE.loadUnparsed(Paths.get(args[0]));
    final ConceptCohortQueryParser parser = new ConceptCohortQueryParser(false, false);
    try {
      final CSVPrinter printer = new CSVPrinter(Files.newBufferedWriter(Paths.get(args[1])), CSVFormat.DEFAULT);
      for (Map.Entry<String, String> entry : queries.entrySet()) {
        printer.printRecord(entry.getKey(), entry.getValue(), parser.parse(entry.getValue()).getQuery());
      }
      printer.flush();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
