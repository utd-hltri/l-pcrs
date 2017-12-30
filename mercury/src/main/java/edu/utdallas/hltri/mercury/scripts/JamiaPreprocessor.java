//package edu.utdallas.hltri.mercury.jamia.scripts;
//
//import org.apache.commons.csv.CSVFormat;
//import org.apache.commons.csv.CSVRecord;
//
//import java.io.BufferedReader;
//import java.io.IOException;
//import java.nio.file.Files;
//import java.nio.file.Path;
//import java.nio.file.Paths;
//import java.util.ArrayList;
//import java.util.Collection;
//
//import edu.utdallas.hltri.eeg.annotators.EegConceptAnnotator;
//import edu.utdallas.hltri.inquire.l2r.CohortPreprocessor;
//import edu.utdallas.hltri.mercury.jamia.CohortTopic;
//import edu.utdallas.hltri.metamap.MetamapOutput;
//import edu.utdallas.hltri.scribe.io.JsonCorpus;
//import edu.utdallas.hltri.scribe.text.BaseDocument;
//import edu.utdallas.hltri.scribe.text.Document;
//
///**
// * Created by travis on 11/4/16.
// */
//public class JamiaPreprocessor {
//  public static void main(String... args) throws IOException {
//    final Path inputTopics = Paths.get(args[0]);
//    final Path outputCorpus = Paths.get(args[1]);
//
//    final Collection<Document<CohortTopic>> topics = new ArrayList<>();
//    try (final BufferedReader reader = Files.newBufferedReader(inputTopics)) {
//       for (final CSVRecord fields : CSVFormat.DEFAULT.parse(reader)) {
//         final Document<CohortTopic> topic = Document.fromString(fields.get("TEXT"));
//         topic.set(BaseDocument.id, fields.get("NAME"));
//       }
//    }
//
//    final CohortPreprocessor<CohortTopic> preprocessor = new CohortPreprocessor<>(outputCorpus);
//
//    final EegConceptAnnotator<CohortTopic> ramon = EegConceptAnnotator.best("nih");
//
//    for (Document<CohortTopic> doc : topics) {
//      preprocessor.preprocess(doc);
//      ramon.annotate(doc);
//      doc.sync();
//    }
//
//    MetamapOutput.outputSentences(outputCorpus.resolve("metamap-input").toAbsolutePath().toString(),
//        200, preprocessor.getCorpus(), "opennlp");
//  }
//}
