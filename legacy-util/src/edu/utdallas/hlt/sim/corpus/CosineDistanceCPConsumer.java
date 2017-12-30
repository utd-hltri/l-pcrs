/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.utdallas.hlt.sim.corpus;


/**
 *
 * @author Bryan
 */
public class CosineDistanceCPConsumer implements ContextPairConsumer {
  double productSum = 0;
  double sum1 = 0;
  double sum2 = 0;
  

  public void visitContextPair(double count1, double count2) {
    assert ! Double.isNaN(count1);
    assert ! Double.isNaN(count2);
    //sum1 += count1*count1;
    sum2 += count2*count2;
    productSum += count1 * count2;
    assert ! Double.isNaN(productSum) : count1 + "   " + count2;
    assert ! Double.isInfinite(productSum) : count1 + "   " + count2;
  }
  
  public double getSimilarity() {
    if (productSum == 0) { return 0; }
    assert sum1 != 0;
    assert sum2 != 0;
    double result = productSum / (Math.sqrt(sum1) * Math.sqrt(sum2));
    assert ! Double.isNaN(result) : productSum + "  " + sum1 + "  " + sum2;
    return result;
  }
  
  public double getSimilarity(double delta) {
    if (productSum == 0) { return 0; }
    assert sum1 != 0;
    assert sum2 != 0;
    double result = productSum / (Math.sqrt(sum1-delta) * Math.sqrt(sum2));
    assert ! Double.isNaN(result) : productSum + "  " + sum1 + "  " + sum2;
    return result;
  }

  public void reset() {
    productSum = 0;
    sum2 = 0;
  }
  
  public void precomputeLeft(double entry) {
    sum1 += entry*entry;
  }
  
}
