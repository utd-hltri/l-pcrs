/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.utdallas.hlt.trecmed.ranking;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import edu.utdallas.hlt.text.Document;
import edu.utdallas.hlt.text.LexiconItem;
import edu.utdallas.hlt.text.Sentence;
import edu.utdallas.hlt.text.TextComparators;
import edu.utdallas.hlt.text.annotators.LexiconMatcher;
import edu.utdallas.hlt.trecmed.Report;
import edu.utdallas.hlt.trecmed.Topic;
import edu.utdallas.hlt.trecmed.Visit;
import edu.utdallas.hlt.trecmed.VisitScorer;
import edu.utdallas.hltri.inquire.lucene.LuceneResult;
import edu.utdallas.hltri.logging.Logger;

/**
 *
 * @author travis
 */
public class DischargeScorer extends VisitScorer {
  private static final Logger log = Logger.get(DischargeScorer.class);

  private boolean skip, cached;
  private final AtomicBoolean penalize = new AtomicBoolean(true);
  private LexiconMatcher keywordMatcher;

  @Override
  public void setTopic(final Topic topic) {
    skip = !topic.asString().contains("discharge") && !topic.asString().contains("discharge");
    keywordMatcher =queryLexiconAdapters.getUnchecked(topic);
  }

  @Override
  public void setVisit(LuceneResult<Visit> visit) {
    penalize.set(true);
  }

  @Override
  public double getRank() {
    if (!skip && penalize.get()) {
      log.info("Demoted {} for having no admission getKeywords", visit);
      return -5;
    }
    return 0;
  }

  @Override
  public void setReport(final Report report) {
    if (skip || cached) { return; }
    Document document = report.getDocument();
    List<LexiconItem> matches = keywordMatcher.find(document);
    if (("DS".equals(document.getMetaDataMap().get("type")) ||
        "DISCHARGE".equals(document.getMetaDataMap().get("suptype"))) &&
         matches.size() >= topic.getKeywords().size()) {
      penalize.set(false);
    }

    for (LexiconItem match : matches) {
      Sentence sentenceAnn = match.getOnlySuper(Sentence.class);
      if (sentenceAnn == null) {
        List<Sentence> sentences = new ArrayList<>(match.getIntersecting(Sentence.class));
        Collections.sort(sentences, TextComparators.startToken());
        sentenceAnn = sentences.get(0);
      }
      String sentence = sentenceAnn.asTokenizedString();
      if (sentence.contains("discharge")) {
        penalize.set(false);
      }
    }
  }
}
