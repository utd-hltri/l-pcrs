package edu.utdallas.hltri.ml.classify;

import edu.utdallas.hltri.ml.vector.DenseFeatureVector;
import edu.utdallas.hltri.ml.vector.SparseFeatureVector;
import edu.utdallas.hltri.ml.label.EnumLabel;

import java.util.Iterator;
import java.util.Set;
import java.util.function.ObjIntConsumer;

/**
 * This class abstract a CRF Suite Feature Vector which is of the form:
 * fineGrainedLabel{\tfeature_name[:scaling_value]}
 * where 'fineGrainedLabel' and 'feature_name' are strings and 'scaling_value' is a double
 * Created by rmm120030 on 10/21/16.
 */
public class CrfsFeatureVector implements SparseFeatureVector<Number> {
  private final EnumLabel label;
  private final SparseFeatureVector<Number> sfv;

  public CrfsFeatureVector(EnumLabel label, SparseFeatureVector<Number> sfv) {
    this.label = label;
    this.sfv = sfv;
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder();
    sb.append(label.asString());
    for (final String fname : sfv.names()) {
      sb.append("\t").append(fname.replaceAll(":", "-"));
      // scaling factor
      if (sfv.getByName(fname).doubleValue() != 1.0) {
        sb.append(":").append(sfv.getByName(fname));
      }
    }
    return sb.toString();
  }

  @Override
  public Double getById(int id) {
    return sfv.getById(id).doubleValue();
  }

  @Override
  public Double getByName(String name) {
    return sfv.getByName(name).doubleValue();
  }

  @Override
  public void addById(int id, Number value) {
    sfv.addById(id, value);
  }

  @Override
  public void addByName(String name, Number value) {
    sfv.addByName(name, value);
  }

  @Override
  public void forEachWithId(ObjIntConsumer<? super Number> action) {
    sfv.forEachWithId(action);
  }

  @Override
  public DenseFeatureVector<Number> toDense(Number emptyValue) {
    return sfv.toDense(emptyValue);
  }

  public DenseFeatureVector<Number> toDense() {
    return toDense(0d);
  }

  @Override
  public Iterator<Number> iterator() {
    return sfv.iterator();
  }

  @Override
  public int[] ids() {
    return sfv.ids();
  }

  @Override
  public String[] names() {
    return sfv.names();
  }

  @Override
  public int size() {
    return sfv.size();
  }
}
