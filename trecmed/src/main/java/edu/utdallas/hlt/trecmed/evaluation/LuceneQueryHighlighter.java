package edu.utdallas.hlt.trecmed.evaluation;

import com.google.common.base.CharMatcher;
import com.google.common.base.Strings;

import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.vectorhighlight.FastVectorHighlighter;
import org.apache.lucene.search.vectorhighlight.FieldQuery;
import org.apache.lucene.search.vectorhighlight.SingleFragListBuilder;
import org.apache.lucene.store.NIOFSDirectory;

import java.nio.file.Paths;

import edu.utdallas.hlt.trecmed.Report;
import edu.utdallas.hlt.trecmed.Topic;
import edu.utdallas.hlt.util.Config;
import edu.utdallas.hltri.logging.Logger;

/**
 *
 * @author travis
 */
public class LuceneQueryHighlighter {
  private static final Logger log = Logger.get(LuceneQueryHighlighter.class);

  private static final IndexReader reader;

  static {
    try {
      reader = DirectoryReader.open(NIOFSDirectory.open(
          Paths.get(Config.get(LuceneQueryHighlighter.class, "PATH").toString())));
    } catch (Exception ex) {
      throw new RuntimeException(ex);
    }
  }

  private final FastVectorHighlighter highlighter;
  private final FieldQuery fieldQuery;

  public LuceneQueryHighlighter(final Topic topic) {
    this.highlighter = new FastVectorHighlighter(
              true,
              false,
              new SingleFragListBuilder(),
              new ExpansionHTMLFragmentBuilder(topic));

    this.fieldQuery = highlighter.getFieldQuery(topic.asLucenePhraseQuery());
  }

  @SuppressWarnings("unused") // Used in Velocity template
  public String highlightReport(final Report report) {
    try {
      String text = highlighter.getBestFragment(
      fieldQuery,
      reader,
      report.getLuceneId(),
      "report_text",
      Integer.MAX_VALUE);

      if (Strings.isNullOrEmpty(text)) {
        text = report.getDocument().asRawString();
      }

      return CharMatcher.WHITESPACE.trimTrailingFrom(text);
    } catch (Exception ex) {
      log.error("Highlighting failed", ex);
      throw new RuntimeException(ex);
    }
  }
}
