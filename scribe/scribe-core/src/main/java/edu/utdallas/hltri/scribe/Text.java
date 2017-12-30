package edu.utdallas.hltri.scribe;

import com.google.common.base.CharMatcher;

import java.util.Map;
import java.util.Set;

import edu.utdallas.hltri.Describable;

/**
 * Created by trg19 on 8/14/2016.
 */
public class Text extends AnnotationBearer implements CharSequence, Comparable<CharSequence>, Describable {
  protected final String content;

  public Text(String content) {
    this.content = content;
  }

  @Override public int length() {
    return toString().length();
  }

  @Override public char charAt(int index) {
    return toString().charAt(index);
  }

  @Override public CharSequence subSequence(int start, int end) {
    return toString().subSequence(start, end);
  }

  @Override public final String toString() {
    return content;
  }

  @Override public int compareTo(CharSequence o) {
    return toString().compareTo(o.toString());
  }

  public String toCollapsedLowercase() {
    return toCollapsedLowercase(' ');
  }

  public String toCollapsedLowercase(char c) {
    return CharMatcher.WHITESPACE.collapseFrom(toString().toLowerCase(), c);
  }

  @Override public String describe() {
    final StringBuilder sb = new StringBuilder();
    sb.append("Text:\n").append(toString()).append('\n');
    sb.append("Annotations:\n").append(((AnnotationBearer) this).describe()).append("\n}\n");
    return sb.toString();
  }
}
