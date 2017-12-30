package edu.utdallas.hlt.trecmed.offline;

import com.google.common.base.Splitter;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import edu.utdallas.hlt.medbase.ICD9Resolver;
import java.io.BufferedReader;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.document.IntPoint;
import org.apache.lucene.document.LongPoint;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexOptions;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.search.similarities.ClassicSimilarity;
import org.apache.lucene.store.NIOFSDirectory;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;

/**
 *
 * @author travis
 */
public class LuceneEMRIndexer implements Closeable {
  private final SAXBuilder builder = new SAXBuilder();
  private final IndexWriter              visits;
  private final IndexWriter              reports;
  private final Multimap<String, String> mappingV2R;
  private final Map<String, String>      mappingR2V;
  private final Map<String, Document>    mappingV2D;

  public LuceneEMRIndexer(Path visitsPath, Path reportsPath, Path mappingFile) {
    try {
      System.out.printf("Creating visit-index at %s%n", visitsPath);
      visits = new IndexWriter(NIOFSDirectory.open(visitsPath),
                               new IndexWriterConfig(new LuceneEMRAnalyzer())
                                 .setOpenMode(OpenMode.CREATE)
                                 .setSimilarity(new ClassicSimilarity()));
      System.out.printf("Creating report-index at %s%n", reportsPath);
      reports = new IndexWriter(NIOFSDirectory.open(reportsPath),
                                new IndexWriterConfig(new LuceneEMRAnalyzer())
                                  .setOpenMode(OpenMode.CREATE)
                                  .setSimilarity(new ClassicSimilarity()));

      mappingR2V = Maps.newHashMap();
      mappingV2R = ArrayListMultimap.create();
      mappingV2D = Maps.newHashMap();
      final Splitter splitter = Splitter.on('\t');
      Iterator<String> it;
      String report;
      //noinspection unused
      String type;
      String visit;
      try (BufferedReader reader = Files.newBufferedReader(mappingFile, Charset.defaultCharset())) {
        for (String line; (line = reader.readLine()) != null; ) {
          it = splitter.split(line).iterator();
          report = it.next();
          //noinspection UnusedAssignment
          type = it.next();
          visit = it.next().intern();
          mappingV2R.put(visit, report);
          mappingR2V.put(report, visit);
          if (!mappingV2D.containsKey(visit)) mappingV2D.put(visit, new Document());
        }
      } catch (IOException ex) {
        throw new RuntimeException(ex);
      }
      System.out.printf("Parsed (%,d : %,d, %,d) mappings from %s%n", mappingR2V.size(), mappingV2R.size(), mappingV2D.size(), mappingFile);
    } catch (IOException ex) {
      throw new RuntimeException(ex);
    }
  }

  private ICD9Resolver resolver;

  private void setICD9Resolver(final ICD9Resolver resolver) {
    this.resolver = resolver;
  }

  private String convertICD9s(final String text) {
    StringBuilder sb = new StringBuilder();
    for (String icd9 : Splitter.on(',').split(text)) {
      if (sb.length() > 0) { sb.append('\n'); }
      sb.append(resolver.decode(icd9));
    }
    return sb.toString();
  }

  private final static DateFormat df = new SimpleDateFormat("yyyy-MM-dd");

  private final static FieldType vectorizedTextField = new FieldType(TextField.TYPE_STORED);
  static {
    vectorizedTextField.setIndexOptions(IndexOptions.DOCS_AND_FREQS_AND_POSITIONS_AND_OFFSETS);
    vectorizedTextField.setTokenized(true);
    vectorizedTextField.setStored(true);
    vectorizedTextField.setStoreTermVectors(true);
    vectorizedTextField.setStoreTermVectorPositions(true);
    vectorizedTextField.setStoreTermVectorOffsets(true);
    vectorizedTextField.setStoreTermVectorPayloads(true);
    vectorizedTextField.freeze();
  }

  public void index(final File file) {
    try {
//      System.err.printf("\rOpening %s%n", file);
      final Element document = builder.build(file).getRootElement();
//        System.err.printf("\rReading %s%n", document);
        final String admit_icd9_text = convertICD9s(document.getChildText("admit_diagnosis")),
                     disch_icd9_text = convertICD9s(document.getChildText("discharge_diagnosis")),
                     report_text = document.getChildText("report_text"),
                     chief_complaint = document.getChildText("chief_complaint"),
                     checksum = document.getChildText("checksum");

        if (mappingR2V.get(checksum) == null) {
          System.err.printf("\rEncountered orphaned EMR: %s for checksum %s%n", file, checksum);
          return;
        }

        final String visit_id = mappingR2V.get(checksum);

//        System.err.printf("\rReport: %s | Visit: %s%n", checksum, visit_id);
        final List<Field> fields = Arrays.asList(
          new StringField("checksum", checksum, Field.Store.YES),
          new StringField("type", document.getChildText("type"), Field.Store.YES),
          new StringField("subtype", document.getChildText("subtype"), Field.Store.YES),
          new Field("chief_complaint", chief_complaint, vectorizedTextField),
          new TextField("admit_diagnosis", document.getChildText("admit_diagnosis"), Field.Store.YES),
          new Field("admit_diagnosis_text", admit_icd9_text, vectorizedTextField),
          new TextField("discharge_diagnosis", document.getChildText("discharge_diagnosis"), Field.Store.YES),
          new Field("discharge_diagnosis_text", disch_icd9_text, vectorizedTextField),
          new IntPoint("year", Integer.valueOf(document.getChildText("year"))),
          new LongPoint("download_time", df.parse(document.getChildText("downlaod_time")).getTime()),
          new Field("report_text", report_text, vectorizedTextField),
          new Field("full_text", admit_icd9_text + disch_icd9_text + chief_complaint + report_text, vectorizedTextField),
          new StringField("file_name", file.getName(), Field.Store.YES),
          new StringField("path", file.getCanonicalPath(), Field.Store.YES),
          new StringField("visit_id", visit_id, Field.Store.YES));

        final Document report = new Document();
        final Document visit = mappingV2D.get(visit_id);
        for (Field field : fields) {
          report.add(field);
          visit.add(field);
        }
        reports.addDocument(report);
        mappingV2R.remove(visit_id, checksum);
        if (!mappingV2R.containsKey(visit_id)) {
          visits.addDocument(visit);
          mappingV2D.remove(visit_id);
        }
        mappingR2V.remove(checksum);
    } catch (JDOMException | ParseException | IOException ex) {
      throw new RuntimeException(ex);
    }
  }

  @Override
  public void close() {
    try {
      visits.commit();
      visits.close();
      reports.commit();
      reports.close();
    } catch (IOException ex) {
      throw new RuntimeException(ex);
    }
  }

  public static void main(String... args) throws Exception {
    System.out.println(Arrays.deepToString(args));
    final File emrSourcePath = Paths.get(args[0]).toFile();
    final Path emrMappingPath = Paths.get(args[1]);
    final Path icd9MappingPath = Paths.get(args[2]);
    final Path visitIndexOutputPath = Paths.get(args[3]);
    final Path reportIndexOutputPath = Paths.get(args[4]);

    try (final LuceneEMRIndexer indexer = new LuceneEMRIndexer(visitIndexOutputPath, reportIndexOutputPath, emrMappingPath)) {
      indexer.setICD9Resolver(ICD9Resolver.getResolver(icd9MappingPath));
      final AtomicLong lastCommit = new AtomicLong(System.currentTimeMillis());
      final AtomicInteger count = new AtomicInteger(1);

      final File[] files = emrSourcePath.listFiles();
      assert files != null;
      for (final File file : files) {
        System.out.printf("\rIndexing file #%,8d: %48s... ", count.getAndIncrement(), file.getName());
        indexer.index(file);
        long time = System.currentTimeMillis();
        if (TimeUnit.MINUTES.convert(time - lastCommit.get(),
            TimeUnit.MILLISECONDS) >= 1) {
          System.out.printf("\rCommitting index... (last commit was at %1$tH:%1$tM:%1$tS) %30s%n",
              new Date(lastCommit.get()));
          indexer.visits.commit();
          indexer.reports.commit();
          lastCommit.set(time);
        }
      }
      System.out.printf("\rIndexed %,d files.%80s%n", count.get(), "");
    }
  }
}
