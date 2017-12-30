package edu.utdallas.hltri.eeg;

import edu.utdallas.hltri.eeg.annotation.EegActivity;
import edu.utdallas.hltri.framework.ProgressLogger;
import edu.utdallas.hltri.inquire.ANN;
import edu.utdallas.hltri.scribe.text.annotation.Annotation;
import edu.utdallas.hltri.scribe.text.annotation.Event;
import edu.utdallas.hltri.scribe.text.annotation.Sentence;
import edu.utdallas.hltri.scribe.text.annotation.Token;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by ramon on 5/21/17.
 */
public class WriteConc2Vec {

  public static void main(String... args) {
    final ProgressLogger plog = ProgressLogger.fixedSize("processing", Data.V060_SIZE, 5L, TimeUnit.SECONDS);
    final List<String> lines = new ArrayList<>();
    final List<String> normalizedConcepts = new ArrayList<>();
    final AtomicInteger i = new AtomicInteger(0);
    Data.v060("genia", "lstm", "opennlp")
//        .getIdStream().filter(id -> i.incrementAndGet() < 5).map(id -> Data.v060("genia", "lstm", "opennlp").load(id)).forEach(doc -> {
        .forEachDocument(doc -> {
      doc.get("opennlp", Sentence.TYPE).forEach(sentence -> {
        final StringBuilder sb = new StringBuilder();
        final List<Annotation<?>> concepts = new ArrayList<>(sentence.getContained("lstm", EegActivity.TYPE));
        concepts.addAll(sentence.getContained("lstm", Event.TYPE));
        concepts.sort((c1, c2) -> Long.compare(c1.get(Annotation.StartOffset), c2.get(Annotation.StartOffset)));
        final Iterator<Annotation<?>> it = concepts.iterator();
        Annotation<?> ann = nextOrNull(it);
        boolean annIsWritten = false;
        for (Token token : sentence.getContained("genia", Token.TYPE)) {
          if (ann == null) {
            sb.append(token.toString().toLowerCase()).append(" ");
          } else {
            if (token.get(Annotation.EndOffset) < ann.get(Annotation.StartOffset)) {
              sb.append(token.toString().toLowerCase()).append(" ");
            } else if (!annIsWritten) {
              final Optional<String> normalized = ConceptNormalization.normalizeConcept(ann);
              if (normalized.isPresent()) {
                final String s = normalized.get().replaceAll("\\s", "_");
                sb.append(s).append(" ");
                normalizedConcepts.add(s);
              } else {
                sb.append(ann.toString().toLowerCase()).append(" ");
              }
              annIsWritten = true;
            } else if (token.get(Annotation.EndOffset) >= ann.get(Annotation.EndOffset)) {
              ann = nextOrNull(it);
              annIsWritten = false;
            }
          }
        }
        lines.add(sb.toString());
      });
      plog.update("{}, {} total sentences", doc.getId(), lines.size());
    });
    try {
      Files.write(Paths.get(args[0], "conc2vec.txt"), lines);
      Files.write(Paths.get(args[0], "concepts.txt"), normalizedConcepts);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private static <T> T nextOrNull(Iterator<T> it) {
    return it.hasNext() ? it.next() : null;
  }
}
