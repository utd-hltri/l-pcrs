package edu.utdallas.hltri.mercury.relevance;

import edu.utdallas.hltri.inquire.text.Query;
import edu.utdallas.hltri.logging.Logger;
import edu.utdallas.hltri.mercury.MercuryQuery;
import edu.utdallas.hltri.scribe.text.Document;
import org.apache.solr.common.SolrDocument;

/**
 * Created by ramon on 5/8/17.
 */
public class SolrRelevanceScorer implements RelevanceScorer<Document<MercuryQuery>, SolrDocument> {
  private final static Logger log = Logger.get(SolrRelevanceScorer.class);

  @Override
  public void setQuery(Document<MercuryQuery> query) {
    // nothing needs to be done here
  }

  @Override
  public double score(SolrDocument document) {
    log.info("Solr relevance for doc {}: {}", document.get("record_id"), (Float)document.get("score"));
    return ((Float)document.get("score")).doubleValue();
  }
}
