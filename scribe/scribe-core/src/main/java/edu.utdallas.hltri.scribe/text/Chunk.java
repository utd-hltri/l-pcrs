package edu.utdallas.hltri.scribe.text;

import javax.annotation.Nonnull;

import edu.utdallas.hltri.scribe.Annotation;

/**
 * Created by trg19 on 8/27/2016.
 */
public class Chunk extends Annotation {
  protected Chunk(@Nonnull edu.utdallas.hltri.scribe.Text parent, @Nonnull String annotationSet, int start, int end) {
    super(parent, annotationSet, start, end);
  }

  public static Chunk create(@Nonnull edu.utdallas.hltri.scribe.Text parent, @Nonnull String annotationSet, int start, int end) {
    return new Chunk(parent, annotationSet, start, end);
  }
}
