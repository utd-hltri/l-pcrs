package edu.utdallas.hltri.mercury;

import com.google.common.base.Throwables;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.common.SolrDocument;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.exception.VelocityException;
import org.apache.velocity.runtime.RuntimeConstants;
import org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader;
import org.apache.velocity.tools.generic.EscapeTool;
import org.apache.velocity.tools.view.WebappResourceLoader;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import edu.utdallas.hltri.conf.Config;

@SuppressWarnings("SqlResolve")
public class JudgmentServlet extends HttpServlet {
  private final static Config eegConfig = Config.load("eeg");
  private final static Config mercuryConfig = eegConfig.getConfig("mercury");

  private final VelocityEngine ve;

  public JudgmentServlet() {
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
  }

  private Map<String, ParsedQuery> parsedQueries;

  @Override
  public void init() throws ServletException {
    super.init();
    try {
      Class.forName("org.h2.Driver");
    } catch (ClassNotFoundException e) {
      throw new ServletException(e);
    }

    ve.setApplicationAttribute("javax.servlet.ServletContext", getServletContext());
    ve.init();

    parsedQueries = ParsedQueryLoader.INSTANCE.load(mercuryConfig.getPath("parsed-queries-path"));
  }

  private Map<String, String> getQueries(final Connection conn) throws SQLException {
    final Map<String, String> queries = new HashMap<>();
    final String sql = "SELECT NAME, TEXT FROM QUERIES";
    try (final Statement stmt = conn.createStatement()) {
      final ResultSet rs = stmt.executeQuery(sql);
      while (rs.next()) {
        final String name = rs.getString("NAME");
        final String query = rs.getString("TEXT");
        queries.put(name, query);
      }
    }
    return queries;
  }

  private List<JudgmentOption> getJudgmentOptions(final Connection conn) throws SQLException {
    final List<JudgmentOption> judgmentOptions = new ArrayList<>();
    final String sql = "SELECT * FROM RELEVANCES";
    try (final Statement stmt = conn.createStatement()) {
      final ResultSet rs = stmt.executeQuery(sql);
      while (rs.next()) {
        final int judgment = rs.getInt("RID");
        final String label = rs.getString("RELEVANCE");
        final String description = rs.getString("DESCRIPTION");
        judgmentOptions.add(new JudgmentOption(judgment, label, description));
      }
    }
    Collections.sort(judgmentOptions, Comparator.comparingInt(JudgmentOption::getJudgment).reversed());
    return judgmentOptions;
  }

  private static final String jdbcUri = mercuryConfig.getString("judgment-db-uri");
  private static final String jdbcUser = "sa",
                              jdbcPass = "sa";
  private Connection getMCJDBConnection() throws SQLException {
    return DriverManager.getConnection(jdbcUri, jdbcUser, jdbcPass);
  }

  private int getQueryId(final Connection conn, final String queryName) throws SQLException {
    final String sql = "SELECT QUERIES.QID FROM QUERIES WHERE QUERIES.NAME = ?";
    try (final PreparedStatement pstmt = conn.prepareStatement(sql)) {
      pstmt.setString(1, queryName);
      pstmt.executeQuery();
      final ResultSet rs = pstmt.getResultSet();
      rs.next();
      return rs.getInt("QID");
    }
  }

  private String getQueryText(final Connection conn, final String queryName) throws SQLException {
    final String sql = "SELECT QUERIES.TEXT FROM QUERIES WHERE QUERIES.NAME = ?";
    try (final PreparedStatement pstmt = conn.prepareStatement(sql)) {
      pstmt.setString(1, queryName);
      pstmt.executeQuery();
      final ResultSet rs = pstmt.getResultSet();
      rs.next();
      return rs.getString("TEXT");
    }
  }

  private List<JudgeableQuery> getJudgments(final Connection conn, final String user) throws SQLException, IOException {
    final Table<String, String, Integer> judgmentTable = HashBasedTable.create();
    final Table<String, String, String> explanationTable = HashBasedTable.create();
    final String sql =
        "SELECT QUERIES.NAME AS QUERY, DID AS REPORTID, RID AS RELEVANCE, EXPLANATIONS " +
            "FROM USERS INNER JOIN JUDGMENTS ON USERS.UID = JUDGMENTS.UID " +
            "           INNER JOIN QUERIES ON JUDGMENTS.QID = QUERIES.QID " +
            "WHERE USERS.NAME = ?";
    try (final PreparedStatement pstmt = conn.prepareStatement(sql)) {
      pstmt.setString(1, user);
      pstmt.executeQuery();
      final ResultSet rs = pstmt.getResultSet();
      while (rs.next()) {
        final String query = rs.getString("QUERY");
        final String reportId = rs.getString("REPORTID");
        final int relevance = Integer.parseInt(rs.getString("RELEVANCE"));
        final String explanations = rs.getString("EXPLANATIONS");
        judgmentTable.put(query, reportId, relevance);
        explanationTable.put(query, reportId, explanations);
      }
    }

    final List<JudgeableQuery> queries = new ArrayList<>();
    for (String qid : judgmentTable.rowKeySet()) {
      final List<JudgeableReport> reports = new ArrayList<>();
      for (String rid : judgmentTable.row(qid).keySet()) {
        final int judgment = judgmentTable.get(qid, rid);
        final String explanation = explanationTable.get(qid, rid);
        reports.add(new JudgeableReport(rid, judgment, explanation));
      }
      queries.add(new JudgeableQuery(qid, getQueryText(conn, qid), reports));
    }
    return queries;
  }

  private void doWelcome(HttpServletRequest request, HttpServletResponse response) throws SQLException, IOException, VelocityException {
    final String user = (String) request.getSession().getAttribute("user");
    final VelocityContext context = new VelocityContext();
    context.put("user", user);
    context.put("esc", new EscapeTool());
    try (final Connection conn = getMCJDBConnection()) {
      final List<JudgeableQuery> judgments = getJudgments(conn, user);
      final Map<String, Integer> nJudged = new HashMap<>();
      for (JudgeableQuery jQuery : judgments) {
        final int n = (int) jQuery.getReports().stream()
            .filter(jReport -> jReport.getJudgment() > -1)
            .count();
        nJudged.put(jQuery.getQueryId(), n);
      }
      context.put("judgments", judgments);
      context.put("queries", getQueries(conn));
      context.put("nJudged", nJudged);
    }

    response.setStatus(HttpServletResponse.SC_OK);
    final PrintWriter out = response.getWriter();
    final Template template;
    template = ve.getTemplate( "welcome.vm" );
    template.merge(context, out);
  }

  private void doJudgment(HttpServletRequest request, HttpServletResponse response) throws IOException, SQLException, SolrServerException, ServletException {
    final VelocityContext context = new VelocityContext();
    context.put("esc", new EscapeTool());
    final String user = (String) request.getSession().getAttribute("user");
    context.put("user", user);
    final String qName = request.getParameter("qid");
    context.put("queryId", qName);
    final String rid = request.getParameter("rid");
    context.put("reportId", rid);

    try (final Connection conn = getMCJDBConnection()) {
      context.put("judgmentOptions", getJudgmentOptions(conn));

      final String sql = "SELECT RID, EXPLANATIONS FROM JUDGMENTS " +
                            "WHERE UID = ? AND DID = ? AND QID = ? ";
      try (final PreparedStatement pstmt = conn.prepareStatement(sql)) {
        pstmt.setInt(1, getUserId(conn, user));
        pstmt.setString(2, rid);
        pstmt.setInt(3, getQueryId(conn, qName));
        pstmt.executeQuery();
        final ResultSet rs = pstmt.getResultSet();
        rs.next();
        final int relevance = rs.getInt("RID");
        final String explanations = rs.getString("EXPLANATIONS");
        context.put("judgment", relevance);
        context.put("explanation", explanations);
      }

      for (JudgeableQuery jQuery : getJudgments(conn, user)) {
        if (Objects.equals(jQuery.getQueryId(), qName)) {
          List<String> reportIds = jQuery.getReports().stream().map(JudgeableReport::getReportId).collect(Collectors.toList());
          int index = reportIds.indexOf(rid);
          if (index > 0) {
            context.put("prev", reportIds.get(index - 1));
          }
          if (index < reportIds.size() - 1) {
            context.put("next", reportIds.get(index + 1));
          }

          final Optional<JudgeableReport> nextJudgeable = jQuery.getReports()
              .stream()
              .filter(jr -> jr.getJudgment() == -1 && !jr.getReportId().equals(rid))
              .findFirst();

          if (nextJudgeable.isPresent()) {
            log("Found next judgable report as " + nextJudgeable.get().getReportId());
            context.put("nextJudgable", nextJudgeable.get().getReportId());
          }
        }
      }

      final String query = getQueryText(conn, qName);
      context.put("q", query);
    }

    final SolrQuery parsedQuery = parsedQueries.get(qName).toSolrQuery();
    if (((Set<String>) request.getSession().getAttribute("groups")).contains("admin")) {
      context.put("admin", true);
      context.put("pq", URLDecoder.decode(parsedQuery.getQuery(), "utf-8"));
    }
    context.put("response", SolrWrapper.INSTANCE.getHighlightedDocument(parsedQuery, rid));
    context.put("esc", new EscapeTool());

    final SolrDocument report = SolrWrapper.INSTANCE.getReportById(rid);
    context.put("doc", report);

    response.setStatus(HttpServletResponse.SC_OK);
    final PrintWriter out = response.getWriter();
    final Template template = ve.getTemplate( "eeg_report_judgment.vm" );
    template.merge(context, out);
  }

  private Set<String> getUserGroups(final Connection conn, final int userId) throws SQLException {
    final Set<String> userGroups = new HashSet<>();
    final String sql = "SELECT ROLES.ROLE FROM "+
        "USER_ROLES INNER JOIN ROLES ON USER_ROLES.ROLE_ID = ROLES.ROLE_ID " +
        "WHERE USER_ROLES.UID = ?";
    try (final PreparedStatement pstmt = conn.prepareStatement(sql)) {
      pstmt.setInt(1, userId);
      final ResultSet rs = pstmt.executeQuery();
      while (rs.next()) {
        userGroups.add(rs.getString("ROLE"));
      }
    }
    log("Found groups " + userGroups + " for user " + userId);
    return userGroups;
  }

  private int getUserId(final Connection conn, final String userName) throws SQLException, ServletException {
    final String sql = "SELECT UID FROM USERS WHERE USERS.NAME = ?";
    try (final PreparedStatement pstmt = conn.prepareStatement(sql)) {
      pstmt.setString(1, userName);
      final ResultSet rs = pstmt.executeQuery();
      if (rs.next()) {
        return rs.getInt("UID");
      }
    }
    throw new ServletException("No user found for user name '" + userName + "'.");
  }

  private void saveJudgment(HttpServletRequest request, HttpServletResponse response) throws IOException {
    final int userName = Integer.parseInt((String) request.getSession().getAttribute("uid"));
    final String reportId = request.getParameter("rid");
    final int judgment = Integer.parseInt(request.getParameter("j"));
    final String explanation = request.getParameter("explanation");

    final String sql = "UPDATE JUDGMENTS SET RID = ?, EXPLANATIONS = ? " +
                         "WHERE UID = ? AND QID = ? AND DID = ?";

    String responseBody;
    try (final Connection conn = getMCJDBConnection()) {
      final int queryId = getQueryId(conn, request.getParameter("qid"));
      try (final PreparedStatement pstmt = conn.prepareStatement(sql)) {
        pstmt.setInt(1, judgment);
        pstmt.setString(2, explanation);
        pstmt.setInt(3, userName);
        pstmt.setInt(4, queryId);
        pstmt.setString(5, reportId);
        pstmt.executeUpdate();
        responseBody = "<b>Success!</b>" +
            "<p>Judgment was saved successfully. Please continue to the next document.</p>";
      }
    } catch (SQLException ex) {
      responseBody = "<b>Error!</b><p>Judgment failed to save.</p>" + "<p>Please e-mail <pre>travis@hlt.utdallas.edu</pre> with the following information:</p>" +
          "<p><b>" + ex.getMessage() + "</b></p>" +
          "<pre>" + Throwables.getStackTraceAsString(ex) + "</pre>";
    }

    response.setHeader("Cache-Control", "no-cache");
    response.setStatus(HttpServletResponse.SC_OK);
    response.getWriter().write(responseBody);
  }

  private void doLogon(HttpServletRequest request, HttpServletResponse response) throws IOException {
    response.setStatus(HttpServletResponse.SC_OK);
    final VelocityContext context = new VelocityContext();
    if (request.getParameter("e") != null) {
      context.put("error", true);
    }
    context.put("source", request.getParameter("source"));
    final PrintWriter out = response.getWriter();
    final Template template;
    try {
      template = ve.getTemplate( "logon.vm" );
      template.merge(context, out);
    } catch (Exception e) {
      throw new IOException(e);
    }
  }

  private void doAuthenticate(HttpServletRequest request, HttpServletResponse response) throws IOException {
    final String userName = request.getParameter("username");
    final String password = request.getParameter("password");
    final String sql = "SELECT * FROM USERS WHERE NAME = ? AND PASS = ?";
    try (final Connection conn = getMCJDBConnection();
         final PreparedStatement pstmt = conn.prepareStatement(sql)) {
      pstmt.setString(1, userName);
      pstmt.setString(2, password);
      final ResultSet rs = pstmt.executeQuery();
      if (rs.next()) {
        final int userId = rs.getInt("UID");
        request.getSession().setAttribute("user", userName);
        request.getSession().setAttribute("uid", Integer.toString(userId));
        request.getSession().setAttribute("groups", getUserGroups(conn, userId));

        final String source = request.getParameter("source");
        if (source != null) {
          response.sendRedirect(URLDecoder.decode(source, "utf-8"));
        } else {
          response.sendRedirect("welcome");
        }
      } else {
        response.sendRedirect("login?e");
      }
    } catch (SQLException e) {
      response.sendRedirect("login?e");
    }
  }

  private void doLogout(HttpServletRequest request, HttpServletResponse response) throws IOException {
    request.getSession().removeAttribute("user");
    request.getSession().removeAttribute("uid");
    response.sendRedirect("login");
  }

  @Override
  public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException {
    response.setContentType("text/html; charset=utf-8");
    response.setHeader("Connection", "close");

    try {
      if (!request.getPathInfo().equals("/login") && request.getSession().getAttribute("user") == null) {
        response.sendRedirect("login?source=" +
            URLEncoder.encode(request.getRequestURI() +
                Optional.ofNullable(request.getQueryString()).map(s -> '?' + s).orElse(""), "utf-8"));
        return;
      }

      switch (request.getPathInfo()) {
        case "/":
        case "/welcome":
          doWelcome(request, response);
          break;
        case "/login":
          doLogon(request, response);
          break;
        case "/logout":
          doLogout(request, response);
          break;
        case "/judge-report":
          doJudgment(request, response);
          break;
        default:
          response.setStatus(HttpServletResponse.SC_NOT_FOUND);
      }
    } catch (ServletException e) {
      log(e.getMessage(), e);
      throw e;
    } catch (IOException | SQLException | SolrServerException e) {
      log(e.getMessage(), e);
      throw new ServletException(e);
    }
  }


  @Override
  public void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException {
    response.setContentType("text/html; charset=utf-8");
    response.setHeader("Connection", "close");
    try {
      if (!request.getPathInfo().equals("/login") && request.getSession().getAttribute("user") == null) {
        response.sendRedirect("login?source=welcome");
        return;
      }
      switch (request.getPathInfo()) {
        case "/save-judgment":
          saveJudgment(request, response);
          break;
        case "/login":
          doAuthenticate(request, response);
          break;
        default:
          response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
      }
    } catch (IOException e) {
      log(e.getMessage(), e);
      throw new ServletException(e);
    }
  }
}
