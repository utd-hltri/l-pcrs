/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.utdallas.hlt.trecmed.ranking;

import com.google.common.collect.ConcurrentHashMultiset;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import com.google.common.collect.Multiset.Entry;
import com.google.common.collect.Sets;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import edu.utdallas.hlt.text.Document;
import edu.utdallas.hlt.text.LexiconItem;
import edu.utdallas.hlt.text.MedicalConcept;
import edu.utdallas.hlt.text.annotators.LexiconMatcher;
import edu.utdallas.hlt.trecmed.Keyword;
import edu.utdallas.hlt.trecmed.Report;
import edu.utdallas.hlt.trecmed.Topic;
import edu.utdallas.hlt.trecmed.Visit;
import edu.utdallas.hlt.trecmed.VisitScorer;
import edu.utdallas.hlt.util.Config;
import edu.utdallas.hltri.inquire.lucene.LuceneResult;
import edu.utdallas.hltri.logging.Logger;

/**
 *
 * @author travis
 */
public class AssertionScorer extends VisitScorer {

  private static final Logger log = Logger.get(AssertionScorer.class);
  public static final Set<String> CONCEPT_TYPES = Sets.newHashSet("PROBLEM", "TREATMENT", "TEST");
  public static final int weight = Config.get(AssertionScorer.class, "WEIGHT").toInteger();

  private final ConcurrentMap<String, Multiset<String>> keywordCounts = new ConcurrentHashMap<>();
  private final Multiset<String>                        keywordTotals = ConcurrentHashMultiset.create();

  private LexiconMatcher keywordLexicon;

  @Override
  public void setTopic(Topic topic) {
    super.setTopic(topic);
    keywordLexicon = new QueryLexiconAdapter().adapt(topic);
  }

  @Override
  public void setVisit(LuceneResult<Visit> visit) {
    super.setVisit(visit);
    keywordCounts.clear();
    keywordTotals.clear();
  }

  @Override
  public double getRank() {
    double negations = 0.0;
    for (Keyword keyword : topic) {
      if (!keyword.isRequired()) { continue; }

      final String term = keyword.asString();
      if (keywordCounts.containsKey(term)) {
        for (Entry<String> entry : keywordCounts.get(term).entrySet()) {
          switch (entry.getElement()) {
            case "ABSENT": negations += 1; break;
            case "ASSOCIATED_WITH_SOMEONE_ELSE": negations += 1; break;
            case "CONDITIONAL": negations += .333; break;
            case "CONDUCTED": break;
            case "HISTORICAL": negations += 1/2 ; break;
            case "HYPOTHETICAL": negations += .333; break;
            case "ONGOING": break;
            case "ORDERED": break;
            case "POSSIBLE": negations += .333; break;
            case "PRESCRIBED": break;
            case "PRESENT": break;
            case "SUGGESTED": negations += .333;
          }
        }
      }
    }

    double score = 0;
    for (Keyword keyword : topic) {
      if (!keyword.isRequired()) { continue; }
      final String term = keyword.asString();

      if (keyword.isNegation() && keywordTotals.count(term) > 3 * negations ||
         !keyword.isNegation() && keywordTotals.count(term) <= 3 * negations) {
        score -= weight;
      }
    }
    return score;
  }

  @Override
  public void setReport(final Report report) {
    final Document document = report.getDocument();
    for (LexiconItem keywordMatch : keywordLexicon.find(document)) {
      keywordTotals.add(keywordMatch.getCategory());
        for (MedicalConcept concept : keywordMatch.getIntersecting(MedicalConcept.class)) {
          if (CONCEPT_TYPES.contains(concept.getType())) {
            keywordCounts.putIfAbsent(keywordMatch.getCategory(), HashMultiset.<String>create());
            keywordCounts.get(keywordMatch.getCategory()).add(concept.getAssertionType());
          }
        }
      }
  }
}
