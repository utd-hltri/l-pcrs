//package edu.utdallas.hltri.eeg.relation;
//
//import edu.utdallas.hltri.scribe.text.Document;
//import edu.utdallas.hltri.scribe.text.annotation.AnnotationType;
//import edu.utdallas.hltri.scribe.text.annotation.Event;
//import edu.utdallas.hltri.scribe.text.relation.AbstractRelation;
//import edu.utdallas.hltri.scribe.text.relation.AbstractRelationType;
//import edu.utdallas.hltri.scribe.text.relation.RelationType;
//
///**
// * Created by ramon on 2/6/16.
// */
//public class EventCoreference extends AbstractRelation<EventCoreference, Event, Event> {
//
//  public EventCoreference(final Document<?> document, final gate.relations.Relation relation, final String annset) {
//    super(document, relation, annset);
//  }
//
//  @Override
//  public RelationType<EventCoreference, Event, Event> relationType() {
//    return TYPE;
//  }
//
//  public static RelationType<EventCoreference, Event, Event> TYPE =
//      new AbstractRelationType<EventCoreference, Event, Event>("Dependency") {
//    @Override
//    public AnnotationType<Event> governorType() {
//      return Event.TYPE;
//    }
//
//    @Override
//    public AnnotationType<Event> dependantType() {
//      return Event.TYPE;
//    }
//
//    @Override
//    public EventCoreference wrap(Document<?> parent, gate.relations.Relation relation, String annSet) {
//      return new EventCoreference(parent, relation, annSet);
//    }
//  };
//}
