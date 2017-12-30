package edu.utdallas.hltri.scribe.text;

import javax.annotation.Nonnull;

/**
 * Created by trg19 on 8/27/2016.
 */
public class Entity extends Chunk  {
  protected Entity(@Nonnull edu.utdallas.hltri.scribe.Text parent, @Nonnull String annotationSet, int start, int end) {
    super(parent, annotationSet, start, end);
  }

  public static Entity create(@Nonnull edu.utdallas.hltri.scribe.Text parent, @Nonnull String annotationSet, int start, int end) {
    return new Entity(parent, annotationSet, start, end);
  }
}
