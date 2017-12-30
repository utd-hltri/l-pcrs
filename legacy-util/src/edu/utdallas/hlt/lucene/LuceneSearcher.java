package edu.utdallas.hlt.lucene;

import com.google.common.collect.Sets;
import edu.utdallas.hlt.util.ANSIColors;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import jline.ArgumentCompletor;
import jline.ConsoleReader;
import jline.History;
import jline.SimpleCompletor;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Fieldable;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.NIOFSDirectory;
import org.apache.lucene.util.Version;

import static edu.utdallas.hlt.util.ANSIColors.Color.*;
import static edu.utdallas.hlt.util.ANSIColors.Style.BRIGHT;

/**
 *
 * @author travis
 */
public class LuceneSearcher {

  private static final File HISTORY_FILE = new File(System.getProperty("user.home"), ".lucene_searcher_history");
  private final int SHOWN_RESULTS;
  private final int MAX_RESULTS;
  private final File INDEX_FILE;
  private final String PATH_FIELD;
  private final String DESC_FIELD;
  private final String VIEW_COMMAND;
  protected final Version LUCENE_VERSION;
  protected Analyzer ANALYZER;
  protected IndexReader reader;
  protected IndexSearcher searcher;
  private List<String> prevResults = Collections.EMPTY_LIST;
  private String prevQuery;

  public LuceneSearcher(String path) {
    Configuration config = loadProperties(path);
    INDEX_FILE = new File(path);
    try {
      SHOWN_RESULTS = config.getInt("shown_results", 20);
      MAX_RESULTS = config.getInt("max_results", 500);
      PATH_FIELD = config.getString("path_field", "path");
      DESC_FIELD = config.getString("desc_field", "path");
      LUCENE_VERSION = Version.valueOf(config.getString("version", "LUCENE_35"));
      VIEW_COMMAND = config.getString("view_cmd", "/usr/bin/less +/\"%2$s\" %1$s");
      ANALYZER = new StandardAnalyzer(LUCENE_VERSION);
      Class<?> clazz = Class.forName(config.getString("analyzer"));
      for (Constructor<?> cons : clazz.getConstructors()) {
        switch (cons.getParameterTypes().length) {
          case 0:
            ANALYZER = (Analyzer) cons.newInstance();
            break;
          case 1:
            ANALYZER = (Analyzer) cons.newInstance(LUCENE_VERSION);
            break;
//          default:
//            throw new RuntimeException("Unable to find suitable constructor for analyzer " + config.getString("analyzer") + ". Found " + cons.toGenericString());
        }
      }
      Directory directory = NIOFSDirectory.open(INDEX_FILE);
      reader = IndexReader.open(directory, true);
      searcher = new IndexSearcher(reader);
    } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | InvocationTargetException | RuntimeException | IOException ex) {
      throw new RuntimeException(ex);
    }
  }

  private static Configuration loadProperties(String path) {
    try {
      return new PropertiesConfiguration(new File(path, "index.properties"));
    } catch (ConfigurationException ex) {
      throw new RuntimeException(ex);
    }
  }

  public void search(Query query) {
    try {
      TopDocs search = searcher.search(query, MAX_RESULTS);
      System.out.print(ANSIColors.color(BRIGHT, GREEN, "Query: "));
      System.out.println(query);
      System.out.print(ANSIColors.color(BRIGHT, BLUE, "Results: "));
      System.out.println("Top " + Math.min(SHOWN_RESULTS, search.scoreDocs.length) + " from " + search.scoreDocs.length + " retrieved with " + search.totalHits + " total hits.");
      int i = 1;
      prevResults = new ArrayList<>();
      String path = null;
      for (ScoreDoc result : search.scoreDocs) {
        Document d = searcher.doc(result.doc);
        Fieldable f = d.getFieldable(PATH_FIELD);

        if (f != null) {
          prevResults.add(f.stringValue());
        }

        if (i > SHOWN_RESULTS) {
          continue;
        }

        System.out.print("  ");
        System.out.print(ANSIColors.color(BRIGHT, MAGENTA, String.format("%2s. ", i++)));
        System.out.print(ANSIColors.color(BRIGHT, WHITE, String.format("%10f", result.score)));

        f = d.getFieldable(DESC_FIELD);
        if (f != null) {
          System.out.print(" " + f.stringValue());
        }
        System.out.println();
      }
    } catch (IOException ex) {
      System.out.println(ANSIColors.color(BRIGHT, YELLOW, "Error: ") + "Unable to search index: " + ex.getMessage());
    }
  }

  public void search(String field, String string) {
    try {
      Query query = new QueryParser(LUCENE_VERSION, field, ANALYZER).parse(string);
            Set<Term> terms = Sets.newHashSet();
      query.rewrite(reader).extractTerms(terms);
      StringBuilder sb = new StringBuilder();
      for (Term term : terms) {
        if (sb.length() > 0) { sb.append("|"); }
        sb.append(term.text());
      }
      prevQuery = sb.toString();
      search(query);
    } catch (IOException ex) {
      throw new RuntimeException(ex);
    } catch (ParseException ex) {
      System.out.println(ANSIColors.color(BRIGHT, YELLOW, "Error: ") + "Unable to parse query: " + ex.getMessage());
    }
  }

  public void view(int index) {
    if (prevResults.isEmpty()) {
      System.out.println(ANSIColors.color(BRIGHT, YELLOW, "Error: ") + "You must search something that yields results before you may view any results.");
      return;
    }

    try {
      System.out.println(ANSIColors.color(BRIGHT, BLACK, "Opening result " + index + ": ") + prevResults.get(index - 1) + ANSIColors.color(BRIGHT, BLACK, " for external viewing."));
      Process process = Runtime.getRuntime().exec(new String[]{
            "xterm",
            "-e",
            String.format(VIEW_COMMAND, prevResults.get(index - 1), prevQuery)
          });
    } catch (IOException ex) {
      throw new RuntimeException(ex);
    } catch (IndexOutOfBoundsException ex) {
      System.out.println(ANSIColors.color(BRIGHT, YELLOW, "Error: ") + "You may only view a document within the bounds of the previous search results [1, " + prevResults.size() + "].");
    }
  }

  public void shell() {
    try {
      ConsoleReader console = new ConsoleReader();
      console.setHistory(new History(HISTORY_FILE));
      console.setBellEnabled(false);
      console.setUseHistory(false);
      Collection<String> fields = reader.getFieldNames(IndexReader.FieldOption.INDEXED);
      console.addCompletor(new ArgumentCompletor(new SimpleCompletor(fields.toArray(new String[ 0 ]))));
      String line;
      String[] args;
      while ((line = console.readLine(ANSIColors.color(BRIGHT, RED, "Command: "))) != null && !line.equals("exit")) {
        args = line.split(" ", 2);
        if (fields.contains(args[0])) {
          prevQuery = args[1].replaceAll("\\b\\w+:", "").replaceAll("\\s+", "|").replaceAll("[^a-zA-Z_|0-9]", "");
          search(args[0], args[1]);
          console.getHistory().addToHistory(line);
        } else if (args[0].equals("view") || args[0].equals("show")) {
          view(Integer.parseInt(args[1]));
        } else if (args[0].equals("help")) {
          usage();
        } else if (args[0].equals("exit") || args[0].equals("q") || args[0].equals("quit")) {
          break;
        } else {
          usage();
        }
      }
    } catch (IOException ex) {
      throw new RuntimeException(ex);
    }
  }

  public void usage() {
    System.out.println("Commands:\n");
    Set<String> fields = new TreeSet<>(reader.getFieldNames(IndexReader.FieldOption.INDEXED));
    String format = "%-24s# %s%n";
    for (String field : fields) {
      System.out.printf(format, field, "Search using the default field \"" + field + "\"");
    }
    System.out.printf(format, "view <index>", "View last result at given index");
    System.out.printf(format, "help", "View this message");
    System.out.printf(format, "exit", "Exit");
  }

  public static void main(String... args) {
    new LuceneSearcher(args[0]).shell();
  }
}
