package edu.utdallas.hltri.mercury.jamia.scripts;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import edu.utdallas.hltri.eeg.Data;
import edu.utdallas.hltri.eeg.EegNote;
import edu.utdallas.hltri.scribe.io.JsonCorpus;
import edu.utdallas.hltri.scribe.text.Document;

/**
 * Created by travis on 11/18/16.
 */
public class GeneratePatientReportMapping {
  public static void main(String... args) throws IOException {
    JsonCorpus<EegNote> notes = Data.v060();

    Iterable<String> lines = notes.getIdStream().map(id -> {
      try (final Document<EegNote> doc = notes.loadFeaturesOnly(id)) {
        final String patientId = doc.get(EegNote.patientId);
        final String reportId = doc.get(EegNote.id);
        return patientId + '\t' + reportId;
      }
    })::iterator;

    Files.write(Paths.get(args[0]), lines);
  }
}
