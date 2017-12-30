package edu.utdallas.hltri.eeg.scripts;

import edu.utdallas.hltri.eeg.EegNote;
import edu.utdallas.hltri.eeg.annotation.EegActivity;
import edu.utdallas.hltri.eeg.io.EegEventBratCorpus;
import edu.utdallas.hltri.logging.Logger;
import edu.utdallas.hltri.scribe.text.Document;

/**
 * Created by rmm120030 on 8/24/16.
 */
public class CombineBratCorpora {
  private static final Logger log = Logger.get(CombineBratCorpora.class);

  public static void main(String... args) {
    // path to brat corpus with gold events
    final String eventCorpusPath = args[0];
    // path to brat corpus with gold activities
    final String activityCorpusPath = args[1];
    // path to where you want to write the combined corpus
    final String outCorpusPath = args[2];
    final String annset = "brat";

    final EegEventBratCorpus evCorpus = EegEventBratCorpus.at(eventCorpusPath, annset);
    final EegEventBratCorpus actCorpus = EegEventBratCorpus.at(activityCorpusPath, annset);
    final EegEventBratCorpus combined = EegEventBratCorpus.at(outCorpusPath, annset);
    evCorpus.getIdStream().forEach(id -> {
      try (final Document<EegNote> evDoc = evCorpus.load(id);
           final Document<EegNote> actDoc = actCorpus.load(id)) {
        // remove EegActivities from evDoc
        evDoc.clear(annset, EegActivity.TYPE);
        // add each activity from actDoc to evDoc
        actDoc.get(annset, EegActivity.TYPE).forEach(act -> EegActivity.duplicateActivity(act, annset, evDoc));
//        int count = (int)actDoc.get(annset, EegActivity.TYPE).stream().filter(a -> a.get(EegActivity.morphology) != null).count();
//        log.info("Doc {} has {}/{} activities with valid morphologies.", id, count, actDoc.get(annset, EegActivity.TYPE).size());
//        count = (int)evDoc.get(annset, EegActivity.TYPE).stream().filter(a -> a.get(EegActivity.morphology) != null).count();
//        log.info("Doc {} has {}/{} activities with valid morphologies.", id, count, actDoc.get(annset, EegActivity.TYPE).size());
        combined.save(evDoc);
      }
    });
  }
}
