
package edu.utdallas.hlt.dataset.googngrams;

/**
 *
 * @author bryan
 */
public interface NGramVisitor {

  public void visit(String[] ngram, String count);
}
