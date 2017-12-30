package edu.utdallas.hlt.trecmed.scripts;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import edu.utdallas.hlt.trecmed.Report;
import edu.utdallas.hlt.trecmed.Visit;
import edu.utdallas.hlt.trecmed.framework.App;
import edu.utdallas.hlt.trecmed.retrieval.LuceneEMRSearcher;
import edu.utdallas.hltri.inquire.eval.QRels;
import edu.utdallas.hltri.inquire.l2r.CohortL2rProcessor;
import edu.utdallas.hltri.logging.Logger;
import edu.utdallas.hltri.scribe.io.Corpus;
import edu.utdallas.hltri.scribe.io.JsonCorpus;
import edu.utdallas.hltri.scribe.text.BaseDocument;
import edu.utdallas.hltri.scribe.text.Document;

/**
 * .
 * Created by travis on 11/3/16.
 */
public class JamiaVectorizer2 {

  private static final Logger log = Logger.get("JAMIA 2016");

  static class TrecMedTopic extends BaseDocument {

  }

  public static void main(String... args) throws IOException {
    App.init(args);

    final Corpus<TrecMedTopic> topics = JsonCorpus.<JamiaVectorizer2.TrecMedTopic>at(Paths.get(args[0])).build();

    topics.forEachDocument(d -> log.info("\n{}\n", d.describe()));

    log.info("Found {} topics: {}", topics.getDocumentList().size(),
        topics.getDocumentList().stream().map(Document::getId).sorted().collect(Collectors.joining(" ", "[", "]")));

    final QRels qrels = QRels.fromFile(Paths.get(args[1]));

    log.info("Found {} QRels topics: {}", qrels.getTopics().size(),
        qrels.getTopics().stream().sorted().collect(Collectors.joining(" ", "[", "]")));

    final Path targetPath = Paths.get(args[2]);

    Files.createDirectories(targetPath);

    try (LuceneEMRSearcher.ReportSearchEngine searcher = LuceneEMRSearcher.getReportSearcher()) {
      final CohortL2rProcessor<TrecMedTopic, Visit, Report> l2r = new CohortL2rProcessor<>(
          searcher,
          new String[]{
          "full_text",
          "report_text",
          "discharge_diagnosis_text",
          "admit_diagnosis_text",
          "chief_complaint"
      });


      final Function<String, Collection<Visit>> queryHits =
          (String topicId) -> {
            log.debug("Getting hits for |{}|", topicId);
            final List<Visit> hits = qrels.getJudgements(topicId)
                .keySet()
                .stream()
                .map(Visit::fromId)
                .collect(Collectors.toList());
            log.debug("Found {} hits", hits);
            return hits;
          };

      l2r.process(topics.getDocumentList().stream().filter(d -> qrels.getTopics().contains(d.getId())).collect(Collectors.toList()),
          queryHits,
          qrels,
          Visit.getMapping()::get,
          targetPath);
    }
  }
}
