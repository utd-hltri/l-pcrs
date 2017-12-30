package edu.utdallas.hltri.ml.label;

import java.io.Serializable;
import java.util.EnumSet;

/**
 * EnumLabel
 *
 * This class is meant to be implemented by an enum such as:
 *   public enum TestLabel implements EnumLabel {
 *     LABEL1, LABEL2, ..., LABELN;
 *   }
 * This way the user has access to each label by name.
 */
public interface EnumLabel extends Serializable, Label {
  long serialVersionUID = 1L;

//  /**
//   * Overriden by enum
//   * @return
//   */
//  static EnumLabel[] values() {throw new UnsupportedOperationException("EnumLabels should be enum types and therefore have a final values() method.");}

  default int numLabels() {
    return EnumSet.allOf(((Enum<?>)this).getClass()).size();
//    return values().length;
  }

  @Deprecated
  default Number numericValue() {
    return ordinal();
  }

  /**
   * Overridden by enum
   * @return the index of this EnumLabel is in the values() array of EnumLabels
   */
  int ordinal();

  /**
   * Overriden by enum
   * @return string name of this EnumLabel
   */
  String name();

  /**
   *
   * @return string name of this EnumLabel
   */
  default String asString() {
    return name();
  }
  default int asInt() {
    return ordinal();
  }

  EnumLabel NULL = NullEnumLabel.NULL;
  enum NullEnumLabel implements EnumLabel {
    NULL
  }
}
