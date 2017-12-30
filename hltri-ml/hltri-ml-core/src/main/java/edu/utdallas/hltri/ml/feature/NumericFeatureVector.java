package edu.utdallas.hltri.ml.feature;

import edu.utdallas.hltri.ml.label.EnumLabel;
import edu.utdallas.hltri.util.IntIdentifier;

import com.google.common.collect.Maps;

import java.io.Serializable;
import java.util.Map;
import java.util.Set;

/**
 * Created by rmn72 on 8/28/2016.
 */
public class NumericFeatureVector implements Serializable {
  private static final long serialVersionUID = 1L;
  private final EnumLabel label;
  private final Map<Integer, Number> map;

  public static NumericFeatureVector fromFeatureVector(final OldFeatureVector fv) {
    final NumericFeatureVector nfv = new NumericFeatureVector(fv.label());
    fv.indices().forEach(i -> nfv.addFeature(i, (Number) fv.getById(i).value()));
    return nfv;
  }

  public NumericFeatureVector(final EnumLabel label) {
    this.map = Maps.newTreeMap();
    this.label = label;
  }

  public void addFeature(final int idx, final Number feature) {
    map.put(idx, feature);
  }

  public Set<Integer> indices() {
    return map.keySet();
  }

  public Number get(final int idx) {
    return map.get(idx);
  }

  public Number getOrZero(final int idx) {
    return map.containsKey(idx) ? map.get(idx) : 0.0;
  }

  public EnumLabel label() {
    return label;
  }

  public int size() {
    return indices().size();
  }

  public void replace(int idx, double newValue) {
    final Number feature = map.get(idx);
    map.put(idx, newValue);
  }

  public boolean hasFeature(int idx) {
    return map.containsKey(idx);
  }

  public void minMaxScale(int idx, double min, double max) {
    final double val = map.get(idx).doubleValue();
    map.put(idx, (val - min) / (max - min));
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder();
    sb.append(label);
    for (final Integer idx : map.keySet()) {
      sb.append(" ").append(idx).append(":").append(map.get(idx).doubleValue());
    }
    return sb.toString();
  }

  public String toSvmLightString() {
    final StringBuilder sb = new StringBuilder();
    if (label != null) {
      sb.append(label.numericValue().intValue());
    }
    else {
      sb.append(0);
    }
    for (final Integer idx : map.keySet()) {
      sb.append(" ").append(idx).append(":").append(map.get(idx).doubleValue());
    }
    return sb.toString();
  }

  public String verboseString(IntIdentifier<String> iid) {
    final StringBuilder sb = new StringBuilder();
    sb.append(label);
    for (final Integer idx : map.keySet()) {
      sb.append("\n").append(iid.get(idx)).append(":").append(map.get(idx).doubleValue());
    }
    return sb.toString();
  }

  public String verboseStringNoZeros(IntIdentifier<String> iid) {
    final StringBuilder sb = new StringBuilder();
    sb.append(label);
    for (final Integer idx : map.keySet()) {
      if (map.get(idx).doubleValue() != 0.0) {
        sb.append("\n").append(iid.get(idx)).append(":").append(map.get(idx).doubleValue());
      }
    }
    return sb.toString();
  }

  public String noLabel() {
    final StringBuilder sb = new StringBuilder();
    for (final Integer idx : map.keySet()) {
      sb.append(idx).append(":").append(map.get(idx).doubleValue()).append(" ");
    }
    if (sb.length() > 0) {
      sb.deleteCharAt(sb.lastIndexOf(" "));
    }
    return sb.toString();
  }
}
