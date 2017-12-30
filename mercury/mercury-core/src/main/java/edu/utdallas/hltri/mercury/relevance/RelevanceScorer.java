package edu.utdallas.hltri.mercury.relevance;

import edu.utdallas.hltri.io.AC;

import java.util.List;

/**
 * Created by rmm120030 on 5/16/17.
 */
public interface RelevanceScorer<Q,D> extends AC {

  void setQuery(Q query);

  double score(D document);
}
