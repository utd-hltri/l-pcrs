package edu.utdallas.hltri;

/**
 * Created by travis on 8/12/14.
 */
public interface Describable {
  default public String describe() {
    return toString();
  }
}
