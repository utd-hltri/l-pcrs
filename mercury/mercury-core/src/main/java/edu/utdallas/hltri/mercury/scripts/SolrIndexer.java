package edu.utdallas.hltri.mercury.scripts;

import edu.utdallas.hltri.eeg.Data;
import edu.utdallas.hltri.eeg.annotation.EegActivity;
import edu.utdallas.hltri.mercury.ConceptUtils;
import edu.utdallas.hltri.scribe.annotators.Annotator;
import edu.utdallas.hltri.scribe.annotators.LingScopeNegationSpanAnnotator;
import edu.utdallas.hltri.scribe.text.BaseDocument;
import edu.utdallas.hltri.scribe.text.annotation.Event;
import edu.utdallas.hltri.scribe.text.annotation.NegationSpan;
import edu.utdallas.hltri.scribe.text.annotation.Sentence;
import edu.utdallas.hltri.scribe.text.annotation.Token;
import edu.utdallas.hltri.util.Lazy;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.common.SolrInputDocument;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import edu.utdallas.hltri.eeg.EegNote;
import edu.utdallas.hltri.framework.ProgressLogger;
import edu.utdallas.hltri.inquire.ie.GenderExtractor;
import edu.utdallas.hltri.io.IOUtils;
import edu.utdallas.hltri.logging.Logger;
import edu.utdallas.hltri.scribe.io.JsonCorpus;
import edu.utdallas.hltri.scribe.text.Document;

/**
 * Created by travis on 10/5/16.
 */
public class SolrIndexer {
  private static transient final Logger log = Logger.get(SolrIndexer.class);

  private static final String conceptAnnset = "lstm";

  private static final Path eegCorpusPath =
      Paths.get("/shared/aifiles/disk1/travis/data/corpora/tuh_eeg/v0.6.0_edf");
  private static final Path eegJsonPath =
      Paths.get("/shared/aifiles/disk1/travis/data/corpora/tuh_eeg/v0.6.0_json");
  private static final JsonCorpus<EegNote> eegNoteJsonCorpus = Data.v060(conceptAnnset);
//      JsonCorpus.<EegNote>at(eegJsonPath).tiered().annotationSets(conceptAnnset).build();

  public static final String solrUrl = "http://pnfs:8983/solr/nih_eeg_alpha/";

  private static final SolrClient solr = new HttpSolrClient(solrUrl);

  private static final String version = "v0.6.0";

  private static final Supplier<Annotator<BaseDocument>> negationSpanAnnotator = Lazy.lazily(() -> new LingScopeNegationSpanAnnotator<>(
      d -> d.get("genia", Sentence.TYPE),
      s -> s.getContained("genia", Token.TYPE)
  ));


  private static String getSection(String text, int start, int... starts) {
    final int end = Math.min(Arrays.stream(starts).filter(x -> x > start).min().orElse(0), text.length());
    try {
      return text.substring(start, end).trim();
    } catch (IndexOutOfBoundsException e) {
      return "";
    }
  }

  private static boolean fileNameFilter(Path path) {
    if (!Files.isReadable(path)) return false;
    final String fileName = path.getFileName().toString();
    return !fileName.startsWith("_") && fileName.endsWith(".txt");
  }

  public static void main(String... args) throws IOException, SolrServerException {
    createIndexWithConcepts();
  }

  @SuppressWarnings("Duplicates")
  private static void createIndexWithConcepts() throws IOException, SolrServerException {
    try (final ProgressLogger plog = ProgressLogger.fixedSize("indexing", Data.V060_SIZE, 1, TimeUnit.SECONDS)) {
      final Pattern ageRegex = Pattern.compile("(\\d+)[- ]years?[- ]old");
      final GenderExtractor genderExtractor = new GenderExtractor();

      Files.walk(eegCorpusPath).filter(SolrIndexer::fileNameFilter).forEach(reportPath -> {
        final String reportId = IOUtils.removeAllExtensions(reportPath.getFileName().toString());
        plog.update("processing {}", reportId);
        final SolrInputDocument solrDoc = new SolrInputDocument();
        solrDoc.addField("version", version);
        solrDoc.addField("record_id", reportId);
        solrDoc.setField("path", reportPath.toString());
        final Path relativePath = eegCorpusPath.relativize(reportPath);
        solrDoc.setField("relative_path", relativePath.toString());
        final String fileName = reportPath.getFileName().toString();
        final String sessionNumber = reportPath.getParent().getFileName().toString();
        if (!Objects.equals(sessionNumber.substring(0, 3), fileName.substring(fileName.length() - 7, fileName.length() - 4))) {
          log.warn("Found report with session ID {} and file name {}", sessionNumber, fileName);
        }
        solrDoc.addField("session_no", sessionNumber);
        final String date = sessionNumber.substring(sessionNumber.indexOf('_') + 1).replaceAll("_", "-");
        log.debug("Parsed date {}", date);
        try {
          DateTimeFormatter.ISO_DATE.parse(date);
          solrDoc.addField("session_date", date);
        } catch (DateTimeParseException e) {
          log.warn("Unable to parse date {}", e);
        }
        final String patientId = reportPath.getParent().getParent().getFileName().toString();
        solrDoc.addField("patient_id", patientId);

        try (final Document<EegNote> eegReport = eegNoteJsonCorpus.load(reportId)) {
          final String text = eegReport.toString();

          if (text.isEmpty()) {
            log.error("Found empty document {}", reportId);
          } else {
            // add text
            solrDoc.addField("text", text.trim());

            // add negated text
            negationSpanAnnotator.get().annotate(eegReport);
            eegReport.get("lingscope", NegationSpan.TYPE);

            // add activities
            eegReport.get(conceptAnnset, EegActivity.TYPE).forEach(a ->
                solrDoc.addField("activity", ConceptUtils.createActivityString(a)));
            // add events
            eegReport.get(conceptAnnset, Event.TYPE).stream().map(ConceptUtils::createEventString).filter(Optional::isPresent)
                .forEach(e -> solrDoc.addField("event", e.get()));

            // add sections
            final int clinicalHistoryStart = text.indexOf("CLINICAL HISTORY:") + "CLINICAL HISTORY:".length();
            final int medicationsStart = text.indexOf("MEDICATIONS:") + "MEDICATIONS:".length();
            final int introStart = text.indexOf("INTRODUCTION:") + "INTRODUCTION:".length();
            final int descriptionStart = text.indexOf("DESCRIPTION OF THE RECORD:") + "DESCRIPTION OF THE RECORD:".length();
            final int impressionStart = text.indexOf("IMPRESSION:") + "IMPRESSION:".length();
            final int correlationStart = text.indexOf("CLINICAL CORRELATION:") + "CLINICAL CORRELATION:".length();
            final int[] offsets = {clinicalHistoryStart, medicationsStart, introStart, descriptionStart, impressionStart, correlationStart};
            solrDoc.addField("history_txt_en", getSection(text, clinicalHistoryStart, offsets));
            solrDoc.addField("medications_txt_en", getSection(text, medicationsStart, offsets));
            solrDoc.addField("introduction_txt_en", getSection(text, introStart, offsets));
            solrDoc.addField("description_txt_en", getSection(text, descriptionStart, offsets));
            solrDoc.addField("impression_txt_en", getSection(text, impressionStart, offsets));
            solrDoc.addField("correlation_txt_en", getSection(text, correlationStart, offsets));

            // add tier path
            solrDoc.addField("tiers", eegJsonPath.relativize(Paths.get(eegNoteJsonCorpus.getTextPath(reportId)).getParent()).toString());

            // add age
            try {
              Matcher matcher = ageRegex.matcher(text);
              if (matcher.find()) {
                final int age = Integer.parseInt(matcher.group(1));
                solrDoc.addField("age", age);
              } else {
                log.warn("No age found for {}", reportId);
              }
            } catch (IndexOutOfBoundsException | IllegalStateException e) {
              log.warn("No age found for {}", reportId);
            }

            // add gender
            final GenderExtractor.Gender gender = genderExtractor.extract(text);
            if (gender != null) {
              solrDoc.addField("gender", gender.toString());
            }

            // commit
            try {
              solr.deleteById(reportId);
              solr.add(solrDoc);
            } catch (SolrServerException | IOException e) {
              throw new RuntimeException(e);
            }
          }
        }
      });
    }

    solr.commit();
    solr.optimize();
    solr.close();
  }

  private static void createIndex() throws IOException, SolrServerException {
    try (final ProgressLogger plog = ProgressLogger.indeterminateSize("indexing", 1, TimeUnit.SECONDS)) {
      final Pattern ageRegex = Pattern.compile("(\\d+)[- ]years?[- ]old");
      final GenderExtractor genderExtractor = new GenderExtractor();

      Files.walk(eegCorpusPath).filter(SolrIndexer::fileNameFilter).forEach(reportPath -> {
        final String reportId = IOUtils.removeAllExtensions(reportPath.getFileName().toString());
        plog.update("processing {}", reportId);
        final SolrInputDocument solrDoc = new SolrInputDocument();
        solrDoc.addField("version", version);
        solrDoc.addField("record_id", reportId);
        solrDoc.setField("path", reportPath.toString());
        final Path relativePath = eegCorpusPath.relativize(reportPath);
        solrDoc.setField("relative_path", relativePath.toString());
        final String fileName = reportPath.getFileName().toString();
        final String sessionNumber = reportPath.getParent().getFileName().toString();
        if (!Objects.equals(sessionNumber.substring(0, 3), fileName.substring(fileName.length() - 7, fileName.length() - 4))) {
          log.warn("Found report with session ID {} and file name {}", sessionNumber, fileName);
        }
        solrDoc.addField("session_no", sessionNumber);
        final String date = sessionNumber.substring(sessionNumber.indexOf('_') + 1).replaceAll("_", "-");
        log.debug("Parsed date {}", date);
        try {
          DateTimeFormatter.ISO_DATE.parse(date);
          solrDoc.addField("session_date", date);
        } catch (DateTimeParseException e) {
          log.warn("Unable to parse date {}", e);
        }
        final String patientId = reportPath.getParent().getParent().getFileName().toString();
        solrDoc.addField("patient_id", patientId);

        try (final Document<EegNote> eegReport = eegNoteJsonCorpus.load(reportId)) {
          final String text = eegReport.toString();
          solrDoc.addField("text", text.trim());

          if (text.isEmpty()) {
            log.error("Found empty document {}", reportId);
          } else {
            final int clinicalHistoryStart = text.indexOf("CLINICAL HISTORY:") + "CLINICAL HISTORY:".length();
            final int medicationsStart = text.indexOf("MEDICATIONS:") + "MEDICATIONS:".length();
            final int introStart = text.indexOf("INTRODUCTION:") + "INTRODUCTION:".length();
            final int descriptionStart = text.indexOf("DESCRIPTION OF THE RECORD:") + "DESCRIPTION OF THE RECORD:".length();
            final int impressionStart = text.indexOf("IMPRESSION:") + "IMPRESSION:".length();
            final int correlationStart = text.indexOf("CLINICAL CORRELATION:") + "CLINICAL CORRELATION:".length();
            final int[] offsets = {clinicalHistoryStart, medicationsStart, introStart, descriptionStart, impressionStart, correlationStart};

            solrDoc.addField("history_txt_en", getSection(text, clinicalHistoryStart, offsets));
            solrDoc.addField("medications_txt_en", getSection(text, medicationsStart, offsets));
            solrDoc.addField("introduction_txt_en", getSection(text, introStart, offsets));
            solrDoc.addField("description_txt_en", getSection(text, descriptionStart, offsets));
            solrDoc.addField("impression_txt_en", getSection(text, impressionStart, offsets));
            solrDoc.addField("correlation_txt_en", getSection(text, correlationStart, offsets));

            solrDoc.addField("tiers", eegJsonPath.relativize(Paths.get(eegNoteJsonCorpus.getTextPath(reportId)).getParent()).toString());

            try {
              Matcher matcher = ageRegex.matcher(text);
              if (matcher.find()) {
                final int age = Integer.parseInt(matcher.group(1));
                solrDoc.addField("age", age);
              } else {
                log.warn("No age found for {}", reportId);
              }
            } catch (IndexOutOfBoundsException | IllegalStateException e) {
              log.warn("No age found for {}", reportId);
            }

            final GenderExtractor.Gender gender = genderExtractor.extract(text);
            if (gender != null) {
              solrDoc.addField("gender", gender.toString());
            }

            try {
              solr.deleteById(reportId);
              solr.add(solrDoc);
            } catch (SolrServerException | IOException e) {
              throw new RuntimeException(e);
            }
          }
        }
      });
    }

    solr.commit();
    solr.optimize();
    solr.close();
  }
}