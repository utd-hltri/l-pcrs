package edu.utdallas.hltri.mercury;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.params.SolrParams;

import java.io.IOException;

import edu.utdallas.hltri.conf.Config;
import edu.utdallas.hltri.logging.Logger;

@SuppressWarnings("WeakerAccess")
public enum SolrWrapper {
  INSTANCE;

  private final Logger log = Logger.get(SolrWrapper.class);

  private final Config eegConfig = Config.load("eeg");
  private final Config mercuryConfig = eegConfig.getConfig("mercury");

  private final SolrClient solr = new HttpSolrClient(mercuryConfig.getString("solr-url"));

  public SolrDocument getReportById(String reportId) throws IOException, SolrServerException {
    return solr.getById(reportId);
  }

  public QueryResponse query(SolrParams params) throws SolrServerException, IOException {
    log.debug("Retrieving query {}", params);
    return solr.query(params);
  }

  public void setHighlighting(SolrQuery query, String preTag, String postTag, int fragmentSize) {
    query.setHighlight(true);
    query.setParam("hl.fl", "text");
    query.setHighlightSimplePre(preTag);
    query.setHighlightSimplePost(postTag);
    query.setHighlightFragsize(fragmentSize);
    query.setParam("hl.maxAnalyzedChars", Integer.toString(Math.max(51200, fragmentSize)));
  }

  public QueryResponse getHighlightedDocument(SolrQuery query, String reportId) throws IOException, SolrServerException {
    query.setFilterQueries("record_id:" + reportId);
    query.setHighlight(true);
    query.setParam("hl.fl", "*");
    query.setHighlightSimplePre("<span class=\"highlight\">");
    query.setHighlightSimplePost("</span>");
    query.setHighlightFragsize(0);
    return this.query(query);
  }
}
