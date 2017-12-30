package edu.utdallas.hltri.eeg.scripts;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import edu.utdallas.hltri.eeg.Data;
import edu.utdallas.hltri.eeg.EegNote;
import edu.utdallas.hltri.framework.ProgressLogger;
import edu.utdallas.hltri.logging.Logger;
import edu.utdallas.hltri.scribe.io.JsonCorpus;
import edu.utdallas.hltri.scribe.text.Document;
import edu.utdallas.hltri.scribe.text.annotation.Section;
import edu.utdallas.hltri.scribe.text.annotation.Sentence;
import edu.utdallas.hltri.scribe.text.annotation.Token;
import edu.utdallas.hltri.util.Unsafe;

/**
 * Converts a JSON Corpus to a single TSV file with one record per line.
 * Each column indicates a section, with the first column being the record ID.
 * Created by travis on 2/15/17.
 */
public class ReportsToSectionedTsvFormatter {
  private final static Logger log = Logger.get(ReportsToSectionedTsvFormatter.class);
  private final JsonCorpus<EegNote> records;
  private final int numRecords;
  private final CSVFormat format = CSVFormat.TDF.withHeader("RECORD_ID", "HISTORY", "MEDICATIONS",
      "INTRODUCTION", "DESCRIPTION", "IMPRESSION", "CORRELATION", "ALL");


  @SuppressWarnings("WeakerAccess")
  public ReportsToSectionedTsvFormatter() {
    /*
     * openNLP: sentences
     * genia: tokens
     * regex-eeg: sections
     */
    this.records = Data.v060("genia", "opennlp", "regex-eeg");
    this.numRecords = (int) records.getIdStream().count();
  }

  private static Stream<String> formatSentencesWithTags(Collection<Sentence> sentences) {
    return sentences.stream()
        .map(sentence -> sentence.getContained("genia", Token.TYPE)
            .stream()
            .map(Token::toString)
            .collect(Collectors.joining(" ", "<s> ", " </s>")));
  }

  private static String formatSection(final Section section) {
    return formatSentencesWithTags(section.getContained("opennlp", Sentence.TYPE))
        .collect(Collectors.joining(" ", "<p> ", " </p>"));
  }

  private static String formatRecord(final Document<EegNote> record) {
    return formatSentencesWithTags(record.get("opennlp", Sentence.TYPE))
        .collect(Collectors.joining(" ", "<d> ", " </d>"));
  }

  private static void annotateSections(final Document<EegNote> record) {
    final Pattern pattern = Pattern.compile("([A-Z][A-Z _]*):");
    final Matcher matcher = pattern.matcher(record);
    int sectionStart = 0;
    String sectionName = null;
    while (matcher.find()) {
      final String match = matcher.group(1);
      final int matchStart = matcher.start();
      final int matchEnd = matcher.end();
      if (null != sectionName) {
        final Section section = Section.TYPE.create(record, "regex-2", sectionStart, matchStart - 1)
            .set(Section.title, sectionName);
        log.info("Created section \"{}\" with name \"{}\"", section, section.get(Section.title));
        for (Sentence sentence : section.getOverlapping("opennlp", Sentence.TYPE)) {
          final boolean trimStart = sentence.get(Sentence.StartOffset) < sectionStart;
//          final boolean trimEnd = sentence.get(Sentence.EndOffset) > matchEnd;
//          if (trimStart || trimEnd)
          if (trimStart) {
            record.get("opennlp", Sentence.TYPE).remove(sentence);
            final Sentence leftSentence = Sentence.TYPE.create(record, "opennlp",
                sentence.get(Sentence.StartOffset), sectionStart - 1);
            final Sentence rightSentence = Sentence.TYPE.create(record, "opennlp",
                sectionStart, sentence.get(Sentence.EndOffset));
            log.warn("Trimming Sentence \"{}\" to \"{}\" and \"{}\"", sentence, leftSentence, rightSentence);
          }

        }
      }
      sectionName = match;
      sectionStart = matchStart;
    }

    if (null != sectionName) {
      final Section section = Section.TYPE.create(record, "regex-2", sectionStart, record.length())
          .set(Section.title, sectionName);
      log.info("Created section \"{}\" with name \"{}\"", section, section.get(Section.title));
      for (Sentence sentence : section.getOverlapping("opennlp", Sentence.TYPE)) {
        final boolean trimStart = sentence.get(Sentence.StartOffset) < sectionStart;
//          final boolean trimEnd = sentence.get(Sentence.EndOffset) > matchEnd;
//          if (trimStart || trimEnd)
        if (trimStart) {
          record.get("opennlp", Sentence.TYPE).remove(sentence);
          final Sentence leftSentence = Sentence.TYPE.create(record, "opennlp",
              sentence.get(Sentence.StartOffset), sectionStart - 1);
          final Sentence rightSentence = Sentence.TYPE.create(record, "opennlp",
              sectionStart, sentence.get(Sentence.EndOffset));
          log.warn("Trimming Sentence \"{}\" to \"{}\" and \"{}\"", sentence, leftSentence, rightSentence);
        }

      }
    }
  }

  /*
   * TSV record format: <0: ID> <1: HISTORY> <2: MEDICATIONS> <3: INTRODUCTION>
   *   <4: DESCRIPTION> <5: IMPRESSION> <6: CORRELATION>
   */
  private String[] ConvertJsonRecordToSectionedTsvRecord(final String jsonRecordId) {
    try (final Document<EegNote> record = records.load(jsonRecordId)) {
      annotateSections(record);
      final String[] tsvRecord = new String[ 8 ];
      tsvRecord[0] = record.getId();
      for (final Section section : record.get("regex-eeg", Section.TYPE)) {
       final String sectionTitle = section.get(Section.title).toLowerCase();
       if (sectionTitle.contains("hist")) tsvRecord[1] = formatSection(section);
       else if (sectionTitle.contains("med")) tsvRecord[2] = formatSection(section);
       else if (sectionTitle.contains("intro")) tsvRecord[3] = formatSection(section);
       else if (sectionTitle.contains("desc")) tsvRecord[4] = formatSection(section);
       else if (sectionTitle.contains("impr") || sectionTitle.contains("sion"))
          tsvRecord[5] = formatSection(section);
       else if (sectionTitle.contains("corr") || sectionTitle.contains("clin"))
          tsvRecord[6] = formatSection(section);
      }
      tsvRecord[7] = formatRecord(record);
      // If any sections are missing (i.e. null) set them to empty
      for (int i = 1; i < tsvRecord.length; i++) {
        if (tsvRecord[i] == null) tsvRecord[i] = "";
      }

//      try {
//        System.in.read();
//      } catch (IOException e) {
//        throw new RuntimeException(e);
//      }

      return tsvRecord;
    }

  }

  @SuppressWarnings("WeakerAccess")
  public void saveRecordsToTsvFile(final Path file) {
    try(final CSVPrinter writer = format.print(Files.newBufferedWriter(file))) {
      try (final ProgressLogger logger = ProgressLogger.fixedSize("formatting", numRecords, 1, TimeUnit.SECONDS)) {
        final AtomicInteger counter = new AtomicInteger();
        records.getIdStream()
            .map(this::ConvertJsonRecordToSectionedTsvRecord)
            .forEach(Unsafe.consumer(tsvRecord -> {
              writer.printRecord((Object[]) tsvRecord);
              logger.update("Formatted record #{}", counter.incrementAndGet());
            }));
      }
    } catch (IOException e ) {
      throw new RuntimeException(e);
    }
  }

  public static void main(String... args) {
    final Path outputPath = Paths.get(args[0]);
    final ReportsToSectionedTsvFormatter formatter = new ReportsToSectionedTsvFormatter();
    formatter.saveRecordsToTsvFile(outputPath);
  }
}
