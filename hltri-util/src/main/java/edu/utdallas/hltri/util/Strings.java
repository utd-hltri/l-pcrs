package edu.utdallas.hltri.util;

/**
 * Created by travis on 7/20/15.
 */
public final class Strings {
  private Strings() {}


  public static String substringUntil(final String str, final String delim) {
    return substringUntil(str, 0, delim);
  }

  public static String substringUntil(final String str, final char delim) {
    return substringUntil(str, 0, delim);
  }

  public static String substringUntil(final String str, final int start, final char delim) {
    final int index = str.indexOf(delim);
    if (index < 0) return str;
    else return str.substring(start, index);
  }

  public static String substringUntil(final String str, final int start, final String delim) {
    final int index = str.indexOf(delim);
    if (index < 0) return str;
    else return str.substring(start, index);
  }

}
