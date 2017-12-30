package edu.utdallas.hltri.mercury.api;

import com.google.common.base.Strings;
import edu.utdallas.hltri.mercury.*;
import edu.utdallas.hltri.conf.Config;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.SolrException;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.util.Arrays;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static edu.utdallas.hltri.mercury.relevance.RelevanceModels.RelevanceModel;

public class ApiServlet extends HttpServlet {
  static final Config config = Config.load("eeg.mercury");

  private final MercurySearchEngine searchEngine;
//  private final ApiKeyManager apiKeyManager = new ApiKeyManager(config.getString("apikeys"));
  private final RelevanceModel relevanceModel = RelevanceModel.solr;

  //  private final String coreName = config.getString("solr-core-name");
  //  private String contextPath;

  public ApiServlet() {
    searchEngine = new MercurySearchEngine(new ConceptCohortQueryParser(), relevanceModel.getScorers());
  }

  @Override
  public void init() throws ServletException {
    super.init();
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
        doSearch(request, response);
        break;
      default:
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
    }
  }


  private static final int MAX_QUERY_LENGTH = 2048;
  private final CharsetEncoder asciiEncoder = Charset.forName("US-ASCII").newEncoder();
  private String validateQuery(String query) throws IllegalArgumentException {
    final boolean isAscii = asciiEncoder.canEncode(query);
    if (!isAscii) {
      throw new IllegalArgumentException("query parameter contains non-ascii characters");
    }
    final boolean isNullOrEmpty = Strings.isNullOrEmpty(query);
    if (isNullOrEmpty) {
      throw new IllegalArgumentException("query parameter is null or empty");
    }
    final boolean isTooLong = query.length() > MAX_QUERY_LENGTH;
    if (isTooLong) {
      throw new IllegalArgumentException("query parameter exceeded maximum length");
    }
    System.out.println("Validated query: " + query);
    return query;
  }

  private final static int DEFAULT_RETURN_SIZE = 10;
  private final static int MAX_RETURN_SIZE = 1_000;
  private final static int MAX_RETURN_STRING_SIZE = Integer.toString(MAX_RETURN_SIZE).length();
  private int validateNumReturned(String numReturned) {
    final boolean isNullOrEmpty = Strings.isNullOrEmpty(numReturned);
    if (isNullOrEmpty) {
      return DEFAULT_RETURN_SIZE;
    }

    final boolean isTooLong = numReturned.length() > MAX_RETURN_STRING_SIZE;
    if (isTooLong) {
      throw new IllegalArgumentException("num returned parameter exceeded parsable length");
    }

    int parsedNum;
    try {
      parsedNum = Integer.parseInt(numReturned);
    } catch (Throwable t) {
      throw new IllegalArgumentException("num returned must be a positive integer");
    }

    final boolean isNegative = parsedNum < 0;
    if (isNegative) {
      throw new IllegalArgumentException("num returned must be a positive integer");
    }

    if (parsedNum > MAX_RETURN_SIZE) {
      return DEFAULT_RETURN_SIZE;
    }

    return parsedNum;
  }

  private final static int DEFAULT_START = 0;
  private final static int MAX_START_OFFSET = 16_339;
  private final static int MAX_START_STRING_LENGTH = Integer.toString(MAX_START_OFFSET).length();
  private int validateStartOffset(String startOffset) {
    final boolean isNullOrEmpty = Strings.isNullOrEmpty(startOffset);
    if (isNullOrEmpty) {
      return DEFAULT_START;
    }

    final boolean isTooLong = startOffset.length() > MAX_START_STRING_LENGTH;
    if (isTooLong) {
      throw new IllegalArgumentException("start offset parameter exceeded parsable length");
    }

    int parsedStart;
    try {
      parsedStart = Integer.parseInt(startOffset);
    } catch (Throwable t) {
      throw new IllegalArgumentException("start offset must be a positive integer");
    }

    final boolean isNegative = parsedStart < 0;
    if (isNegative) {
      throw new IllegalArgumentException("start offset must be a positive integer");
    }

    if (parsedStart > MAX_START_OFFSET) {
      throw new IllegalArgumentException("start offset exceeded maximum collection size");
    }

    return parsedStart;
  }

//  private String validateApiKey(String apikey) {
//    final boolean isAscii = asciiEncoder.canEncode(apikey);
//    if (!isAscii) {
//      throw new IllegalArgumentException("apikey parameter contains non-ascii characters");
//    }
//
//    final boolean isNullOrEmpty = Strings.isNullOrEmpty(apikey);
//    if (isNullOrEmpty) {
//      throw new IllegalArgumentException("apikey parameter is null or empty");
//    }
//
//    if (!apiKeyManager.hasAccess(apikey)) {
//      throw new IllegalArgumentException("invalid apikey: " + apikey);
//    }
//
//    return apikey;
//  }


  private void doSearch(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
    try {
      System.out.println("Doing search...");
//      final String apikey = validateApiKey(request.getParameter("apikey"));

      final int numRecords = validateNumReturned(request.getParameter("limit"));

      final int startOffset = validateStartOffset(request.getParameter("start"));

      final String query = validateQuery(request.getParameter("query"));
      final Consumer<SolrQuery> queryOptionsHandler = parsedQuery -> {
        parsedQuery.add("fl", "record_id");
        parsedQuery.setStart(startOffset);
        parsedQuery.set("wt", "json");
      };

      final SolrResultList results = searchEngine.search(query, numRecords, queryOptionsHandler);
//      QueryResponse queryResponse = searchResult.getResponse();
//      final SolrDocumentList results = queryResponse.getResults();
      final int numFound = results.getTotalHits();
      System.out.println("Found " + numFound + " results...");

      response.setStatus(HttpServletResponse.SC_OK);
      final PrintWriter out = response.getWriter();
      out.append("{\"numFound\"=\"")
          .append(Integer.toString(numFound))
          .append("\",\"numReturned\"=\"")
          .append(Integer.toString(numRecords))
          .append("\",\"start\"=\"")
          .append(Integer.toString(startOffset))
          .append("\",\"results\"=[");
      for (int i = 0; i < results.size(); i++) {
        final SolrDocument result = results.get(i).getValue();
        if (i > 0) {
          out.append(",");
        }
        out.append("{\"record_id\"=\"")
            .append(result.getFieldValue("record_id").toString())
            .append("\",\"relative_path\"=\"")
            .append(result.getFieldValue("relative_path").toString())
            .append("\"}");
      }
      out.append("]}");
    } catch (IllegalArgumentException e) {
      response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
      final PrintWriter out = response.getWriter();
      out.write("{\"error\"=\"" + e.getMessage() + "\"}");
    } catch (SolrException e ) {
      response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
      final PrintWriter out = response.getWriter();
      out.write("{\"error\"=\"solr error:" + e.getLocalizedMessage() + "\"}");
    } catch (IOException e) {
      response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
      final PrintWriter out = response.getWriter();
      out.write("{\"error\"=\"invalid server error\"}");
    } catch (Throwable t) {
      response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
      System.out.printf("Error: %s\nStack Trace: %s\n", t.toString(), Arrays.stream(t.getStackTrace())
          .map(StackTraceElement::toString).collect(Collectors.joining("\n")));
      final PrintWriter out = response.getWriter();
      out.write("{\"error\"=\"invalid request\"}");
    }
  }
}
