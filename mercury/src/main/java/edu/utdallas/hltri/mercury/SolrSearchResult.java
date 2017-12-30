package edu.utdallas.hltri.mercury;

import edu.utdallas.hltri.inquire.SearchResult;

/**
 * Created by travis on 9/29/16.
 */
public class SolrSearchResult<T> implements SearchResult<T> {
  private final T value;
  private final int rank;

  public SolrSearchResult(T value, int rank) {
    this.value = value;
    this.rank = rank;
  }

  @Override public T getValue() {
    return value;
  }

  @Override
  public int getRank() {
    return rank;
  }

  @Override
  public double getScore() {
    return 1 - Math.tanh(-rank + 1);
  }
}
