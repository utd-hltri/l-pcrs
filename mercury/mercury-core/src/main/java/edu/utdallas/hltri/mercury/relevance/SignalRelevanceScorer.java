package edu.utdallas.hltri.mercury.relevance;

import edu.utdallas.hltri.inquire.ANN;
import edu.utdallas.hltri.logging.Logger;
import edu.utdallas.hltri.math.Vectors;
import edu.utdallas.hltri.mercury.MercuryQuery;
import edu.utdallas.hltri.scribe.text.Document;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by ramon on 5/9/17.
 */
public class SignalRelevanceScorer implements RelevanceScorer<Document<MercuryQuery>, SolrDocument> {
  private static final Logger log = Logger.get(SignalRelevanceScorer.class);

  private final ANN signalIndex;
  private final int K;

  private final List<double[]> topKSignals;

  public SignalRelevanceScorer(String signalIndexFile, int k) {
    this.signalIndex = new ANN(signalIndexFile, ANN.AutoIndex.defaultParams());
    this.K = k;
    topKSignals = new ArrayList<>(K);
    log.info("Loaded signal embeddings from {}", signalIndexFile);
  }

  @Override
  public void setQuery(Document<MercuryQuery> query) {
    final SolrDocumentList list = query.get(MercuryQuery.solrResults);
    int k = Math.min(list.size(), K);
    topKSignals.clear();
    for (int i = 0; i < k; i++) {
      String rid = (String) list.get(i).get("record_id");
      if (signalIndex.containsVector(rid)) {
        topKSignals.add(signalIndex.getVector(rid));
      } else {
        log.info("{} not in signal index!", rid);
        k++;
      }
    }
  }

  @Override
  public double score(SolrDocument document) {
    final String rid = (String) document.get("record_id");
    if (signalIndex.containsVector(rid)) {
      double[] vector = signalIndex.getVector(rid);
      double score = topKSignals.stream().mapToDouble(v -> Vectors.angularSimilarity(vector, v)).max().getAsDouble();
      log.info("Signal relevance for doc {}: {}", rid, score);
      return score;
    } else {
      log.info("{} not in signal index!", rid);
      return 0.0;
    }
  }

  @Override
  public void close() {

  }
}
