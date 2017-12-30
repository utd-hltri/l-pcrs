package edu.utdallas.hltri.scribe.text.documents;

import edu.utdallas.hltri.scribe.text.DocumentAttribute;

/**
 * Created by travis on 7/30/15.
 */
public interface HasDoi {
  DocumentAttribute<HasDoi, String> doi = DocumentAttribute.typed("doi", String.class);
}
