package edu.utdallas.hltri.ml;

/**
 * Created with IntelliJ IDEA.
 * User: bryan
 * Date: 12/18/12
 * Time: 8:50 AM
 */
public interface Evaluator<T> {
  public EvaluationResult<T> evaluate(Iterable<? extends T> instances);
}
