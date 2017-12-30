package edu.utdallas.hlt.trecmed.ranking;

import com.google.common.collect.ConcurrentHashMultiset;
import com.google.common.collect.Multiset;

import edu.utdallas.hlt.text.Document;
import edu.utdallas.hlt.text.LexiconItem;
import edu.utdallas.hlt.text.NegationSpan;
import edu.utdallas.hlt.text.Text;
import edu.utdallas.hlt.text.annotators.LexiconMatcher;
import edu.utdallas.hlt.trecmed.Keyword;
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
public class NegHedgeScorer extends VisitScorer {
  private static final Logger log = Logger.get(NegHedgeScorer.class);

  private final Multiset<String> frequencies = ConcurrentHashMultiset.create(),
                                 negations = ConcurrentHashMultiset.create();

  private LexiconMatcher matcher;

  @Override
  public void setTopic(Topic topic) {
    super.setTopic(topic);
    matcher = queryLexiconAdapters.getUnchecked(topic);
  }

  @Override
  public void setVisit(LuceneResult<Visit> visit) {
    super.setVisit(visit);
    frequencies.clear();
    negations.clear();
  }

  private void count(Multiset<String> counts, Text text, LexiconMatcher matcher) {
    for (LexiconItem match : matcher.find(text)) {
      counts.add(match.getCategory());
    }
  }

  @Override
  public void setReport(final Report report) {
    final Document document = report.getDocument();
    count(frequencies, document, matcher);
    for (NegationSpan span : document.getSub(NegationSpan.class)) {
      count(negations, span, matcher);
    }
  }

  @Override
  public double getRank() {
    double score = 0;
    for (Keyword keyword : topic) {
      String term = keyword.asString();
      if (!keyword.isRequired()) { continue; }
      if (keyword.isNegation() && frequencies.count(term) > 3 * negations.count(term)) {
        score -= 10;
      } else if (!keyword.isNegation() && frequencies.count(term) <= 3 * negations.count(term)) {
        score -= 10;
      }
    }
    return score;
  }
}
