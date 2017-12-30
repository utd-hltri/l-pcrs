package edu.utdallas.hltri.ml.vector;

import java.util.function.ObjIntConsumer;

/**
 * Created by rmm120030 on 10/21/16.
 */
public interface SparseFeatureVector<K> extends FeatureVector<K> {
  void addById(int id, K value);
  void addByName(String name, K value);
  void forEachWithId(ObjIntConsumer<? super K> action);

  DenseFeatureVector<K> toDense(K emptyValue);
}
