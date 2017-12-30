package edu.utdallas.hltri.eeg.scripts;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

import edu.utdallas.hltri.eeg.Data;
import edu.utdallas.hltri.eeg.EegNote;
import edu.utdallas.hltri.logging.Logger;
import edu.utdallas.hltri.scribe.io.JsonCorpus;
import edu.utdallas.hltri.scribe.text.Document;

/**
 * Created by travis on 9/15/16.
 */
public class MakeEegClassifierData {
  private static final Logger log = Logger.get(MakeEegClassifierData.class);

  public static void main(String... args) throws IOException {
    final Path outFile = Paths.get(args[0]);

    final JsonCorpus<EegNote> data =
        JsonCorpus.<EegNote>at(Data.config.getPath("corpus.v060.json-path")).tiered().build();

    final ConcurrentMap<String, String> edfLocations =
        Files.walk(Data.config.getPath("corpus.v060.edf-path"))
          .filter(p -> p.getFileName().toString().endsWith(".txt"))
          .collect(Collectors.toConcurrentMap(
              p -> com.google.common.io.Files.getNameWithoutExtension(p.getFileName().toString()),
              p -> p.resolveSibling("a_.edf.bz2").toAbsolutePath().toString()));

    try (BufferedWriter writer = Files.newBufferedWriter(outFile)) {
      data.getIdStream().forEach(id -> {
        try (Document<EegNote> note = data.loadFeaturesOnly(id)) {
          final String clazz = note.get(EegNote.interpretation);
          final String edfLocation = edfLocations.get(id);
          if (edfLocation == null) {
            log.error("Failed to get EDF for {}", id);
          } else {
            writer.append(id).append('\t').append(clazz).append('\t').append(edfLocation);
            writer.newLine();
          }
        } catch (IOException e) {
          log.error("Failed to write {}: {}", id, e.getMessage());
        }
      });
    }
  }
}
