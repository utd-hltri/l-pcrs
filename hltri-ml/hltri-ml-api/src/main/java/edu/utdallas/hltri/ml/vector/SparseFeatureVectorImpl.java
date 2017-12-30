package edu.utdallas.hltri.ml.vector;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;

import java.util.Arrays;
import java.util.Iterator;
import java.util.function.ObjIntConsumer;
import java.util.stream.Collectors;

import edu.utdallas.hltri.util.IntIdentifier;

/**
 * Created by rmm120030 on 10/21/16.
 */
class SparseFeatureVectorImpl<K> implements SparseFeatureVector<K> {
  private final IntIdentifier<String> featureIdentifier;
  private final Int2ObjectMap<K> map = new Int2ObjectOpenHashMap<>();

  SparseFeatureVectorImpl(final IntIdentifier<String> featureIdentifier) {
    this.featureIdentifier = featureIdentifier;
  }

  @Override
  public K getById(int id) {
    return map.get(id);
  }

  @Override
  public K getByName(String name) {
    assert featureIdentifier.getID(name) > -1;
    return map.get(featureIdentifier.getID(name));
  }

  /**
   * @return an array containing only those feature ids whose values have been set in this FeatureVector
   */
  @Override
  public int[] ids() {
    return map.keySet().toIntArray();
  }

  /**
   * @return an array containing only those feature names whose values have been set in this FeatureVector
   */
  @Override
  //TODO: ask travis if this is how he wants this to function
  public String[] names() {
//    return Iterables.toArray(featureIdentifier.getItems(), String.class);
    return Arrays.stream(ids()).mapToObj(featureIdentifier::get).toArray(String[]::new);
  }

  @Override
  public int size() {
    assert featureIdentifier.isLocked() : "Call to FeatureVector.size but this FeatureVector's featureIdentifier is not locked.";
    return featureIdentifier.size();
  }

  @Override
  public void addById(int id, K value) {
    map.put(id, value);
  }

  @Override
  public void addByName(String name, K value) {
    map.put(featureIdentifier.getIDOrAdd(name), value);
  }

  @Override
  public Iterator<K> iterator() {
    return map.values().iterator();
  }

  //TODO: why no lambda?
  @Override
  public void forEachWithId(ObjIntConsumer<? super K> action) {
    map.forEach((b, a) -> action.accept(a, b));
  }

  @SuppressWarnings("unchecked")
  @Override
  public DenseFeatureVector<K> toDense(K emptyValue) {
    final int size = featureIdentifier.size();
    final String[] names = new String[ size ];
    final K[] values = (K[]) new Object [ size ];
    for (int i = 0; i < size; i++) {
      names[i] = featureIdentifier.get(i);
      values[i] = map.getOrDefault(i, emptyValue);
    }
    return new DenseFeatureVectorImpl<>(names, values, featureIdentifier);
  }

  @Override
  public String toString() {
    return "Dimension: " + featureIdentifier.size() + ". Set features: [" +
        Arrays.stream(ids()).mapToObj(i -> i + "").collect(Collectors.joining(", ")) +
        "]";
  }
}
