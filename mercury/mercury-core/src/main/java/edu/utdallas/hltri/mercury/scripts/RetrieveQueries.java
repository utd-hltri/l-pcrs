package edu.utdallas.hltri.mercury.scripts;

import edu.utdallas.hltri.inquire.eval.TrecRunWriter;
import edu.utdallas.hltri.logging.Logger;
import edu.utdallas.hltri.mercury.ParsedQuery;
import edu.utdallas.hltri.mercury.ParsedQueryLoader;
import edu.utdallas.hltri.mercury.SolrWrapper;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;

/**
 * Created by travis on 10/6/16.
 */
public class RetrieveQueries {
  private static final transient Logger log = Logger.get(RetrieveQueries.class);

  public static void main(String... args) throws IOException, SolrServerException {
    // final CohortQueryParser parser = new CohortQueryParser();
    final Path queries = Paths.get(args[0]);
    final Path qrelsPath = Paths.get(args[1]);
    Files.createDirectories(qrelsPath.getParent());
    try (final TrecRunWriter qrels = new TrecRunWriter(qrelsPath,  "MERCuRY")) {
      for (final Map.Entry<String, ParsedQuery> query : ParsedQueryLoader.INSTANCE.load(queries).entrySet()) {
        final SolrQuery solrQuery = query.getValue().toSolrQuery();
        log.debug("Generated query {}", solrQuery);
        solrQuery.setRows(100);
        final SolrDocumentList results = SolrWrapper.INSTANCE.query(solrQuery).getResults();

        int rank = 1;
        if (results.size() < 100) {
          log.warn("Returned only {} results for query {}", results.size(), query.getKey());
        }
        for (final SolrDocument doc : results) {
          qrels.writeResult(query.getKey(), doc.getFieldValue("record_id"), rank, (Math.exp(1 - Math.tanh(rank))));
          rank++;
          if (rank > 100) break;
        }
      }
    }
  }
}
