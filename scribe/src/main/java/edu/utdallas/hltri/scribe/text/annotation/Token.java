package edu.utdallas.hltri.scribe.text.annotation;

import java.util.Optional;

import edu.utdallas.hltri.scribe.text.Attribute;
import edu.utdallas.hltri.scribe.text.Document;
import gate.*;

/**
 *
 */
public class Token extends AbstractAnnotation<Token> {
  private static final long serialVersionUID = 1L;

  protected Token(Document<?> parent, gate.Annotation ann) {
    super(parent, ann);
  }

  public static final Attribute<Token, String> PoS   = Attribute.typed("kind", String.class);
  public static final Attribute<Token, String> Lemma = Attribute.typed("lemma", String.class);

  @Deprecated
  public Token setPOS(String partOfSpeech) {
    set(PoS, partOfSpeech);
    return this;
  }

  @Deprecated
  public Token setLemma(String lemma) {
    set(Lemma, lemma);
    return this;
  }

  @Deprecated
  public Optional<String> getPOS() {
    return getOptional(PoS);
  }

  @Deprecated
  public Optional<String> getLemma() {
    return getOptional(Lemma);
  }

  public static final AnnotationType<Token> TYPE = new AbstractAnnotationType<Token>("Token") {
    @Override public Token wrap(Document<?> parent, gate.Annotation ann) {
      return new Token(parent, ann);
    }
  };
}
