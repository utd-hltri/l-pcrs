/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.utdallas.hlt.trecmed.retrieval;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.BoostQuery;
import org.apache.lucene.search.PhraseQuery;

import java.util.List;

/**
 *
 * @author travis
 */
public class LucenePhrasePhraseConverter implements LucenePhraseConverter<BoostQuery>{
  @Override public BoostQuery convert(String phrase, double boost, Term field) {
    PhraseQuery.Builder query = new PhraseQuery.Builder();
    final List<String> tokens = LuceneEMRSearcher.getVisitSearcher().tokenize(field.field(), phrase);
    for (final String token : tokens) {
      query.add(new Term(field.field(), token));
    }
    query.setSlop(1);
    return new BoostQuery(query.build(), (float) boost);
  }
}
