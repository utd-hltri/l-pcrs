package edu.utdallas.hltri.scribe.text;


/**
 * Created by travis on 8/15/14.
 */
public interface PropertyMap<B> {
  /**
   * Returns the value associated with the given key or null if
   * none is provided.
   */
  @SuppressWarnings("unchecked")
  default public <T> T get(Property<? super B, T > key) {
    return key.get((B) this);
  }
  default public <T> T apply(Property<? super B, T> key) {
    return get(key);
  }

  /**
   * Associates the given value with the given type for future calls
   * to getUnsafeAnnotations.  Returns the value removed or null if no value was present.
   */
  @SuppressWarnings("unchecked")
  default public <T> B set(Property<? super B, T> key, T value) {
    key.set((B) this, value);
    return (B) this;
  }
  default public <T> B update(Property<? super B, T> key, T value) {
    return set(key, value);
  }
}
