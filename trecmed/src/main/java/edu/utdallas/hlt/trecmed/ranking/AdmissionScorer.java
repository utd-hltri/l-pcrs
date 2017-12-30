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
public class AdmissionScorer extends VisitScorer {
  private static final Logger log = Logger.get(AdmissionScorer.class);

  private LexiconMatcher keywordMatcher;
  private final AtomicBoolean penalize = new AtomicBoolean(false);
  private boolean cached;

  public void setTopic(Topic topic) {
    super.setTopic(topic);
    keywordMatcher = queryLexiconAdapters.getUnchecked(topic);
  }

  @Override
  public void setVisit(LuceneResult<Visit> visit) {
    super.setVisit(visit);
    penalize.set(true);
  }

  @Override
  public void setReport(final Report report) {
    if (cached) { return; }
    final Document document = report.getDocument();
    List<LexiconItem> matches = keywordMatcher.find(document);
    if (document.getMetaDataMap().get("subtype").equals("ADMISSION") &&
        matches.size() >= topic.getKeywords().size()) {
      penalize.set(false);
    }
    for (LexiconItem match : matches) {
      String section = report.getPreviousSection(match).asStemmedString();
      if (section.contains("ADMISSION") || section.contains("ADMITTING")) {
        penalize.set(false);
      }
      Sentence sentenceAnn = match.getOnlySuper(Sentence.class);
      if (sentenceAnn == null) {
        List<Sentence> sentences = new ArrayList<>(match.getIntersecting(Sentence.class));
        Collections.sort(sentences, TextComparators.startToken());
        sentenceAnn = sentences.get(0);
      }
      String sentence = sentenceAnn.asTokenizedString();
      if (sentence.contains("admit for") ||
          sentence.contains("admit to the hospital for") ||
          sentence.contains("present to the hospital")) {
        penalize.set(false);
      }
    }
  }

  @Override
  public double getRank() {
    return (penalize.get()) ? -5 : 0;
  }
}
