package edu.utdallas.hltri.eeg.annotation;

import edu.utdallas.hltri.scribe.text.Document;
import edu.utdallas.hltri.scribe.text.annotation.AbstractAnnotation;
import edu.utdallas.hltri.scribe.text.annotation.AbstractAnnotationType;
import edu.utdallas.hltri.scribe.text.annotation.AnnotationType;

/**
 * Created by rmm120030 on 9/8/15.
 */
public class EegLocation extends AbstractAnnotation<EegLocation> {
  private static final long serialVersionUID = 1L;

  protected EegLocation(final Document<?> parent, final gate.Annotation ann) {
    super(parent, ann);
  }

  public static final AnnotationType<EegLocation> TYPE = new AbstractAnnotationType<EegLocation>("EegLocation") {
    @Override public EegLocation wrap(Document<?> parent, gate.Annotation ann) {
      return new EegLocation(parent, ann);
    }
  };
}