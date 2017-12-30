package edu.utdallas.hltri.mercury;

import edu.utdallas.hlt.medbase.umls.UMLSManager;
import edu.utdallas.hltri.func.CloseableFunction;
import edu.utdallas.hltri.inquire.ie.AgeExtractor;
import edu.utdallas.hltri.inquire.ie.AgeExtractor.AgeRange;
import edu.utdallas.hltri.inquire.ie.GenderExtractor;
import edu.utdallas.hltri.inquire.text.Keyword;
import edu.utdallas.hltri.inquire.text.Query;
import edu.utdallas.hltri.inquire.text.annotators.KeywordAnnotator;
import edu.utdallas.hltri.logging.Logger;
import edu.utdallas.hltri.scribe.annotators.*;
import edu.utdallas.hltri.scribe.text.BaseDocument;
import edu.utdallas.hltri.scribe.text.Document;
import edu.utdallas.hltri.scribe.text.annotation.Annotation;
import edu.utdallas.hltri.scribe.text.annotation.NegationSpan;
import edu.utdallas.hltri.scribe.text.annotation.Sentence;
import edu.utdallas.hltri.scribe.text.annotation.Token;
import org.apache.solr.client.solrj.SolrQuery;

import java.util.List;

/**
 * The job of the CohortQueryParser is to taken a cohort query (expressed in natural language)
 * and convert it into a machine-readable Solr query
 *
 * Steps:
 *   1. pre-process query to determine (1) phrase chunks and (2) negation spans
 *   2. extract patient's age (if specified)
 *   3. extract patient's gender (if specified)
 *   4. format as Solr query
 *
 */
public class CohortQueryParser implements AutoCloseable {
  private final static transient Logger log = Logger.get(CohortQueryParser.class);

  private final UMLSManager umls;
  private final String tokenAnnset;
  private final boolean useUmlsQueryExpansion;
//  private final PMCSearchEngine pmc = new PMCSearchEngine();

//  private final Joiner joiner = Joiner.on(' ');

  private final KeywordAnnotator<Query> pmcKeywordAnalyzer;

  private final Annotator<Query> sentenceSplitter = new OpenNLPSentenceAnnotator<>();
  private final Annotator<Query> tokenizer;
//  private final Annotator<Query> chunker = new OpenNLPChunker<>();
  private final Annotator<BaseDocument> negationSpanner;

  @SuppressWarnings("WeakerAccess")
  protected final CloseableFunction<? super Document<? extends Query>, AgeRange>
      ageExtractor = new AgeExtractor();

  @SuppressWarnings("WeakerAccess")
  protected final CloseableFunction<? super Document<? extends Query>, GenderExtractor.Gender>
      genderExtractor = new GenderExtractor();



  CohortQueryParser() {
    this(false, "opennlp", "opennlp");
  }

  public CohortQueryParser(boolean useUmlsQueryExpension, String tokenAnnset, String sentenceAnnset) {
    this.tokenAnnset = tokenAnnset;
    this.useUmlsQueryExpansion = useUmlsQueryExpension;
    this.umls = useUmlsQueryExpension ? new UMLSManager() : null;
    pmcKeywordAnalyzer = new KeywordAnnotator<>(
        tokenAnnset,
        sentenceAnnset,
        (List<Token> tokens) -> {
//        for (final Token token : tokens) {
//          final int tokenHits = pmc.getHitCount(pmc.newParsedQuery(token));
//          if (tokenHits <= 2 || tokenHits > 100_000)
//            return false;
//        }
          return tokens.size() < 6;
        },
        (CharSequence keyword) -> {
//        final int hits = pmc.getHitCount(
//          pmc.newSpanQuery(keyword,
//              PMCSearchEngine.TEXT_FIELD,
//              pmc.getAnalyzer(),
//              1.0));
//        log.debug("Found {} hits for |{}|", hits, keyword);
//        return hits > 3 && hits < 10_000;
          return true;
        },
        "keywords"
    );
    tokenizer = (tokenAnnset.equals("opennlp")) ? new OpenNLPTokenizer<>() :
        GeniaAnnotator.tokenAnnotator(d -> d.get(sentenceAnnset, Sentence.TYPE), false);

    negationSpanner = new LingScopeNegationSpanAnnotator<>(
        d -> d.get(sentenceAnnset, Sentence.TYPE),
        s -> s.getContained(tokenAnnset, Token.TYPE)
    );
    ConceptUtils.createAnnotatedQueryDocument("dummy");
  }

  public <Q extends Query> Document<Q> preprocessQuery(String cohortDescription) {
    final Document<Q> query = Document.fromString(cohortDescription + ".");
    query.set(BaseDocument.id, "$query");
    log.debug("Got cohort description: {}", cohortDescription);


    // 1. Annotate stuff
    log.debug("Annotating sentences...");
    sentenceSplitter.annotate(query);
    log.debug("Annotating tokens...");
    tokenizer.annotate(query);
//    log.debug("Annotating phrase chunks...");
//    chunker.annotate(query);
    log.debug("Annotating negation spans... on doc {}", query.describe());
    negationSpanner.annotate(query);


    pmcKeywordAnalyzer.annotate(query);

    log.debug("Annotated cohort description: {}", query.describe());
    return query;
  }

  public SolrQuery parse(String cohortDescription) {
    return parse(preprocessQuery(cohortDescription));
  }

  public <Q extends Query> SolrQuery parse(Document<Q> query) {
    // 2. Extract structured info
    final AgeExtractor.AgeRange ageRange = ageExtractor.apply(query);
    final GenderExtractor.Gender gender = genderExtractor.apply(query);

    // 3. Build SolrQuery
    final SolrQuery solrQuery = new SolrQuery();

    final StringBuilder solrMainQueryString = new StringBuilder();
    final StringBuilder solrPositiveQueryString = new StringBuilder();
    final StringBuilder solrNegativeQueryString = new StringBuilder();

    //query.get("opennlp", PhraseChunk.TYPE)

    if (ageRange != null) {
      solrMainQueryString.append(" age:[").append(ageRange.getStart()).append(" TO ").append(ageRange.getEnd()).append("]");
    }
    if (gender != null) {
      solrMainQueryString.append(" gender:").append(gender.name().toLowerCase());
    }


    query.get("keywords", Keyword.TYPE).forEach(chunk ->
        addKeywords(chunk, solrPositiveQueryString, solrNegativeQueryString));

    final String queryString = solrMainQueryString.toString() + ' ' + solrPositiveQueryString.toString();

    log.debug("Input query: " + query.asString());
    log.debug("Generated query: " + queryString);

    solrQuery.setQuery(queryString);
    solrQuery.add("fl", "*,score");   // includes the relevancy score in each SolrDocument returned by this query
    return solrQuery;
  }

  @SuppressWarnings("WeakerAccess")
  protected <A extends Annotation<A>> void addKeywords(A chunk, StringBuilder solrPositiveQueryString,
                                                       StringBuilder solrNegativeQueryString) {
    final StringBuilder solrQueryString =
        (chunk.getCovering("lingscope", NegationSpan.TYPE).isEmpty()) ? solrPositiveQueryString
            : solrNegativeQueryString;

    if (solrQueryString.length() > 0) {
      solrQueryString.append(' ');
    }

    final int chunkLength = chunk.getContained(tokenAnnset, Token.TYPE).size();
    final String chunkString = chunk.toString();
    if (chunkLength == 1) {
      if (chunk.getContained(tokenAnnset, Token.TYPE).get(0).length() > 2) {
        solrQueryString.append("text:").append(chunkString.replaceAll("\\W+", ""));
      }
    } else {
      solrQueryString.append("text:\"")
          .append(chunkString.replaceAll("\\W+", " ").trim())
          .append("\"~")
          .append(Integer.toString((int) Math.ceil(chunkLength / 2.0)));
    }

    if (useUmlsQueryExpansion) {
      umls.expand(chunkString).stream().filter(exp -> !exp.equalsIgnoreCase(chunkString)).map(String::trim)
          .forEach(expansion -> {
        final int expLength = expansion.split("\\W+").length;
        // if only 1 term, simply add it to the query
        if (expLength == 1) {
          // skip terms shorter than 2 chars
          if (expansion.length() > 2) {
            solrQueryString.append(" ").append(expansion);
          }
        // else we need to wrap it in quotes and tell lucene to softmatch each term in the phrase
        } else {
          solrQueryString.append(" \"")
              .append(expansion.replaceAll("\\W+", " "))
              .append("\"~")
              .append(Integer.toString((int) Math.ceil(expLength / 2.0)));
        }
      });
    }
  }

  @Override
  public void close() throws Exception {
    sentenceSplitter.close();
    tokenizer.close();
//    chunker.close();
    negationSpanner.close();
    ageExtractor.close();
    genderExtractor.close();
    pmcKeywordAnalyzer.clear();
  }
}
