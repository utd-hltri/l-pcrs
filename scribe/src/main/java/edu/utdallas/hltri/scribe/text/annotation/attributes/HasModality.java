package edu.utdallas.hltri.scribe.text.annotation.attributes;

import edu.utdallas.hltri.scribe.text.Attribute;

/**
 * Created by rmm120030 on 9/14/16.
 */
public interface HasModality {
  Attribute<HasModality, String> modality = Attribute.typed("modality", String.class);
}
