package edu.utdallas.hltri.mercury.relevance;

import com.google.common.base.Splitter;
import edu.utdallas.hltri.eeg.ConceptNormalization;
import edu.utdallas.hltri.eeg.Data;
import edu.utdallas.hltri.eeg.EegNote;
import edu.utdallas.hltri.eeg.ExtractTriples;
import static edu.utdallas.hltri.eeg.ExtractTriples.RelationType.*;
import edu.utdallas.hltri.eeg.annotation.EegActivity;
import edu.utdallas.hltri.framework.ProgressLogger;
import edu.utdallas.hltri.logging.Logger;
import edu.utdallas.hltri.math.Vectors;
import edu.utdallas.hltri.mercury.ConceptUtils;
import edu.utdallas.hltri.mercury.MercuryQuery;
import edu.utdallas.hltri.scribe.io.Corpus;
import edu.utdallas.hltri.scribe.text.Document;
import edu.utdallas.hltri.scribe.text.annotation.Event;
import edu.utdallas.hltri.struct.Weighted;
import org.apache.solr.common.SolrDocument;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Created by ramon on 5/9/17.
 */
public class KgRelevanceScorer implements RelevanceScorer<Document<MercuryQuery>, SolrDocument> {
  private static final Logger log = Logger.get(KgRelevanceScorer.class);

  protected final Map<String, double[]> embeddings;
  protected final Map<String, List<String>> docs2activities;
  protected final Map<String, List<String>> docs2events;

  // query information
  protected List<String> qActivities;
  protected List<String> qEvents;

  @SuppressWarnings("unchecked")
  public KgRelevanceScorer(Path embeddingMapFile, Path cache, Corpus<EegNote> corpus, String annset) {
    log.info("Loading KG embeddings from {}", embeddingMapFile);
    this.embeddings = new HashMap<>();
    Splitter splitter = Splitter.on('\t').omitEmptyStrings();
    try {
      final ArrayList<Double> values = new ArrayList<>(20);
      Files.readAllLines(embeddingMapFile).forEach(line -> {
        final Iterator<String> it = splitter.split(line).iterator();
        String word = it.next();
        values.clear();
        while (it.hasNext()) {
          values.add(Double.parseDouble(it.next()));
        }
        embeddings.put(word.replaceAll("\\s", "_"), values.stream().mapToDouble(d -> d).toArray());
      });
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    if (embeddings.containsKey("IMPACTS")) {
       embeddings.put("EVIDENCES", embeddings.get("IMPACTS"));
    } else {
      embeddings.put("IMPACTS", embeddings.get("EVIDENCES"));
    }

//    final Path cache = embeddingMapFile.getParent().resolve("concepts.cache");
    if (cache.toFile().exists()) {
      try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(cache.toFile()))) {
        log.info("Loading normalized concepts for the entire corpus from {}...", cache);
        docs2activities = (Map<String, List<String>>) ois.readObject();
        docs2events = (Map<String, List<String>>) ois.readObject();
      } catch (IOException | ClassNotFoundException e) {
        throw new RuntimeException(e);
      }
    } else {
      docs2activities = new HashMap<>(Data.V060_SIZE);
      docs2events = new HashMap<>(Data.V060_SIZE);
      final ProgressLogger plog = ProgressLogger.fixedSize("loading concepts", Data.V060_SIZE, 5L, TimeUnit.SECONDS);
      corpus.forEachDocument(doc -> {
        docs2activities.put(doc.getId(), doc.get(annset, EegActivity.TYPE).stream()
            .map(ConceptNormalization::normalizeConcept).filter(Optional::isPresent).map(Optional::get)
            .map(s -> s.replaceAll("(Te_)|(Tr_)|(P_)", "").toLowerCase())
            .filter(embeddings::containsKey).collect(Collectors.toList()));
        docs2events.put(doc.getId(), doc.get(annset, Event.TYPE).stream()
            .map(ConceptNormalization::normalizeConcept).filter(Optional::isPresent).map(Optional::get)
            .map(s -> s.replaceAll("(Te_)|(Tr_)|(P_)", "").toLowerCase())
            .filter(embeddings::containsKey).collect(Collectors.toList()));
        plog.update("{} has {} activities and {} events", doc.getId(), docs2activities.get(doc.getId()).size(),
            docs2events.get(doc.getId()).size());
      });
      try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(cache.toFile()))) {
        oos.writeObject(docs2activities);
        oos.writeObject(docs2events);
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }
  }

  @Override
  public void setQuery(Document<MercuryQuery> query) {
    qActivities = query.get(ConceptUtils.CONC_ANNSET, EegActivity.TYPE).stream()
        .map(ConceptNormalization::normalizeConcept).filter(Optional::isPresent).map(Optional::get)
        .filter(embeddings::containsKey).collect(Collectors.toList());
    qEvents = query.get(ConceptUtils.CONC_ANNSET, Event.TYPE).stream()
        .map(ConceptNormalization::normalizeConcept).filter(Optional::isPresent).map(Optional::get)
        .filter(embeddings::containsKey).collect(Collectors.toList());
    log.info("Created KG relevance model for query with activities: {} and events: {}", qActivities, qEvents);
  }

  @Override
  public double score(SolrDocument document) {
    final String rid = (String) document.get("record_id");
    if (docs2activities.containsKey(rid)) {
      final List<String> dActivities = docs2activities.get(rid);
      final List<String> dEvents = docs2events.get(rid);

      final List<Weighted<String>> similarities = new ArrayList<>();
      for (String qact : qActivities) {
        for (String dact : dActivities) {
          if (!qact.equals(dact)) {
            similarities.add(similarity(qact, OCCURS_WITH, dact));
          }
        }
        for (String dev : dEvents) {
          similarities.add(similarity(dev, EVOKES, qact));
          if (dev.startsWith("P")) {
            similarities.add(similarity(qact, EVIDENCES, dev));
          }
        }
      }
      for (String qev : qEvents) {
        for (String dact : dActivities) {
          similarities.add(similarity(qev, EVOKES, dact));
          if (qev.startsWith("P")) {
            similarities.add(similarity(dact, EVIDENCES, qev));
          }
        }
        for (String dev : dEvents) {
          if (!dev.equals(qev)) {
            if (qev.startsWith(dev.substring(0, 2))) {
              similarities.add(similarity(qev, OCCURS_WITH, dev));
            } else if (qev.startsWith("P")) {
              if (dev.startsWith("Tr")) {
                similarities.add(similarity(dev, TREATMENT_FOR, qev));
              } else {
                similarities.add(similarity(dev, EVIDENCES, qev));
              }
            } else if (dev.startsWith("P")) {
              if (qev.startsWith("Tr")) {
                similarities.add(similarity(qev, TREATMENT_FOR, dev));
              } else {
                similarities.add(similarity(qev, EVIDENCES, dev));
              }
            }
          }
        }
      }

//        double score = (similarities.size() > 0) ? similarities.stream().reduce(0.0, Double::sum) / similarities.size() : 0.0;
      Weighted<String> score = similarities.stream().reduce(Weighted.create(0, "norel"),
          (w1, w2) -> (w1.compareTo(w2) > 0) ? w1 : w2);
      log.info("Kg relevance for doc {}: {} for {} relations", rid, score, similarities.size());
      return score.getWeight();
    }
    else {
      log.info("Doc {} is not in JsonCorpus!", rid);
      return 0.0;
    }
  }

  private Weighted<String> similarity(String subj, ExtractTriples.RelationType rel, String obj) {
    if ((rel != EVOKES && rel != EVIDENCES) || !embeddings.containsKey(subj) || !embeddings.containsKey(obj)) {
      return Weighted.create(0, rel.name());
    }
    double[] s = embeddings.get(subj);
    assert s != null : "No embedding for concept: " + subj;
    double[] r = (rel == OCCURS_WITH) ? embeddings.get(rel.name() + "_" + subj.split("_")[0]) : embeddings.get(rel.name());
    assert r != null : "No embedding for relation: " + rel.name();
    double[] o = embeddings.get(obj);
    assert o != null : "No embedding for concept: " + obj;

    final Weighted<String> distance = Weighted.create(
        subj + "_" + rel + "_" + obj,
        Vectors.l1Norm(
            Vectors.elementWiseDifference(Vectors.elementWiseSum(s, r),
                                          o))
    );
    final Weighted<String> similarity = Weighted.create(subj + "_" + rel + "_" + obj, 1.0 / (1.0 + distance.getWeight()));
//      Weighted<String> similarity = Weighted.create(Vectors.angularSimilarity(s, o), subj + "_" + rel + "_" + obj);
    log.info("Distance: {}. Similarity: {}", distance, similarity);
    return similarity;
  }
}
