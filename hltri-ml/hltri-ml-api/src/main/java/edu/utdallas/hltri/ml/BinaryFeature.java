package edu.utdallas.hltri.ml;

/**
 * Created by travis on 8/5/16.
 */
public class BinaryFeature extends AbstractFeature<Boolean> {

  BinaryFeature(String name, Boolean value) {
    super(name, value);
  }

  @Override
  public NumericFeature toNumericFeature() {
    return Feature.numericFeature(name, value ? 1 : 0);
  }

  @Override
  public StringFeature toStringFeature() {
    return new StringFeature(name, value ? "true" : "false");
  }

  @Override
  public BinaryFeature toBinaryFeature() {
    return this;
  }
}
