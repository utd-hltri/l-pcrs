package edu.utdallas.hlt.trecmed.offline;

import com.google.common.base.CharMatcher;
import com.google.common.collect.ImmutableMap;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.AnalyzerWrapper;
import org.apache.lucene.analysis.core.KeywordAnalyzer;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.analysis.miscellaneous.PerFieldAnalyzerWrapper;
import org.apache.lucene.util.Version;

import edu.utdallas.hltri.inquire.lucene.CharMatcherAnalyzer;

/**
 *
 * @author travis
 */
public final class LuceneEMRAnalyzer extends AnalyzerWrapper {
  public static final Version LUCENE_VERSION = Version.LUCENE_5_2_1;
  private static final Analyzer LUCENE_ANALYZER;
  static {
    final Analyzer keyword = new KeywordAnalyzer();
    final Analyzer comma = new CharMatcherAnalyzer(CharMatcher.is(','));
    LUCENE_ANALYZER = new PerFieldAnalyzerWrapper(new EnglishAnalyzer(),
            ImmutableMap.<String, Analyzer>builder()
            .put("file_name", keyword)
            .put("checksum", keyword)
            .put("visit_id", keyword)
            .put("year", keyword)
            .put("discharge_diagnosis", comma)
            .put("admit_diagnosis", comma)
            .build());
  }

  public LuceneEMRAnalyzer() {
    super(AnalyzerWrapper.PER_FIELD_REUSE_STRATEGY);
  }

  @Override
  protected Analyzer getWrappedAnalyzer(String fieldName) {
    return LUCENE_ANALYZER;
  }

  @Override
  protected TokenStreamComponents wrapComponents(String fieldName, TokenStreamComponents components) {
    return components;
  }


}
