package edu.utdallas.hltri.scribe.text.annotation.attributes;

import edu.utdallas.hltri.scribe.text.Attribute;

/**
 * Created by travis on 2/17/15.
 */
public interface HasAssertion {
  public static Attribute<HasAssertion, String>
      ASSERTION = Attribute.typed("assertion", String.class);
}
