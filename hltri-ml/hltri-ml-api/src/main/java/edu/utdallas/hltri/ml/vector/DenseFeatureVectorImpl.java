package edu.utdallas.hltri.ml.vector;

import java.util.Arrays;
import java.util.Iterator;
import java.util.function.ObjIntConsumer;

import edu.utdallas.hltri.util.IntIdentifier;

/**
 * Created by travis on 11/3/16.
 */
public class DenseFeatureVectorImpl<K> implements DenseFeatureVector<K> {
  final int size;

  final String[] names;
  final K[] values;

  private final IntIdentifier<String> featureIdentifier;

  DenseFeatureVectorImpl(String[] names, K[] values, IntIdentifier<String> featureIdentifier) {
    this.size = featureIdentifier.size();
    this.names = names;
    this.values = values;
    this.featureIdentifier = featureIdentifier;
  }

  @Override
  public void setById(int id, K value) {
    values[id] = value;
  }

  @Override
  public K getById(int id) {
    return values[id];
  }

  @Override
  public void setByName(String name, K value) {
    values[featureIdentifier.getID(name)] = value;
  }

  @Override
  public K getByName(String name) {
    return values[featureIdentifier.getID(name)];
  }

  @Override
  public int[] ids() {
    int[] ids = new int[ size ];
    for (int i = 0; i < size; i++) {
      ids[i] = i;
    }
    return ids;
  }

  @Override
  public String[] names() {
    return names;
  }

  @Override
  public Iterator<K> iterator() {
    return Arrays.asList(values).iterator();
  }

  @Override
  public int size() {
    return size;
  }

  @Override
  public void forEachWithId(ObjIntConsumer<? super K> action) {
    for (int i = 0; i < size; i++) {
      action.accept(values[i], i);
    }
  }
}
