package edu.utdallas.hltri.ml;

/**
 * The results of an evaluation on instances of type T
 * User: bryan
 * Date: 5/13/13
 * Time: 4:22 PM
 * Created with IntelliJ IDEA.
 */
public interface EvaluationResult<T> {
  /** The single overarching number to use for things like parameter optimization */
  public double getOverallScore();

  /** Returns all of the instances which were "correct" such as true positives and true negatives */
  public Iterable<T> correctInstances();

  /** Returns all of the instances which were "incorrect" such as false positives and false negatives */
  public Iterable<T> incorrectInstances();
}
