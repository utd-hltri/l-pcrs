package edu.utdallas.hltri.eeg.relation;

import edu.utdallas.hltri.scribe.text.Attribute;

/**
 * Created by rmm120030 on 9/25/17.
 */
public interface EegRelation {
  Attribute<EegRelation, String> type = Attribute.typed("label", String.class);
  String getRelationType();
}
