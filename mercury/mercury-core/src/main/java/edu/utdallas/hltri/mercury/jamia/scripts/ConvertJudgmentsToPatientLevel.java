package edu.utdallas.hltri.mercury.jamia.scripts;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.csv.QuoteMode;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

import edu.utdallas.hltri.eeg.Data;
import edu.utdallas.hltri.eeg.EegNote;
import edu.utdallas.hltri.scribe.io.JsonCorpus;
import edu.utdallas.hltri.scribe.text.Document;

/**
 * Created by travis on 11/8/16.
 */
public class ConvertJudgmentsToPatientLevel {
  public static void main(String... args) throws IOException {
    final Path inputQrels = Paths.get(args[0]);
    final Path outputQrels = Paths.get(args[1]);

    final JsonCorpus<EegNote> notes = Data.v060();

    final Table<String, String, Integer> judgments = HashBasedTable.create();

    final CSVFormat qrels = CSVFormat.TDF.withAllowMissingColumnNames().withQuoteMode(QuoteMode.NONE).withTrim();

    try (BufferedReader reader = Files.newBufferedReader(inputQrels)) {
      for (CSVRecord fields : qrels.parse(reader)) {
        try (Document<EegNote> note = notes.loadFeaturesOnly(fields.get(2))) {
          final String patientId = note.get(EegNote.patientId);
          final String queryId = fields.get(0);
          final int judgment = Integer.parseInt(fields.get(3));

          if (!judgments.contains(queryId, patientId) || judgments.get(queryId, patientId) < judgment) {
            judgments.put(queryId, patientId, judgment);
          }
        }
      }
    }

    try (BufferedWriter writer = Files.newBufferedWriter(outputQrels);
         CSVPrinter printer = qrels.print(writer)) {
      for (String queryId : judgments.rowKeySet()) {
        for (Map.Entry<String,  Integer> entry : judgments.row(queryId).entrySet()) {
          printer.printRecord(queryId, "0", entry.getKey(), entry.getValue());
        }
      }
    }
  }
}
