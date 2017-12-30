package edu.utdallas.hltri.ml.vector;

/**
 * Created by rmm120030 on 10/21/16.
 */
public interface DenseFeatureVector<K> extends FeatureVector<K> {
  void setById(int id, K value);
  void setByName(String name, K value);

  int size();
}
