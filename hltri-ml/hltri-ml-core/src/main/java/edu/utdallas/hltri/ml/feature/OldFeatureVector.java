package edu.utdallas.hltri.ml.feature;

import edu.utdallas.hltri.ml.vector.FeatureVector;
import edu.utdallas.hltri.ml.label.EnumLabel;
import com.google.common.collect.Maps;
import edu.utdallas.hltri.ml.Feature;
import edu.utdallas.hltri.util.IntIdentifier;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.function.ObjIntConsumer;
import java.util.stream.IntStream;

/**
 * Created by ramon on 2/25/16.
 */
@Deprecated
public class OldFeatureVector implements FeatureVector<Feature<?>>{
  // Class fineGrainedLabel
  protected final EnumLabel label;

  // Sparse feature map
  // TODO: Make this an IntMap or a SparseVector ala Mallet
  protected final Map<Integer, Feature<?>> map;

  public OldFeatureVector(final EnumLabel label) {
    this.map = Maps.newTreeMap();
    this.label = label;
  }

  @Override
  public int[] ids() {
    return map.keySet().stream().mapToInt(i -> i).toArray();
  }

  @Override
  public String[] names() {
    return map.values().stream().map(Feature::name).toArray(String[]::new);
  }

  @Override
  public void forEachWithId(ObjIntConsumer<? super Feature<?>> action) {
    map.forEach((id, feat) -> action.accept(feat, id));
  }

  @Override
  public Iterator<Feature<?>> iterator() {
    return map.values().iterator();
  }

  public NumericFeatureVector asNumeric() {
    return NumericFeatureVector.fromFeatureVector(this);
  }

  public void addFeature(final int idx, final Feature<?> feature) {
    map.put(idx, feature);
  }

  // TODO: Rename this to "getSetIndices()"
  public Set<Integer> indices() {
    return map.keySet();
  }

  // TODO: Rename this to getLabel()
  public EnumLabel label() {
    return label;
  }

  // TODO: Rename this to getSetFeatures()
  // TODO: Add getCardinality() that contains the maximum size of this vector
  public int size() {
    return indices().size();
  }

  // TODO: rename this guy to updateValue
  public void replace(int idx, double newValue) {
    final Feature<?> feature = map.get(idx);
    map.put(idx, Feature.numericFeature(feature.name(), newValue));
  }

  public boolean hasFeature(int idx) {
    return map.containsKey(idx);
  }

  public void minMaxScale(int idx, double min, double max) {
    final Feature<? extends Number> feature = (Feature<? extends Number>) map.get(idx);
    final double val = ((Number)feature.value()).doubleValue();
    map.put(idx, Feature.numericFeature(feature.name(), (val - min) / (max - min)));
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder();
    sb.append(label);
    for (final Integer idx : map.keySet()) {
      sb.append(" ").append(idx).append(":").append(map.get(idx).value());
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
      sb.append(" ").append(idx).append(":").append(map.get(idx).value());
    }
    return sb.toString();
  }

  public String verboseString(IntIdentifier<String> iid) {
    final StringBuilder sb = new StringBuilder();
    sb.append(label);
    for (final Integer idx : map.keySet()) {
      sb.append("\n").append(iid.get(idx)).append(":").append(map.get(idx).value());
    }
    return sb.toString();
  }

  public String verboseStringNoZeros(IntIdentifier<String> iid) {
    final StringBuilder sb = new StringBuilder();
    sb.append(label);
    for (final Integer idx : map.keySet()) {
      if (((Number)map.get(idx).value()).doubleValue() != 0.0) {
        sb.append("\n").append(iid.get(idx)).append(":").append(map.get(idx).value());
      }
    }
    return sb.toString();
  }

  public String noLabel() {
    final StringBuilder sb = new StringBuilder();
    for (final Integer idx : map.keySet()) {
      sb.append(idx).append(":").append(map.get(idx).value()).append(" ");
    }
    if (sb.length() > 0) {
      sb.deleteCharAt(sb.lastIndexOf(" "));
    }
    return sb.toString();
  }

  @Override
  public Feature<?> getById(int id) {
    return map.get(id);
  }

  @Override
  public Feature<?> getByName(String name) {
    throw new UnsupportedOperationException("I can't believe you've done this. But for real, you're using the wrong class, see SparseFeatureVector");
  }
}
