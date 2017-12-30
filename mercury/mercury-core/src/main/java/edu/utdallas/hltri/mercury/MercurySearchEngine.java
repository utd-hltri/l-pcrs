package edu.utdallas.hltri.mercury;

import edu.utdallas.hltri.inquire.engines.SearchEngine;
import edu.utdallas.hltri.mercury.relevance.RelevanceScorer;
import edu.utdallas.hltri.scribe.text.Document;
import edu.utdallas.hltri.struct.Weighted;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;

import java.io.IOException;
import java.util.*;
import java.util.function.Consumer;

/**
 * Created by rmm120030 on 5/16/17.
 */
public class MercurySearchEngine implements SearchEngine<String, SolrDocument, SolrResult> {
  private final CohortQueryParser parser;
  private final Collection<Weighted<RelevanceScorer<Document<MercuryQuery>,SolrDocument>>> weightedScorers;
  private final Random random = new Random(1337);

  public MercurySearchEngine(CohortQueryParser parser,
                             Collection<Weighted<RelevanceScorer<Document<MercuryQuery>,SolrDocument>>> weightedScorers) {
    this.parser = parser;
    this.weightedScorers = weightedScorers;
  }

  @Override
  public SolrResultList search(String query, int numResults) {
    try {
      return search(query, numResults, s -> {});
    } catch (IOException|SolrServerException e) {
      throw new RuntimeException(e);
    }
  }

  public SolrResultList search(String query, int numResults, Consumer<SolrQuery> queryOptionsHandler)
      throws IOException, SolrServerException {
    final Document<MercuryQuery> queryDocument = parser.preprocessQuery(query);
    final SolrQuery solrQuery = parser.parse(queryDocument);

    // apply query options
    queryOptionsHandler.accept(solrQuery);
    solrQuery.setRows(numResults);

    final QueryResponse response = SolrWrapper.INSTANCE.query(solrQuery);
    final SolrDocumentList results = response.getResults();
    queryDocument.set(MercuryQuery.solrResults, results);
    Collections.shuffle(results, random);

    for (Weighted<RelevanceScorer<Document<MercuryQuery>, SolrDocument>> weightedScorer : weightedScorers) {
      weightedScorer.getValue().setQuery(queryDocument);
    }

    final List<SolrResult> reranked = RankingUtils.rerank(weightedScorers, results);
    return new SolrResultList(reranked, response);
  }
}
