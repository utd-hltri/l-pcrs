package edu.utdallas.hltri.mercury;

import edu.utdallas.hltri.logging.Logger;
import edu.utdallas.hltri.mercury.relevance.RelevanceScorer;
import edu.utdallas.hltri.struct.Pair;
import edu.utdallas.hltri.struct.Weighted;
import org.apache.solr.common.SolrDocument;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Created by rmm120030 on 5/16/17.
 */
public class RankingUtils {
  private static final Logger log = Logger.get(RankingUtils.class);

  public static <A> List<SolrResult> rerank(Collection<Weighted<RelevanceScorer<A, SolrDocument>>> weightedScorers,
                                            List<SolrDocument> documents) {
    final double[] combinedScores = new double[documents.size()];
    for (Weighted<RelevanceScorer<A, SolrDocument>> weightedScorer : weightedScorers) {
      final RelevanceScorer<?, SolrDocument> scorer = weightedScorer.getValue();
      final Double weight = weightedScorer.getWeight();
      final double[] scores = new double[documents.size()];
      final Iterator<SolrDocument> it = documents.iterator();
      for (int i = 0; it.hasNext(); i++) {
        scores[i] = scorer.score(it.next());
      }
      minMaxScale(scores, "");
      for (int i = 0; i < scores.length; i++) {
        combinedScores[i] += weight * scores[i];
      }
    }

    final List<SolrResult> results = new ArrayList<>(documents.size());

    final List<Weighted<Integer>> didWithScores = IntStream.range(0, combinedScores.length)
        .mapToObj(i -> Weighted.create(combinedScores[i], (Integer) i)).collect(Collectors.toList());
    didWithScores.sort(Comparator.reverseOrder());
    for (int i = 0; i < didWithScores.size(); i++) {
      final Weighted<Integer> didWithScore = didWithScores.get(i);
      results.add(new SolrResult(documents.get(didWithScore.getValue()), i + 1, didWithScore.getWeight()));
    }

    // this didn't seem to work for some reason... maybe the score field is somehow immutable?
//    IntStream.range(0, combinedScores.length).forEach(i -> documents.get(i).setField("score", combinedScores[i]));
//    documents.sort((d1, d2) -> Double.compare((Double)d2.getFieldValue("score"), (Double)d1.getFieldValue("score")));
//    for (int i = 0; i < documents.size(); i++) {
//      results.add(new SolrResult(documents.get(i), i + 1));
//    }
    return results;
  }

  public static void minMaxScale(final Map<String, Double> scoreMap, String name) {
    final DoubleSummaryStatistics dss = scoreMap.values().stream().mapToDouble(s -> s).summaryStatistics();
    scoreMap.entrySet().stream().forEach(e -> scoreMap.put(e.getKey(), minMaxScale(e.getValue(), dss, name)));
  }

  public static void minMaxScale(double[] scores, String name) {
    final DoubleSummaryStatistics dss = Arrays.stream(scores).summaryStatistics();
    for (int i = 0; i < scores.length; i++) {
      scores[i] = minMaxScale(scores[i], dss, name);
    }
  }

  public static double minMaxScale(double score, DoubleSummaryStatistics dss, String name) {
    double scaled = ((score - dss.getMin()) / (dss.getMax() - dss.getMin()));
    if (Double.isNaN(scaled)) {
      log.warn("[{}]NaN score for {} with summary statistics: {}", name, score, dss);
      return 0.0;
    }
    if (!Double.isFinite(scaled)) {
      log.warn("[{}]Infinite score for {} with summary statistics: {}", name, score, dss);
      return 0.0;
    }
    return scaled;
  }
}
