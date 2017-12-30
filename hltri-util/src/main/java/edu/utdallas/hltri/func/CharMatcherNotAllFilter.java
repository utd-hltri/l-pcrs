package edu.utdallas.hltri.func;

import com.google.common.base.CharMatcher;

import javax.annotation.Nullable;

import edu.utdallas.hltri.logging.Logger;

/**
 * Created by travis on 7/23/14.
 */
public class CharMatcherNotAllFilter implements CloseablePredicate<CharSequence> {
  private static final Logger log = Logger.get(CharMatcherNotAllFilter.class);

  final CharMatcher matcher;

  public CharMatcherNotAllFilter(final CharMatcher matcher) {
    this.matcher = matcher;
  }

  @Override public boolean test(@Nullable CharSequence input) {
    final String string = input.toString();
    if (matcher.matchesAllOf(string)) {
      log.trace("|{}| matched {}", string, matcher);
      return false;
    }
    return true;
  }

  @Override public String toString() {
    return "NotAll(" + matcher.toString() + ")";
  }
}
