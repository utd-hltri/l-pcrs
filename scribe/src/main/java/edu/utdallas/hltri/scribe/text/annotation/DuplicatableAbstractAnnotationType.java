package edu.utdallas.hltri.scribe.text.annotation;

import edu.utdallas.hltri.logging.Logger;
import edu.utdallas.hltri.scribe.text.Document;
import gate.AnnotationSet;
import gate.Factory;
import gate.util.InvalidOffsetException;

import java.util.function.BiFunction;

/**
 * Created with IntelliJ IDEA.
 * User: Ramon
 * Date: 8/20/14
 * Time: 11:20 AM
 */
public abstract class DuplicatableAbstractAnnotationType<T extends Annotation<T>> implements AnnotationType<T> {
  private static final Logger log = Logger.get(AnnotationType.class);
  protected final String name;

  protected DuplicatableAbstractAnnotationType(final String name) {
    this.name = name;
  }

  @Override public String getName() {
    return name;
  }

  @Override public T create(Document<?> parent, long start, long end) {
    final AnnotationSet set = parent.asGate().getAnnotations();
    final AnnotationSet old = set.get(getName(), start, end);
    try {
      final Integer id = set.add(start, end, getName(), Factory.newFeatureMap());
      final T annotation = wrap(parent, set.get(id));
      log.trace("Creating {}", annotation.toString());
      return annotation;
    } catch (InvalidOffsetException e) {
      throw new RuntimeException(e);
    }
  }

  @Override public T create(Document<?> parent, String annotationSet, long start, long end) {
    final AnnotationSet set = parent.asGate().getAnnotations(annotationSet);
    final AnnotationSet old = set.get(getName(), start, end);
    try {
      final Integer id = set.add(start, end, getName(), Factory.newFeatureMap());
      final T annotation = wrap(parent, set.get(id));
      log.trace("Creating {}:{}", annotationSet, annotation.toString());
      return annotation;
    } catch (InvalidOffsetException e) {
      throw new RuntimeException(e);
    }
  }

  public static <T extends Annotation<T>> AnnotationType<T> register(String name, BiFunction<Document<?>, gate.Annotation, T> creator) {
    return new AbstractAnnotationType<T>(name) {
      @Override public T wrap(Document<?> parent, gate.Annotation annotation) {
        return creator.apply(parent, annotation);
      }
    };
  }
}
