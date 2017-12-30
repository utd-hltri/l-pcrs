package edu.utdallas.hltri.eeg.annotation.label;

import edu.utdallas.hltri.ml.label.EnumLabel;

/**
 * Created by ramon on 2/26/16.
 */
public enum EventTypeLabel implements EnumLabel {
  EEG_EVENT(0),
  TEST(1),
  PROBLEM(2),
  TREATMENT(3),
  ANATOMICAL_SITE(4),
  CLINICAL_DEPT(5),
  EVIDENTIAL(6),
  OCCURRENCE(7),
  PATIENT_STATE(8),
  EEG_TECHNIQUE(9),
  EEG_IMPRESSION(10),
  EEG_CLINICAL_INTERPRETATION(11);

  @Override
  public int numLabels() {
    return 12;
  }

  private final Number numValue;
  EventTypeLabel(final Number numValue) {
    this.numValue = numValue;
  }

  @Override
  public Number numericValue() {
    return numValue;
  }
}
