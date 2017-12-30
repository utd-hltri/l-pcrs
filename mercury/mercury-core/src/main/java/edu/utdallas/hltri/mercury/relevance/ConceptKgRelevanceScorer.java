package edu.utdallas.hltri.mercury.relevance;

import edu.utdallas.hltri.eeg.EegNote;
import edu.utdallas.hltri.logging.Logger;
import edu.utdallas.hltri.math.Vectors;
import edu.utdallas.hltri.scribe.io.Corpus;
import edu.utdallas.hltri.struct.Weighted;
import org.apache.solr.common.SolrDocument;

import java.nio.file.Path;
import java.util.List;

/**
 * Created by ramon on 5/25/17.
 */
public class ConceptKgRelevanceScorer extends KgRelevanceScorer {
  private static Logger log = Logger.get(ConceptKgRelevanceScorer.class);

  public ConceptKgRelevanceScorer(Path embeddingMapFile, Path cache, Corpus<EegNote> corpus, String annset) {
    super(embeddingMapFile, cache, corpus, annset);
  }

  private Weighted<String> conceptSimilarity(String c1, String c2) {
    double[] c1_emb = embeddings.get(c1);
    if (c1_emb == null) {
      log.warn("No embedding for concept: {}", c1);
      return Weighted.create(0.0, "no_comparison");
    }
    double[] c2_emb = embeddings.get(c2);
    if (c2_emb == null) {
      log.warn("No embedding for concept: {}", c2);
      return Weighted.create(0.0, "no_comparison");
    }

    return Weighted.create(Vectors.angularSimilarity(c1_emb, c2_emb), c1 + "_" + c2);
  }

  @Override
  public double score(SolrDocument document) {
    final String rid = (String) document.get("record_id");
    final List<String> docConcepts = docs2activities.get(rid);
    docConcepts.addAll(docs2events.get(rid));

    Weighted<String> maxSim = Weighted.create(0.0, "no_comparison");
    for (String docConcept : docConcepts) {
      for (String qActivity : qActivities) {
        if (!qActivity.equals(docConcept)) {
          final Weighted<String> sim = conceptSimilarity(docConcept, qActivity);
          maxSim = (sim.getWeight() > maxSim.getWeight()) ? sim : maxSim;
        }
      }
      for (String qEvent : qEvents) {
        if (!qEvent.equals(docConcept)) {
          final Weighted<String> sim = conceptSimilarity(docConcept, qEvent);
          maxSim = (sim.getWeight() > maxSim.getWeight()) ? sim : maxSim;
        }
      }
    }

    log.info("Kg relevance for doc {}: {}", rid, maxSim);
    return maxSim.getWeight();
  }
}
