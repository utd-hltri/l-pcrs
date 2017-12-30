package edu.utdallas.hltri.scribe.text;

import com.google.common.collect.Lists;
import edu.utdallas.hltri.scribe.text.annotation.Annotation;
import gate.AnnotationSet;

import java.util.AbstractList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by travis on 7/15/14.
 */
public class AnnotationList<T extends Annotation<T>> extends AbstractList<T> {
  protected final List<T> inner;
  protected final AnnotationSet parent;

  protected AnnotationList(AnnotationSet parent, List<T> inner) {
    this.parent = parent;
    this.inner = inner;
  }

  public static <T extends Annotation<T>> AnnotationList<T> empty(AnnotationSet parent) {
    return new AnnotationList<T>(parent, Lists.<T>newArrayListWithCapacity(parent.size()));
  }

  @SafeVarargs public static <T extends Annotation<T>> AnnotationList<T> create(AnnotationSet parent, T... elems) {
    return new AnnotationList<T>(parent, Arrays.asList(elems));
  }

  public static <T extends Annotation<T>> AnnotationList<T> create(AnnotationSet parent, Iterable<T> it) {
    return new AnnotationList<T>(parent, Lists.<T>newArrayList(it));
  }

  @Override public void add(int index, T element) {
    parent.add(element.asGate());
    inner.add(index, element);
  }

  @Override public T remove(int index) {
    parent.remove(get(index).asGate());
    return inner.remove(index);
  }

  /**
   * {@inheritDoc}
   *
   * @param index
   * @throws IndexOutOfBoundsException {@inheritDoc}
   */
  @Override public T get(int index) {
    return inner.get(index);
  }

  @Override public int size() {
    return inner.size();
  }
}
