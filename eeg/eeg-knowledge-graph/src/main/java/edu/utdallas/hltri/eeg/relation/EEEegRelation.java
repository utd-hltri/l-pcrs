package edu.utdallas.hltri.eeg.relation;

import edu.utdallas.hltri.scribe.text.Document;
import edu.utdallas.hltri.scribe.text.annotation.AnnotationType;
import edu.utdallas.hltri.scribe.text.annotation.Event;
import edu.utdallas.hltri.scribe.text.relation.AbstractRelation;
import edu.utdallas.hltri.scribe.text.relation.AbstractRelationType;
import edu.utdallas.hltri.scribe.text.relation.RelationType;
import gate.relations.Relation;

/**
 * Created by rmm120030 on 9/25/17.
 */
public class EEEegRelation extends AbstractRelation<EEEegRelation, Event, Event> implements EegRelation{
  public EEEegRelation(Document<?> document, Relation gateRelation, String annotationSet) {
    super(document, gateRelation, annotationSet);
  }

  @Override
  public RelationType<EEEegRelation, Event, Event> relationType() {
    return TYPE;
  }

  public static RelationType<EEEegRelation, Event, Event> TYPE = new AbstractRelationType<EEEegRelation, Event, Event>("EeEegRelation") {
    @Override
    public AnnotationType<Event> dependantType() {
      return Event.TYPE;
    }

    @Override
    public AnnotationType<Event> governorType() {
      return Event.TYPE;
    }

    @Override
    public EEEegRelation wrap(Document<?> parent, gate.relations.Relation relation, String annSet) {
      return new EEEegRelation(parent, relation, annSet);
    }
  };

  @Override
  public String getRelationType() {
    return get(EegRelation.type);
  }
}
