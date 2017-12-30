package edu.utdallas.hlt.trecmed.analysis;

import com.google.common.base.CharMatcher;
import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import edu.utdallas.hlt.text.Text;
import edu.utdallas.hlt.text.Token;
import edu.utdallas.hlt.trecmed.Keyword;
import edu.utdallas.hlt.trecmed.KeywordExtractor;
import edu.utdallas.hlt.trecmed.Topic;
import edu.utdallas.hlt.util.Config;
import edu.utdallas.hltri.io.ExternalBinarySearch;
import edu.utdallas.hltri.io.StusMagicLargeFileReader;
import edu.utdallas.hltri.logging.Logger;
import edu.utdallas.hltri.util.LongestSequenceIterable;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

/**
 *
 * @author travis
 */
public class WikipediaKeywordExtractor implements KeywordExtractor, Closeable {
  private static final Logger log = Logger.get(WikipediaKeywordExtractor.class);

  private final StusMagicLargeFileReader wikiTitles;

  private final CharMatcher matcher  = CharMatcher.javaLetterOrDigit()
      .or(CharMatcher.is('/')).negate();
  private final Splitter    splitter = Splitter.on(matcher).omitEmptyStrings();

  private boolean useFallback = false;

  WikipediaKeywordExtractor() {
    try {
      log.debug("Loading Wikipedia data...");
      wikiTitles = new StusMagicLargeFileReader(new File(Config.get(WikipediaKeywordExtractor.class, "WIKIPEDIA_TITLES").toString()));
      log.debug("Loading ignoreword data...");

    } catch (IOException ex) {
      throw new RuntimeException(ex);
    }
  }

  WikipediaKeywordExtractor useFallback() {
    this.useFallback = true;
    return this;
  }

  @Override public Iterable<Keyword> extract(Topic topic) {
    final Collection<Keyword> keywords = getKeywords(topic.getText());
    if (useFallback) {
      keywords.addAll(getFallbackKeywords(topic.getText(), keywords));
    }
    return keywords;
  }

  @Override public void close() throws IOException {
    wikiTitles.close();
  }

  void condense(Iterable<Keyword> keywords) {
    for (final Iterator<Keyword> it = keywords.iterator(); it.hasNext();) {
      final Keyword itKeyword = it.next();
      final String itString = itKeyword.asString().toLowerCase();

      boolean contains = false;
      for (final Keyword jtKeyword : keywords) {
        final String jtString = jtKeyword.asString().toLowerCase();

        // Compare against all other elements
        if (!jtString.equals(itString) && jtString.contains(itString)) {
          contains = true;
          break;
        }
      }

      if (contains) {
        it.remove();
      }
    }
  }

  public Collection<Keyword> getKeywords(Text text) {
    final Collection<Keyword> results = new HashSet<>();
    final List<Token> tokens = text.getTokens();

    for (final List<Token> seq : LongestSequenceIterable.of(tokens)) {
      final Text keywordText = seq.get(0).union(seq.get(seq.size() - 1));
      final String keyword = keywordText.asRawString().toLowerCase();

      if (CharMatcher.javaLetterOrDigit().matchesNoneOf(seq.get(0).asRawString()) ||
          CharMatcher.javaLetterOrDigit().matchesNoneOf(seq.get(seq.size() - 1).asRawString())) {
        continue;
      }

      if (isValidKeyword(keyword)) {
        results.add(new Keyword(keywordText));
      }
    }

    final List<Keyword> list = Lists.newArrayList(results);
    list.sort(new KeywordStartOffsetComparator());
    return list;
  }

  private boolean isValidKeyword(String keyword) {
    if (!Keyword.isValidKeyword(keyword)) { return false; }
    String prefix = splitter.split(keyword).iterator().next();
    if (prefix.length() == 0) { prefix = matcher.removeFrom(keyword); }
    final String formatted = matcher.replaceFrom(keyword, ' ');
    synchronized(wikiTitles) {
      for (final String word : ExternalBinarySearch.binarySearch(wikiTitles, prefix)) {
        if (matcher.replaceFrom(word, ' ').equalsIgnoreCase(formatted)) {
          return true;
        }
      }
    }
    return false;
  }

  private List<Keyword> getFallbackKeywords(final Text query, final Collection<Keyword> original) {
    final List<Keyword> keywords = Lists.newArrayList(original);
    final List<Keyword> fallback = new ArrayList<>();
    final List<Token> tokens = query.getTokens();
    keywords.sort(new KeywordStartOffsetComparator());

    int start = 0;
    for (final Keyword keyword : keywords) {
      final int end = keyword.getText().getStartTokenOffset();
      if (end > 0) {
        for (final List<Token> seq : LongestSequenceIterable.of(tokens.subList(start, end))) {
            final Text keywordText = seq.get(0).union(seq.get(seq.size() - 1));
            final String string = keywordText.asRawString().toLowerCase();
            boolean skip = false;

            for (final Token tok : seq) {
              if (!Keyword.isValidKeyword(tok.asRawString())) {
                skip = true;
                break;
              }
            }

            log.trace("Checking sequence |{}| for fallback getKeywords", string);
            if (!skip) {
              log.debug("Added fallback keyword {} (PMC of {})", keywordText);
              fallback.add(new Keyword(keywordText).setOptional());
            }
        }
      }
      start = keyword.getText().getEndTokenOffset();
    }
    condense(fallback);
    return fallback;
  }

  static class KeywordStartOffsetComparator implements Comparator<Keyword> {
    @Override public int compare(final Keyword o1, final Keyword o2) {
      return Integer.compare(o1.getText().getStartTokenOffset(), o2.getText().getStartTokenOffset());
    }
  }
}


