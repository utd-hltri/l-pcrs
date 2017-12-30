package edu.utdallas.hltri.eeg.scripts;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Lists;
import com.google.common.collect.Multiset;
import edu.utdallas.hlt.medbase.metamaplite.MetaMapLiteWrapper;
import edu.utdallas.hltri.eeg.Data;
import edu.utdallas.hltri.framework.ProgressLogger;
import edu.utdallas.hltri.scribe.text.annotation.Event;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Created by rmm120030 on 2/9/17.
 */
public class CountAnnotations {

  public static void main(String... args) {
    final String annset = "run10";
    final Multiset<String> events = HashMultiset.create(),
        problems = HashMultiset.create(),
        tests = HashMultiset.create(),
        treatments = HashMultiset.create(),
        probCuis = HashMultiset.create(),
        testCuis = HashMultiset.create(),
        trCuis = HashMultiset.create();
    final ProgressLogger plog = ProgressLogger.fixedSize("counting", Data.V060_SIZE, 5L, TimeUnit.SECONDS);
    final MetaMapLiteWrapper mml = MetaMapLiteWrapper.getInstance();
    Data.v060(annset).forEachDocument(doc -> {
      doc.get(annset, Event.TYPE).forEach(ev -> {
        final String str = ev.asString().toLowerCase().replaceAll("\\n", " ");
        Optional<String> cuiOp;
        switch (ev.get(Event.type).trim().toUpperCase()) {
          case "PROBLEM": problems.add(str);
            cuiOp = mml.getBestCuiForEntireSpan(ev.asString());
            if (cuiOp.isPresent()) {
              probCuis.add(cuiOp.get());
            } else {
              probCuis.add("NO_CUI");
            }
            break;
          case "EEG_EVENT": events.add(str);
            break;
          case "TEST": tests.add(str);
            cuiOp = mml.getBestCuiForEntireSpan(ev.asString());
            if (cuiOp.isPresent()) {
              testCuis.add(cuiOp.get());
            } else {
              testCuis.add("NO_CUI");
            }
            break;
          case "TREATMENT": treatments.add(str);
            cuiOp = mml.getBestCuiForEntireSpan(ev.asString());
            if (cuiOp.isPresent()) {
              trCuis.add(cuiOp.get());
            } else {
              trCuis.add("NO_CUI");
            }
            break;
          default: throw new RuntimeException("Bad event type: " + ev.get(Event.type));
        }
      });
      plog.update(doc.getId());
    });
    writeSortedMultiset(Paths.get(args[0], "events.tsv"), events);
    writeSortedMultiset(Paths.get(args[0], "problems.tsv"), problems);
    writeSortedMultiset(Paths.get(args[0], "tests.tsv"), tests);
    writeSortedMultiset(Paths.get(args[0], "treatments.tsv"), treatments);
    writeSortedMultiset(Paths.get(args[0], "problem_cuis.tsv"), probCuis);
    writeSortedMultiset(Paths.get(args[0], "test_cuis.tsv"), testCuis);
    writeSortedMultiset(Paths.get(args[0], "treatment_cuis.tsv"), trCuis);
  }

  private static void writeSortedMultiset(Path outFile, Multiset<String> ms) {
    final ArrayList<String> list = Lists.newArrayList(ms.elementSet());
    Collections.sort(list, (s1, s2) -> Integer.compare(ms.count(s2), ms.count(s1)));
    try {
      Files.write(outFile, list.stream().map(s -> s + "\t" + ms.count(s)).collect(Collectors.toList()));
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
