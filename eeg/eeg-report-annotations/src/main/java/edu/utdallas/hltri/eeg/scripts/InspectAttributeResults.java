package edu.utdallas.hltri.eeg.scripts;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import edu.utdallas.hltri.eeg.Data;
import edu.utdallas.hltri.eeg.EegNote;
import edu.utdallas.hltri.eeg.TensorflowUtils;
import edu.utdallas.hltri.eeg.annotation.EegActivity;
import edu.utdallas.hltri.eeg.annotators.AttributeNetworkActiveLearner.*;
import edu.utdallas.hltri.ml.label.Label;
import edu.utdallas.hltri.scribe.text.Document;
import edu.utdallas.hltri.scribe.text.annotation.Sentence;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Created by rmm120030 on 10/13/16.
 */
public class InspectAttributeResults {

  public static void main(String... args) {
    final List<AttributeConfidence> confidences = TensorflowUtils.readActivityConfidences(Paths.get(args[0]));
    final Map<String, List<AttributeConfidence>> map = Maps.newHashMap();
    confidences.forEach(c -> {
      final List<AttributeConfidence> list = map.containsKey(c.getAttribute()) ? map.get(c.getAttribute()) : Lists.newArrayList();
      list.add(c);
      map.put(c.getAttribute(), list);
    });
    map.keySet().forEach(attr -> {
      final List<AttributeConfidence> list = map.get(attr);
      Collections.sort(list, (c1, c2) -> Double.compare(c1.getConfidence(), c2.getConfidence()));
      map.put(attr, list); // probably don't need this line
    });
    final Map<String, Document<EegNote>> docmap = Maps.newHashMap();
    Data.activeLearning().forEach(d -> docmap.put(d.getId(), d));

    final Path outDir = Paths.get(args[1]);
    outDir.toFile().mkdir();
    for (String attr : map.keySet()) {
      final Label[] labels = EegActivity.getAttributeLabelByName(attr);
      if (!attr.equals("LOCATION")) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(outDir.resolve(attr + ".txt").toFile()))) {
          for (Label label : labels) {
            writer.write(label.asInt() + ":" + label.toString() + ", ");
          }
          writer.newLine();
          writer.newLine();
          for (AttributeConfidence confidence : map.get(attr)) {
            if (!confidence.isCorrect()) {
              final Document<EegNote> doc = docmap.get(confidence.getDocId());
              final EegActivity act = doc.getAnnotationById(confidence.getAid(), EegActivity.TYPE).get();
              final Sentence sentence = act.getCovering("opennlp", Sentence.TYPE).get(0);
              writer.append("Doc: ").append(doc.getId()).append(", Activity: [").append(act)
                  .append("], Uncertainty: " + confidence.getConfidence())
                  .append("\n Gold: ").append(labels[Integer.parseInt(confidence.getGoldLabel())].toString())
                  .append(", Prediction: ").append(labels[Integer.parseInt(confidence.getPredictedLabel())].toString())
                  .append(", Logits: ").append(confidence.getLogits())
                  .append("\n Sentence: [").append(sentence.asString().replaceAll("\n", "<newline>")).append("]");
              writer.newLine();
              writer.newLine();
            }
          }
        } catch (IOException e) {
          throw new RuntimeException(e);
        }
      }
    }
  }
}
