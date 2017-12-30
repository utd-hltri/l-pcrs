package edu.utdallas.hlt.trecmed.analysis;

import edu.utdallas.hlt.trecmed.Keyword;
import edu.utdallas.hlt.trecmed.KeywordExtractor;
import edu.utdallas.hlt.trecmed.Topic;
import edu.utdallas.hltri.inquire.ie.AgeExtractor;
import edu.utdallas.hltri.inquire.ie.GenderExtractor;
import java.io.Closeable;
import java.io.IOException;
import java.util.Collection;

/**
 *
 * @author travis
 */
public class TRECKeywordAnalyzer implements KeywordExtractor, Closeable {

  private final WikipediaKeywordExtractor wiki;
  private final AgeExtractor              age;
  private final GenderExtractor           gender;

  public TRECKeywordAnalyzer() {
    age = new AgeExtractor();
    wiki = new WikipediaKeywordExtractor().useFallback();
    gender = new GenderExtractor();
  }

  @Override
  public Topic extract(Topic topic) {
    topic.getKeywords().addAll(wiki.getKeywords(topic.getText()));
    wiki.condense(topic.getKeywords());
    for (final Keyword keyword : topic) {
      final String keywordText = keyword.getText().asRawString();
      final Collection<Keyword> subKeywords = wiki.getKeywords(keyword.getText());
      subKeywords.removeIf(subKeyword -> subKeyword.getText().asRawString().equals(keywordText));
      wiki.condense(subKeywords);
      keyword.addSubKeywords(subKeywords);
    }
    NegationAnalyzer.analyze(topic);
    age.extract(topic);
    gender.extract(topic);
    DisjunctionAnalyzer.filterOrGroups(topic);

    return topic;
  }

  @Override public void close() throws IOException {
    wiki.close();
    age.close();
    gender.close();
  }
}
