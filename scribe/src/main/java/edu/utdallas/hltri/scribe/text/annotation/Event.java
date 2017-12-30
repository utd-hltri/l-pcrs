package edu.utdallas.hltri.scribe.text.annotation;

import edu.utdallas.hltri.scribe.text.Attribute;
import edu.utdallas.hltri.scribe.text.Document;
import edu.utdallas.hltri.scribe.text.annotation.attributes.HasModality;
import edu.utdallas.hltri.scribe.text.annotation.attributes.HasPolarity;

/**
 * Created by rmm120030 on 7/24/15.
 */
public class Event extends AbstractAnnotation<Event> implements HasPolarity, HasModality {
  private final static long serialVersionUID = 1l;

  public static final Attribute<Event, String> type = Attribute.typed("type", String.class);
  public static final Attribute<Event, String> polarity = Attribute.typed("polarity", String.class);
  public static final Attribute<Event, String> modality = Attribute.typed("modality", String.class);

  protected Event(Document<?> parent, gate.Annotation ann) {
    super(parent, ann);
  }

  public static final AnnotationType<Event> TYPE = new AbstractAnnotationType<Event>("Event") {
    @Override public Event wrap(Document<?> parent, gate.Annotation ann) {
      return new Event(parent, ann);
    }
  };
}
