package edu.utdallas.hltri.scribe;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;

import edu.utdallas.hltri.util.Unsafe;

/**
 * Created by trg19 on 8/27/2016.
 */
@SuppressWarnings("unused")
public abstract class Annotation extends SubSpan implements FeatureBearer, Identifiable {
  private final String id;
  private final int numericId;

  protected Text parent;
  protected int start;
  protected int end;
  protected String annotationSet;

  /**
   * Attaches this annotation to the given Text within the given annotation set and start/end offsets
   * NOTE: annotationSet is interned!
   * @param parent Text this annotation should belong to
   * @param annotationSet String containing the name of the annotation set this annotation should belong to
   * @param start starting character offset in parent
   * @param end ending character offset in parent
   */
  public Annotation(@Nonnull Text parent, @Nonnull String annotationSet, int start, int end) {
    super(parent.toCharArray(), start, end - start);
    assert start < end : "annotation start offset must be < end offset";
    this.parent = parent;
    this.annotationSet = annotationSet.intern();
    this.start = start;
    this.end = end;
    this.numericId = this.parent.annotations.add(this.start, this.end, Unsafe.cast(this));
    this.id = "#" + numericId + ":" + parent + "." + annotationSet + "." + getAnnotationType();
  }

  public String getAnnotationType() {
    return this.getClass().getSimpleName();
  }

  @Nonnull
  @Override
  public String getId() {
    return id;
  }

  @Override
  public long getNumericId() {
    return numericId;
  }

  /**
   * Returns a subset of the given set containing only annotations in the given annotation set with the given type
   * NOTE: by interning Annotation.annotationSet as well as the passed annotationSet, we can
   * compare annotation sets using equality rather than Object.equals!
   * @param source Set of annotations
   * @param type Class of desired annotation type
   * @param annotationSet Name of desired AnnotationSet
   * @param <T> Type correspond to type
   * @return Sub-set of source containing annotations of type 'type' in annotation set 'annotationSet'
   */
  @SuppressWarnings("StringEquality")
  private <T extends Annotation> Set<T> getTypedSubset(final Set<? extends Annotation> source,
                                                              final Class<T> type,
                                                              final String annotationSet) {
    final String targetSet = annotationSet.intern();
    return Unsafe.cast(source.stream().filter(a -> a != this && a.annotationSet == targetSet && type.isAssignableFrom(a.getClass())).collect(Collectors.toSet()));
  }

  /**
   * Returns a subset of the given set containing only annotations with the given type
   * @param source Set of annotations
   * @param type Class of desired annotation type
   * @param <T> Type correspond to type
   * @return Sub-set of source containing annotations of type 'type'
   */
  private <T extends Annotation> Set<T> getTypedSubset(final Set<? extends Annotation> source,
                                                              final Class<T> type) {
    return Unsafe.cast(source.stream().filter(a -> a != this && type.isAssignableFrom(a.getClass())).collect(Collectors.toSet()));
  }

  /**
   * Returns annotations of the given type that are fully contained by this annotation
   *
   * this:       *-----------------*
   *          *------------*              not returned
   *                *-------------*       returned
   *           *---------------------*    not returned
   *        *----*                        not returned
   *                               *-*    not returned
   * @param annotationType Class of desired annotations
   * @return Sub-set of annotations with the given type 'type' which are contained by this annotation
   */
  public <T extends Annotation> Set<T> getContained(Class<T> annotationType) {
    return getTypedSubset(parent.annotations.findContained(start, end), annotationType);
  }

  /**
   * Returns annotations in the given annotation set of the given type that are fully contained by this annotation
   *
   * this:       *-----------------*
   *          *------------*              not returned
   *                *-------------*       returned
   *           *---------------------*    not returned
   *        *----*                        not returned
   *                               *-*    not returned
   * @param annotationType Class of desired annotations
   * @param annotationSet Name of the desired annotation set
   * @return Sub-set of annotations with the given type 'type' in the given annotation set which are contained by this annotation
   */
  public <T extends Annotation> Set<T> getContained(String annotationSet, Class<T> annotationType) {
    return getTypedSubset(parent.annotations.findContained(start, end), annotationType, annotationSet);
  }

  /**
   * Returns annotations of the given type that are fully contain this annotation
   *
   * this:       *-----------------*
   *          *------------*              not returned
   *                *-------------*       not returned
   *           *---------------------*    returned
   *        *----*                        not returned
   *                               *-*    not returned
   * @param annotationType Class of desired annotations
   * @return Sub-set of annotations with the given type 'type' which are contained by this annotation
   */
  public <T extends Annotation> Set<T> getContaining(Class<T> annotationType) {
    return getTypedSubset(parent.annotations.findContaining(start, end), annotationType);
  }

  /**
   * Returns annotations in the given annotation set of the given type that fully contain this annotation
   *
   * this:       *-----------------*
   *          *------------*              not returned
   *                *-------------*       not returned
   *           *---------------------*    returned
   *        *----*                        not returned
   *                               *-*    not returned
   * @param annotationType Class of desired annotations
   * @param annotationSet Name of the desired annotation set
   * @return Sub-set of annotations with the given type 'type' in the given annotation set which fully contain this annotation
   */
  public <T extends Annotation> Set<T> getContaining(String annotationSet, Class<T> annotationType) {
    return getTypedSubset(parent.annotations.findContaining(start, end), annotationType, annotationSet);
  }

  /**
   * Returns annotations of the given type that overlap with this annotation
   *
   * this:       *-----------------*
   *          *------------*              returned
   *                *-------------*       not returned
   *           *---------------------*    not returned
   *        *----*                        returned
   *                               *-*    returned
   * @param annotationType Class of desired annotations
   * @return Sub-set of annotations with the given type 'type' which overlap this annotation
   */
  public <T extends Annotation> Set<T> getOverlapping(Class<T> annotationType) {
    return getTypedSubset(parent.annotations.findOverlapping(start, end), annotationType);
  }

  /**
   * Returns annotations in the given annotation set of the given type that overlap with this annotation
   *
   * this:       *-----------------*
   *          *------------*              returned
   *                *-------------*       not returned
   *           *---------------------*    not returned
   *        *----*                        returned
   *                               *-*    returned
   * @param annotationType Class of desired annotations
   * @param annotationSet Name of the desired annotation set
   * @return Sub-set of annotations with the given type 'type' in the given annotation set which overlap this annotation
   */
  public <T extends Annotation> Set<T> getOverlapping(String annotationSet, Class<T> annotationType) {
    return getTypedSubset(parent.annotations.findOverlapping(start, end), annotationType, annotationSet);
  }

  public void changeStartOffset(final int start) {
    assert start < end : "annotation start offset must be < end offset";
    this.parent.annotations.remove(this);
    this.start = start;
    this.parent.annotations.add(start, end, Unsafe.cast(this));
  }

  public void changeEndOffset(final int end) {
    assert start < end : "annotation start offset must be < end offset";
    this.parent.annotations.remove(this);
    this.end = end;
    this.parent.annotations.add(start, end, Unsafe.cast(this));
  }

  public void changeOffsets(final int start, final int end) {
    assert start < end : "annotation start offset must be < end offset";
    this.parent.annotations.remove(this);
    this.start = start;
    this.end = end;
    this.parent.annotations.add(start, end, Unsafe.cast(this));
  }

  public void changeParent(final @Nonnull Text newParent) {
    this.parent.annotations.remove(this);
    this.parent = newParent;
    this.parent.annotations.add(start, end, Unsafe.cast(this));
  }

  public void changeParent(final @Nonnull String newAnnotationSet) {
    this.annotationSet = newAnnotationSet;
  }

  public void detach() {
    this.parent.annotations.remove(this);
    this.parent = null;
  }

  public void moveTo(final @Nonnull Text parent, final @Nonnull String annotationSet, int start, int end) {
    assert start < end : "annotation start offset must be < end offset";
    this.parent.annotations.remove(this);
    this.start = start;
    this.end = end;
    this.annotationSet = annotationSet;
    this.parent = parent;
  }

}
