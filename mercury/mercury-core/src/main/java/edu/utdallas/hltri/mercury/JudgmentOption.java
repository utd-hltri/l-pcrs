package edu.utdallas.hltri.mercury;

/**
 * Created by travis on 10/13/16.
 */
@SuppressWarnings({"unused", "WeakerAccess"})
public class JudgmentOption {
  private final int judgment;
  private final String label;
  private final String description;

  public JudgmentOption(int judgment, String label, String description) {
    this.judgment = judgment;
    this.label = label;
    this.description = description;
  }

  public int getJudgment() {
    return judgment;
  }

  public String getLabel() {
    return label;
  }

  public String getDescription() {
    return description;
  }

  @Override
  public String toString() {
    return "#" + judgment +
        ": '" + label + '\'' +
        ": '" + description + '\'';
  }
}
