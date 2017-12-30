package edu.utdallas.hltri.eeg.scripts;

import edu.utdallas.hltri.eeg.Data;
import edu.utdallas.hltri.eeg.EegNote;
import edu.utdallas.hltri.eeg.classifier.Crf;
import edu.utdallas.hltri.eeg.classifier.Svm;
import edu.utdallas.hltri.scribe.text.Document;

import java.io.File;
import java.util.List;

/**
 * Created by rmm120030 on 11/3/16.
 */
public class TrainAllModels {

  public static void main(String... args) {
    final String goldAnnset = "gold";
    final String modelPath = args[0];
    new File(modelPath).mkdir();
    final List<Document<EegNote>> documentList = Data.activeLearning();

    Crf.trainEventBoundaries(documentList, modelPath, goldAnnset);
    Crf.trainActivityBoundaries(documentList, modelPath, goldAnnset);
    Svm.trainSvmAllAttrs(documentList, modelPath, goldAnnset);
    Svm.trainTypeSvm(documentList, modelPath, goldAnnset);
    Svm.trainEventModalitySvm(documentList, modelPath, goldAnnset);
    Svm.trainEventPolaritySvm(documentList, modelPath, goldAnnset);
  }
}
