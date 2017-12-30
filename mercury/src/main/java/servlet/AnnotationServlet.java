//package servlet;
//
//import edu.utdallas.hltri.conf.Config;
//import edu.utdallas.hltri.eeg.EegNote;
//import edu.utdallas.hltri.inquire.SearchResult;
//import edu.utdallas.hltri.logging.Logger;
//import edu.utdallas.hltri.mercury.CohortQueryParser;
//import edu.utdallas.hltri.mercury.SimpleEegNote;
//import edu.utdallas.hltri.mercury.SolrSearchResult;
//import edu.utdallas.hltri.scribe.io.JsonCorpus;
//import edu.utdallas.hltri.scribe.text.BaseDocument;
//import edu.utdallas.hltri.scribe.text.Document;
//
//import org.apache.solr.client.solrj.SolrClient;
//import org.apache.solr.client.solrj.SolrQuery;
//import org.apache.solr.client.solrj.SolrServerException;
//import org.apache.solr.client.solrj.impl.HttpSolrClient;
//import org.apache.solr.client.solrj.response.QueryResponse;
//import org.apache.solr.common.SolrDocument;
//import org.apache.solr.common.SolrDocumentList;
//import org.apache.velocity.Template;
//import org.apache.velocity.VelocityContext;
//import org.apache.velocity.app.VelocityEngine;
//import org.apache.velocity.runtime.RuntimeConstants;
//import org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader;
//
//import javax.servlet.ServletException;
//import javax.servlet.http.HttpServlet;
//import javax.servlet.http.HttpServletRequest;
//import javax.servlet.http.HttpServletResponse;
//import java.io.IOException;
//import java.io.PrintWriter;
//import java.nio.file.Paths;
//import java.util.ArrayList;
//import java.util.List;
//import java.util.Optional;
//
//public class MercuryServlet extends HttpServlet {
//  private final Config eegConfig = Config.load("eeg");
//  private final Config mercuryConfig = eegConfig.load("mercury");
//
//  private final int maxSearchResults = mercuryConfig.getInt("max-search-results");
//  private final int maxSearchResultsPerPage = mercuryConfig.getInt("max-search-results-per-page");
//
//  /**
//   * We need access to (1) EEG reports (to display)
//   *                   (2) the Solr server (for retrieval)
//   *                   (3) EEG signal data?
//   *                   (4) EEG annotations?
//   */
//
//  private final JsonCorpus<EegNote> eegReports =
//      JsonCorpus.<EegNote>at(eegConfig.getPath("corpus.v060.json-path")).tiered().build();
//
//  private final CohortQueryParser parser = new CohortQueryParser();
//
//  private final SolrClient solr = new HttpSolrClient(mercuryConfig.getString("solr-url"));
//
//  // Used to create & format HTML results
//  private final VelocityEngine ve;
//
//  private final Logger log = Logger.get(MercuryServlet.class);
//
//  public MercuryServlet() {
//    // Setup the JSON corpus used to find/view EEG reports
//    this.eegReports = JsonCorpus.<BaseDocument>at(eegConfig.getString("json-path"))
//        .annotationSets()
//        .tiered()
//        .build();
//
//    // Setup the Velocity template engine
//    this.ve = new VelocityEngine();
//
//    /* Velocity uses log4j to log, while we use log4j2.
//     * While we could bridge it, we don't because it caused weird runtime errors that I
//     * couldn't figure out how to resolve.
//     */
//    ve.setProperty(RuntimeConstants.RUNTIME_LOG_LOGSYSTEM_CLASS, "org.apache.velocity.runtime.log.NullLogSystem");
//
//    // Tell velocity to search for templates in the classpath (i.e. resources/)
//    ve.setProperty(RuntimeConstants.RESOURCE_LOADER, "classpath");
//    ve.setProperty("classpath.resource.loader.class", ClasspathResourceLoader.class.getName());
//
//    // Start the engine!
//    ve.init();
//  }
//
//  @Override
//  public void doGet(HttpServletRequest request, HttpServletResponse response)
//      throws IOException, ServletException {
//    response.setContentType("text/html; charset=utf-8");
//    response.setHeader("Connection", "close");
//    switch (request.getPathInfo()) {
//      case "/search":
//        doSearch(request, response);
//        break;
//      case "/view":
//        doViewReport(request, response);
//        break;
//      default:
//        throw new ServletException("unable to handle request \"" + request + "\" with path |" +
//            request.getPathInfo() + "|");
//    }
//  }
//
//  private void doViewReport(HttpServletRequest request, HttpServletResponse response) throws IOException {
//    String docid = request.getParameter("id");
//
//    final VelocityContext context = new VelocityContext();
//    context.put("reportId", docid);
//
//    try (Document<EegNote> doc = eegReports.load(docid)) {
//      final String text = doc.toString();
////      final String signalFile = Paths.get(doc.get(BaseDocument.path))
////          .resolveSibling("a_.edf.bz2")
////          .toString();
//      context.put("reportText", text);
////      context.put("signal", no);
//    }
//
//    response.setStatus(HttpServletResponse.SC_OK);
//    final PrintWriter out = response.getWriter();
//    final Template template;
//    try {
//      template = ve.getTemplate( "eeg_report.vm" );
//      template.merge(context, out);
//    } catch (Exception e) {
//      throw new IOException(e);
//    }
//  }
//
//  private void doSearch(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
//    // Determine start & end offsets
//    final int startOffset = Optional.ofNullable(request.getParameter("start"))
//        .map(Integer::parseInt)
//        .orElse(0);
//
//    final int endOffset =Optional.ofNullable(request.getParameter("end"))
//        .map(Integer::parseInt)
//        .orElse(startOffset + maxSearchResultsPerPage);
//
//    // Parse the query
//    final String query = request.getParameter("query");
//    final SolrQuery parsedQuery = parser.parse(query);
//    parsedQuery.setStart(startOffset);
////    parsedQuery.set("group", true);
////    parsedQuery.set("group.field", "patient_id")
//
//    final List<SearchResult<SimpleEegNote>> results = new ArrayList<>();
//    try {
//      final QueryResponse queryResponse = solr.query(parsedQuery);
//      int rank = 1;
//      for (SolrDocument d : queryResponse.getResults()) {
//        final SearchResult<SimpleEegNote> relevantEegNote = new SolrSearchResult<SimpleEegNote>(
//            new SimpleEegNote((String) d.getFieldValue("record_id"),
//                (String) d.getFieldValue("path"),
//                (String) d.getFieldValue("text")),
//            rank
//        );
//        results.add(relevantEegNote);
//      }
//    } catch (SolrServerException e) {
//      throw new ServletException(e);
//    }
//
//    final VelocityContext context = new VelocityContext();
//    context.put("query", query);
//
//    // Make a PogerTool to do pagination
//    PagerTool pagerTool = new PagerTool();
//    pagerTool.setItems(List<Result>);
//    context.put("results", results);
//
//    response.setStatus(HttpServletResponse.SC_OK);
//    final PrintWriter out = response.getWriter();
//    final Template template = ve.getTemplate( "search.vm" );
//    template.merge(context, out);
//  }
//}
