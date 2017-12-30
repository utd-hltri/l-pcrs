package edu.utdallas.hltri.ml;

/**
 * User: bryan
 * Date: 12/18/12
 * Time: 8:59 AM
 * Created with IntelliJ IDEA.
 */
public interface MLConfig {
  public void put(String key, String value);
  public String get(String key);
}
