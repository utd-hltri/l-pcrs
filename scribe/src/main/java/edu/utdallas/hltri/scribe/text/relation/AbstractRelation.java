package edu.utdallas.hltri.scribe.text.relation;

import edu.utdallas.hltri.logging.Logger;
import edu.utdallas.hltri.scribe.text.Document;
import edu.utdallas.hltri.scribe.text.annotation.Annotation;

import java.util.Map;
import java.util.Objects;

/**
 * Created by ramon on 5/18/16.
 */
public abstract class AbstractRelation<R extends Relation<R,G,D>, G extends Annotation<G>, D extends Annotation<D>>
    implements Relation<R, G, D> {
  private static final Logger log = Logger.get(Relation.class);

  protected final Document<?> document;
  protected final gate.relations.Relation gateRelation;
  protected final String annotationSet;

  public abstract RelationType<R,G,D> relationType();

  public AbstractRelation(final Document<?> document, gate.relations.Relation gateRelation, final String annotationSet) {
    this.document = document;
    this.gateRelation = gateRelation;
    this.annotationSet = annotationSet;
  }

  /**
   * Get the Document this Annotation is attached to
   *
   * @return Document this Annotation was created on
   */
  public Document<?> getDocument() {
    return document;
  }

//  /**
//   * Get the FeatureMap associ8ed with this relation
//   *
//   * @return FeatureMap associ8ed with this relation
//   */
//  Map<String, Object> getFeatures();

  public gate.relations.Relation asGate() {
    return gateRelation;
  }

  /**
   * Get the type of this Relation
   *
   * @return type of this Relation
   */
  public String getType() {
    return asGate().getType();
  }

  /**
   * Get the dependant of this relation
   *
   * @return dependant of this relation
   */
  @Override
  public G getGovernor() {
    return relationType().governorType().wrap(getDocument(), getDocument().asGate().getAnnotations(annotationSet)
        .get(gateRelation.getMembers()[0]));
  }

  /**
   * Get the dependant of this relation
   *
   * @return dependant of this relation
   */
  @Override
  public D getDependant() {
    return relationType().dependantType().wrap(getDocument(), getDocument().asGate().getAnnotations(annotationSet)
        .get(gateRelation.getMembers()[1]));
  }

  @Override
  public String toString() {
    return getType() + "(" + getGovernor() + ":" + getGovernor().getId() + ", " + getDependant() + ":"
        + getDependant().getId() + ")";
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    AbstractRelation<?, ?, ?> relation = (AbstractRelation<?, ?, ?>) o;
    return Objects.equals(document, relation.document) &&
        Objects.equals(getType(), relation.getType()) &&
        Objects.equals(gateRelation, relation.gateRelation) &&
        Objects.equals(annotationSet, relation.annotationSet);
  }

  @Override
  public int hashCode() {
    return Objects.hash(document, getType(), gateRelation, annotationSet);
  }

  @Override
  public String describe() {
    final StringBuilder sb = new StringBuilder();
    sb.append(getType())
        .append(" for (|")
        .append(getGovernor().getId()).append(":").append(getGovernor())
        .append("|, |")
        .append(getDependant().getId()).append(":").append(getDependant())
        .append("|):");
    for (final Map.Entry<Object, Object> entry : gateRelation.getFeatures().entrySet()) {
      sb.append(' ').append(entry.getKey()).append(": ").append(entry.getValue());
    }
    return sb.toString();
  }
}
