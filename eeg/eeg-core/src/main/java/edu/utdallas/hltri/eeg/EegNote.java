package edu.utdallas.hltri.eeg;

import edu.utdallas.hltri.scribe.text.BaseDocument;
import edu.utdallas.hltri.scribe.text.DocumentAttribute;

/**
 * Created by ramon on 2/4/16.
 */
public abstract class EegNote extends BaseDocument {
  public static final DocumentAttribute<EegNote, String> patientId   = DocumentAttribute.typed("patientid", String.class);
  public static final DocumentAttribute<EegNote, String> date   = DocumentAttribute.typed("date", String.class);
  public static final DocumentAttribute<EegNote, String> interpretation = DocumentAttribute.typed("interpretation", String.class);
}
