package edu.utdallas.hltri.mercury;

import org.apache.solr.client.solrj.SolrQuery;

/**
 * Created by travis on 10/13/16.
 */
@SuppressWarnings("unused")
public class ParsedQuery {
  private final String originalQuery;
  private final String parsedQuery;

  public ParsedQuery(String originalQuery, String parsedQuery) {
    this.originalQuery = originalQuery;
    this.parsedQuery = parsedQuery;
  }

  public String getOriginalQuery() {
    return originalQuery;
  }

  public String getParsedQuery() {
    return parsedQuery;
  }

  public SolrQuery toSolrQuery() {
    return new SolrQuery(parsedQuery);
  }
}
