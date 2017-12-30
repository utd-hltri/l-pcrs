package edu.utdallas.hltri.scribe;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Created by trg19 on 8/27/2016.
 */
public interface AnnotationSet {
  List<? extends Annotation> getAnnotations();

  @SuppressWarnings("unchecked")
  default <T extends Annotation> List<T> getAnnotations(Class<T> annotationType) {
    final List<? extends Annotation> list = getAnnotations()
        .stream()
        .filter(annotation -> annotationType.isAssignableFrom(annotation.getClass()))
        .collect(Collectors.toList());
    return (List<T>) list;
  }

  default <T extends Annotation> Stream<T> getAnnotationStream(Class<T> annotationType) {
    Reflections
    return getAnnotations(annotationType).stream();
  }
}
