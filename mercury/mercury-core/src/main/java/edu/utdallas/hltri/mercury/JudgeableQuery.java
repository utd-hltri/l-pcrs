package edu.utdallas.hltri.mercury;

import java.util.List;

/**
 * Created by travis on 10/13/16.
 */
@SuppressWarnings("WeakerAccess")
public class JudgeableQuery {
  private final String qid;
  private final String query;
  private final List<JudgeableReport> reports;

  public JudgeableQuery(String qid, String query, List<JudgeableReport> reports) {
    this.qid = qid;
    this.query = query;
    this.reports = reports;
  }

  public String getQueryId() {
    return qid;
  }

  public String getQuery() {
    return query;
  }

  public List<JudgeableReport> getReports() {
    return reports;
  }
}
