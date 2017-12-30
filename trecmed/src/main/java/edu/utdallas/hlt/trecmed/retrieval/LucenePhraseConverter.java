/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.utdallas.hlt.trecmed.retrieval;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.Query;

/**
 *
 * @author travis
 */
public interface LucenePhraseConverter<T extends Query> {
  T convert(String phrase, double boost, Term field);
}
