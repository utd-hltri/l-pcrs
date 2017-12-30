package edu.utdallas.hltri.eeg.annotation;

import edu.utdallas.hltri.scribe.text.Attribute;
import edu.utdallas.hltri.scribe.text.Document;
import edu.utdallas.hltri.scribe.text.annotation.AbstractAnnotation;
import edu.utdallas.hltri.scribe.text.annotation.AbstractAnnotationType;
import edu.utdallas.hltri.scribe.text.annotation.AnnotationType;

import java.util.List;

/**
 * Created by rmm120030 on 9/24/15.
 */
public class CtakesUmlsMention extends AbstractAnnotation<CtakesUmlsMention> {
  private static final long serialVersionUID = 1L;

  //TODO: make separate annotations for AnatomicalSite, Medication, DiseaseDisorder, SignSymptom, and Procedure instead of using the type attr

  public static final Attribute<CtakesUmlsMention, List<UmlsConcept>> concepts = Attribute.inferred("concepts");
  public static final Attribute<CtakesUmlsMention, String> type = Attribute.typed("type", String.class);
//  public static final Attribute<CtakesUmlsMention, Boolean> polarity = Attribute.typed("polarity", Boolean.class);
//  public static final Attribute<CtakesUmlsMention, String> conditional = Attribute.typed("conditional", String.class);
//  public static final Attribute<CtakesUmlsMention, String> conditional = Attribute.typed("conditional", String.class);

  protected CtakesUmlsMention(final Document<?> parent, final gate.Annotation ann) {
    super(parent, ann);
  }

  public static final AnnotationType<CtakesUmlsMention> TYPE = new AbstractAnnotationType<CtakesUmlsMention>("UmlsMention") {
    @Override public CtakesUmlsMention wrap(Document<?> parent, gate.Annotation ann) {
      return new CtakesUmlsMention(parent, ann);
    }
  };
}
