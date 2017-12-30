package edu.utdallas.hltri.scribe.text.relation;

import edu.utdallas.hltri.scribe.text.annotation.Annotation;

/**
 * Created by rmm120030 on 5/18/16.
 */
public abstract class AbstractRelationType<R extends Relation<R,G,D>, G extends Annotation<G>, D extends Annotation<D>>
    implements RelationType<R,G,D> {
  protected final String name;

  protected AbstractRelationType(final String name) {
    this.name = name;
  }

  @Override public String getName() {
    return name;
  }
}
