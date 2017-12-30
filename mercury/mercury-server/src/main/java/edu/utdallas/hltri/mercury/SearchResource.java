package edu.utdallas.hltri.mercury;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocumentList;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Optional;
import java.util.function.Supplier;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import edu.utdallas.hltri.conf.Config;
import edu.utdallas.hltri.eeg.Data;
import edu.utdallas.hltri.eeg.EegNote;
import edu.utdallas.hltri.scribe.io.JsonCorpus;
import edu.utdallas.hltri.util.Lazy;


import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;

@Path("/search")
public class SearchResource {
  private static final Config eegConfig = Config.load("eeg");
  private static final Config mercuryConfig = eegConfig.getConfig("mercury");

  private final CohortQueryParser parser = new CohortQueryParser();
  private final String conceptAnnset = mercuryConfig.getString("eeg-concept-annset");
  private final int limit = mercuryConfig.getInt("api_record_limit");
  private final Supplier<JsonCorpus<EegNote>> corpusSupplier = Lazy.lazily(() -> Data.v060(conceptAnnset));

  @GET
  @Produces("text/json")
  public String getSearchResults(
                            @QueryParam("query") String query,
      @DefaultValue("0")    @QueryParam("start") int start,
      @DefaultValue("100")  @QueryParam("limit") int limit) {
    return "{" +
        "query = \"" + query + "\",\n" +
        "start = " + start + ",\n" +
        "limit = " + limit + "\n" +
        "}";
  }


//  private void doSearch(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
//    final long startTime = System.currentTimeMillis();
//
//    final int numRecords = Optional.ofNullable(request.getParameter("limit"))
//        .map(Integer::parseInt)
//        .filter(i -> i > 0 && i < 1000)
//        .orElseGet(() -> limit);
//
//    // Determine start & end offsets
//    final int startOffset = Optional.ofNullable(request.getParameter("start"))
//        .map(Integer::parseInt)
//        .orElse(0);
//
//    // Parse the query
////    final String query = request.getParameter("q");
//    final SolrQuery parsedQuery = Optional.ofNullable(request.getParameter("q"))
//        .map(parser::parse).orElseThrow(() -> new IllegalArgumentException("no query parameter provided"));
//    parsedQuery.setRows(limit);
//    parsedQuery.setStart(startOffset);
//    parsedQuery.set("wt", "json");
//    parsedQuery.set("fl", "record_id");
//
//    int pagesAvailable;
//    try {
//      final QueryResponse queryResponse = SolrWrapper.INSTANCE.query(parsedQuery);
//      final SolrDocumentList results = queryResponse.getResults();
//      // pigdog
//      final long numTotalResults = results.getNumFound();
//
//      log("Found " + queryResponse.getResults() + " results for " + parsedQuery);
//    } catch (SolrServerException e) {
//      throw new ServletException(e);
//    }
//
//    response.setStatus(HttpServletResponse.SC_OK);
//    final PrintWriter out = response.getWriter();
//    // TODO: bullshit json guy to write json
//  }
}
