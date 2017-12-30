package edu.utdallas.hltri.ml;

import java.io.Serializable;

/**
 * Created by ramon on 2/25/16.
 */
public interface Label extends Serializable {
  static final long serialVersionUID = 1L;
  int numLabels();
  default Number numericValue() {
    return ordinal();
  }
  int ordinal();

  public static Label NULL = new Label() {
    @Override
    public int numLabels() {
      return 0;
    }

    @Override
    public Number numericValue() {
      return null;
    }

    @Override
    public int ordinal() {
      return -1;
    }
  };
}
