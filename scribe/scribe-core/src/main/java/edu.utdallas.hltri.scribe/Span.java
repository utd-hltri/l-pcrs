package edu.utdallas.hltri.scribe;

/**
 * Created by trg19 on 8/27/2016.
 */
public class Span implements CharSequence, Comparable<CharSequence> {

  private String body;

  public Span(String body) {
    this.body = body;
  }

  @Override public int compareTo(CharSequence o) {
    int len1 = body.length();
    int len2 = o.length();
    int lim = Math.min(len1, len2);
    char v1[] = body.toCharArray();
    char v2[] = o.toString().toCharArray();

    int k = 0;
    while (k < lim) {
      char c1 = v1[k];
      char c2 = v2[k];
      if (c1 != c2) {
        return c1 - c2;
      }
      k++;
    }
    return len1 - len2;
  }

  @Override
  public int length() {
    return body.length();
  }

  public boolean isEmpty() {
    return body.isEmpty();
  }

  @Override
  public char charAt(int index) {
    return body.charAt(index);
  }

  public byte[] getBytes() {
    return body.getBytes();
  }

  public boolean contentEquals(StringBuffer sb) {
    return body.contentEquals(sb);
  }

  public boolean contentEquals(CharSequence cs) {
    return body.contentEquals(cs);
  }

  public boolean equalsIgnoreCase(String anotherString) {
    return body.equalsIgnoreCase(anotherString);
  }

  public int compareToIgnoreCase(String str) {
    return body.compareToIgnoreCase(str);
  }

  public boolean regionMatches(int toffset, String other, int ooffset, int len) {
    return body.regionMatches(toffset, other, ooffset, len);
  }

  public boolean regionMatches(boolean ignoreCase, int toffset, String other, int ooffset, int len) {
    return body.regionMatches(ignoreCase, toffset, other, ooffset, len);
  }

  public boolean startsWith(String prefix, int toffset) {
    return body.startsWith(prefix, toffset);
  }

  public boolean startsWith(String prefix) {
    return body.startsWith(prefix);
  }

  public boolean endsWith(String suffix) {
    return body.endsWith(suffix);
  }

  public int indexOf(int ch) {
    return body.indexOf(ch);
  }

  public int indexOf(int ch, int fromIndex) {
    return body.indexOf(ch, fromIndex);
  }

  public int lastIndexOf(int ch) {
    return body.lastIndexOf(ch);
  }

  public int lastIndexOf(int ch, int fromIndex) {
    return body.lastIndexOf(ch, fromIndex);
  }

  public int indexOf(String str) {
    return body.indexOf(str);
  }

  public int indexOf(String str, int fromIndex) {
    return body.indexOf(str, fromIndex);
  }

  public int lastIndexOf(String str) {
    return body.lastIndexOf(str);
  }

  public int lastIndexOf(String str, int fromIndex) {
    return body.lastIndexOf(str, fromIndex);
  }

  public String substring(int beginIndex) {
    return body.substring(beginIndex);
  }

  public String substring(int beginIndex, int endIndex) {
    return body.substring(beginIndex, endIndex);
  }

  @Override
  public CharSequence subSequence(int beginIndex, int endIndex) {
    return body.subSequence(beginIndex, endIndex);
  }

  public boolean contains(CharSequence s) {
    return body.contains(s);
  }

  public String toLowerCase() {
    return body.toLowerCase();
  }

  public String toUpperCase() {
    return body.toUpperCase();
  }

  public String trim() {
    return body.trim();
  }

  @Override
  public String toString() {
    return body;
  }

  public char[] toCharArray() {
    return body.toCharArray();
  }
}