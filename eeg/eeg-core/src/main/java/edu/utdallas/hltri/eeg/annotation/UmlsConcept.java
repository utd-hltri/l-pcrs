package edu.utdallas.hltri.eeg.annotation;

import java.io.Serializable;

/**
 * Created by rmm120030 on 9/24/15.
 */
public class UmlsConcept implements Serializable {
  private static final long serialVersionUID = 1l;

  private final String codingScheme;
  private final String code;
  private final String cui;
  private final String tui;

  private UmlsConcept(final String codingScheme, final String code, final String cui, final String tui) {
    this.codingScheme = codingScheme;
    this.code = code;
    this.cui = cui;
    this.tui = tui;
  }

  public static class Builder {
    private String codingScheme;
    private String code;
    private String cui;
    private String tui;

    public Builder codingScheme(final String codingScheme) {
      this.codingScheme = codingScheme;
      return this;
    }

    public Builder code(final String code) {
      this.code = code;
      return this;
    }

    public Builder cui(final String cui) {
      this.cui = cui;
      return this;
    }

    public Builder tui(final String tui) {
      this.tui = tui;
      return this;
    }

    public UmlsConcept build() {
      assert codingScheme != null : "coding scheme null";
      assert code != null : "code null";
      assert cui != null : "cui null";
      assert tui != null : "tui null";
      return new UmlsConcept(codingScheme, code, cui, tui);
    }
  }

  public String getCodingScheme() {
    return codingScheme;
  }

  public String getCode() {
    return code;
  }

  public String getCui() {
    return cui;
  }

  public String getTui() {
    return tui;
  }

  @Override
  public String toString() {
    return "UmlsConcept{" +
        "codingScheme='" + codingScheme + '\'' +
        ", code='" + code + '\'' +
        ", cui='" + cui + '\'' +
        ", tui='" + tui + '\'' +
        '}';
  }
}
