package edu.utdallas.hltri.scribe.text;

import java.util.function.Function;

import gate.Factory;
import gate.creole.ResourceInstantiationException;

/**
 * Created by travis on 2/20/15.
 */
@FunctionalInterface
public interface DocumentFactory<D extends BaseDocument> {
  public Document<D> wrap(gate.Document gateDocument);

  default public Document<D> create(String str) {
    try {
      return wrap(Factory.newDocument(str));
    } catch (ResourceInstantiationException e) {
      throw new RuntimeException(e);
    }
  }
}
