package edu.utdallas.hltri.scribe.text.annotation.attributes;

import edu.utdallas.hltri.scribe.text.Attribute;

/**
 * Created by travis on 8/15/14.
 */
public interface HasType {
  Attribute<HasType, String> type = Attribute.typed("type", String.class);
}
