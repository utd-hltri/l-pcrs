package edu.utdallas.hltri.combinatorics;

import edu.utdallas.hltri.logging.Logger;
import edu.utdallas.hltri.math.FisherExact;

/**
 *
 * @author travis
 */
public class FisherExactMeasure implements CooccurrenceMeasure {

  private static Logger log = Logger.get(FisherExactMeasure.class);
  private static FisherExact fisher;

  private FisherExact getFisher(int N) {
    if (fisher == null) {
      fisher = new FisherExact(N);
    }
    return fisher;
  }

  @Override public double measure(long x, long y, long xy, long N) {
    int a = (int) xy;
    int b = (int) (y - a);
    int c = (int) (x - a);
    int d = (int) (N - a - b - c);
    int n = (int) N;

    try {
      return getFisher(n).getLogP(a, b, c, d);
    }
    catch (ArrayIndexOutOfBoundsException ex) {
      log.error("Unable to get score", ex);
      log.error("A={}, B={}, C={}, D={}, N={}", new Object[]{a, b, c, d, n});
      throw ex;
    }
  }
}
