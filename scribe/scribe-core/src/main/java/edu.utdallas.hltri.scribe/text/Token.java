package edu.utdallas.hltri.scribe.text;

import javax.annotation.Nonnull;

import edu.utdallas.hltri.scribe.*;

/**
 * Created by trg19 on 8/27/2016.
 */
@AnnotationType("token")
public class Token extends Annotation {
  protected Token(@Nonnull edu.utdallas.hltri.scribe.Text parent, @Nonnull String annotationSet, int start, int end) {
    super(parent, annotationSet, start, end);
  }

  public static Token create(@Nonnull edu.utdallas.hltri.scribe.Text parent, @Nonnull String annotationSet, int start, int end) {
    return new Token(parent, annotationSet, start, end);
  }
}
