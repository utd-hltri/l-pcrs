package edu.utdallas.hltri.struct;

import java.io.Serializable;

/**
 * User: bryan
 * Date: 5/13/13
 * Time: 5:10 PM
 * Created with IntelliJ IDEA.
 */
@SuppressWarnings("unused")
public class Pair<K,V> implements Serializable {

  private static final long serialVersionUID = 1L;

  private K k;
  private V v;

  public Pair(K k, V v) {
    this.k = k;
    this.v = v;
  }

  public static <K,V> Pair<K,V> of(K key, V value) {
    return new Pair<>(key, value);
  }


  public K key() { return k; }
  public K first() { return k; }

  public V value() { return v; }
  public V second() { return v; }

  @Override
  public int hashCode() {
    int result = k != null ? k.hashCode() : 0;
    result = 31 * result + (v != null ? v.hashCode() : 0);
    return result;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    Pair<?, ?> pair = (Pair<?, ?>) o;

    if (k != null ? !k.equals(pair.k) : pair.k != null) return false;
    if (v != null ? !v.equals(pair.v) : pair.v != null) return false;

    return true;
  }

  @Override
  public String toString() {
    return "Pair{" +
        "k=" + k +
        ", v=" + v +
        '}';
  }
}
