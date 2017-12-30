package edu.utdallas.hltri.util;

import com.google.common.base.CharMatcher;

/**
 * Created by travis on 7/23/14.
 */
@SuppressWarnings("unused")
public abstract class CharMatchers {
  private CharMatchers() { /* Empty */ }

  public static final CharMatcher PUNCTUATION = new CharMatcher() {
    @Override public String toString() {
      return "PunctuationMatcher";
    }

    @Override public boolean matches(char c) {
      switch (Character.getType(c)) {
        case Character.START_PUNCTUATION:
        case Character.END_PUNCTUATION:
        case Character.OTHER_PUNCTUATION:
        case Character.CONNECTOR_PUNCTUATION:
        case Character.DASH_PUNCTUATION:
        case Character.INITIAL_QUOTE_PUNCTUATION:
        case Character.FINAL_QUOTE_PUNCTUATION:
          return true;
        default:
          return false;
      }
    }
  };

  public static final CharMatcher LETTER = CharMatcher.inRange('a', 'z').or(CharMatcher.inRange('A', 'Z')).precomputed();

  public static final CharMatcher DIGIT = CharMatcher.inRange('0', '9').precomputed();


  public static final CharMatcher SYMBOL = new CharMatcher() {
    @Override public String toString() {
      return "SymbolMatcher";
    }

    @Override public boolean matches(char c) {
      switch (Character.getType(c)) {
        case Character.CURRENCY_SYMBOL:
        case Character.MATH_SYMBOL:
        case Character.OTHER_SYMBOL:
          return true;
        default:
          return false;
      }
    }
  };
}
