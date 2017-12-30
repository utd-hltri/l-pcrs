package edu.utdallas.hltri.scribe.text.relation;

import edu.utdallas.hltri.scribe.text.annotation.Annotation;
import edu.utdallas.hltri.scribe.text.annotation.AnnotationType;

/**
 * Created by rmm120030 on 5/18/16.
 */
public interface Relatable<A extends Annotation<A>> {
  AnnotationType<A> type();
}
