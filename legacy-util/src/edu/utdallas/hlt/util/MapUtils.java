package edu.utdallas.hlt.util;

import java.util.*;

/**
 *
 * @author bryan
 */
public class MapUtils {
  public static <K,V extends Comparable> List<K> sortKeys(final Map<K,V> map) {
    List<K> keys = new ArrayList<K>(map.keySet());
    Collections.sort(keys, new Comparator<K>() {
      @Override
      public int compare(K o1, K o2) {
        return map.get(o1).compareTo(map.get(o2));
      }
    });
    return keys;
  }
}
