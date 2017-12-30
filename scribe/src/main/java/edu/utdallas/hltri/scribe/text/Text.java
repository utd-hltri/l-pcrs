package edu.utdallas.hltri.scribe.text;

import com.google.common.base.CharMatcher;

import edu.utdallas.hltri.Describable;

/**
 * Created by travis on 7/15/14.
 */
public abstract class Text implements CharSequence, Describable, Comparable<CharSequence> {

  /**
   * View the surface-level String representation of this Object
   * Differs from toString which may instead return a human-readable String for debugging
   * @return String raw surface-level String corresponding to this object
   */
  public abstract String asString();

  @Override public int length() {
    return asString().length();
  }

  @Override public char charAt(int index) {
    return asString().charAt(index);
  }

  @Override public CharSequence subSequence(int start, int end) {
    return asString().subSequence(start, end);
  }

  @Override public final String toString() {
    return asString();
  }

  @Override public int compareTo(CharSequence o) {
    return toString().compareTo(o.toString());
  }

  public String toCollapsedLowercase() {
    return toCollapsedLowercase(' ');
  }

  public String toCollapsedLowercase(char c) {
    return CharMatcher.WHITESPACE.collapseFrom(asString().toLowerCase(), c);
  }
}
