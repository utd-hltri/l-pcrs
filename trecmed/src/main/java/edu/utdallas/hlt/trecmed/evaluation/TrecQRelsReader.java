
package edu.utdallas.hlt.trecmed.evaluation;

import com.google.common.base.CharMatcher;
import com.google.common.base.Splitter;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Table;
import com.google.common.collect.Tables;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import edu.utdallas.hlt.trecmed.Topic;
import edu.utdallas.hltri.logging.Logger;

/**
 *
 * @author travis
 */
public class TrecQRelsReader {
  transient private static final Logger log = Logger.get(TrecQRelsReader.class);
  /**
   * Enumeration of possible relevance values:
   * <dl>
   *  <dt>NONRELEVANT
   *  <dd>The visit was judged as not relevant for this question
   *
   *  <dt>PARTIAL
   *  <dd>The visit was judged as partially relevant for this question
   *      (this is considered relevant)
   *
   *  <dt>RELEVANT
   *  <dd>The visit was judged as relevant for this question
   *
   *  <dt>UNKNOWN
   *  <dd>The visit was not judged for this question
   * </dl>
   */
  public enum Relevance {
    NONRELEVANT,
    PARTIAL,
    RELEVANT,
    UNKNOWN;

    public int toInt() {
      switch(this) {
        case PARTIAL:
          return 1;
        case RELEVANT:
          return 2;
        case NONRELEVANT:
        case UNKNOWN:
        default:
          return 0;
      }
    }

    public int toCollapsedInt() {
      switch(this) {
        case PARTIAL:
        case RELEVANT:
          return 1;
        case NONRELEVANT:
        case UNKNOWN:
        default:
          return 0;
      }
    }

    public boolean toBoolean() {
      return toCollapsedInt() == 1;
    }
  }

  /*
   * Create a table that remembers the order of both the questions and
   * the visits when reading the question relevance files.
   *
   * This is necessary because MAP requires results being in order,
   * and we'd also like to be able to iterate questions in order.
   *
   * Because guava has no factory for a linked hash table, we have to do
   * it ourselves.
   */
  private final Table<String, String, Relevance> qrels = Tables.newCustomTable(
      new LinkedHashMap<>(),
          LinkedHashMap::new);
  private final Path path;

  public TrecQRelsReader(final String string) {
    this(Paths.get(string));
  }

  /**
   * Creates a QRels object from the question relevance file at the given Path
   * @param path Path containing the question relevances
   */
  public TrecQRelsReader(Path path) {
    this.path = path;
    parse();
  }

  private void parse() {
    final Relevance[] values = Relevance.values();
    final Splitter splitter = Splitter.on(CharMatcher.WHITESPACE).limit(5).omitEmptyStrings();
    try (BufferedReader reader = Files.newBufferedReader(path, Charset.defaultCharset())) {
      for (String line; (line = reader.readLine()) != null; ) {
        String[] fields = Iterables.toArray(splitter.split(line), String.class);
        try {
          qrels.put(fields[0], fields[2], values[Integer.valueOf(fields[3])]);
        } catch (ArrayIndexOutOfBoundsException ex) {
          qrels.put(fields[0], fields[2], Relevance.UNKNOWN);
        }
      }
    } catch (IOException ex) {
      throw new RuntimeException(ex);
    }
    log.debug("Parsed {} qrels.", qrels.size());
  }

  /**
   * Returns the relevance of the indicated visit for the given question
   * @param question Question id
   * @param documentId Visit id
   * @return Relevance of the desired visit for the given question
   */
  public Relevance getRelevance(String question, String documentId) {
    if (qrels.contains(question, documentId)) {
      return qrels.get(question, documentId);
    } else {
      return Relevance.UNKNOWN;
    }
  }

  /**
   * Returns the qrel information for the given question
   * @param question Question id
   * @return Map of qrel information
   */
  public Map<String, Relevance> findQuestion(String question) {
    return qrels.row(question);
  }

  /**
   * Returns the set of questions in this QRels object
   * @return Set of question ids
   */
  public Set<String> getQuestions() {
    return qrels.rowKeySet();
  }

  public Map<String, Relevance> getDocumentRelevances(Topic topic) { return getDocumentRelevances(
      topic.getId()); }
  public Map<String, Relevance> getDocumentRelevances(String question) {
    return qrels.row(question);
  }

  public List<String> getDocuments(String question) {
    return Lists.newArrayList(qrels.row(question).keySet());
  }


  public Set<String> getAllDocuments() {
    final Set<String> visits = new HashSet<>();
    for (final String question : getQuestions()) {
      visits.addAll(getDocumentRelevances(question).keySet());
    }
    return Collections.unmodifiableSet(visits);
  }

  public Path getPath() {
    return path;
  }
}
