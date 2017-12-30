package edu.utdallas.hlt.trecmed.scripts;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

import edu.utdallas.hlt.trecmed.framework.App;
import edu.utdallas.hltri.inquire.l2r.CohortPreprocessor;
import edu.utdallas.hltri.inquire.text.WikipediaKeywordAnnotator;
import edu.utdallas.hltri.metamap.MetamapOutput;
import edu.utdallas.hltri.scribe.annotators.KirkConceptAnnotator;
import edu.utdallas.hltri.scribe.text.BaseDocument;
import edu.utdallas.hltri.scribe.text.Document;
import edu.utdallas.hltri.scribe.text.annotation.Sentence;

/**
 * Created by travis on 11/4/16.
 */
public class JamiaPreprocessor {

  public static void main(String... args) {
    final List<Document<JamiaVectorizer2.TrecMedTopic>> topics = App.readQuestion(args[0])
        .stream()
        .map(topic -> {
          final Document<JamiaVectorizer2.TrecMedTopic> doc = Document.fromString(topic.asString());
          doc.set(BaseDocument.id, topic.getId());
          return doc;
        })
        .collect(Collectors.toList());

    final KirkConceptAnnotator<JamiaVectorizer2.TrecMedTopic> kirk =
        new KirkConceptAnnotator.Builder<JamiaVectorizer2.TrecMedTopic>()
            .useSentences(d -> d.get("opennlp", Sentence.TYPE)).build();

    final WikipediaKeywordAnnotator<JamiaVectorizer2.TrecMedTopic> wiki =
        new WikipediaKeywordAnnotator<JamiaVectorizer2.TrecMedTopic>(
        x -> true
    ).clear();

    final Path destPath = Paths.get(args[1]);
    final CohortPreprocessor<JamiaVectorizer2.TrecMedTopic> preprocessor =
        new CohortPreprocessor<>(destPath);

    for (Document<JamiaVectorizer2.TrecMedTopic> doc : topics) {
      preprocessor.preprocess(doc);
      kirk.annotate(doc);
      wiki.annotate(doc);
      doc.sync();
    }

    MetamapOutput.outputSentences(destPath.resolve("metamap-input").toAbsolutePath().toString(),
        200, preprocessor.getCorpus(), "opennlp");
  }
}
