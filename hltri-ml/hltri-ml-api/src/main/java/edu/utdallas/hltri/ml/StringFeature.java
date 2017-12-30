package edu.utdallas.hltri.ml;

/**
 * Created with IntelliJ IDEA.
 * User: bryan
 * Date: 12/16/12
 * Time: 12:30 AM
 */
public class StringFeature extends AbstractFeature<String> {

  StringFeature(String name, String value) {
    super(name, value);
  }

  public NumericFeature toNumericFeature() {
    return Feature.numericFeature(this.name() + "=" + this.value(), 1);
  }

  @Override
  public StringFeature toStringFeature() {
    return this;
  }

  @Override
  public BinaryFeature toBinaryFeature() {
    return Feature.binaryFeature(this.name() + "=" + this.value(), true);
  }

  @SuppressWarnings("SimplifiableIfStatement")
  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    StringFeature that = (StringFeature) o;

    if (!name.equals(that.name)) return false;
    return !(value() != null ? !value().equals(that.value()) : that.value() != null);

  }

  @Override
  public int hashCode() {
    int result = value() != null ? value().hashCode() : 0;
    result = 31 * result + name.hashCode();
    return result;
  }
}
