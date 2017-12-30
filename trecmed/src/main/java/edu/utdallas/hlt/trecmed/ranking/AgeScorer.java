package edu.utdallas.hlt.trecmed.ranking;

import java.util.concurrent.atomic.AtomicInteger;

import edu.utdallas.hlt.text.LexiconItem;
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
public class AgeScorer extends VisitScorer {
  private static final Logger log = Logger.get(AgeScorer.class);

  private LexiconMatcher matcher;
  private boolean skip = false;

  private AtomicInteger withinRange, outsideRange;

  @Override
  public void setTopic(final Topic topic) {
    super.setTopic(topic);
    skip = !topic.hasAgeRequirement();
    if (!skip) {
      matcher = AgeRangeLexiconAdapter.adapt(topic.getAgeRange());
      skip = false;
    }
  }

  @Override
  public void setVisit(final LuceneResult<Visit> visit) {
    withinRange = new AtomicInteger(0);
    outsideRange = new AtomicInteger(0);
  }

  @Override
  public void setReport(final Report report) {
    if (skip) { return; }
    for (final LexiconItem match : matcher.find(report.getDocument())) {
      if (Boolean.parseBoolean(match.getCategory())) {
        withinRange.incrementAndGet();
      } else {
        outsideRange.incrementAndGet();
      }
    }
  }

  @Override
  public double getRank() {
    if (skip) { return 0; }
    if (withinRange.get() + outsideRange.get() == 0) {
      log.debug("Demoting visit {} because no age was found", visit.getValue().getId());
      return 50;
    } else if (withinRange.get() > outsideRange.get()) {
      log.trace("Passing visit {} with {} more valid ({}) than invalid ({}) ages in range {}",
        visit.getValue().getId(),
        withinRange.get() - outsideRange.get(),
        withinRange,
        outsideRange,
        visit);
      return 100;
    } else {
      log.debug("Demoting visit {} with {} less valid ({}) than invalid ({}) ages in range {}",
        visit.getValue().getId(),
        outsideRange.get() - withinRange.get(),
        withinRange,
        outsideRange,
        visit);
      return 0;
    }
  }
}
