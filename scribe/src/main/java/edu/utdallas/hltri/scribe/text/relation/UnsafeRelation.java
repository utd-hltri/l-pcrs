package edu.utdallas.hltri.scribe.text.relation;

import com.google.common.collect.Lists;

import java.util.AbstractMap;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import edu.utdallas.hltri.Describable;
import edu.utdallas.hltri.logging.Logger;
import edu.utdallas.hltri.scribe.text.Document;
import edu.utdallas.hltri.scribe.text.HasFeatureMap;
import edu.utdallas.hltri.scribe.text.annotation.AnnotationType;
import edu.utdallas.hltri.scribe.text.annotation.UnsafeAnnotation;
import gate.FeatureMap;
import gate.relations.*;

/**
 * Created by rmm120030 on 7/14/15.
 */

//class UnsafeRelationType implements RelationType<UnsafeAnnotation, UnsafeAnnotation> {
//  @Override
//  public AnnotationType<UnsafeAnnotation> getGovernorType() {
//    return UnsafeAnnotation.TYPE;
//  }
//
//  @Override
//  public AnnotationType<UnsafeAnnotation> getDependantType() {
//    return UnsafeAnnotation.TYPE;
//  }
//
//  @Override
//  public String getName() {
//    throw new UnsupportedOperationException();
//  }
//}


public class UnsafeRelation extends AbstractRelation<UnsafeRelation, UnsafeAnnotation, UnsafeAnnotation> implements
                                                                                                 Iterable<Map.Entry<String, Object>>,
                                                                                                 HasFeatureMap,
                                                                                                 Describable {
  public static RelationType<UnsafeRelation, UnsafeAnnotation, UnsafeAnnotation> TYPE =
      new AbstractRelationType<UnsafeRelation, UnsafeAnnotation, UnsafeAnnotation>("Unsafe-Relation") {

        @Override
        public AnnotationType<UnsafeAnnotation> governorType() {
          return UnsafeAnnotation.TYPE;
        }

        @Override
        public AnnotationType<UnsafeAnnotation> dependantType() {
          return UnsafeAnnotation.TYPE;
        }

        @Override
        public UnsafeRelation wrap(Document<?> parent, gate.relations.Relation relation, String annotationSet) {
          return new UnsafeRelation(parent, annotationSet, relation);
        }
      };

  public static final Logger log = Logger.get(UnsafeRelation.class);


  private UnsafeRelation(final Document<?> document, final String annotationSet, final gate.relations.Relation gateRelation) {
    super(document, gateRelation, annotationSet);
  }

  public static UnsafeRelation create(final Document<?> document, final String type, final int govId, final int depId,
                                      final String annotationSet) {
    final RelationSet set = document.asGate().getAnnotations(annotationSet).getRelations();
    final gate.relations.Relation gateRelation = set.addRelation(type, govId, depId);
    return new UnsafeRelation(document, annotationSet, gateRelation);
  }

  public static UnsafeRelation wrap(final Document<?> document, final String annotationSet, final gate.relations.Relation gateRelation) {
    return new UnsafeRelation(document, annotationSet, gateRelation);
  }

  @Override
  public FeatureMap getFeatureMap() {
    return gateRelation.getFeatures();
  }

  @Override
  public RelationType<UnsafeRelation, UnsafeAnnotation, UnsafeAnnotation> relationType() {
    return TYPE;
  }

  public String getType() {
    return gateRelation.getType();
  }

  public int getGovernorId() {
    return gateRelation.getMembers()[0];
  }

  public int getDependantId() {
    return gateRelation.getMembers()[1];
  }

  public Collection<Object> values() {
    return gateRelation.getFeatures().values();
  }

  public int size() {
    return gateRelation.getFeatures().size();
  }

  public boolean isEmpty() {
    return gateRelation.getFeatures().isEmpty();
  }

  public int getId() {
    return gateRelation.getId();
  }

  @Override public Iterator<Map.Entry<String, Object>> iterator() {
    final List<Map.Entry<String, Object>> entries = Lists.newArrayList();
    for (Map.Entry<Object, Object> entry : gateRelation.getFeatures().entrySet()) {
      assert (entry.getKey() instanceof String) : "non-string key in feature map";
      entries.add(new AbstractMap.SimpleEntry<>((String) entry.getKey(), entry.getValue()));
    }
    return entries.iterator();
  }
}
