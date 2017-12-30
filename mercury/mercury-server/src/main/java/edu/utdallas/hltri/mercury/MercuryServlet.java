package edu.utdallas.hltri.mercury;

import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import edu.utdallas.hltri.eeg.Data;
import edu.utdallas.hltri.eeg.EegNote;
import edu.utdallas.hltri.eeg.annotation.EegActivity;
import edu.utdallas.hltri.inquire.eval.QRels;
import edu.utdallas.hltri.mercury.relevance.RelevanceModels;
import edu.utdallas.hltri.scribe.io.JsonCorpus;
import edu.utdallas.hltri.scribe.text.Document;
import edu.utdallas.hltri.scribe.text.annotation.Annotation;
import edu.utdallas.hltri.scribe.text.annotation.Event;
import edu.utdallas.hltri.util.Lazy;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.RuntimeConstants;
import org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader;
import org.apache.velocity.tools.generic.ComparisonDateTool;
import org.apache.velocity.tools.generic.DisplayTool;
import org.apache.velocity.tools.generic.EscapeTool;
import org.apache.velocity.tools.generic.MathTool;
import org.apache.velocity.tools.generic.NumberTool;
import org.apache.velocity.tools.generic.SortTool;
import org.apache.velocity.tools.view.WebappResourceLoader;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import edu.utdallas.hltri.conf.Config;

public class MercuryServlet extends HttpServlet {
  private static final Config eegConfig = Config.load("eeg");
  private static final Config mercuryConfig = eegConfig.getConfig("mercury");
  private static final TrecEval eval = TrecEval.fromFile(mercuryConfig.getPath("trev-evals-path"));
  private static final Map<String, ParsedQuery> parsedQueries = ParsedQueryLoader.INSTANCE.load(mercuryConfig.getPath("parsed-queries-path"));

  private final int maxSearchResultsPerPage = mercuryConfig.getInt("max-search-results-per-page");
  private final int maxSearchResults = mercuryConfig.getInt("max-search-results");
  private final CohortQueryParser parser = new CohortQueryParser();
  private final ConceptCohortQueryParser conceptParser = new ConceptCohortQueryParser();
  private final VelocityEngine ve;
  private final String conceptAnnset = mercuryConfig.getString("eeg-concept-annset");
  private final Supplier<JsonCorpus<EegNote>> corpusSupplier = Lazy.lazily(() -> Data.v060(conceptAnnset));
  private final MercurySearchEngine defaultSearchEngine;

  private final RankedDocsByQuery docsByQuery = RankedDocsByQuery.fromFile(mercuryConfig.getPath("ranked-qrels-path"));
  private final QRels qRels = QRels.fromFile(mercuryConfig.getPath("judges-qrels-path"));
  private static List<String> qids;
  static {
    qids = parsedQueries.keySet().stream().filter(qid -> eval.getNdcg(qid) > 0).collect(Collectors.toList());
    Collections.sort(qids, (q1, q2) -> Double.compare(eval.getNdcg(q2), eval.getNdcg(q1)));
  }

  public MercuryServlet() {
    // Setup the Velocity template engine
    this.ve = new VelocityEngine();

    /* Velocity uses log4j to log, while we use log4j2.
     * While we could bridge it, we don't because it caused weird runtime errors that I
     * couldn't figure out how to resolve.
     */
    ve.setProperty(RuntimeConstants.RUNTIME_LOG_LOGSYSTEM_CLASS, "org.apache.velocity.runtime.log.ServletLogChute");
//    ve.setProperty(RuntimeConstants.VM_LIBRARY, "_macros.vm,VM_global_library.vm,macros.vm");
//    ve.setProperty(RuntimeConstants.VM_LIBRARY_AUTORELOAD, "true");

    // Tell velocity to search for templates in the classpath (i.e. resources/)
    ve.setProperty(RuntimeConstants.RESOURCE_LOADER, "webapp,classpath");
    ve.setProperty("webapp.resource.loader.class", WebappResourceLoader.class.getName());
    ve.setProperty("webapp.resource.loader.path", "/WEB-INF/templates/");
    ve.setProperty("webapp.resource.loader.cache", false);

    ve.setProperty("classpath.resource.loader.class", ClasspathResourceLoader.class.getName());
    ve.setProperty("classpath.resource.loader.cache", false);

    defaultSearchEngine = new MercurySearchEngine(new ConceptCohortQueryParser(), RelevanceModels.RelevanceModel.solr.getScorers());
  }

  @Override
  public void init() throws ServletException {
    super.init();
    ve.setApplicationAttribute("javax.servlet.ServletContext", getServletContext());
    ve.init();
  }

  @Override
  public void doGet(HttpServletRequest request, HttpServletResponse response)
      throws IOException, ServletException {
    response.setContentType("text/html; charset=utf-8");
    response.setHeader("Connection", "close");
    if (request.getPathInfo() == null) {
      response.sendRedirect("/");
      return;
    }

    switch (request.getPathInfo()) {
      case "/search":
        try {
          doSearch(request, response);
        } catch (SolrServerException e) {
          throw new ServletException(e);
        }
        break;
      case "/view":
        doViewReport(request, response);
        break;
      case "/":
        doHome(request, response);
        break;
      case "/browse":
        doBrowse(request, response);
        break;
      default:
        response.setStatus(HttpServletResponse.SC_NOT_FOUND);
    }
  }

  @SuppressWarnings("UnusedParameters")
  private void doHome(HttpServletRequest request, HttpServletResponse response) throws IOException {
    final VelocityContext context = new VelocityContext();
    context.put("qids", qids);
    response.setStatus(HttpServletResponse.SC_OK);
    final Map<String,String> queries = new HashMap<>();
    qids.forEach(qid -> queries.put(qid, parsedQueries.get(qid).getOriginalQuery()));
    context.put("queries", queries);
    final PrintWriter out = response.getWriter();
    final Template template;
    try {
      template = ve.getTemplate( "home.vm" );
      template.merge(context, out);
    } catch (Exception e) {
      throw new IOException(e);
    }
  }

  private void doBrowse(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
    final VelocityContext context = new VelocityContext();
    context.put("eval", eval);
    final String qid = request.getParameter("qid");
    context.put("qid", qid);
    final ParsedQuery parsedQuery = parsedQueries.get(qid);
    final String query = parsedQuery.getOriginalQuery();
    context.put("query", query);
    final Map<String, Integer> relevanceMap = Maps.newHashMap();

    try {
      final SolrQuery solrQuery = parsedQuery.toSolrQuery();
      SolrWrapper.INSTANCE.setHighlighting(solrQuery, "<em>", "</em>", 256);
      solrQuery.setRows(100);
      final QueryResponse queryResponse = SolrWrapper.INSTANCE.query(solrQuery);
      context.put("response", queryResponse);
      final SolrDocumentList allResults = queryResponse.getResults();
      final List<SolrDocument> results = Lists.newArrayListWithCapacity(10);
      for (String did : docsByQuery.getBestDocs(qid)) {
        final Optional<SolrDocument> recordOp = allResults.stream().filter(sd -> sd.getFirstValue("record_id").equals(did)).findAny();
        if (recordOp.isPresent()) {
          final QRels.Relevance relevance = qRels.getRelevance(qid, did);
          relevanceMap.put(did, relevance.toInt());
          results.add(recordOp.get());
        }
      }
      // if we don't have at least 10 documents, add more till we get to 10
      final Iterator<SolrDocument> iterator = allResults.iterator();
      while (results.size() < 10 && iterator.hasNext()) {
        final SolrDocument result = iterator.next();
        final String did = (String) result.getFirstValue("record_id");
        // add document if not already in list
        if (!results.stream().anyMatch(sd -> sd.getFirstValue("record_id").equals(did))) {
          final QRels.Relevance relevance = qRels.getRelevance(qid, did);
          relevanceMap.put(did, relevance.toInt());
          results.add(result);
        }
      }
      context.put("results", results);

//        log("Found " + queryResponse.getResults() + " allResults for " + parsedQuery);
    } catch (SolrServerException e) {
      throw new ServletException(e);
    }

    response.setStatus(HttpServletResponse.SC_OK);
    final PrintWriter out = response.getWriter();
    try {
      final Template template = ve.getTemplate("browse.vm");
      template.merge(context, out);
    } catch (Exception e) {
      throw new IOException(e);
    }
  }

  private static VelocityContext createContext() {
    VelocityContext context = new VelocityContext();
    // Register useful Velocity "tools"
    context.put("esc", new EscapeTool());
    context.put("date", new ComparisonDateTool());
    context.put("math", new MathTool());
    context.put("number", new NumberTool());
    context.put("sort", new SortTool());
    context.put("display", new DisplayTool());
    return context;
  }

  private void doViewReport(HttpServletRequest request, HttpServletResponse response) throws IOException {
    final String docid = request.getParameter("id");

    final VelocityContext context = createContext();
    context.put("reportId", docid);

    final JsonCorpus<EegNote> corpus = corpusSupplier.get();
    try (final Document<EegNote> document = corpus.load(docid)) {
      final String conceptAnnset = (document.hasAnnotationSet("gold") ? "gold" : this.conceptAnnset);
      final List<Annotation<?>> annotations = Lists.newArrayList(document.get(conceptAnnset, Event.TYPE));
      annotations.addAll(document.get(conceptAnnset, EegActivity.TYPE));
      // sort all annotations descending by end offset
      Collections.sort(annotations, (a1, a2) -> Long.compare(a2.get(Annotation.EndOffset), a1.get(Annotation.EndOffset)));
      final StringBuilder docString = new StringBuilder(document.asString());
      for (Annotation<?> annotation : annotations) {
        docString.replace(annotation.get(Annotation.StartOffset).intValue(), annotation.get(Annotation.EndOffset).intValue(),
            htmlWrapConcept(annotation));
      }
      final EscapeTool esc = new EscapeTool();
      final String attributeTooltips = annotations.stream().map(this::makeAttrToolTip).reduce("", (a, b) -> a + b + "\n");
      context.put("docString", esc.html(docString.toString().trim()).replaceAll("\\{\\{", "<").replaceAll("\\}\\}", ">")
          .replaceAll("\\|\\|", "\""));
      context.put("attrs", esc.html(attributeTooltips.trim()).replaceAll("\\{\\{", "<").replaceAll("\\}\\}", ">")
          .replaceAll("\\|\\|", "\""));
    }
    try {
      context.put("doc", SolrWrapper.INSTANCE.getReportById(docid));
    } catch (SolrServerException e) {
      throw new IOException(e);
    }


    response.setStatus(HttpServletResponse.SC_OK);
    final PrintWriter out = response.getWriter();
    final Template template;
    try {
      template = ve.getTemplate( "eeg_report.vm" );
      template.merge(context, out);
    } catch (Exception e) {
      throw new IOException(e);
    }
  }

  @SuppressWarnings("Duplicates")
  private void doSearch(HttpServletRequest request, HttpServletResponse response)
      throws IOException, ServletException, SolrServerException {
    final long startTime = System.currentTimeMillis();

    // Determine start & end offsets
    final int startOffset = Optional.ofNullable(request.getParameter("start"))
        .map(Integer::parseInt)
        .orElse(0);

    final int pageNumber = startOffset / maxSearchResultsPerPage + 1;


    // Parse the query
    final String query = request.getParameter("q");
    final Consumer<SolrQuery> queryOptionHandler = parsedQuery -> {
      SolrWrapper.INSTANCE.setHighlighting(parsedQuery, "<em>", "</em>", 256);
      parsedQuery.setRows(maxSearchResults);
      parsedQuery.setStart(startOffset);
    };

    VelocityContext context = createContext();

    final MercurySearchEngine engine = loadSearchEngine(request);
    final SolrResultList results = engine.search(query, maxSearchResults, queryOptionHandler);
    final QueryResponse queryResponse = results.getResponse();

    final long numTotalResults = results.size();
    int pagesAvailable = (int) (numTotalResults / maxSearchResultsPerPage + 1);
    context.put("debug",queryResponse.getDebugMap());
    context.put("response", queryResponse);
    context.put("results", results);
    context.put("numResults", numTotalResults);
    context.put("pagesAvailable", pagesAvailable);

//    log("Found " + results + " results for " + parsedQuery);

    context.put("resultsPerPage", maxSearchResultsPerPage);
    context.put("pageNum", pageNumber);
    IntStream slip;
    if (pageNumber <= 3 ) {
      slip = IntStream.rangeClosed(1, Math.min(pagesAvailable, 5));
    } else {
      slip = IntStream.rangeClosed(pageNumber - 2, Math.min(pagesAvailable, pageNumber + 2));
    }
    context.put("slip", slip.toArray());


    context.put("qTime", (System.currentTimeMillis() - startTime) / 1_000d);
    context.put("q", query);
    context.put("pq", query);

    response.setStatus(HttpServletResponse.SC_OK);
    final PrintWriter out = response.getWriter();
    try {
      final Template template = ve.getTemplate("search.vm");
      template.merge(context, out);
    } catch (Exception e) {
      throw new IOException(e);
    }
  }

  private MercurySearchEngine loadSearchEngine(HttpServletRequest request) {
    final double solrWeight = readWeight(request.getParameter("solrw"));
    final double kgWeight = readWeight(request.getParameter("kgw"));
    final double signalWeight = readWeight(request.getParameter("sigw"));
    return new MercurySearchEngine(conceptParser, RelevanceModels.getCombination(solrWeight, kgWeight, signalWeight));
  }

  private double readWeight(String s) {
    return Strings.isNullOrEmpty(s) ? 0.0 : Double.parseDouble(s);
  }

  public static SolrDocumentList findTopKHeap(SolrDocumentList list, int k, final Map<String, Double> scoreMap) {
    PriorityQueue<SolrDocument> pq = new PriorityQueue<>((sd1, sd2) ->
        Double.compare(scoreMap.get((String)sd2.get("record_id")), scoreMap.get((String)sd1.get("record_id"))));
    for (SolrDocument doc : list) {
      if (pq.size() < k) pq.add(doc);
      else if (scoreMap.get((String)pq.peek().get("record_id")) < scoreMap.get((String)doc.get("record_id"))) {
        pq.poll();
        pq.add(doc);
      }
    }
    final SolrDocumentList results = new SolrDocumentList();
    for (int i =0; i < k; i++) results.add(pq.poll());
    return results;

  }

  @SuppressWarnings("Duplicates")
  private void doVanillaSearch(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
    final long startTime = System.currentTimeMillis();

    // Determine start & end offsets
    final int startOffset = Optional.ofNullable(request.getParameter("start"))
        .map(Integer::parseInt)
        .orElse(0);

    final int pageNumber = startOffset / maxSearchResultsPerPage + 1;

    // Parse the query
    final String query = request.getParameter("q");
    final SolrQuery parsedQuery = Optional.ofNullable(request.getParameter("pq"))
        .map(SolrQuery::new)
        .orElse(parser.parse(query));
    SolrWrapper.INSTANCE.setHighlighting(parsedQuery, "<em>", "</em>", 256);
    parsedQuery.setRows(maxSearchResultsPerPage);
    parsedQuery.setStart(startOffset);

    VelocityContext context = createContext();

    int pagesAvailable;
    try {
      final QueryResponse queryResponse = SolrWrapper.INSTANCE.query(parsedQuery);
      final SolrDocumentList results = queryResponse.getResults();
      // pigdog
      final long numTotalResults = results.getNumFound();
      pagesAvailable = (int) (numTotalResults / maxSearchResultsPerPage + 1);
      context.put("debug",queryResponse.getDebugMap());
      context.put("response", queryResponse.getResponse());
      context.put("results", results);
      context.put("numResults", numTotalResults);
      context.put("pagesAvailable", pagesAvailable);

      log("Found " + queryResponse.getResults() + " results for " + parsedQuery);
    } catch (SolrServerException e) {
      throw new ServletException(e);
    }

    context.put("resultsPerPage", maxSearchResultsPerPage);
    context.put("pageNum", pageNumber);
    IntStream slip;
    if (pageNumber <= 3 ) {
      slip = IntStream.rangeClosed(1, Math.min(pagesAvailable, 5));
    } else {
      slip = IntStream.rangeClosed(pageNumber - 2, Math.min(pagesAvailable, pageNumber + 2));
    }
    context.put("slip", slip.toArray());


    context.put("qTime", (System.currentTimeMillis() - startTime) / 1_000d);
    context.put("q", query);
    context.put("pq", parsedQuery.getQuery());

    response.setStatus(HttpServletResponse.SC_OK);
    final PrintWriter out = response.getWriter();
    try {
      final Template template = ve.getTemplate("search.vm");
      template.merge(context, out);
    } catch (Exception e) {
      throw new IOException(e);
    }
  }

  private String htmlWrapConcept(Annotation<?> concept) {
    return "{{span id=||a" + concept.getGateId() + "|| class=||concept " +
        ((concept instanceof Event) ? ((Event)concept).get(Event.type).toLowerCase() : "activity")
        + "||}}" + concept.toString() + "{{/span}}";
  }

  private String makeAttrToolTip(Annotation<?> concept) {
    // define tooltip
    // <div class="mdl-tooltip attr hidden" data-mdl-for="aXXX">
    final StringBuilder sb = new StringBuilder("{{div class=||mdl-tooltip attr hidden|| data-mdl-for=||a");
    sb.append(concept.getGateId()).append("||}}");
    // define table
    // <table class="attr-tbl">
    sb.append("{{table class=||attr-tbl||}}");
    if (concept instanceof Event) {
      final Event event = (Event) concept;
      sb.append(tableRow("TYPE:", event.get(Event.type)))
          .append(tableRow("MOD:", event.get(Event.modality)))
          .append(tableRow("POL:", event.get(Event.polarity)));
    } else if (concept instanceof EegActivity) {
      final EegActivity activity = (EegActivity) concept;
      sb.append(tableRow("WAVEFORM:", activity.get(EegActivity.morphology)))
          .append(tableRow("FREQ BAND:", activity.get(EegActivity.band)))
          .append(tableRow("HEMISPHERE:", activity.get(EegActivity.hemisphere)))
          .append(tableRow("DISPERSAL:", activity.get(EegActivity.dispersal)))
          .append(tableRow("RECURRENCE:", activity.get(EegActivity.recurrence)))
          .append(tableRow("MAGNITUDE:", activity.get(EegActivity.magnitude)))
          .append(tableRow("BACKGROUND:", activity.get(EegActivity.in_background)))
          .append(tableRow("LOCATION(S):", activity.get(EegActivity.location)))
          .append(tableRow("MOD:", activity.get(EegActivity.modality)))
          .append(tableRow("POL:", activity.get(EegActivity.polarity)));
    }
    else {
      throw new RuntimeException("concept is neither Event nor EegActivity: " + concept.describe());
    }
    sb.append("{{/table}}");
    sb.append("{{/div}}");
    return sb.toString();
  }

  /**
   *
   * @param key
   * @param value
   * @return
   */
  private String tableRow(final String key, final String value) {
    return "{{tr}}" +
        "{{td}}{{span class=||attr-td|| style=||font-weight: bold||}}" + key + "{{/span}}{{/td}}" +
        "{{td}}{{span class=||attr-td||}}" + format(value) +"{{span}}{{/td}}" +
        "{{/tr}}";
  }

  // Turns 'SPIKE_AND_SLOW_WAVE' into 'Spike And Slow Wave'
  private static final Splitter splitter = Splitter.on('_');
  private String format(final String str) {
    if (str == null) {
      return "None";
    }
    return splitter.splitToList(str.toLowerCase()).stream().map(s -> s.substring(0,1).toUpperCase() + s.substring(1))
        .reduce("", (s1, s2) -> s1 + s2 + " ").trim();
  }

  /**
   * <div class="mdl-list__item">
   *   <span class="mdl-list__item-primary-content">Key</span>
   *   <span class="mdl-list__item-secondary-action">Value</span>
   * </div>
   * @param key key
   * @param value value
   * @return the above html populated with the passed key and value
   */
  private String listItem(final String key, final String value) {
    return "{{li class=||mdl-list__item||}}" +
        "{{span class=||mdl-list__item-primary-content||}}" + key + "{{/span}}" +
        "{{span class=||mdl-list__item-secondary-content||}}" + value +"{{/span}}" +
        "{{/li}}";
  }
}
