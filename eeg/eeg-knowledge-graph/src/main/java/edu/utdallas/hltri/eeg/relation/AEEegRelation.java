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
public class AEEegRelation extends AbstractRelation<AEEegRelation, EegActivity, Event> implements EegRelation{
  public AEEegRelation(Document<?> document, Relation gateRelation, String annotationSet) {
    super(document, gateRelation, annotationSet);
  }

  @Override
  public RelationType<AEEegRelation, EegActivity, Event> relationType() {
    return TYPE;
  }

  public static RelationType<AEEegRelation, EegActivity, Event> TYPE = new AbstractRelationType<AEEegRelation, EegActivity, Event>("AeEegRelation") {
    @Override
    public AnnotationType<Event> dependantType() {
      return Event.TYPE;
    }

    @Override
    public AnnotationType<EegActivity> governorType() {
      return EegActivity.TYPE;
    }

    @Override
    public AEEegRelation wrap(Document<?> parent, gate.relations.Relation relation, String annSet) {
      return new AEEegRelation(parent, relation, annSet);
    }
  };

  @Override
  public String getRelationType() {
    return get(EegRelation.type);
  }
}
