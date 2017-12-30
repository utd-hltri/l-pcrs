package edu.utdallas.hltri.eeg.relation;

import edu.utdallas.hltri.eeg.annotation.EegActivity;
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
public class EAEegRelation extends AbstractRelation<EAEegRelation, Event, EegActivity> implements EegRelation{
  public EAEegRelation(Document<?> document, Relation gateRelation, String annotationSet) {
    super(document, gateRelation, annotationSet);
  }

  @Override
  public RelationType<EAEegRelation, Event, EegActivity> relationType() {
    return TYPE;
  }

  public static RelationType<EAEegRelation, Event, EegActivity> TYPE = new AbstractRelationType<EAEegRelation, Event, EegActivity>("EaEegRelation") {
    @Override
    public AnnotationType<Event> governorType() {
      return Event.TYPE;
    }

    @Override
    public AnnotationType<EegActivity> dependantType() {
      return EegActivity.TYPE;
    }

    @Override
    public EAEegRelation wrap(Document<?> parent, gate.relations.Relation relation, String annSet) {
      return new EAEegRelation(parent, relation, annSet);
    }
  };

  @Override
  public String getRelationType() {
    return get(EegRelation.type);
  }
}
