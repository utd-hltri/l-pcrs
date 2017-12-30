package edu.utdallas.hltri.mercury.lucene;

import edu.utdallas.hltri.eeg.EegNote;
import edu.utdallas.hltri.inquire.lucene.DocumentFactory;
import edu.utdallas.hltri.util.Unsafe;
import java.io.IOException;
import org.apache.lucene.analysis.en.EnglishAnalyzer;

import edu.utdallas.hltri.conf.Config;
import edu.utdallas.hltri.inquire.lucene.LuceneSearchEngine;

/**
 * Created by travis on 11/8/16.
 */
public class EegSearchEngine extends LuceneSearchEngine<IndexedEegNote> {

  private final static Config config = Config.load("eeg").getConfig("edu/utdallas/hltri/mercury").getConfig("index");

  public EegSearchEngine() {
    super(
        config.getString("lucene-index-path"),
        new EnglishAnalyzer(),
        config.getString("default-field"),
        (reader, id) -> {
          try {
            return new IndexedEegNote(reader.document(id), id);
          } catch (IOException e) {
            throw new RuntimeException(e);
          }
        }
    );
  }
}
