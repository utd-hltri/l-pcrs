package edu.utdallas.hltri.eeg.scripts;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import edu.utdallas.hltri.eeg.Data;
import edu.utdallas.hltri.eeg.annotation.EegActivity;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

/**
 * Created by rmm120030 on 8/17/16.
 */
public class GenerateAttributeExamples {

  public static void main(String... args) {
    final Multiset<String> waveforms = HashMultiset.create();
    final Multiset<String> osciallations = HashMultiset.create();
    final Multiset<String> locations = HashMultiset.create();
    final Multiset<String> distributions = HashMultiset.create();
    final Multiset<String> hemispheres = HashMultiset.create();
    final Multiset<String> magnitudes = HashMultiset.create();
    final Multiset<String> backgrounds = HashMultiset.create();
    final Multiset<String> frequencies = HashMultiset.create();

    Data.seed().forEachDocument(doc -> doc.get("seed", EegActivity.TYPE).forEach(act -> {
      waveforms.add(act.get(EegActivity.morphology));
      osciallations.add(act.get(EegActivity.band));
      locations.add(act.get(EegActivity.location));
      distributions.add(act.get(EegActivity.dispersal));
      hemispheres.add(act.get(EegActivity.hemisphere));
      magnitudes.add(act.get(EegActivity.magnitude));
      backgrounds.add(act.get(EegActivity.in_background));
      frequencies.add(act.get(EegActivity.recurrence));
    }));
    try (BufferedWriter writer = new BufferedWriter(new FileWriter(args[0]))) {
      for (String s : waveforms.elementSet()) {
        writer.append(s).append(",").append(Integer.toString(waveforms.count(s)));
        writer.newLine();
      }
      writer.newLine();
      for (String s : osciallations.elementSet()) {
        writer.append(s).append(",").append(Integer.toString(osciallations.count(s)));
        writer.newLine();
      }
      writer.newLine();
      for (String s : locations.elementSet()) {
        writer.append(s).append(",").append(Integer.toString(locations.count(s)));
        writer.newLine();
      }
      writer.newLine();
      for (String s : distributions.elementSet()) {
        writer.append(s).append(",").append(Integer.toString(distributions.count(s)));
        writer.newLine();
      }
      writer.newLine();
      for (String s : hemispheres.elementSet()) {
        writer.append(s).append(",").append(Integer.toString(hemispheres.count(s)));
        writer.newLine();
      }
      writer.newLine();
      for (String s : magnitudes.elementSet()) {
        writer.append(s).append(",").append(Integer.toString(magnitudes.count(s)));
        writer.newLine();
      }
      writer.newLine();
      for (String s : backgrounds.elementSet()) {
        writer.append(s).append(",").append(Integer.toString(backgrounds.count(s)));
        writer.newLine();
      }
      writer.newLine();
      for (String s : frequencies.elementSet()) {
        writer.append(s).append(",").append(Integer.toString(frequencies.count(s)));
        writer.newLine();
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
