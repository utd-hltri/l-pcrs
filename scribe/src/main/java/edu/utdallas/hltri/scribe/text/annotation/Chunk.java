package edu.utdallas.hltri.scribe.text.annotation;

import edu.utdallas.hltri.scribe.text.Attribute;
import edu.utdallas.hltri.scribe.text.Document;

/**
 * Created by travis on 7/15/14.
 */
public class Chunk extends AbstractAnnotation<Chunk> {
  private static final long serialVersionUID = 1L;

  protected Chunk(Document<?> parent, gate.Annotation ann) {
    super(parent, ann);
  }

  public static final Attribute<Chunk, Integer> headTokenId = Attribute.typed("head-token-id", Integer.class);

  public static final AnnotationType<Chunk> TYPE = new AbstractAnnotationType<Chunk>("Chunk") {
    @Override public Chunk wrap(Document<?> parent, gate.Annotation ann) {
      return new Chunk(parent, ann);
    }
  };
}
