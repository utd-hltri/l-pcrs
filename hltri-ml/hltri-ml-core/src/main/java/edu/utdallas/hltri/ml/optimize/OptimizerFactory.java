package edu.utdallas.hltri.ml.optimize;

import cc.mallet.optimize.*;

/**
 * User: bryan
 * Date: 12/20/12
 * Time: 10:35 AM
 * Created with IntelliJ IDEA.
 */
public interface OptimizerFactory {
  public Optimizer createOptimizer(Optimizable.ByGradientValue optimizable);
}
