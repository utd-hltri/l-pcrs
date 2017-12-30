package edu.utdallas.hltri.scribe.text;

import java.util.Optional;

/**
 * Created by travis on 8/13/14.
 */
public interface AttributeMap<B> {

  /**
   * Base type of keys for the map.  The classes that implement Key are
   * the keys themselves - not instances of those classes.
   *
   * @param <T> The type of the value associated with this key.
   */

  /**
   * Returns true if the map contains the given key.
   */
  public <T> boolean has(Attribute<? super B, T> key);

  /**
   * Returns the value associated with the given key or null if
   * none is provided.
   */
  public <T> T get(Attribute<? super B, T> key);
  default public <T>T apply(Attribute<? super B, T> key) {
    return get(key);
  }

  default public <T> T getOrElse(Attribute<? super B, T> key, T value) {
    if (!has(key)) {
      return value;
    }
    return get(key);
  }

  default public <T> Optional<T> getOptional(Attribute<? super B, T> key) {
    return Optional.ofNullable(get(key));
  }

  /**
   * Associates the given value with the given type for future calls
   * to getUnsafeAnnotations.  Returns the value removed or null if no value was present.
   */
  public <T> B set(Attribute<? super B, T> key, T value);
  default public <T> B update(Attribute<? super B, T> key, T value) {
    return set(key, value);
  }

  /**
   * Removes the given key from the map, returning the value removed.
   */
  public <T> B remove(Attribute<? super B, T> key);

  /**
   * Returns the number of keys in the map.
   */
  public int numAttributes();
}
