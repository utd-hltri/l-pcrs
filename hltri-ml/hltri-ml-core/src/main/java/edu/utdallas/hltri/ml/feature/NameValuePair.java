package edu.utdallas.hltri.ml.feature;

import edu.utdallas.hltri.ml.Feature;

/**
* User: bryan
* Date: 12/20/12
* Time: 12:40 PM
* Created with IntelliJ IDEA.
*/
class NameValuePair {
  public String name;
  public Object value;
  private int hashCode = -1;

  NameValuePair(Feature<?> feat) {
    this.name = feat.name();
    this.value = feat.value();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    NameValuePair that = (NameValuePair) o;

    if (this.hashCode() != that.hashCode()) return false;
    if (!name.equals(that.name)) return false;
    if (value != null ? !value.equals(that.value) : that.value != null) return false;

    return true;
  }

  @Override
  public int hashCode() {
    if (this.hashCode != -1) { return hashCode; }
    int result = name.hashCode();
    result = 31 * result + (value != null ? value.hashCode() : 0);
    this.hashCode = result;
    return result;
  }
}
