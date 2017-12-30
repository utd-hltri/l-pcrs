package edu.utdallas.hltri.scribe.text.annotation;

import edu.utdallas.hltri.logging.Logger;
import edu.utdallas.hltri.scribe.text.Document;
import edu.utdallas.hltri.scribe.text.Identifiable;
import edu.utdallas.hltri.scribe.text.Text;
import edu.utdallas.hltri.util.Unsafe;

import java.io.Serializable;
import java.util.Map;
import java.util.Objects;

public abstract class AbstractAnnotation<S extends Annotation<S>> extends Text implements Annotation<S>, Serializable, Identifiable {
  private static final long serialVersionUID = 1l;
  private static final Logger log = Logger.get(AbstractAnnotation.class);

  protected final Document<?> document;
  protected final gate.Annotation annotation;
  protected final gate.FeatureMap features;

  protected AbstractAnnotation(Document<?> document, gate.Annotation gateAnnotation) {
    this.document = document;
    this.annotation = gateAnnotation;
    this.features = annotation.getFeatures();
  }

  public int getGateId() {
    return annotation.getId();
  }

  @Override
  public String asString() {
    return gate.Utils.stringFor(document.asGate(), annotation);
  }

  @Override
  public Document<?> getDocument() {
    return document;
  }

  @Override
  public gate.Annotation asGate() {
    return annotation;
  }

  @Override
  public String describe() {
    final StringBuilder sb = new StringBuilder();
    sb.append("Doc: ")
      .append(getDocument().getId())
      .append(": ")
      .append(annotation.getType())
      .append(" for ")
      .append(getId())
      .append(":|")
      .append(asString())
      .append("|@[")
      .append(get(StartOffset))
      .append(",")
      .append(get(EndOffset))
      .append(")");
    for (final Map.Entry<Object, Object> entry : features.entrySet()) {
      sb.append(' ').append(entry.getKey()).append(": ").append(entry.getValue());
    }
    return sb.toString();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    AbstractAnnotation<S> that = Unsafe.cast(o);

    if (!Objects.equals(this.getDocument(), that.getDocument())) return false;
    if (!Objects.equals(this.get(StartOffset), that.get(StartOffset))) return false;
    if (!Objects.equals(this.get(EndOffset), that.get(EndOffset))) return false;
    if (!Objects.equals(this.toString(), that.toString())) return false;

    return true;
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.getDocument(), this.get(StartOffset), this.get(EndOffset), this.toString());
  }

  @Override
  public String getId() {
    return annotation.getType() + getGateId();
  }
}
