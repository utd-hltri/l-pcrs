package edu.utdallas.hltri.scribe.annotators;

import edu.utdallas.hltri.io.AC;
import edu.utdallas.hltri.scribe.text.BaseDocument;
import edu.utdallas.hltri.scribe.text.Document;

import java.io.Closeable;

/**
 * Created by travis on 7/15/14.
 */
@SuppressWarnings("unused")
public interface Annotator<D extends BaseDocument> extends AC {
  <B extends D> void annotate(Document<B> document);

  default <B extends D> void annotateAll(Iterable<Document<B>> documents) {
    for (Document<B> document : documents) {
      annotate(document);
    }
  }

  abstract class Builder<D extends BaseDocument, B extends Builder<D,B>> {
    protected abstract B self();
    public abstract Annotator<D> build();
  }
}