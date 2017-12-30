package edu.utdallas.hltri.util;

import java.util.*;
import java.util.Map.Entry;

/**
 *
 * @author bryan
 */
@SuppressWarnings("unused")
public class Maps {

  public static <K,V> List<K> sortKeysByMapValues(final Map<K,V> map, final Comparator<? super V> comparator) {

    final List<Map.Entry<K,V>> sortedEntries = new ArrayList<>(map.entrySet());
    Collections.sort(sortedEntries, new Comparator<Map.Entry<K,V>>() {
      @Override public int compare(Entry<K, V> o1, Entry<K, V> o2) {
        return comparator.compare(o1.getValue(), o2.getValue());
      }
    });

    final List<K> sortedKeys = new ArrayList<>(sortedEntries.size());
    for (final Map.Entry<K,V> entry : sortedEntries) {
      sortedKeys.add(entry.getKey());
    }

    return sortedKeys;
  }
  
  public static <K, V extends Comparable<V>> List<K> sortKeys(final Map<K, V> map) {
    final List<K> keys = new ArrayList<>(map.keySet());

    Collections.sort(keys, new Comparator<K>() {
      @Override public int compare(final K o1, final K o2) {
        return map.get(o1).compareTo(map.get(o2));
      }
    });
    return keys;
  }
}
