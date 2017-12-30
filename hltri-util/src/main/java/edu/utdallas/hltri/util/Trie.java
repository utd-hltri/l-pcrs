package edu.utdallas.hltri.util;

import java.util.Map;
import java.util.TreeMap;

@SuppressWarnings("unused")
public class Trie<K> implements java.io.Serializable {
  private static final long serialVersionUID = 1L;

  final protected Map<K, Trie<K>> children = new TreeMap<>();

  public void add(K key, Trie<K> node) {
    children.put(key, node);
  }

  public Trie<K> get(K key) {
    Trie<K> ret = children.get(key);
    assert ret != this;
    return ret;
  }

  public Trie<K> getOrAdd(K key) {
    if (get(key) == null) {
      Trie<K> node = new Trie<>();
      add(key, node);
      return node;
    }
    return get(key);
  }

  public boolean isLeaf() {
    return children.isEmpty();
  }

  @Override
  public String toString() {
    return "Trie{" + children + '}';
  }
}
