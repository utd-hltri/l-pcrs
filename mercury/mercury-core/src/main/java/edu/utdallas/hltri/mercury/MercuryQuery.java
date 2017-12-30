package edu.utdallas.hltri.mercury;

import edu.utdallas.hltri.inquire.text.Query;
import edu.utdallas.hltri.scribe.text.DocumentAttribute;
import org.apache.solr.common.SolrDocumentList;

/**
 * Created by rmm120030 on 5/16/17.
 */
public class MercuryQuery extends Query {
  // for the SignalRelevanceScorer
  public static final DocumentAttribute<MercuryQuery, SolrDocumentList> solrResults = DocumentAttribute.inferred("solr-results");
}
