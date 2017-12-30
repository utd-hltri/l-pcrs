package edu.utdallas.hltri.ml.feature;

import java.util.Objects;
import java.util.function.Function;

import edu.utdallas.hltri.ml.label.Label;

/**
 * Created by TREVOR on 8/10/2016.
 */
public class ResettableIncrementalLabeller<T> implements Function<T, Label> {

  private int last = 0;



  @Override
  public Label apply(T t) {

    final Label label = new Label() {
      @Override
      public int asInt() {
        return id;
      }

      @Override
      public String asString() {
        return Objects.toString(id);
      }

      final int id = last;
    };
    last++;
    return label;
  }

  public void reset() {
    last = 0;
  }
}
