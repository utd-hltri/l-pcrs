package edu.utdallas.hltri.scribe.text.annotation;

import edu.utdallas.hltri.scribe.text.Document;
import edu.utdallas.hltri.scribe.text.annotation.attributes.HasType;

/**
 * Created by travis on 7/15/14.
 */
public class PhraseChunk extends AbstractAnnotation<PhraseChunk> implements HasType {
  private static final long serialVersionUID = 1L;

  protected PhraseChunk(Document<?> parent, gate.Annotation ann) {
    super(parent, ann);
  }

  @Deprecated
  public PhraseChunk setType(String t) {
    set(type, t);
    return this;
  }

  @Deprecated
  public String getType() {
    return get(type);
  }

  public static final AnnotationType<PhraseChunk> TYPE = new AbstractAnnotationType<PhraseChunk>("Phrase_Chunk") {
    @Override public PhraseChunk wrap(Document<?> parent, gate.Annotation ann) {
      return new PhraseChunk(parent, ann);
    }
  };
}
