/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.utdallas.hlt.sim.corpus;

/**
 *
 * @author Bryan
 */
public interface ContextPairConsumer {
  public void visitContextPair(double count1, double count2);
  
  public double getSimilarity();
  
  public void reset();
}
