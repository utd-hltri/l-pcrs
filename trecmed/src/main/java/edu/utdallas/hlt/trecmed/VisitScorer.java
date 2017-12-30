/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.utdallas.hlt.trecmed;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

import java.util.Collection;

import edu.utdallas.hlt.text.annotators.LexiconMatcher;
import edu.utdallas.hlt.trecmed.ranking.QueryLexiconAdapter;
import edu.utdallas.hltri.inquire.lucene.LuceneResult;

/**
 *
 * @author travis
 */
public abstract class VisitScorer {
  protected Topic                   topic;
  protected LuceneResult<Visit>            visit;
  protected Report                  report;
  protected Collection<LuceneResult<Visit>> visits;

  protected final LoadingCache<Topic, LexiconMatcher> queryLexiconAdapters = CacheBuilder
      .newBuilder()
      .concurrencyLevel(Runtime.getRuntime().availableProcessors())
      .build(CacheLoader.from(topic -> new QueryLexiconAdapter().adapt(topic)));

  public void setTopic(final Topic topic) {
    this.topic = topic;
  }

  public void setQuery(final Topic topic, final Collection<LuceneResult<Visit>> visits) {
    setTopic(topic);
    this.visits = visits;
  }

  public void setVisit(final LuceneResult<Visit> visit) {
    this.visit = visit;
  }

  public void setVisit(final Visit visit) {
    this.visit = new LuceneResult<>(visit, 0d, 0, -1);
  }

  public void setReport(final Report report) {
    this.report = report;
  }

  public abstract double getRank();
}
