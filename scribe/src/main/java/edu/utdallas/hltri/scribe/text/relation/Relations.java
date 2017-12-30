//package edu.utdallas.hltri.scribe.text.relation;
//
//import edu.utdallas.hltri.scribe.text.Document;
//import edu.utdallas.hltri.scribe.text.annotation.Annotation;
//import edu.utdallas.hltri.scribe.text.annotation.AnnotationType;
//import edu.utdallas.hltri.scribe.text.relation.RelationImpl;
//
//import java.util.Arrays;
//
///**
// * Created by travis on 8/14/14.
// */
//public class Relations {
//  public static <S extends Annotation> RelationImpl<S> attach(Document parent, String annotationSet, String name, AnnotationType<S> type, S... members) {
//    final int[] memberIds = new int[members.length];
//    for (int i = 0; i < memberIds.length; i++) {
//      memberIds[i] = members[i].asGate().getId();
//    }
//
//    final gate.relations.Relation gateRelation = parent.asGate().getAnnotations(annotationSet).getUnsafeRelations().addRelation(name, memberIds);
//
//    return new RelationImpl<S>(parent, gateRelation, Arrays.asList(members));
//  }
//}
