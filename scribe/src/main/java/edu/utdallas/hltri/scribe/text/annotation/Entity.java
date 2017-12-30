package edu.utdallas.hltri.scribe.text.annotation;

import edu.utdallas.hltri.scribe.text.Attribute;
import edu.utdallas.hltri.scribe.text.Document;
import edu.utdallas.hltri.scribe.text.annotation.attributes.HasType;

/**
 * Created by travis on 7/15/14.
 */
public final class Entity extends AbstractAnnotation<Entity> implements HasType {
  private static final long serialVersionUID = 1L;

  protected Entity(Document<?> parent, gate.Annotation ann) {
    super(parent, ann);
  }

  public static final Attribute<HasType, String> type = Attribute.typed("type", String.class);

  @Deprecated
  public Entity setType(String entity) {
    set(type, entity);
    return this;
  }

  @Deprecated
  public String getType() {
    return get(type);
  }

  public static final AnnotationType<Entity> TYPE = new AbstractAnnotationType<Entity>("NamedEntity") {
    @Override public Entity wrap(Document<?> parent, gate.Annotation ann) {
      return new Entity(parent, ann);
    }
  };
}
