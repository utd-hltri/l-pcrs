package edu.utdallas.hltri.eeg.scripts;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import edu.utdallas.hltri.eeg.Data;
import edu.utdallas.hltri.eeg.annotation.EegActivity;
import edu.utdallas.hltri.framework.ProgressLogger;
import edu.utdallas.hltri.scribe.text.annotation.Event;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Created by rmm120030 on 9/27/17.
 */
public class CountConcepts {
  public static void main(String... args) {
    final Multiset<String> counts = HashMultiset.create();
    final ProgressLogger plog = ProgressLogger.fixedSize("counting", Data.V060_SIZE, 5, TimeUnit.SECONDS);
    Data.v060("best").forEachDocument(doc -> {
      final int numA = doc.get("best", EegActivity.TYPE).size();
      counts.add("activity", numA);
      counts.add("attr", numA*18);
      final List<Event> events = doc.get("best", Event.TYPE);
      counts.add("problems", (int) events.stream().filter(e -> e.get(Event.type).equals("PROBLEM")).count());
      counts.add("tests", (int) events.stream().filter(e -> e.get(Event.type).equals("TEST")).count());
      counts.add("treatments", (int) events.stream().filter(e -> e.get(Event.type).equals("TREATMENT")).count());
      counts.add("events", (int) events.stream().filter(e -> e.get(Event.type).equals("EEG_EVENT")).count());
      plog.update(doc.getId());
    });
    for (String name : counts.elementSet()) {
      System.out.printf("%s: %d\n", name, counts.count(name));
    }
  }
}
