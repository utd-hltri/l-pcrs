package edu.utdallas.hltri.eeg.annotators;

import edu.utdallas.hltri.conf.Config;
import edu.utdallas.hltri.eeg.EegNote;
import edu.utdallas.hltri.eeg.TensorflowUtils;
import edu.utdallas.hltri.eeg.al.ActiveLearner;
import edu.utdallas.hltri.eeg.annotation.EegActivity;
import edu.utdallas.hltri.logging.Logger;
import edu.utdallas.hltri.scribe.text.Document;
import edu.utdallas.hltri.struct.Pair;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * Created by rmm120030 on 10/12/16.
 */
public class AttributeNetworkActiveLearner implements ActiveLearner<EegActivity, EegNote> {
  private static final Logger log = Logger.get(AttributeNetworkActiveLearner.class);
  private final Path modelDir, dataDir;
  private final String annset;
  private final Config conf;

  public AttributeNetworkActiveLearner(final Path modelDir, final Path dataDir, final String annset) {
    this.modelDir = modelDir;
    this.dataDir = dataDir;
    this.annset = annset;
    this.conf = Config.load("eeg.nn");
  }

  @Override
  public <B extends EegNote> List<Pair<EegActivity, double[]>> annotateWithConfidence(Document<B> document) {
    throw new UnsupportedOperationException("Use annotateAllWithConfidence. Annotation of individual documents is not supported.");
  }

  public List<AttributeConfidence> annotateAllWithConfidence(List<Document<EegNote>> documents) {
    TensorflowUtils.writeActivityVectors(documents, dataDir.toString(), annset);
    final String predictionFile = modelDir.toString() + File.separator + "predictions.csv";
    final ProcessBuilder pb = new ProcessBuilder("pythontf9",
        conf.getString("pythonfile"),
        "--depth=" + 5,
        "--data_dir=" + dataDir.toString(),
        "--model=highway",
        "--model_dir=" + modelDir.toString(),
        "--reportfile=" + predictionFile);
    pb.directory(conf.getPath("dir").toFile());
    log.info("Running command: {}...", pb.command());
    final Process process;
    try {
      process = pb.start();
      process.waitFor();
      log.info("done!");
    } catch (IOException | InterruptedException e) {
      throw new RuntimeException(e);
    }
    return TensorflowUtils.readActivityConfidences(Paths.get(predictionFile));
  }

  @Override
  public <B extends EegNote> void annotate(Document<B> document) {
    annotateWithConfidence(document);
  }

  public static class AttributeConfidence {
    private final double confidence;
    private final int aid;
    private final String docId, attribute, predictedLabel, goldLabel, logits;

    public AttributeConfidence(String line) {
      final String[] fields = line.split(",");
      this.docId = fields[0].substring(0, fields[0].indexOf(':'));
      this.aid = Integer.parseInt(fields[0].substring(fields[0].indexOf(':') + 9));
      this.attribute = fields[1];
      this.confidence = Double.parseDouble(fields[2]);
      this.predictedLabel = fields[3];
      this.goldLabel = fields[4];
      this.logits = fields[5];
    }

    public double getConfidence() {
      return confidence;
    }

    public int getAid() {
      return aid;
    }

    public String getDocId() {
      return docId;
    }

    public String getAttribute() {
      return attribute;
    }

    public String getPredictedLabel() {
      return predictedLabel;
    }

    public String getGoldLabel() {
      return goldLabel;
    }

    public String getLogits() {
      return logits;
    }

    public boolean isCorrect() {
      return predictedLabel.equals(goldLabel);
    }
  }
}
