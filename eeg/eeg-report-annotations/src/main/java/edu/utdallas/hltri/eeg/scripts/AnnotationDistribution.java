package edu.utdallas.hltri.eeg.scripts;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Maps;
import com.google.common.collect.Multiset;
import edu.utdallas.hltri.eeg.Data;
import edu.utdallas.hltri.eeg.EegNote;
import edu.utdallas.hltri.eeg.annotation.EegActivity;
import edu.utdallas.hltri.eeg.annotation.label.PolarityLabel;
import edu.utdallas.hltri.scribe.text.Document;
import edu.utdallas.hltri.scribe.text.annotation.Event;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * Created by rmm120030 on 9/22/16.
 */
public class AnnotationDistribution {

  public static void main(String... args) {
    final List<Document<EegNote>> docs = Data.activeLearning();
    final Map<String, Multiset<String>> map = Maps.newConcurrentMap();
    final String annset = args[1];

    docs.forEach(doc -> {
      final List<EegActivity> activities = doc.get(annset, EegActivity.TYPE);
      final List<Event> events = doc.get(annset, Event.TYPE);
      Multiset<String> ms = map.containsKey("Boundaries") ? map.get("Boundaries") : HashMultiset.create();
      ms.add("Activity", activities.size());
      ms.add("Event", events.size());
      map.put("Boundaries", ms);
      for (EegActivity activity : activities) {
        ms = map.containsKey("Morphology") ? map.get("Morphology") : HashMultiset.create();
        ms.add(activity.get(EegActivity.morphology));
        map.put("Morphology", ms);

        ms = map.containsKey("Frequency Band") ? map.get("Frequency Band") : HashMultiset.create();
        ms.add(activity.get(EegActivity.band));
        map.put("Frequency Band", ms);

        ms = map.containsKey("Hemisphere") ? map.get("Hemisphere") : HashMultiset.create();
        ms.add(activity.get(EegActivity.hemisphere));
        map.put("Hemisphere", ms);

        ms = map.containsKey("Magnitude") ? map.get("Magnitude") : HashMultiset.create();
        ms.add(activity.get(EegActivity.magnitude));
        map.put("Magnitude", ms);

        ms = map.containsKey("Recurrence") ? map.get("Recurrence") : HashMultiset.create();
        ms.add(activity.get(EegActivity.recurrence));
        map.put("Recurrence", ms);

        ms = map.containsKey("Dispersal") ? map.get("Dispersal") : HashMultiset.create();
        ms.add(activity.get(EegActivity.dispersal));
        map.put("Dispersal", ms);

        ms = map.containsKey("Background") ? map.get("Background") : HashMultiset.create();
        ms.add(activity.get(EegActivity.in_background));
        map.put("Background", ms);

        ms = map.containsKey("Act-Modality") ? map.get("Act-Modality") : HashMultiset.create();
        ms.add(activity.get(EegActivity.modality));
        map.put("Act-Modality", ms);

        ms = map.containsKey("Act-Polarity") ? map.get("Act-Polarity") : HashMultiset.create();
        ms.add(PolarityLabel.fromString(activity.get(EegActivity.polarity)).name());
        map.put("Act-Polarity", ms);

        ms = map.containsKey("Background") ? map.get("Background") : HashMultiset.create();
        ms.add(activity.get(EegActivity.in_background));
        map.put("Background", ms);

        for (EegActivity.Location loc : activity.getLocations()) {
          ms = map.containsKey("Location") ? map.get("Location") : HashMultiset.create();
          ms.add(loc.toString());
          map.put("Location", ms);
        }
      }
      for (Event event : events) {
        ms = map.containsKey("Type") ? map.get("Type") : HashMultiset.create();
        ms.add(event.get(Event.type));
        map.put("Type", ms);
        ms = map.containsKey("Ev-Modality") ? map.get("Ev-Modality") : HashMultiset.create();
        ms.add(event.get(Event.modality));
        map.put("Ev-Modality", ms);
        ms = map.containsKey("Ev-Polarity") ? map.get("Ev-Polarity") : HashMultiset.create();
        ms.add(PolarityLabel.fromString(event.get(Event.polarity)).name());
        map.put("Ev-Polarity", ms);
      }
    });

    try (BufferedWriter writer = new BufferedWriter(new FileWriter(args[0]))) {
      for (String key : map.keySet()) {
        writer.write(key);
        final Multiset<String> ms = map.get(key);
        for (String attr : ms.elementSet()) {
          writer.append("\n").append(attr).append("," + ms.count(attr));
        }
        writer.newLine();
        writer.newLine();
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
