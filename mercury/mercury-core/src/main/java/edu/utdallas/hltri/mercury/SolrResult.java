package edu.utdallas.hltri.mercury;

import edu.utdallas.hltri.inquire.SearchResult;
import org.apache.solr.common.SolrDocument;

/**
 * Created by rmm120030 on 5/16/17.
 */
public class SolrResult implements SearchResult<SolrDocument> {
  private final SolrDocument value;
  private final int rank;
  private final double score;
  private final String id;

  public SolrResult(SolrDocument document, int rank, double score) {
    this.value = document;
    this.rank = rank;
    this.score = score;
    this.id = (String) document.get("record_id");
  }

  public SolrResult(SolrDocument document, int rank) {
    this.value = document;
    this.rank = rank;
    this.score = (Double)document.getFieldValue("score");
    this.id = (String) document.get("record_id");
  }

  @Override
  public SolrDocument getValue() {
    return value;
  }

  @Override
  public int getRank() {
    return rank;
  }

  @Override
  public double getScore() {
    return score;
  }

  public String getId() {
    return id;
  }
}
