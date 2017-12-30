package edu.utdallas.hltri.eeg.scripts;

import com.google.common.base.Charsets;
import edu.utdallas.hltri.eeg.EegNote;
import edu.utdallas.hltri.eeg.annotation.EegActivity;
import edu.utdallas.hltri.eeg.annotators.EegConceptAnnotator;
import edu.utdallas.hltri.eeg.io.EegJsonCorpus;
import edu.utdallas.hltri.io.TextFiles;
import edu.utdallas.hltri.logging.Logger;
import edu.utdallas.hltri.scribe.text.BaseDocument;
import edu.utdallas.hltri.scribe.text.Document;
import edu.utdallas.hltri.scribe.text.annotation.Event;
import edu.utdallas.hltri.scribe.text.annotation.Sentence;
import org.apache.commons.csv.CSVRecord;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Scanner;

/**
 * Annotates Queries
 * Created by rmm120030 on 10/7/16.
 */
public class AnnotateQueries {
  private static Logger log = Logger.get(AnnotateQueries.class);

  public static void main (String... args) {
    switch (args[0]) {
      case "server": server();
        break;
      case "file": tagQueryFile(Paths.get(args[1]), args[2]);
        break;
      case "csv": tagCsvFile(Paths.get(args[1]), args[2]);
        break;
      default: throw new RuntimeException("first argument must be from [server, file]");
    }
  }

  private static void server() {
    final String annset = "run10";
    final EegConceptAnnotator<EegNote> annotator = EegConceptAnnotator.bestWithLstm(annset);

    // annotate a dummy document so that all the preprocessing models get loaded
    final Document<EegNote> dd = Document.fromString("dummy document");
    Sentence.TYPE.create(dd, "opennlp", 0, dd.length());
    Sentence.TYPE.create(dd, "stanford", 0, dd.length());
    EegJsonCorpus.preprocess(dd);
    annotator.annotate(dd);

//    final IntIdentifier<String> iid = new IntIdentifier<>();
//    AnnotationVectorizer<Token> vectorizer = new AnnotationVectorizer<>(FeatureUtils.boundaryDetectionFeatureExtractors(),
//        FeatureUtils.boundaryLabeler("gold", EegActivity.TYPE), iid);
    System.out.println("Ready...");
    final Scanner scanner = new Scanner(System.in);
    int qid = 1;
    while (scanner.hasNext()) {
      final String input = scanner.nextLine();
      final Document<EegNote> doc = createDocument(""+qid++, input);
      annotator.annotate(doc);
//      for (Token token : doc.get("genia", Token.TYPE)) {
//        log.info("{}: {}", token, vectorizer.vectorizeAnnotation(token).verboseString(iid).replaceAll("\n", " "));
//      }
      final List<EegActivity> activities = doc.get(annset, EegActivity.TYPE);
      final List<Event> events = doc.get(annset, Event.TYPE);
      System.out.println(activities.size() + events.size());
      for (EegActivity act : activities) {
        System.out.println(activityString(act));
      }
      for (Event ev : doc.get(annset, Event.TYPE)) {
        System.out.println(eventString(ev));
      }
    }
  }

  private static void tagCsvFile(Path infile, String outfile) {
//    final Config conf = Config.load("eeg");
    final String annset = "best";
    final EegConceptAnnotator<EegNote> annotator = EegConceptAnnotator.best(annset);
    try (BufferedWriter writer = new BufferedWriter(new FileWriter(outfile))) {
      writer.write("NAME,POLARITY,MODALITY,TYPE|MORPHOLOGY|FREQUENCY|HEMISPHERE|DISPERSAL|RECURRENCE|BACKGROUND|MAGNITUDE|LOCATIONS");
      writer.newLine();
      for (CSVRecord record : TextFiles.loadCsv(infile, true)) {
        final Document<EegNote> doc = createDocument(record.get(0), new String(record.get(1).getBytes(Charsets.US_ASCII)));
        annotator.annotate(doc);
        log.info("Found {} events and {} activities in doc {}", doc.get(annset, Event.TYPE),
            doc.get(annset, EegActivity.TYPE).size(), doc.getId());
        for (EegActivity act : doc.get(annset, EegActivity.TYPE)) {
            writer.write(activityString(act));
            writer.newLine();
          }
          for (Event ev : doc.get(annset, Event.TYPE)) {
            writer.write(eventString(ev));
            writer.newLine();
          }
          doc.close();
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private static Document<EegNote> createDocument(String qid, String queryText) {
    final Document<EegNote> doc = Document.fromString(queryText);
    doc.set(BaseDocument.id, qid);
    log.info("Processing query {}:{}", doc.getId(), doc.asString());
    Sentence.TYPE.create(doc, "opennlp", 0, doc.length());
    Sentence.TYPE.create(doc, "stanford", 0, doc.length());
    EegJsonCorpus.preprocess(doc);
    log.debug(doc.describe());
    return doc;
  }

  private static void tagQueryFile(Path infile, String outfile) {
//    final Config conf = Config.load("eeg");
    final String annset = "run10";
    final EegConceptAnnotator<EegNote> annotator = EegConceptAnnotator.best(annset);
    try (BufferedWriter writer = new BufferedWriter(new FileWriter(outfile))) {
      writer.write("NAME,POLARITY,MODALITY,TYPE|MORPHOLOGY|FREQUENCY|HEMISPHERE|DISPERSAL|RECURRENCE|BACKGROUND|MAGNITUDE|LOCATIONS");
      writer.newLine();
      for (final String line : Files.readAllLines(infile)) {
        if (!line.startsWith("NAME")) {
          final Document<EegNote> doc = createDocument(line.substring(0, line.indexOf(',')), line.substring(line.indexOf(',') + 1));
          annotator.annotate(doc);
          for (EegActivity act : doc.get(annset, EegActivity.TYPE)) {
            writer.write(activityString(act));
            writer.newLine();
          }
          for (Event ev : doc.get(annset, Event.TYPE)) {
            writer.write(eventString(ev));
            writer.newLine();
          }
          doc.close();
        }
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private static String activityString(EegActivity act) {
    final StringBuilder sb = new StringBuilder();
    if (act.toString().contains(",")) {
      sb.append("\"").append(act.toString()).append("\"");
    }
    else {
      sb.append(act.toString());
    }
    sb.append(',').append(act.get(EegActivity.polarity)).append(',')
        .append(act.get(EegActivity.modality)).append(",EEG_ACTIVITY|")
        .append(act.get(EegActivity.morphology)).append("|")
        .append(act.get(EegActivity.band)).append("|")
        .append(act.get(EegActivity.hemisphere)).append("|")
        .append(act.get(EegActivity.dispersal)).append("|")
        .append(act.get(EegActivity.recurrence)).append("|")
        .append(act.get(EegActivity.in_background)).append("|")
        .append(act.get(EegActivity.magnitude));
    for (EegActivity.Location location : act.getLocations()) {
      sb.append("|").append(location.name());
    }
    return sb.toString();
  }

  private static String eventString(Event ev) {
    return (ev.toString().contains(",")) ? "\"" + ev.toString() + "\"" : ev.toString() + ',' + ev.get(Event.polarity)
        + ',' + ev.get(Event.modality) + ',' + ev.get(Event.type);
  }
}
