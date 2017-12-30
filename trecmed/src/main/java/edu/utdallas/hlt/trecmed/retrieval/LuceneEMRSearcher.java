package edu.utdallas.hlt.trecmed.retrieval;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

import gnu.trove.map.TObjectFloatMap;
import gnu.trove.map.hash.TObjectFloatHashMap;

import javax.annotation.Nonnull;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

import edu.utdallas.hlt.trecmed.Report;
import edu.utdallas.hlt.trecmed.Visit;
import edu.utdallas.hlt.trecmed.offline.LuceneEMRAnalyzer;
import edu.utdallas.hlt.util.Config;
import edu.utdallas.hltri.inquire.lucene.LuceneSearchEngine;
import edu.utdallas.hltri.logging.Logger;

/**
 * @author travis
 */
@SuppressWarnings({"unused", "WeakerAccess", "SpellCheckingInspection"})
public class LuceneEMRSearcher {
  private static final Logger log = Logger.get(LuceneEMRSearcher.class);

  public final static String TEXT_FIELDNAME                = "report_text";
  public final static String CHIEF_COMPLAINT_FIELDNAME     = "chief_complaint";
  public final static String DISCHARGE_DIAGNOSIS_FIELDNAME = "discharge_diagnosis";
  public final static String ADMISSION_DIAGNOSIS_FIELDNAME = "admit_diagnosis";
  public final static String VISIT_ID_FIELDNAME            = "visit_id";
  public final static String CHECKSUM_FIELDNAME            = "checksum";
  public final static String TYPE_FIELDNAME                = "type";
  public final static String SUBTYPE_FIELDNAME             = "subtype";
  public final static String FILE_NAME_FIELDNAME           = "file_name";
  public final static String PATH_FIELDNAME                = "path";
  public final static String FULL_TEXT_FIELDNAME           = "full_text";

  public final static Term TEXT_FIELD                = new Term(TEXT_FIELDNAME);
  public final static Term CHIEF_COMPLAINT_FIELD     = new Term(CHIEF_COMPLAINT_FIELDNAME);
  public final static Term DISCHARGE_DIAGNOSIS_FIELD = new Term(DISCHARGE_DIAGNOSIS_FIELDNAME);
  public final static Term ADMISSION_DIAGNOSIS_FIELD = new Term(ADMISSION_DIAGNOSIS_FIELDNAME);


  public static class VisitSearchEngine extends LuceneSearchEngine<Visit> {
    private VisitSearchEngine() {
      super(Config.get(LuceneEMRSearcher.class, "VISITS").toString(),
          new LuceneEMRAnalyzer(),
          FULL_TEXT_FIELDNAME,
          Visit::fromLucene);
      log.info("Creating visit searcher at {}", Config.get(LuceneEMRSearcher.class, "VISITS"));
    }


    public int getDocId(String visitID) {
      return withSearcher(searcher -> {
        try {
          Query q = new TermQuery(new Term(VISIT_ID_FIELDNAME, visitID));
          return searcher.search(q, 1).scoreDocs[0].doc;
        } catch (IOException | ArrayIndexOutOfBoundsException ex) {
          log.error("Unable to find visit id |{}|", visitID, ex);
          throw new RuntimeException(ex);
        }
      });
    }
  }

  public static class ReportSearchEngine extends LuceneSearchEngine<Report> {
    private ReportSearchEngine() {
      super(Config.get(LuceneEMRSearcher.class, "REPORTS").toString(),
          new LuceneEMRAnalyzer(),
          FULL_TEXT_FIELDNAME,
          Report::fromLucene);
      log.info("Creating report searcher at {}",
          Config.get(LuceneEMRSearcher.class, "REPORTS"));
    }

    private class ReportCacheLoader extends CacheLoader<Query, TObjectFloatMap<String>> {
      public TObjectFloatMap<String> load(final @Nonnull Query query) throws IOException {
        return withSearcher(searcher -> {
          TObjectFloatMap<String> scores = new TObjectFloatHashMap<>();
          try {
            for (ScoreDoc sd : searcher.search(query, 95701).scoreDocs) {
              scores.put(searcher.doc(sd.doc).get("checksum"), sd.score);
            }
          } catch (IOException ex) {
            throw new RuntimeException(ex);
          }
          return scores;
        });
      }
    }


    private final LoadingCache<Query, TObjectFloatMap<String>> reports =
        CacheBuilder.newBuilder()
            .maximumSize(100)
            .concurrencyLevel(Runtime.getRuntime().availableProcessors())
            .build(new ReportCacheLoader());

    public int getDocId(final String checksum) {
      return withSearcher(searcher -> {
        try {
          Query q = new TermQuery(new Term(CHECKSUM_FIELDNAME, checksum));
          return searcher.search(q, 1).scoreDocs[0].doc;
        } catch (IOException | ArrayIndexOutOfBoundsException ex) {
          log.error("Unable to find report for checksum |{}|", checksum, ex);
          throw new RuntimeException(ex);
        }
      });
    }

    @SuppressWarnings("unused")
    public double scoreReport(Query query, Report report) {
      try {
        return reports.get(query).get(report.getId());
      } catch (ExecutionException ex) {
        throw new RuntimeException(ex);
      }
    }
  }

  private static VisitSearchEngine  visitInstance  = null;
  private static ReportSearchEngine reportInstance = null;

  public static VisitSearchEngine getVisitSearcher() {
    if (visitInstance == null) {
      visitInstance = new VisitSearchEngine();
    }
    return visitInstance;
  }

  public static ReportSearchEngine getReportSearcher() {
    if (reportInstance == null) {
      reportInstance = new ReportSearchEngine();
    }
    return reportInstance;
  }

}
