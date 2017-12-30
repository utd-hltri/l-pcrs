package edu.utdallas.hltri.mercury.jamia.scripts;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.TermQuery;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import edu.utdallas.hltri.inquire.eval.QRels;
import edu.utdallas.hltri.inquire.l2r.CohortL2rProcessor;
import edu.utdallas.hltri.inquire.lucene.LuceneResult;
import edu.utdallas.hltri.logging.Logger;
import edu.utdallas.hltri.mercury.jamia.CohortPatient;
import edu.utdallas.hltri.mercury.jamia.CohortTopic;
import edu.utdallas.hltri.mercury.lucene.EegSearchEngine;
import edu.utdallas.hltri.mercury.lucene.IndexedEegNote;
import edu.utdallas.hltri.scribe.io.Corpus;
import edu.utdallas.hltri.scribe.io.JsonCorpus;

/**
 * .
 * Created by travis on 11/3/16.
 */



public class JamiaVectorizer2 {

  private static final Logger log = Logger.get("JAMIA 2016");

  public static void main(String... args) throws IOException {
    final Corpus<CohortTopic> topics = JsonCorpus.<CohortTopic>at(Paths.get(args[0])).build();
    final QRels qrels = QRels.fromFile(Paths.get(args[1]));
    final Path targetPath = Paths.get(args[2]);

    Files.createDirectories(targetPath);

    try (EegSearchEngine searcher = new EegSearchEngine()) {
      final CohortL2rProcessor<CohortTopic, CohortPatient, IndexedEegNote> l2r = new CohortL2rProcessor<>(
          searcher,
          new String[]{
          "text",
          "description_txt_en",
          "history_txt_en",
          "introduction_txt_en",
          "correlation_txt_en",
          "impression_txt_en",
      });

      final Function<String, Collection<CohortPatient>> queryHits =
          (String topicId) -> {
            log.debug("Getting hits for |{}|", topicId);
            final List<CohortPatient> hits = qrels.getJudgements(topicId)
                .keySet()
                .stream()
                .map(CohortPatient::new)
                .collect(Collectors.toList());
            log.debug("Found {} hits", hits);
            return hits;
          };

      l2r.process(topics.getDocumentList(),
          queryHits,
          qrels,
          patient ->
              searcher.search(
                  new TermQuery(new Term("patient_id", patient.getId())),
                  Integer.MAX_VALUE)
                .getResults()
                .stream()
                .map(LuceneResult::getValue)
                .collect(Collectors.toList()),
          targetPath);
    }
  }
}
