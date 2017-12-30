package edu.utdallas.hltri.scribe.text.annotation;

import edu.utdallas.hltri.scribe.text.Document;
import edu.utdallas.hltri.scribe.text.annotation.attributes.HasAssertion;
import edu.utdallas.hltri.scribe.text.annotation.attributes.HasType;

/**
 * Created by travis on 7/16/14.
 */
public class MedicalConcept extends AbstractAnnotation<MedicalConcept> implements HasAssertion,
                                                                                  HasType {
  private static final long serialVersionUID = 1L;

  public MedicalConcept(Document<?> document, gate.Annotation gateAnnotation) {
    super(document, gateAnnotation);
  }

  public static AnnotationType<MedicalConcept> from2010 = new AbstractAnnotationType<MedicalConcept>("i2b2_2010_concept") {
    @Override public MedicalConcept wrap(Document<?> parent, gate.Annotation annotation) {
      return new MedicalConcept(parent, annotation);
    }
  };

  public static AnnotationType<MedicalConcept> from2011 = new AbstractAnnotationType<MedicalConcept>("i2b2_2011_concept") {
    @Override public MedicalConcept wrap(Document<?> parent, gate.Annotation annotation) {
      return new MedicalConcept(parent, annotation);
    }
  };

  public static AnnotationType<MedicalConcept> medtagger = new AbstractAnnotationType<MedicalConcept>("medtagger_concept") {
    @Override public MedicalConcept wrap(Document<?> parent, gate.Annotation annotation) {
      return new MedicalConcept(parent, annotation);
    }
  };
}
