package edu.utdallas.hlt.trecmed.ranking;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import edu.utdallas.hlt.trecmed.Report;
import edu.utdallas.hlt.trecmed.Topic;
import edu.utdallas.hlt.trecmed.Visit;
import edu.utdallas.hlt.trecmed.VisitScorer;
import edu.utdallas.hlt.trecmed.framework.App;
import edu.utdallas.hltri.concurrent.BlockPolicy;
import edu.utdallas.hltri.inquire.lucene.LuceneResult;
import edu.utdallas.hltri.logging.Logger;

/**
 *
 * @author travis
 */
public class CompositeScorer extends VisitScorer {
  private static final Logger log = Logger.get(App.class);

  private final VisitScorer[] scorers;
  private ExecutorService service;

  public CompositeScorer(VisitScorer... scorers) {
    this.scorers = scorers;
  }

  protected void prepare() {
    service = new ThreadPoolExecutor(
      Runtime.getRuntime().availableProcessors(),
      Runtime.getRuntime().availableProcessors(),
      10, TimeUnit.SECONDS,
      new ArrayBlockingQueue<Runnable>(1000),
      new BlockPolicy());
  }

  protected void finish() {
    try {
      service.shutdown();
      service.awaitTermination(1, TimeUnit.DAYS);
    } catch (InterruptedException ex) {
      throw new RuntimeException(ex);
    }
  }

  @Override
  public void setTopic(final Topic topic) {
    prepare();
    for (final VisitScorer scorer : scorers) {
      scorer.setTopic(topic);
    }
  }

  @Override
  public void setVisit(LuceneResult<Visit> visit) {
    for (VisitScorer scorer : scorers) {
      scorer.setVisit(visit);
    }
  }

  @Override
  public void setReport(final Report report) {
    for (final VisitScorer scorer : scorers) {
      service.execute(new Runnable() {
        public void run() {
          scorer.setReport(report);
        }
      });
    }
  }

  @Override
  public double getRank() {
    finish();
    double total = 0.0;
    for (VisitScorer scorer : scorers) {
      total += scorer.getRank();
    }
    return total;
  }
}
