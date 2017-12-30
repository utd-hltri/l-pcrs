package edu.utdallas.hltri.scribe.text;

/**
 * Created by travis on 8/13/14.
 */
public class DuplicateAnnotationException extends RuntimeException {
  private static final long serialVersionUID = 1l;

  public final gate.Annotation old;

  public DuplicateAnnotationException(gate.Annotation old) {
    super();
    this.old = old;
  }
}
