package edu.utdallas.hlt.trecmed.ranking;

import com.google.common.base.Joiner;
import com.google.common.collect.Sets;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import edu.utdallas.hlt.text.annotators.LexiconMatcher;
import edu.utdallas.hlt.trecmed.Keyword;
import edu.utdallas.hlt.trecmed.Topic;
import edu.utdallas.hltri.struct.Weighted;

/**
 *
 * @author travis
 */
public class QueryLexiconAdapter {
  public LexiconMatcher adapt(final Topic... queries) {
    try {
      final Map<String, String> trie = new HashMap<>();
      final Collection<String> ids = new HashSet<>();
      for (final Topic topic : queries) {
        ids.add(topic.getId());
        for (Keyword keyword : topic) {
          final String category = keyword.asString();
          final Collection<Weighted<String>> expansions =
              Sets.newHashSet(keyword.getSafeWeightedTerms());
          for (Keyword subKeyword : keyword.getSubKeywords()) {
            expansions.addAll(subKeyword.getWeightedTerms());
          }
          for (Weighted<String> expansion : expansions) {
            trie.put(expansion.value, category);
          }
        }
      }

      return new LexiconMatcher(
          Joiner.on(',').join(ids),
          trie,
          LexiconMatcher.Option.CASE_INSENSITIVE,
          LexiconMatcher.Option.STEM_WORDS);
    } catch (IOException ex) {
      throw new RuntimeException(ex);
    }
  }
}
