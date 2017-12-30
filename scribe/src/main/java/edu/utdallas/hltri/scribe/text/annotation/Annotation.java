package edu.utdallas.hltri.scribe.text.annotation;

import edu.utdallas.hltri.Describable;
import edu.utdallas.hltri.scribe.text.*;
import edu.utdallas.hltri.util.Offset;
import edu.utdallas.hltri.util.Unsafe;

import java.util.List;

/**
 * Annotation.
 *
 * Created by travis on 7/15/14.
 */
@SuppressWarnings("unused")
public interface Annotation<B extends Annotation<B>> extends AttributeMap<B>, PropertyMap<B>, Describable, Identifiable, Offset {
  /**
   * Get the Document this Annotation is attached to
   * @return Document this Annotation was created on
   */
  Document<?> getDocument();

  Property<Annotation<?>, Long> StartOffset = Property.readOnly(a -> a.asGate().getStartNode().getOffset());
  Property<Annotation<?>, Long> EndOffset   = Property.readOnly(a -> a.asGate().getEndNode().getOffset());


  default String getType() {
    return asGate().getType();
  }

  /**
   * EXPERT: Get the underlying gate.Annotation wrapped by this Object
   *
   * @return underlying gate.Annotation
   */
  gate.Annotation asGate();

  int getGateId();

  /**
   * Get the character offset where this annotation starts (inclusive)
   * @return long character offset
   */
  @Deprecated
  default  long getStartCharOffset() {
    return asGate().getStartNode().getOffset();
  }

  /**
   * Get the character offset where this annotation ends (exclusive)
   * @return long character offset
   */
  @Deprecated
  default long getEndCharOffset() {
    return asGate().getEndNode().getOffset();
  }

  /**
   * Checks of the passed annotation is contained within this one.
   * @param other the other annotation
   * @return true if the passed annotation is contained within this one, false otherwise.
   */
  default boolean contains(Annotation<?> other) {
    return (long)other.get(Annotation.StartOffset) >= this.get(Annotation.StartOffset)
        && (long)other.get(Annotation.EndOffset) <= this.get(Annotation.EndOffset);
  }

  /**
   * Get all the annotations from the annotation set provided that lie within
   * the range of the containing annotation.
   */
  default <T extends Annotation<T>> List<T> getContained(String annotationSet, AnnotationType<T> type) {
    final Document<?> document = getDocument();
    final gate.AnnotationSet set = document.getAnnotationSet(annotationSet, type);
    return document.convertAnnotationSet(gate.Utils.getContainedAnnotations(set, asGate()), type);
  }

  /**
   * Get all the annotations from the default annotation set for the passed type that lie within
   * the range of the containing annotation.
   */
  default <T extends Annotation<T>> List<T> getContained(AnnotationType<T> type) {
    final Document<?> document = getDocument();
    final gate.AnnotationSet set = document.getAnnotationSet(type);
    return document.convertAnnotationSet(gate.Utils.getContainedAnnotations(set, asGate()), type);
  }

  /**
   * Get all the annotations from the passed annotation set that cover
   * the range of the specified annotation.
   */
  default <T extends Annotation<T>> List<T> getCovering(String annotationSet, AnnotationType<T> type) {
    final Document<?> document = getDocument();
    final gate.AnnotationSet set = document.getAnnotationSet(annotationSet, type);
    return document.convertAnnotationSet(gate.Utils.getCoveringAnnotations(set, asGate()), type);
  }

  /**
   * Get all the annotations from the default annotation set for the passed type that cover
   * the range of the specified annotation.
   */
  default <T extends Annotation<T>> List<T> getCovering(AnnotationType<T> type) {
    final Document<?> document = getDocument();
    final gate.AnnotationSet set = document.getAnnotationSet(type);
    return document.convertAnnotationSet(gate.Utils.getCoveringAnnotations(set, asGate()), type);
  }

  /**
   * Get all the annotations from the passed annotation set that partly or totally overlap
   * the range of the specified annotation.
   */
  default <T extends Annotation<T>> List<T> getOverlapping(String annotationSet, AnnotationType<T> type) {
    final Document<?> document = getDocument();
    final gate.AnnotationSet set = document.getAnnotationSet(annotationSet, type);
    return document.convertAnnotationSet(gate.Utils.getOverlappingAnnotations(set, asGate()), type);
  }

  /**
   * Get all the annotations from the default annotation set for the passed type that partly or totally overlap
   * the range of the specified annotation.
   */
  default <T extends Annotation<T>> List<T> getOverlapping(AnnotationType<T> type) {
    final Document<?> document = getDocument();
    final gate.AnnotationSet set = document.getAnnotationSet(type);
    return document.convertAnnotationSet(gate.Utils.getOverlappingAnnotations(set, asGate()), type);
  }

  /**
   * Returns true if the map contains the given key.
   *
   * @param key key
   */
  @Override default <T> boolean has(Attribute<? super B, T> key) {
    return asGate().getFeatures().containsKey(key);
  }

  /**
   * Returns the value associated with the given key or null if
   * none is provided.
   *
   * @param key key
   */
  @Override default <T> T get(Attribute<? super B, T> key) {
    return key.type.cast(asGate().getFeatures().get(key.name));
  }

  /**
   * Associates the given value with the given type for future calls
   * to getUnsafeAnnotations.  Returns the value removed or null if no value was present.
   *
   * @param key key
   * @param value value
   */
  @Override default <T> B set(Attribute<? super B, T> key, T value) {
    asGate().getFeatures().put(key.name, value);
    getDocument().setDirty();
    return Unsafe.cast(this);
  }

  /**
   * Removes the given key from the map, returning the value removed.
   *
   * @param key key
   */
  @Override default <T> B remove(Attribute<? super B, T> key) {
    asGate().getFeatures().remove(key.name);
    getDocument().setDirty();
    return Unsafe.cast(this);
  }

  /**
   * Returns the number of keys in the map.
   */
  @Override default int numAttributes() {
    return asGate().getFeatures().size();
  }

  @Override default String describe() { return asGate().toString(); }

  @Override
  default long getStart() {
    return get(Annotation.StartOffset);
  }

  @Override
  default long getEnd() {
    return get(Annotation.EndOffset);
  }
}
