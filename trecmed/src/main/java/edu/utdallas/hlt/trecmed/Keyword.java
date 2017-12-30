package edu.utdallas.hlt.trecmed;

import com.google.common.base.CharMatcher;
import com.google.common.collect.Iterables;
import edu.utdallas.hlt.text.StopWords;
import edu.utdallas.hlt.text.Text;
import edu.utdallas.hlt.trecmed.retrieval.LuceneEMRSearcher;
import edu.utdallas.hlt.trecmed.retrieval.LucenePhraseConverter;
import edu.utdallas.hlt.trecmed.retrieval.LuceneSpanPhraseConverter;
import edu.utdallas.hltri.logging.Logger;
import edu.utdallas.hltri.struct.Weighted;
import edu.utdallas.hltri.util.Expansion;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.spans.SpanBoostQuery;
import org.apache.lucene.search.spans.SpanNearQuery;
import org.apache.lucene.search.spans.SpanOrQuery;
import org.apache.lucene.search.spans.SpanQuery;


/**
 *
 * @author travis
 */
public class Keyword extends edu.utdallas.hltri.scribe.text.Text implements Serializable {
  private static final Logger log = Logger.get(Keyword.class);

  private static final long serialVersionUID = 1L;

  static {
    BooleanQuery.setMaxClauseCount(8192);
  }

  public static boolean isValidKeyword(String word) {
    if (word.length() < 2) {
      return false;
    } else if (StopWords.isStopWord(word) || StopWords.isPronoun(word)) {
      return false;
    } else if (CharMatcher.javaLetterOrDigit().matchesNoneOf(word)) {
      return false;
    }
    return true;
  }

  final transient private Text   text;
  private final String string;

  private static float DEFAULT_BOOST = 16f;
//  public static float DEFAULT_SCALE = 1f;

  private float scale = DEFAULT_BOOST;
//  private float boost = DEFAULT_SCALE;

  private boolean isDisjunction = false;
  private boolean isRequired    = true;
  private boolean isNegation    = false;

  private List<Keyword>             disjunctionGroup = new ArrayList<>();
  private final Collection<Keyword>       subKeywords      = new ArrayList<>();
  private final Collection<Expansion<Weighted<String>>> expansions       = new ArrayList<>();

  private BooleanQuery bq = null;

  public Keyword(Text text) {
    this.text = text;
    this.string = text.asRawString().toLowerCase(); // TODO: check lowercase
  }

  @SuppressWarnings("UnusedReturnValue")
  public Keyword addSubKeywords(Collection<Keyword> keywords) {
    subKeywords.addAll(keywords);
    if (!subKeywords.isEmpty()) {
      log.info("Keyword {} added sub-getKeywords: {}", this, keywords);
    }
    return this;
  }

//  Keyword setBoost(float boost) {
//    this.boost = boost;
//    return this;
//  }

  public Keyword setOptional() {
    this.isRequired = false;
    return this;
  }

  @SuppressWarnings("UnusedReturnValue")
  public Keyword setNegated() {
    this.isNegation = true;
    return this;
  }

  public boolean isRequired() {
    return isRequired;
  }

  public boolean isNegation() {
    return isNegation;
  }

  boolean isDisjunction() {
    return isDisjunction;
  }

//  public float getBoost() {
//    return boost;
//  }

  public void setDisjunctionGroup(List<Keyword> disjunctionGroup) {
    this.disjunctionGroup = disjunctionGroup;
  }

  List<Keyword> getDisjunctionGroup() {
    return disjunctionGroup;
  }

  @Override public String asString() {
    return string;
  }

  public Set<Keyword> getSubKeywords() {
    return new LinkedHashSet<>(subKeywords);
  }

  public Set<Weighted<String>> getWeightedTerms() {
    final Set<Weighted<String>> set = new HashSet<>();
    for (Expansion<Weighted<String>> exp : expansions) {
      set.addAll(exp);
    }
    return set;
  }

  public Set<Weighted<String>> getSafeWeightedTerms() {
    return getWeightedTerms().stream()
        .filter(Weighted.newThresholdPredicate(6.0))
        .collect(Collectors.toSet());
  }

  public Collection<Expansion<Weighted<String>>> getExpansions() {
    return expansions;
  }

  Expansion<Weighted<String>> getExpansion(String source) {
    for (Expansion<Weighted<String>> exp : expansions) {
      if (exp.getName().equals(source)) {
        return exp;
      }
    }
    return null;
  }

  public Text getText() {
    return text;
  }

  private <T extends org.apache.lucene.search.Query> Collection<T> collectTermQueries(
      LucenePhraseConverter<T> converter) {
    final Collection<T> queries = new ArrayList<>();

    for (Expansion<Weighted<String>> expansion : expansions) {
      for (Weighted<String> term : expansion) {
        if (term.weight > 0) {
          T q = converter.convert(term.value, term.weight* scale, LuceneEMRSearcher.TEXT_FIELD);
          queries.add(q);
          if (q instanceof SpanQuery) {
            SpanQuery sq = (SpanQuery) q;
            if (sq.getField() == null) {
              log.info("Created {} from {} with {} for {}", q, term, expansion, this);
            }
          }
          if (term.weight >= 6) {
            queries.add(converter.convert(term.value, term.weight * 2 * scale,
                LuceneEMRSearcher.CHIEF_COMPLAINT_FIELD));
          }
        }
      }
    }
    return queries;
  }

  public BooleanQuery asLuceneQuery() {
    if (bq == null) {
      final Iterable<SpanQuery> terms = collectTermQueries(new LuceneSpanPhraseConverter());
      final BooleanQuery.Builder bqb = new BooleanQuery.Builder();
      for (final SpanQuery term : terms) { bqb.add(term, Occur.SHOULD); }
      bqb.add(getSubQuery(terms, LuceneEMRSearcher.TEXT_FIELD, 1 * scale), Occur.SHOULD);
      bqb.add(getSubQuery(terms, LuceneEMRSearcher.CHIEF_COMPLAINT_FIELD, 2 * scale),
          Occur.SHOULD);
      bq = bqb.build();
    }
    return bq;
  }

  <T extends org.apache.lucene.search.Query> BooleanQuery toLuceneQuery(
      LucenePhraseConverter<T> converter) {
    final BooleanQuery.Builder bq = new BooleanQuery.Builder();
    Collection<T> terms = collectTermQueries(converter);
    try {
      for (final T term : terms) {
        bq.add(term, Occur.SHOULD);
      }
      final Iterable<SpanQuery> terms2 = collectTermQueries(new LuceneSpanPhraseConverter());
      bq.add(getSubQuery(terms2, LuceneEMRSearcher.TEXT_FIELD, 1 * scale), Occur.SHOULD);
      bq.add(getSubQuery(terms2, LuceneEMRSearcher.CHIEF_COMPLAINT_FIELD, 2 * scale),
          Occur.SHOULD);
    } catch (BooleanQuery.TooManyClauses ex) {
      log.error("Too many expansions for string {} ({} expansions; max boolean clauses is {})",
          this, terms.size(), BooleanQuery.getMaxClauseCount());
    }
    return bq.build();
  }


  private static SpanQuery getSubQuery(Iterable<SpanQuery> terms, Term field, double boost) {
    final ArrayList<SpanQuery> queries = new ArrayList<>();
    final List<SpanQuery> clauses = new ArrayList<>();
    for (final SpanQuery term : terms) {
      if (term.getField().equals(field.field())) {
        clauses.add(new SpanBoostQuery(term, (float) boost));
      }
    }
    queries.add(new SpanOrQuery(Iterables.toArray(clauses, SpanQuery.class)));

    if (queries.size() > 1)
      return new SpanNearQuery(Iterables.toArray(queries, SpanQuery.class), 2, false);
    else
      return queries.get(0);
  }


  public Keyword addExpansion(Expansion<Weighted<String> >exp) {
    exp.removeIf(stringWeighted -> !isValidKeyword(stringWeighted.value));
    expansions.add(exp);
    return this;
  }

  @Override public String describe() {
    String prefix = "";
    if (isRequired) {
      prefix = "+";
    } else if (isNegation) {
      prefix = "-";
    } else if (isDisjunction) {
      prefix = "~";
    }
    return prefix + asString();
  }

  @Override public boolean equals(Object obj) {
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    final Keyword other = (Keyword) obj;
    return Objects.equals(this.string, other.string);
  }

  @Override public int hashCode() {
    int hash = 5;
    hash = 59 * hash + Objects.hashCode(this.string);
    return hash;
  }
}
