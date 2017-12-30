package edu.utdallas.hltri.mercury.lucene;

import edu.utdallas.hltri.inquire.lucene.HasLuceneId;
import edu.utdallas.hltri.scribe.text.Identifiable;

/**
 * Created by travis on 11/8/16.
 */
public class IndexedEegNote implements Identifiable, HasLuceneId {

  private final int luceneId;
  private final org.apache.lucene.document.Document luceneDocument;

  IndexedEegNote(org.apache.lucene.document.Document luceneDocument, Integer luceneId) {
    this.luceneId = luceneId;
    this.luceneDocument = luceneDocument;
  }

  @Override public int getLuceneId() {
    return luceneId;
  }

  @Override public String getId() {
    return luceneDocument.get("record_id");
  }
}
