package edu.utdallas.hltri.scribe.text.relation;

import edu.utdallas.hltri.logging.Logger;
import edu.utdallas.hltri.scribe.text.Document;
import edu.utdallas.hltri.scribe.text.annotation.Annotation;
import edu.utdallas.hltri.scribe.text.annotation.AnnotationType;
import gate.relations.RelationSet;

/**
 * Created by rmm120030 on 7/15/15.
 */
public interface RelationType<R extends Relation<R,G,D>, G extends Annotation<G>, D extends Annotation<D>> {
  static final Logger log = Logger.get(RelationType.class);

  /**
   * Get the unique, textual name of this Relation type
   * (Used internally as an identifier which should uniquely identify this class of relation)
   * @return String unique name of this RelationType
   */
  String getName();
  AnnotationType<G> governorType();
  AnnotationType<D> dependantType();

  /**
   * EXPERT: Constructs a new Relation of type R which wraps the given gate Relation on the given parent Document
   * @param parent      Document to attach the relation to
   * @param relation  gate.relations.Relation to wrap
   * @param annotationSet annotation set of the relation
   * @return a Relation corresponding to this RelationtionType
   * @see edu.utdallas.hltri.scribe.text.relation.Relation
   */
  R wrap(Document<?> parent, gate.relations.Relation relation, String annotationSet);

  default R create(String annotationSet, G governor, D dependant) {
    assert governor.getDocument() == dependant.getDocument() :
      String.format("Governor and Dependant do not have matching documents. Gov doc: %s, Dep doc: %s",
          governor.getDocument(), dependant.getDocument());
    final RelationSet set = governor.getDocument().asGate().getAnnotations(annotationSet).getRelations();
    log.trace("Creating relation from ann {} -> ann {} of type {} in annset {}", governor.getGateId(), dependant.getGateId(),
        getName(), annotationSet);
    final gate.relations.Relation gateRelation = set.addRelation(getName(), governor.getGateId(), dependant.getGateId());
    governor.getDocument().setDirty();
    return wrap(governor.getDocument(), gateRelation, annotationSet);
  }
}
