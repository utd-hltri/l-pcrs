package edu.utdallas.hltri.mercury;

/**
 * Created by travis on 10/13/16.
 */
@SuppressWarnings({"WeakerAccess", "unused"})
public class JudgeableReport {
  private final String rid;
  private int judgment;
  private final String explanation;

  public JudgeableReport(String rid, int judgment, String explanation) {
    this.rid = rid;
    this.judgment = judgment;
    this.explanation = explanation;
  }

  public String getReportId() {
    return rid;
  }

  public int getJudgment() {
    return judgment;
  }

  public String getExplanation() {
    return explanation;
  }
}
