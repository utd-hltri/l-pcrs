package edu.utdallas.hltri.ml.vector;

import java.util.function.ObjIntConsumer;

/**
 * Feature Vector
 * Created by ramon on 10/20/2016.
 */
public interface FeatureVector<K> extends Iterable<K> {
  /**
   * Returns the value of the feature at index id
   * @param id id
   * @return the value of the feature at index id
   */
  K getById(int id);

  /**
   * Returns the value of the feature with name name
   * @param name name
   * @return the value of the feature with name name
   */
  K getByName(String name);

  /**
   * Returns the
   * @return
   */
  int[] ids();
  String[] names();

  /**
   * Returns the dimension of the vector. Should be the same for sparse and dense representations.
   * @return the dimension of the vector
   */
  int size();

  void forEachWithId(ObjIntConsumer<? super K> action);
}
