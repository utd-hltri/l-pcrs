package edu.utdallas.hlt.trecmed;

import com.google.common.collect.Sets;
import com.google.common.primitives.Doubles;
import edu.utdallas.hlt.trecmed.retrieval.LuceneEMRSearcher;
import edu.utdallas.hlt.trecmed.retrieval.LucenePhraseConverter;
import edu.utdallas.hlt.trecmed.retrieval.LucenePhrasePhraseConverter;
import edu.utdallas.hlt.trecmed.retrieval.LuceneSpanPhraseConverter;
import edu.utdallas.hltri.inquire.ie.AgeExtractor.AgeRange;
import edu.utdallas.hltri.inquire.ie.GenderExtractor.Gender;
import edu.utdallas.hltri.logging.Logger;
import edu.utdallas.hltri.scribe.text.Text;
import edu.utdallas.hltri.struct.Weighted;
import edu.utdallas.hltri.util.Expansion;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import javax.annotation.Nonnull;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.BooleanQuery;

/**
 *
 * @author travis
 */
public class Topic extends Text implements Iterable<Keyword>, Serializable {

  private static final long serialVersionUID = 1L;

  private static final Logger log = Logger.get(Topic.class);

  private final     String                     id;
  private final     String                     rawText;
  private transient edu.utdallas.hlt.text.Text text;

  private final Set<Keyword> keywords = new TreeSet<>();
  private       AgeRange     ageRange = null;
  private       Gender       gender   = null;

  public Topic(String id, String rawText) {
    this.id = id;
    this.rawText = rawText;
  }

  public Topic(edu.utdallas.hlt.text.Text text) {
    this.text = text;
    this.id = text.getDocumentID();
    this.rawText = text.asRawString();
  }

  public edu.utdallas.hlt.text.Text getText() {
    return text;
  }

  public String getId() {
    return id;
  }

  @Override public String describe () {
    return "Query #" + id + ": '" + rawText + "'";
  }

  @Override @Nonnull
  public Iterator<Keyword> iterator() {
    return keywords.iterator();
  }

  public AgeRange getAgeRange() {
    return ageRange;
  }

  public Gender getGender() {
    return gender;
  }

  public void setGender(Gender gender) {
    this.gender = gender;
  }

  public boolean hasGenderRequirement() {
    return this.gender != null;
  }

  public boolean hasAgeRequirement() {
    return this.ageRange != null;
  }

  @Override public String asString() {
    return this.rawText;
  }

  public Set<Keyword> getKeywords() {
    return keywords;
  }

  public void filter(String filter) {
    log.trace("Filtering {} with {}.", keywords.toString(), filter);
    for (String word : filter.split(" ")) {
      for (Iterator<Keyword> it = keywords.iterator(); it.hasNext();) {
        Keyword keyword = it.next();
        if (word.equalsIgnoreCase(keyword.asString())) {
          it.remove();
          log.debug("Removed keyword \"{}\" because it matched \"{}\".", keyword, word);
        }
      }
    }
  }

  public static void mergeDuplicateKeywords(Set<Keyword> keywords) {
    boolean change = false;
    for (List<Keyword> pairs : Sets.cartesianProduct(Arrays.asList(keywords, keywords))) {
      final Keyword first = pairs.get(0),
                    second = pairs.get(1);
      if (first.equals(second)) { continue; }
      final Set<Weighted<String>> x = first.getSafeWeightedTerms(),
                              y = second.getSafeWeightedTerms();
      final int intersection = Sets.intersection(x, y).size();
      final int union = Sets.union(x, y).size();
      final double jaccard = intersection / (double) union;
      if (Doubles.isFinite(jaccard) && Double.compare(jaccard, 0.05) >= 0) {
        log.warn ("Merging {} & {} (Jaccard of {})", first, second, jaccard);
        for (Expansion<Weighted<String>> exp : second.getExpansions()) {
          String source = exp.getName();
          int original = first.getExpansion(source).size();
          int other = second.getExpansion(source).size();
          first.getExpansion(exp.getName()).addAll(exp);
          log.debug("Merging {}: original size: {}, added {}, final size = {}", source, original, other, first.getExpansion(source).size());
        }
        keywords.remove(second);
        change = true;
        break;
      }
    }
    if (change) {
      mergeDuplicateKeywords(keywords);
    } else {
      for (Keyword keyword : keywords) {
        mergeDuplicateKeywords(keyword.getSubKeywords());
      }
    }
  }

  public int getNumberOfTokens() {
       return LuceneEMRSearcher.getReportSearcher().tokenize(asString(), LuceneEMRSearcher.FULL_TEXT_FIELDNAME).size();
  }

  public BooleanQuery asLuceneQuery() {
    return asLuceneSpanQuery();
  }

  private BooleanQuery asLuceneSpanQuery() {
    return toLuceneQuery(new LuceneSpanPhraseConverter());
  }

  public BooleanQuery asLucenePhraseQuery() {
    return toLuceneQuery(new LucenePhrasePhraseConverter());
  }


  private BooleanQuery toLuceneQuery(LucenePhraseConverter<?> converter) {
    BooleanQuery.Builder bq = new BooleanQuery.Builder();

    for (Keyword keyword : keywords) {
      if (keyword.isNegation()) {
        continue;
      }

      org.apache.lucene.search.Query q = keyword.toLuceneQuery(converter);

      if (keyword.isDisjunction()) {
        BooleanQuery.Builder ibq = new BooleanQuery.Builder();
        List<Keyword> disjunctionGroup = keyword.getDisjunctionGroup();
        if (disjunctionGroup.indexOf(keyword) == 0) {
          for (Keyword disjunctionMember : disjunctionGroup) {
            (ibq).add(disjunctionMember.asLuceneQuery(), Occur.SHOULD);
          }
        } else {
          continue;
        }
        q = ibq.build();
      }

      if (keyword.isRequired()) {
        bq.add(q, Occur.SHOULD);
      } else {
        bq.add(q, Occur.SHOULD);
      }
    }

    return bq.build();
  }

  @Override public boolean equals(Object obj) {
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    final Topic other = (Topic) obj;
    return Objects.equals(this.id, other.id) && Objects.equals(this.rawText, other.rawText);
  }

  @Override public int hashCode() {
    int hash = 5;
    hash = 67 * hash + Objects.hashCode(this.id);
    hash = 67 * hash + Objects.hashCode(this.rawText);
    return hash;
  }
}
