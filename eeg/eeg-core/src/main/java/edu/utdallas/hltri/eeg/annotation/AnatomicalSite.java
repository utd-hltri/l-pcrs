package edu.utdallas.hltri.eeg.annotation;

import edu.utdallas.hltri.scribe.text.Document;
import edu.utdallas.hltri.scribe.text.annotation.AbstractAnnotation;
import edu.utdallas.hltri.scribe.text.annotation.AbstractAnnotationType;
import edu.utdallas.hltri.scribe.text.annotation.AnnotationType;

/**
 * Created by rmm120030 on 9/8/15.
 */
public class AnatomicalSite extends AbstractAnnotation<AnatomicalSite> {
  private static final long serialVersionUID = 1L;

  protected AnatomicalSite(final Document<?> parent, final gate.Annotation ann) {
    super(parent, ann);
  }

  public static final AnnotationType<AnatomicalSite> TYPE = new AbstractAnnotationType<AnatomicalSite>("AnastomicalSite") {
    @Override public AnatomicalSite wrap(Document<?> parent, gate.Annotation ann) {
      return new AnatomicalSite(parent, ann);
    }
  };
}