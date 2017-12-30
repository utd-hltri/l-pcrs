package edu.utdallas.hltri.scribe.text.annotation;

import java.util.function.BiFunction;

import edu.utdallas.hltri.logging.Logger;
import edu.utdallas.hltri.scribe.text.Document;

/**
 * Created by travis on 7/15/14.
 */
public abstract class AbstractAnnotationType<T extends Annotation<T>> implements AnnotationType<T> {
  private static final Logger log = Logger.get(AnnotationType.class);
  protected final String name;

  protected AbstractAnnotationType(final String name) {
    this.name = name;
  }

  public static <T extends Annotation<T>> AnnotationType<T> register(String name, BiFunction<Document<?>, gate.Annotation, T> creator) {
    return new AbstractAnnotationType<T>(name) {
      @Override public T wrap(Document<?> parent, gate.Annotation annotation) {
        return creator.apply(parent, annotation);
      }
    };
  }

  @Override public String getName() {
    return name;
  }
}