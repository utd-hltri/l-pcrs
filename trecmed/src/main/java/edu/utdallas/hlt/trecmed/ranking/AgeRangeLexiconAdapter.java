package edu.utdallas.hlt.trecmed.ranking;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import edu.utdallas.hlt.text.annotators.LexiconMatcher;
import edu.utdallas.hltri.inquire.ie.AgeExtractor.AgeRange;
import edu.utdallas.hltri.logging.Logger;

/**
 *
 * @author travis
 */
public class AgeRangeLexiconAdapter {
  private static final Logger log = Logger.get(AgeRangeLexiconAdapter.class);
  public static LexiconMatcher adapt(final AgeRange range) {
    final Map<String, String> trie = new HashMap<>();
    for (int i = AgeRange.LOWER_BOUND; i < AgeRange.UPPER_BOUND; i += 10) {
      if (range.getStart() <= i && i <= range.getEnd()) {
        trie.put(AgeRange.format(i), "true");
      } else {
        trie.put(AgeRange.format(i), "false");
      }
    }
    log.trace("Created trie {} = {}", range.toString(), trie);
    try {
      return new LexiconMatcher(range.toString(), trie);
    } catch (IOException ex) {
      throw new RuntimeException(ex);
    }
  }
}
