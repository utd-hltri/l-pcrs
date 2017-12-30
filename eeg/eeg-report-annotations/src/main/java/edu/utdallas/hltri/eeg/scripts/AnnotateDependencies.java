package edu.utdallas.hltri.eeg.scripts;

import edu.utdallas.hltri.eeg.Data;
import edu.utdallas.hltri.eeg.EegNote;
import edu.utdallas.hltri.framework.ProgressLogger;
import edu.utdallas.hltri.scribe.annotators.StanfordCoreAnnotator;
import edu.utdallas.hltri.scribe.text.Document;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Created by rmm120030 on 9/14/16.
 */
public class AnnotateDependencies {

  public static void main(String... args) {
    final ProgressLogger plog = ProgressLogger.fixedSize("annotating", Data.V060_SIZE, 5, TimeUnit.SECONDS);
    final StanfordCoreAnnotator<EegNote> stanford = new StanfordCoreAnnotator.Builder<EegNote>().all().clear().build();
/*    Data.v060("stanford").forEachDocument(doc -> {
      stanford.annotate(doc);
      plog.update(doc.getId());
      doc.sync();
    });
*/
    final List<Document<EegNote>> al = Data.seed().getDocumentList();
    al.forEach(doc -> {
      stanford.annotate(doc);
      doc.sync();
    });
  }
}
