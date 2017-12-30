package edu.utdallas.hlt.trecmed.evaluation;

import com.google.common.base.Predicates;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import java.io.IOException;
import java.lang.ProcessBuilder.Redirect;
import java.nio.file.Path;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import edu.utdallas.hlt.trecmed.Topic;
import edu.utdallas.hlt.trecmed.Visit;
import edu.utdallas.hlt.trecmed.evaluation.TrecQRelsReader.Relevance;
import edu.utdallas.hltri.inquire.lucene.LuceneResult;
import edu.utdallas.hltri.logging.Logger;
import edu.utdallas.hltri.scribe.text.Identifiable;

/**
 * @author travis
 */
@SuppressWarnings({"unused", "WeakerAccess"})
public class Evaluator {
  private static final Logger log = Logger.get(Evaluator.class);
  private final TrecQRelsReader systemQRels;
  private final TrecQRelsReader targetQRels;
  private       TrecEvalReader  reader;

  public static class Builder {
    private final Path systemQRelsPath, targetQRelsPath;

    public Builder(final Path systemQRelsPath, final Path targetQRelsPath) {
      this.systemQRelsPath = systemQRelsPath;
      this.targetQRelsPath = targetQRelsPath;
    }

    public Builder writeQRels(final Iterable<Topic> queries,
                              final Map<Topic, ? extends List<? extends LuceneResult<? extends Identifiable>>> results,
                              final String runtag) {
      new TrecRunWriter().writeQRels(systemQRelsPath, queries, results, runtag);
      return this;
    }

    public Evaluator build() {
      return new Evaluator(this);
    }
  }

  private Evaluator(final Builder builder) {
    this.systemQRels = new TrecQRelsReader(builder.systemQRelsPath);
    this.targetQRels = new TrecQRelsReader(builder.targetQRelsPath);
  }

  /**
   * Determines if a visit is relevant for a given question
   *
   * @param question Question id
   * @param visit    Visit id
   * @return true if relevant
   */
  public boolean isRelevant(String question, String visit) {
    switch (targetQRels.getRelevance(question, visit)) {
      case RELEVANT:
      case PARTIAL:
        return true;
      case NONRELEVANT:
      case UNKNOWN:
      default:
        return false;
    }
  }

  /**
   * Returns the relevancy for a given question
   *
   * @param question Question id
   * @param visit    Visit id
   * @return Relevance of visit for given question
   */
  public Relevance getRelevancy(String question, String visit) {
    return targetQRels.getRelevance(question, visit);
  }

  /**
   * Gets the visit list associated with a given question
   *
   * @param question Question id
   * @return list of Visit objects
   */
  public List<Visit> getVisits(final String question, final int max) {
    Set<Visit> visits = new LinkedHashSet<>();

    int i = 0;
    for (String visitId : systemQRels.findQuestion(question).keySet()) {
      if (i++ < max || isRelevant(question, visitId)) {
        visits.add(Visit.fromId(visitId));
      }
    }

    for (String visitId : targetQRels.findQuestion(question).keySet()) {
      if (isRelevant(question, visitId)) {
        visits.add(Visit.fromId(visitId));
      }
    }

    return Lists.newArrayList(visits);
  }


  public String getVisitRank(final String question, final String id) {
    int i = 1;
    for (final String visitId : systemQRels.findQuestion(question).keySet()) {
      if (id.equals(visitId)) {
        return String.valueOf(i);
      }
      i++;
    }
    return "N/A";
  }

  private void runProcess(final ProcessBuilder pb, final Path path) {
    try {
      Process process = pb.start();
      process.waitFor();
      process.getOutputStream().close();
      log.info("Official evaluations saved to {}", path);
      reader = new TrecEvalReader(path);
    } catch (IOException | InterruptedException ex) {
      throw new RuntimeException(ex);
    }
  }

  /**
   * Runs official TREC evaluations and outputs them to the provided path
   *
   * @param path Where to save official TREC evaluations
   */
  public void runOfficialEvaluations(final Path path) {
    ProcessBuilder pb = new ProcessBuilder("trec_eval",
        "-q",
        "-m", "all_trec",
        targetQRels.getPath().toString(),
        systemQRels.getPath().toString());
    pb.redirectError(Redirect.INHERIT);
    pb.redirectOutput(path.toFile());

    runProcess(pb, path);
  }

  public void runOfficialSampledEvaluations(final Path path) {
    ProcessBuilder pb = new ProcessBuilder("trec_sample_eval",
        "-q",
        "-m", "all_trec",
        targetQRels.getPath().toString(),
        systemQRels.getPath().toString());
    pb.redirectError(Redirect.INHERIT);
    pb.redirectOutput(path.toFile());

    runProcess(pb, path);
  }

  /**
   * Gets the official measure for the given question
   *
   * @param questionId Question id
   * @param measure    Measure as it appears in evaluation file
   * @return double value of measure
   */
  public double getOfficialMeasure(String questionId, String measure) {
    return reader.get(questionId, measure);
  }

  public Map<String, Double> getOfficialMeasures(final String questionId, final Collection<String> measure) {
    return Maps.filterKeys(reader.evaluations.row(questionId), Predicates.in(measure));
  }
}
