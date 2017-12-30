package edu.utdallas.hlt.trecmed.expansion;

import com.google.common.base.CharMatcher;
import edu.utdallas.hltri.util.AbstractExpander;
import java.util.Collection;
import java.util.TreeSet;

/**
* Created by travis on 7/11/14.
*/
public final class PunctuationExpander extends AbstractExpander<CharSequence, String> {
  public static final PunctuationExpander INSTANCE = new PunctuationExpander();

  private PunctuationExpander() {
    super("Punctuation");
  }

  private final CharMatcher hyphen = CharMatcher.is('-');
  private final CharMatcher english = CharMatcher.whitespace().or(CharMatcher.javaLetterOrDigit());

  @Override public Collection<String> getExpansions(CharSequence term) {
    final Collection<String> forms = new TreeSet<>();

    if (hyphen.indexIn(term) > -1) {
      forms.add(hyphen.replaceFrom(term, " - "));
      forms.add(hyphen.replaceFrom(term, " "));
      forms.add(hyphen.removeFrom(term));
    }

    forms.add(english.retainFrom(term));

    forms.remove(term.toString());

    return forms;
  }
}
