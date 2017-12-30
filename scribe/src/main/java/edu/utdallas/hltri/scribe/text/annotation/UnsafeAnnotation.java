package edu.utdallas.hltri.scribe.text.annotation;

import com.google.common.collect.Lists;

import java.util.AbstractMap;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import edu.utdallas.hltri.logging.Logger;
import edu.utdallas.hltri.scribe.text.Document;
import edu.utdallas.hltri.scribe.text.HasFeatureMap;
import gate.Annotation;
import gate.AnnotationSet;
import gate.FeatureMap;
import gate.util.InvalidOffsetException;

/**
 * Created by travis on 7/16/14.
 */
public class UnsafeAnnotation extends AbstractAnnotation<UnsafeAnnotation> implements Iterable<Map.Entry<String, Object>>, HasFeatureMap {
  private static final long serialVersionUID = 1L;

  public static final Logger log = Logger.get(UnsafeAnnotation.class);

  public static final AnnotationType<UnsafeAnnotation> TYPE = new AnnotationType<UnsafeAnnotation>(){
    @Override
    public String getName() {
      throw new UnsupportedOperationException();
    }

    @Override
    public UnsafeAnnotation wrap(Document<?> parent, Annotation annotation) {
      return new UnsafeAnnotation(parent, annotation);
    }

    @Override
    public UnsafeAnnotation create(Document<?> parent, String annotationSet, long start, long end) {
      throw new UnsupportedOperationException();
    }

    @Override
    public UnsafeAnnotation createOrWrap(Document<?> parent, String annotationSet, long start, long end) {
      throw new UnsupportedOperationException();
    }
  };


  public UnsafeAnnotation(Document<?> document, Annotation gateAnnotation) {
    super(document, gateAnnotation);
  }

  @Override
  public FeatureMap getFeatureMap() {
    return asGate().getFeatures();
  }

  //  public <T> Optional<T> getUnsafeAnnotations(String featureName) {
//    return Optional.fromNullable(Unsafe.<T>cast(features.getUnsafeAnnotations(featureName)));
//  }
//
//  public <T> void set(String featureName, T featureValue) {
//    features.put(featureName, featureValue);
//  }

  public Collection<Object> values() {
    return features.values();
  }

  public int size() {
    return features.size();
  }

  public boolean isEmpty() {
    return features.isEmpty();
  }

  public String getType() {
    return asGate().getType();
  }

  public int getGateId() {
    return annotation.getId();
  }

//  public static UnsafeAnnotation at(Document<?> doc, String annotationSet, String annotationType, long start, long end) {
//    try {
//      final AnnotationSet annSet = doc.asGate().getAnnotations(annotationSet);
//      final Integer annId = annSet.add(start, end, annotationType, gate.Factory.newFeatureMap());
//      final UnsafeAnnotation ua = new UnsafeAnnotation(doc, annSet.getUnsafeAnnotations(annId));
//      return ua;
//    } catch (InvalidOffsetException e) {
//      log.error("start: {}, end: {}, type: {}, annset: {}, doc length: {}", start, end, annotationType, annotationSet, doc.asString().length());
//      throw new RuntimeException(e);
//    }
//  }

  public static UnsafeAnnotation at(Document<?> doc, String annotationSet, String annotationType, long start, long end, int id) {
    try {
      final AnnotationSet annSet = doc.asGate().getAnnotations(annotationSet);
      annSet.add(id, start, end, annotationType, gate.Factory.newFeatureMap());
      final UnsafeAnnotation ua = new UnsafeAnnotation(doc, annSet.get(id));
      return ua;
    } catch (InvalidOffsetException e) {
      log.error("Doc: {}. annid: {}, start: {}, end: {}, type: {}, annset: {}, doc length: {}", doc.getId(), id,
          start, end, annotationType, annotationSet, doc.asString().length());
      throw new RuntimeException(e);
    }
  }

  @Override public Iterator<Map.Entry<String, Object>> iterator() {
    final List<Map.Entry<String, Object>> entries = Lists.newArrayList();
    for (Map.Entry<Object, Object> entry : features.entrySet()) {
      if (entry.getKey() instanceof String) {
        entries.add(new AbstractMap.SimpleEntry<>((String) entry.getKey(), entry.getValue()));
      } else {
        log.warn("Encountered non-String feature {} = {}", entry.getKey(), entry.getValue());
      }
    }
    return entries.iterator();
  }
}
