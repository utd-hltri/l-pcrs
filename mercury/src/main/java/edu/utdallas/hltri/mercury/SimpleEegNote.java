package edu.utdallas.hltri.mercury;

/**
 * Created by travis on 9/29/16.
 */
public class SimpleEegNote {

  private final String id;

  private final String path;

  private final String text;

  public SimpleEegNote(String id, String path, String text) {
    this.id = id;
    this.path = path;
    this.text = text;
  }

  public String getId() {
    return id;
  }

  public String getPath() {
    return path;
  }

  public String getText() {
    return text;
  }
}
