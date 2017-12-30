package edu.utdallas.hltri.ml;

/**
 * Created by rmm120030 on 8/26/16.
 */
public enum BinaryLabel implements Label {
  FALSE(0),
  TRUE(1);

  @Override
  public int numLabels() {
    return values().length;
  }

  private final Number numValue;
  BinaryLabel(final Number numValue) {
    this.numValue = numValue;
  }

  @Override
  public Number numericValue() {
    return numValue;
  }

}
