package edu.utdallas.hltri.scribe.text.documents;

import edu.utdallas.hltri.scribe.text.DocumentAttribute;

/**
 * Created by travis on 7/30/15.
 */
public interface HasTitle {
  DocumentAttribute<HasTitle, String> title = DocumentAttribute.typed("title", String.class);
}
