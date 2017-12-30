package edu.utdallas.hltri.scribe.text.annotation;

import edu.utdallas.hltri.scribe.text.Document;

/**
 * Created by travis on 8/5/16.
 */
public class NegationSpan extends AbstractAnnotation<NegationSpan> {
  private final static long                     serialVersionUID = 1l;


  protected NegationSpan(Document<?> parent, gate.Annotation ann) {
    super(parent, ann);
  }

  public static final AnnotationType<NegationSpan> TYPE = new AbstractAnnotationType<NegationSpan>("negation-span") {
    @Override public NegationSpan wrap(Document<?> parent, gate.Annotation ann) {
      return new NegationSpan(parent, ann);
    }
  };
}
