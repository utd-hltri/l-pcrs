package edu.utdallas.hltri.mercury;

import com.google.common.base.Splitter;
import edu.utdallas.hltri.conf.Config;
import edu.utdallas.hltri.eeg.ConceptNormalization;
import edu.utdallas.hltri.eeg.annotation.EegActivity;
import edu.utdallas.hltri.inquire.ANN;
import edu.utdallas.hltri.inquire.ie.AgeExtractor;
import edu.utdallas.hltri.inquire.ie.GenderExtractor;
import edu.utdallas.hltri.inquire.text.Query;
import edu.utdallas.hltri.logging.Logger;
import edu.utdallas.hltri.scribe.text.Document;
import edu.utdallas.hltri.scribe.text.annotation.*;
import edu.utdallas.hltri.struct.Weighted;
import org.apache.solr.client.solrj.SolrQuery;

import java.util.*;

/**
 * CohortQueryParser that uses EEG Activities/Events as its keywords.
 *
 * Created by rmm120030 on 4/27/17.
 */
public class ConceptCohortQueryParser extends CohortQueryParser {
  private static final Logger log = Logger.get(ConceptCohortQueryParser.class);
  private static final int NUM_EXPANSIONS = 1;

  private final boolean useKgQueryExpansion;
  private final ANN conceptEmbeddingIndex;
  private CohortQueryParser fallBackParser = new CohortQueryParser(false, "genia", "opennlp");

  public ConceptCohortQueryParser(boolean useUmlsQueryExpansion, boolean useKgQueryExpansion) {
    super(useUmlsQueryExpansion, "genia", "opennlp");
    // We annotate a dummy document to initialize the annotation pipeline
    ConceptUtils.createAnnotatedQueryDocument("dummy");

    this.useKgQueryExpansion = useKgQueryExpansion;
    if (this.useKgQueryExpansion) {
      final Config config = Config.load("eeg.mercury");
      log.info("Concept Cohort Query Parser using KG query expansion with embeddings from {}.",
          config.getString("concept-embedding-index"));
      conceptEmbeddingIndex = new ANN(config.getString("concept-embedding-index"), ANN.AutoIndex.defaultParams(),
          Splitter.on("\t").omitEmptyStrings());
    } else {
      conceptEmbeddingIndex = null;
    }
  }

  public ConceptCohortQueryParser() {
    this(false, false);
  }

  @Override
  public <Q extends Query> Document<Q> preprocessQuery(String cohortDescription) {
    log.debug("Got cohort description: {}", cohortDescription);

    // 1. Annotate stuff
    final Document<Q> query = ConceptUtils.createAnnotatedQueryDocument(cohortDescription);
    log.debug("Annotated cohort description: {}", query.describe());
    return query;
  }

  @Override
  public SolrQuery parse(String cohortDescription) {
    final Document<Query> query = preprocessQuery(cohortDescription);
    return parse(query);
  }

  @Override
  public <Q extends Query> SolrQuery parse(Document<Q> query) {
    // 2. Extract structured info
    final AgeExtractor.AgeRange ageRange = ageExtractor.apply(query);
    final GenderExtractor.Gender gender = genderExtractor.apply(query);

    // 3. Build SolrQuery
    final SolrQuery solrQuery = new SolrQuery();
    final StringBuilder solrMainQueryString = new StringBuilder();
    final StringBuilder solrPositiveQueryString = new StringBuilder();
    final StringBuilder solrNegativeQueryString = new StringBuilder();

    if (ageRange != null && ageRange != AgeExtractor.AgeRange.NONE) {
      solrMainQueryString.append("age:[").append(ageRange.getStart()).append(" TO ").append(ageRange.getEnd()).append("]");
    }
    if (gender != null) {
      solrMainQueryString.append(" gender:").append(gender.name().toLowerCase());
    }

    // TODO: handle abnormal EEG mentions


    final List<Weighted<String>> candidateExpansionTerms = new ArrayList<>();
    // add activity keywords
    query.get(ConceptUtils.CONC_ANNSET, EegActivity.TYPE).forEach(a -> {
      solrMainQueryString.append(" activity:").append(ConceptUtils.createActivityStringWithWildcards(a));
      if (useKgQueryExpansion) {
        candidateExpansionTerms.addAll(expand(a));
      }
      addKeywords(a, solrPositiveQueryString, solrNegativeQueryString);
    });
    if (useKgQueryExpansion) {
      solrMainQueryString.append(createExpandedQuery(candidateExpansionTerms, 0));
      candidateExpansionTerms.clear();
    }

    // add event keywords
    query.get(ConceptUtils.CONC_ANNSET, Event.TYPE).forEach(e -> {
      Optional<String> normalizedEvent = ConceptUtils.createEventString(e);
      if (normalizedEvent.isPresent()){
        solrMainQueryString.append(" event:").append(normalizedEvent.get());
        if (useKgQueryExpansion) {
          candidateExpansionTerms.addAll(expand(e));
        }
      }
      addKeywords(e, solrPositiveQueryString, solrNegativeQueryString);
    });
    if (useKgQueryExpansion) {
      solrMainQueryString.append(createExpandedQuery(candidateExpansionTerms, 3));
    }

    final String queryString = (solrMainQueryString.toString() + ' ' + solrPositiveQueryString.toString()).trim();
    if(queryString.isEmpty()) {
      return fallBackParser.parse(query);
    }

    log.info("Input query {}", query.asString());
    log.info("Generated query string: {}", queryString);

    solrQuery.setQuery(queryString);
    solrQuery.add("fl", "*,score");   // includes the relevancy score in each SolrDocument returned by this query
    return solrQuery;
  }

  private String createExpandedQuery(List<Weighted<String>> candidateExpansionTerms, int numExpansions) {
//    candidateExpansionTerms.sort(Weighted::compareTo);
    candidateExpansionTerms.sort(Comparator.reverseOrder());
    final Set<String> expansionTerms = new HashSet<>();
    final Iterator<Weighted<String>> it = candidateExpansionTerms.iterator();
    while (expansionTerms.size() < numExpansions && it.hasNext()) {
      final Weighted<String> candidate = it.next();
      if (candidate.getWeight() > 0) {
        expansionTerms.add(candidate.getValue());
      }
    }
    final StringBuilder sb = new StringBuilder();
    for (String expansionTerm : expansionTerms) {
      if (expansionTerm.startsWith("A")) {
        sb.append(" activity:").append(expansionTerm.substring(2));
      } else {
        sb.append(" event:").append(expansionTerm);
      }
      sb.append("|*");
    }
    return sb.toString();
  }

  private <A extends Annotation<A>> List<Weighted<String>> expand(A ann) {
    final Optional<String> normalized = ConceptNormalization.normalizeConcept(ann);
    if (normalized.isPresent() && conceptEmbeddingIndex.containsVector(normalized.get())) {
      return conceptEmbeddingIndex.getNearest(normalized.get(), NUM_EXPANSIONS + 1);
    } else {
      return Collections.emptyList();
    }
  }
}
