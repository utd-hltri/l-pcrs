package edu.utdallas.hltri.scribe.text.annotation.attributes;

import edu.utdallas.hltri.scribe.text.Attribute;

/**
 * Created by rmm120030 on 9/14/16.
 */
public interface HasPolarity {
  Attribute<HasPolarity, String> polarity = Attribute.typed("polarity", String.class);
}
