package edu.utdallas.hltri.mercury.relevance;

import edu.utdallas.hltri.conf.Config;
import edu.utdallas.hltri.eeg.Data;
import edu.utdallas.hltri.eeg.EegNote;
import edu.utdallas.hltri.mercury.MercuryQuery;
import edu.utdallas.hltri.mercury.relevance.KgRelevanceScorer;
import edu.utdallas.hltri.mercury.relevance.RelevanceScorer;
import edu.utdallas.hltri.mercury.relevance.SignalRelevanceScorer;
import edu.utdallas.hltri.mercury.relevance.SolrRelevanceScorer;
import edu.utdallas.hltri.scribe.io.JsonCorpus;
import edu.utdallas.hltri.scribe.text.Document;
import edu.utdallas.hltri.struct.Pair;
import edu.utdallas.hltri.struct.Weighted;
import edu.utdallas.hltri.util.Lazy;
import org._3pq.jgrapht.WeightedGraph;
import org.apache.solr.common.SolrDocument;

import java.util.*;
import java.util.function.Supplier;

/**
 * Created by rmm120030 on 5/31/17.
 */
public class RelevanceModels {

  private static final Config config = Config.load("eeg.mercury");
  private static final String conceptAnnset = config.getString("eeg-concept-annset");
  private static final Supplier<JsonCorpus<EegNote>> corpusSupplier = Lazy.lazily(() -> Data.v060(conceptAnnset));

  private static final Supplier<SolrRelevanceScorer> solrScorer = Lazy.lazily(SolrRelevanceScorer::new);
  private static final Supplier<KgRelevanceScorer> kgScorer = Lazy.lazily(() -> new KgRelevanceScorer(
      config.getPath("concept-embedding-index"),
      config.getPath("concept-cache"),
      corpusSupplier.get(),
      conceptAnnset
  ));
  private static final Supplier<SignalRelevanceScorer> signalScorer = Lazy.lazily(() -> new SignalRelevanceScorer(
      config.getString("signal-index"),
      3
  ));

  public enum RelevanceModel {
    combo(Arrays.asList(
        Weighted.create(0.34, solrScorer.get()),
        Weighted.create(0.33, kgScorer.get()),
        Weighted.create(0.33, signalScorer.get())
    )),
    kg(Collections.singletonList(Weighted.create(1.0, kgScorer.get()))),
    signal(Collections.singletonList(Weighted.create(1.0, signalScorer.get()))),
    solr(Collections.singletonList(Weighted.create(1.0, solrScorer.get())));
    private final Collection<Weighted<RelevanceScorer<Document<MercuryQuery>, SolrDocument>>> scorers;

    RelevanceModel(Collection<Weighted<RelevanceScorer<Document<MercuryQuery>, SolrDocument>>> scorers) {
      this.scorers = scorers;
    }

    public Collection<Weighted<RelevanceScorer<Document<MercuryQuery>, SolrDocument>>> getScorers() {
      return scorers;
    }
  }

  public static Collection<Weighted<RelevanceScorer<Document<MercuryQuery>, SolrDocument>>> getCombination(
      double solrWeight, double kgWeight, double signalWeight) {
    final List<Weighted<RelevanceScorer<Document<MercuryQuery>, SolrDocument>>> list = new ArrayList<>();
    if (solrWeight > 0) {
      list.add(Weighted.create(solrWeight, solrScorer.get()));
    }
    if (kgWeight > 0) {
      list.add(Weighted.create(kgWeight, kgScorer.get()));
    }
    if (signalWeight > 0) {
      list.add(Weighted.create(signalWeight, signalScorer.get()));
    }

    return list;
  }
}
