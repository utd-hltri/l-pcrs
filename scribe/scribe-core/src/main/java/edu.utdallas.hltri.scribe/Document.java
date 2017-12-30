package edu.utdallas.hltri.scribe;

import javax.annotation.Nonnull;

import edu.utdallas.hltri.Describable;

/**
 * Created by trg19 on 8/27/2016.
 */
public class Document extends Text implements FeatureBearer, Identifiable, Describable {

  @Feature("name")
  private final String name;

  private final int id;

  public Document(String body, String name, int id) {
    super(body);
    this.name = name;
    this.id = id;
  }

  public Document(String body, int id) {
    this(body, "D#" + id, id);
  }

  @Nonnull @Override
  public String getId() {
    return name;
  }

  @Override public long getNumericId() {
    return id;
  }
}
