package edu.utdallas.hltri.eeg.scripts;

import edu.utdallas.hltri.eeg.Data;
import edu.utdallas.hltri.eeg.EegNote;
import edu.utdallas.hltri.eeg.io.EegEventBratCorpus;
import edu.utdallas.hltri.eeg.io.EegJsonCorpus;
import edu.utdallas.hltri.eeg.io.EegJsonCorpus;
import edu.utdallas.hltri.scribe.io.JsonCorpus;

/**
 * Created by rmm120030 on 9/2/16.
 */
public class WriteSpecificBratDocs {

  public static void main(String... args) {
    final EegEventBratCorpus brat = Data.loadBrat(args[0], args[1]);
    final JsonCorpus<EegNote> json = Data.v060(Data.getDefaultAnnSets(args[1]));
    for (int i = 2; i < args.length; i++) {
      brat.save(json.load(args[i]));
    }
  }
}
