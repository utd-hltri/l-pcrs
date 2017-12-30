package edu.utdallas.hltri.mercury.jamia;

import edu.utdallas.hltri.scribe.text.Identifiable;

/**
 * Created by travis on 11/8/16.
 */
public class CohortPatient implements Identifiable {
  private final String id;

  public CohortPatient(String id) {
    this.id = id;
  }

  @Override
  public String getId() {
    return id;
  }
}
