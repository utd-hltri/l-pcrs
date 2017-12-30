/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.utdallas.hlt.trecmed.retrieval;

import com.google.common.collect.Iterables;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.spans.SpanBoostQuery;
import org.apache.lucene.search.spans.SpanNearQuery;
import org.apache.lucene.search.spans.SpanQuery;
import org.apache.lucene.search.spans.SpanTermQuery;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author travis
 */
public class LuceneSpanPhraseConverter implements LucenePhraseConverter<SpanQuery> {
  @Override public SpanQuery convert(String phrase, double boost, Term field) {
    final List<SpanQuery> clauses = new ArrayList<>();
    final List<String> tokens = LuceneEMRSearcher.getVisitSearcher().tokenize(field.field(), phrase);
    for (final String token : tokens) {
      SpanTermQuery term = new SpanTermQuery(new Term(field.field(), token));
      clauses.add(new SpanBoostQuery(term, (float) boost));
    }

    if (clauses.size() > 1)
      return new SpanNearQuery(Iterables.toArray(clauses, SpanQuery.class), 3, false);
    else
      return clauses.get(0);
  }
}
