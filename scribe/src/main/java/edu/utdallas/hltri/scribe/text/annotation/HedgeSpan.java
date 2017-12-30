package edu.utdallas.hltri.scribe.text.annotation;

import edu.utdallas.hltri.scribe.text.Document;

/**
 * Created by travis on 8/5/16.
 */
public class HedgeSpan extends AbstractAnnotation<HedgeSpan> {
  private final static long                     serialVersionUID = 1l;


  protected HedgeSpan(Document<?> parent, gate.Annotation ann) {
    super(parent, ann);
  }

  public static final AnnotationType<HedgeSpan> TYPE = new AbstractAnnotationType<HedgeSpan>("hedge-span") {
    @Override public HedgeSpan wrap(Document<?> parent, gate.Annotation ann) {
      return new HedgeSpan(parent, ann);
    }
  };
}
