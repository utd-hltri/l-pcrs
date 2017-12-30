package edu.utdallas.hltri.scribe.text.relation;

import java.util.Optional;

import edu.utdallas.hltri.Describable;
import edu.utdallas.hltri.scribe.text.Attribute;
import edu.utdallas.hltri.scribe.text.Document;
import edu.utdallas.hltri.scribe.text.annotation.Annotation;

public interface Relation<R extends Relation<R, G, D>, G extends Annotation<G>, D extends Annotation<D>> extends Describable {

//  Attribute<Relation<?,?,?>, String> id = Attribute.typed("id", String.class);

  gate.relations.Relation asGate();
  G getGovernor();
  D getDependant();
  Document<?> getDocument();

//  /**
//   * For setting the features of this relation from an unsafe relation that is being wrapped
//   * @param features the unsafe relation's feature map
//   */
//  void setFeatures(Map<String,Object> features);

  default int getId() {
    return asGate().getId();
  }

  /**
   * Returns true if the map contains the given key.
   */
  default <T> boolean has(Attribute<? super R, T> key) {
    return asGate().getFeatures().containsKey(key.name);
  }

  /**
   * Returns the value associated with the given key or null if
   * none is provided.
   */
  default <T> T get(Attribute<? super R, T> key) {
    return key.type.cast(asGate().getFeatures().get(key.name));
  }

  default <T>T apply(Attribute<? super R, T> key) {
    return get(key);
  }

  default <T> T getOrElse(Attribute<? super Relation<R,G,D>, T> key, T value) {
    if (!has(key)) {
      set(key, value);
    }
    return get(key);
  }

  default <T> Optional<T> getOptional(Attribute<? super R, T> key) {
    return Optional.ofNullable(get(key));
  }

  /**
   * Associates the given value with the given type for future calls
   * to getUnsafeAnnotations.  Returns the value removed or null if no value was present.
   */
  default <T> Relation<R,G,D> set(Attribute<? super R, T> key, T value) {
//    key.setter.accept(this, value);
    asGate().getFeatures().put(key.name, value);
    return this;
  }

  default <T> Relation<R,G,D> update(Attribute<? super R, T> key, T value) {
    return set(key, value);
  }

  /**
   * Removes the given key from the map, returning the value removed.
   */
  default <T> Relation<R,G,D> remove(Attribute<? super R, T> key) {
    asGate().getFeatures().remove(key);
    return this;
  }
}
