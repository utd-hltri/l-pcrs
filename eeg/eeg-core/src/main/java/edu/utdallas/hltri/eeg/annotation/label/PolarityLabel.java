package edu.utdallas.hltri.eeg.annotation.label;

import edu.utdallas.hltri.ml.label.EnumLabel;

import java.security.InvalidParameterException;

/**
 * Created by ramon on 2/25/16.
 */
public enum PolarityLabel implements EnumLabel {
  POSITIVE, NEGATIVE;

  public static PolarityLabel fromString(String s) {
    switch (s.trim().toUpperCase()) {
      case "POS":
      case "POSITIVE": return POSITIVE;
      case "NEG":
      case "NEGATIVE": return NEGATIVE;
      default: throw new InvalidParameterException("No valid PolarityLabel for " + s);
    }
  }
}
