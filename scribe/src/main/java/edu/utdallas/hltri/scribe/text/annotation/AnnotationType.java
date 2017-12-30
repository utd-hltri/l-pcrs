package edu.utdallas.hltri.scribe.text.annotation;

import edu.utdallas.hltri.logging.Logger;
import edu.utdallas.hltri.scribe.text.Document;
import edu.utdallas.hltri.scribe.text.DuplicateAnnotationException;
import gate.AnnotationSet;
import gate.Factory;
import gate.util.InvalidOffsetException;

/**
 * Created by travis on 7/15/14.
 */
public interface AnnotationType<T extends Annotation<T>> /* extends TypesafeMap.Key<T> */ {
  /**
   * Get the unique, textual name of this Annotation type
   * (Used internally as an identifier which should uniquely identify this class of annotation)
   * @return String unique name of this AnnotationType
   */
  public String getName();

  /**
   * EXPERT: Constructs a new Annotation of type T which wraps the given gate Annotation on the given parent Document
   * @param parent      Document to attach the annotation to
   * @param annotation  gate.Annotation to wrap
   * @return Annotation corresponding to this AnnotationType
   * @see edu.utdallas.hltri.scribe.text.annotation.Annotation
   */
  public T wrap(Document<?> parent, gate.Annotation annotation);

  /**
   * Constructs a new Annotation on the given Document across the given character offsets
   * @param parent Document to create the annotation on
   * @param start  character offset where the Annotation starts (inclusive)
   * @param end    character offset where the Annotation ends (exclusive)
   * @return Annotation corresponding to this AnnotationType
   * @see edu.utdallas.hltri.scribe.text.annotation.Annotation
   */
  @Deprecated default T create(Document<?> parent, long start, long end) {
    final AnnotationSet set = parent.asGate().getAnnotations();
    checkDuplicate(set, start, end);
    try {
      parent.setDirty();
      final Integer id = set.add(start, end, getName(), Factory.newFeatureMap());
      final T annotation = wrap(parent, set.get(id));
      log.trace("Creating {}", annotation.toString());
//        annotation.set(Annotation.id, annotation.getDocument().registerAnnotation(annotation));
      return annotation;
    } catch (InvalidOffsetException e) {
      throw new RuntimeException(e);
    }
  }

  default void checkDuplicate(final AnnotationSet set, long start, long end) {
    final AnnotationSet inRange = set.get(getName(), start, end);
    if (!inRange.isEmpty()) {
      for (gate.Annotation gateAnn : inRange) {
        if (gateAnn.getStartNode().getOffset() == start && gateAnn.getEndNode().getOffset() == end && gateAnn.getType().equals(getName())) {
          throw new DuplicateAnnotationException(gateAnn);
        }
      }
    }
  }

  /**
   * Constructs a new Annotation on the given Document across the given character offsets beloneing to the given AnnotationSet
   *
   * @param parent Document to create the annotation on
   * @param annotationSet name of the annotation set this Annotation will belong to
   * @param start  character offset where the Annotation starts (inclusive)
   * @param end    character offset where the Annotation ends (exclusive)
   * @return Annotation corresponding to this AnnotationType
   * @see edu.utdallas.hltri.scribe.text.annotation.Annotation
   */
  default T create(Document<?> parent, String annotationSet, long start, long end) {
    final AnnotationSet set = parent.asGate().getAnnotations(annotationSet);
    checkDuplicate(set, start, end);
    try {
      parent.setDirty();
      final Integer id = set.add(start, end, getName(), Factory.newFeatureMap());
      final T annotation = wrap(parent, set.get(id));
      log.trace("Creating {}:{}", annotationSet, annotation.toString());
//        annotation.set(Annotation.id, annotation.getDocument().registerAnnotation(annotation));
      return annotation;
    } catch (InvalidOffsetException e) {
      throw new RuntimeException(e);
    }
  }

  default T createOrWrap(Document<?> parent, String annotationSet, long start, long end) {
    try {
      return create(parent, annotationSet, start, end);
    } catch (DuplicateAnnotationException e) {
      return wrap(parent, e.old);
    }
  }

  static final Logger log = Logger.get(AnnotationType.class);
}
