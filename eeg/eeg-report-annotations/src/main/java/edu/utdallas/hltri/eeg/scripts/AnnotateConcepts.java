package edu.utdallas.hltri.eeg.scripts;

import edu.utdallas.hltri.eeg.Data;
import edu.utdallas.hltri.eeg.EegNote;
import edu.utdallas.hltri.eeg.annotators.EegConceptAnnotator;
import edu.utdallas.hltri.eeg.io.EegJsonCorpus;
import edu.utdallas.hltri.framework.ProgressLogger;
import edu.utdallas.hltri.scribe.io.JsonCorpus;
import java.util.concurrent.TimeUnit;

/**
 * Annotates concepts with attributes on each document of the v060 eeg corpus
 * Created by rmm120030 on 11/3/16.
 */
public class AnnotateConcepts {
  public static void main(String... args) {
//    if (args.length != 2) {
//      System.err.println("Two arguments expected: <annotation set name> <model directory>");
//      System.exit(-1);
//    }
    //TODO: annotates v060 (or a specified corpus) with models from a provided directory
    final String annset = args[0];
//    final String modelDir = args[1];
//    final String corpusName = (args.length > 1) ? args[2] : "v060";
    final JsonCorpus<EegNote> corpus = Data.v060(Data.getDefaultAnnSets(annset));
    final EegConceptAnnotator<EegNote> eca = EegConceptAnnotator.bestWithLstm(annset);
    try (ProgressLogger plog = ProgressLogger.fixedSize("annotating", Data.V060_SIZE, 10L, TimeUnit.SECONDS)) {
      corpus.forEachDocument(doc -> {
        doc.clear(annset);
        EegJsonCorpus.preprocess(doc);
        eca.annotate(doc);
        doc.sync();
        plog.update("doc {}", doc.getId());
      });
    }
  }
}
