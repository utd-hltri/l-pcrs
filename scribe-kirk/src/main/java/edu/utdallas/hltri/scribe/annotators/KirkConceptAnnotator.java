package edu.utdallas.hltri.scribe.annotators;


import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;
import edu.utdallas.hlt.genia_wrapper.GeniaToken;
import edu.utdallas.hlt.i2b2.Concept;
import edu.utdallas.hlt.i2b2.I2B2AndODIEConceptAnnotator;
import edu.utdallas.hlt.text.Text;
import edu.utdallas.hltri.logging.Logger;
import edu.utdallas.hltri.scribe.gate.GateUtils;
import edu.utdallas.hltri.scribe.text.BaseDocument;
import edu.utdallas.hltri.scribe.text.Document;
import edu.utdallas.hltri.scribe.text.DuplicateAnnotationException;
import edu.utdallas.hltri.scribe.text.annotation.Annotation;
import edu.utdallas.hltri.scribe.text.annotation.MedicalConcept;
import edu.utdallas.hltri.scribe.text.annotation.Sentence;
import edu.utdallas.hltri.scribe.text.annotation.Token;
import edu.utdallas.hltri.scribe.text.annotation.attributes.HasAssertion;
import edu.utdallas.hltri.scribe.text.annotation.attributes.HasType;

import java.util.Optional;
import java.util.Set;

/**
 * Created by rmm120030 on 6/24/15.
 */
public class KirkConceptAnnotator<D extends BaseDocument> extends KirkAnnotator<D> {

  public enum ConceptType { i2010, i2011, BOTH }

  private static final Logger log = Logger.get(KirkConceptAnnotator.class);
  private static final Set<String> ODIE_CONCEPTS = Sets.newHashSet("ANATOMICAL_SITE", "DISEASE_OR_SYNDROME",
      "INDICATOR_REAGENT_DIAGNOSTIC_AID", "LABORATORY_OR_TEST_RESULT", "ORGAN_OR_TISSUE_FUNCTION", "OTHER",
      "PEOPLE", "PROCEDURE", "SIGN_OR_SYMPTOM", "NONE");
  private static final String ANNOTATION_SET_NAME = "i2b2";

  private final ConceptType conceptType;
  private final boolean doAssertions;

  public static class Builder<D extends BaseDocument> extends KirkAnnotator.Builder<D,Builder<D>> {
    private ConceptType conceptType = ConceptType.BOTH;
    private boolean doAssertions = true;

    public Builder() {}

    @Override
    public Builder<D> self() {
      return this;
    }

    public Builder<D> conceptType(ConceptType type) {
      this.conceptType = type;
      return self();
    }

    public Builder<D> doAssertions(boolean b) {
      this.doAssertions = b;
      return self();
    }

    @Override
    public KirkConceptAnnotator<D> build() {
      if (tokenProvider == null) {
        log.info("No token function provided, defaulting to genia.");
        useTokens(a -> a.getContained("genia", Token.TYPE));
      }
      if (sentenceProvider == null) {
        log.info("No sentence function provided, defaulting to genia.");
        useSentences(d -> d.get("genia", Sentence.TYPE));
      }

      return new KirkConceptAnnotator<>(self());
    }
  }

  protected KirkConceptAnnotator(Builder<D> builder) {
    super(builder);
    try {
      edu.utdallas.hlt.util.Config.init("kirk");
      GateUtils.init();

      conceptType  = builder.conceptType;
      doAssertions = builder.doAssertions;

      annotator = new I2B2AndODIEConceptAnnotator();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Annotates I2B2 concepts and assertions according using Kirk's classifier.
   * - can include/exclude 2010 concepts, 2011 concepts, and assertions depending on how the annotator was built.
   * @param document the document to be annotated
   */
  @Override
  public <B extends D> void annotate(Document<B> document) {
    if (document.getUnsafeAnnotations(ANNOTATION_SET_NAME).isEmpty()) {
      edu.utdallas.hlt.text.Document kirkDoc = new edu.utdallas.hlt.text.Document(document.asString());

      edu.utdallas.hlt.text.Token first = null;
      edu.utdallas.hlt.text.Token last = null;
      for (Sentence sentence : sentenceProvider.apply(document)) {
        final Iterable<Token> tokens = tokenProvider.apply(sentence);
        if (Iterables.size(tokens) > 30) { continue; }
        for (Token token : tokens) {
          edu.utdallas.hlt.text.Token kirkToken = new edu.utdallas.hlt.text.Token(kirkDoc,
              token.get(Annotation.StartOffset).intValue(),
              token.get(Annotation.EndOffset).intValue());
          kirkToken.setPOS(token.get(Token.PoS));
          kirkToken.setStem(token.get(Token.Lemma));
          kirkDoc.addToken(kirkToken);

          // add genia token
          GeniaToken genToken = new GeniaToken(kirkToken, token.get(Token.Lemma), token.get(Token.PoS),
              token.get(GeniaAnnotator.phraseIOB), token.get(GeniaAnnotator.entityIOB));
          kirkDoc.addAnnotation(genToken);

          // save the first and last kirkToken of this sentence
          if (first == null) {
            first = kirkToken;
          }
          last = kirkToken;
        }
        // add kirk sentence
        kirkDoc.addAnnotation(new edu.utdallas.hlt.text.Sentence(Text.create(first, last)));
        first = null;
      }

      kirkDoc.addAnnotatedType(edu.utdallas.hlt.text.Token.TYPE);
      kirkDoc.addAnnotatedType(edu.utdallas.hlt.text.Token.POS_TYPE);
      kirkDoc.addAnnotatedType(edu.utdallas.hlt.text.Token.STEM_TYPE);
      kirkDoc.addAnnotatedType(edu.utdallas.hlt.genia_wrapper.GeniaToken.TYPE);
      kirkDoc.addAnnotatedType(edu.utdallas.hlt.text.Sentence.TYPE);

      log.info("Annotating medical concepts...");
      if (conceptType != ConceptType.i2010) {
        ((I2B2AndODIEConceptAnnotator) annotator).annotateODIE(kirkDoc);
      } else {
        ((I2B2AndODIEConceptAnnotator) annotator).annotateI2B2(kirkDoc);
      }

      log.info("Loading medical concepts into gate...");
      for (edu.utdallas.hlt.i2b2.Concept kirkConcept :
          kirkDoc.getAnnotations(edu.utdallas.hlt.i2b2.Concept.class)) {
        if (kirkConcept.getType() == null ) { continue; }
        try {
          MedicalConcept concept = null;

          // if an ODIE concept
          if (kirkConcept.getType().isODIE()) {
            // if we want ODIE concepts
            if (conceptType != ConceptType.i2010) {
              concept = MedicalConcept.from2011.create(document, ANNOTATION_SET_NAME,
                  kirkConcept.getStartCharOffset(), kirkConcept.getEndCharOffset());
            }
          } else {
            // if we want 2010 concepts
            if (conceptType != ConceptType.i2011) {
              concept = MedicalConcept.from2010.create(document, ANNOTATION_SET_NAME,
                  kirkConcept.getStartCharOffset(), kirkConcept.getEndCharOffset());
            }
          }
          if (concept != null) {
            concept.set(HasType.TYPE, kirkConcept.getType().toString());
            if (doAssertions) {
              concept.set(HasAssertion.ASSERTION, Optional.ofNullable(kirkConcept.getAssertionType()).orElse(Concept.AssertionType.NONE).toString());
            }
          }
        } catch (DuplicateAnnotationException e) {
          log.warn("Duplicate annotation: {}", kirkConcept);
        }
      }
    }
  }

  @Override
  public void close() {}
}

