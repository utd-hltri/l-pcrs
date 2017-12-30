package edu.utdallas.hltri.ml.eval;

import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * Created by rmm120030 on 6/23/16.
 */
public class Evaluator<A> {
  private final Predicate<A> tpTester;
  private final Predicate<A> fpTester;
  private final Predicate<A> fnTester;

  public Evaluator(Predicate<A> tpTester,
                   Predicate<A> fpTester,
                   Predicate<A> fnTester) {
    this.tpTester = tpTester;
    this.fpTester = fpTester;
    this.fnTester = fnTester;
  }

  public F1EvaluationResult<A> evaluate(Stream<A> stream) {
    final F1EvaluationResult<A> result = new F1EvaluationResult<>();
    stream.forEach(a -> evaluate(a, result));
    return result;
  }

  public void evaluate(A a, F1EvaluationResult<A> result) {
    if (tpTester.test(a)) {
      result.truePositive(a);
    } else if (fpTester.test(a)) {
      result.falsePositive(a);
    } else if (fnTester.test(a)) {
      result.falseNegative(a);
    }
  }
}
