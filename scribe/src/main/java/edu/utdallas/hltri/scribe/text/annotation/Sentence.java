package edu.utdallas.hltri.scribe.text.annotation;

import edu.utdallas.hltri.scribe.text.Document;
import edu.utdallas.hltri.scribe.text.Identifiable;

/**
 * Created by travis on 7/15/14.
 */
public class Sentence extends AbstractAnnotation<Sentence> implements Identifiable {
  private static final long serialVersionUID = 1L;

  public Sentence(Document<?> document, gate.Annotation annotation) {
    super(document, annotation);
  }


  public static final AnnotationType<Sentence> TYPE = new AbstractAnnotationType<Sentence>("Sentence") {
    @Override public Sentence wrap(Document<?> parent, gate.Annotation annotation) {
      return new Sentence(parent, annotation);
    }
  };
}
