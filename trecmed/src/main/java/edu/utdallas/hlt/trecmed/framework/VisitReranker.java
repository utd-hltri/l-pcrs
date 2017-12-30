package edu.utdallas.hlt.trecmed.framework;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import edu.utdallas.hlt.trecmed.Report;
import edu.utdallas.hlt.trecmed.Topic;
import edu.utdallas.hlt.trecmed.Visit;
import edu.utdallas.hlt.trecmed.VisitScorer;
import edu.utdallas.hltri.inquire.lucene.LuceneResult;

public class VisitReranker {
  private final VisitScorer scorer;

  public VisitReranker(VisitScorer scorer) {
    this.scorer = scorer;
  }

  public List<LuceneResult<Visit>> rerank(final Topic topic, final Collection<LuceneResult<Visit>> visits) {
    List<LuceneResult<Visit>> ranking = new ArrayList<>(1000);
    scorer.setQuery(topic, visits);
    LuceneResult<Visit> visit;
    for (int i = 0; i < ranking.size(); i++) {
      visit = ranking.get(i);
      scorer.setVisit(visit);
      for (Report report : visit.getValue()) {
        scorer.setReport(report);
      }
      ranking.add(new LuceneResult<>(visit.getValue(), scorer.getRank(), -1, visit.getLuceneDocId()));
    }
    ranking.sort(Collections.reverseOrder());
    for (int i = 0; i < ranking.size(); i++) {
      ranking.get(i).setRank(i);
    }
    return ranking;
  }
}
