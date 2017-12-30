package edu.utdallas.hltri.scribe.annotators;

import com.google.common.base.CharMatcher;
import com.google.common.collect.Lists;
import edu.utdallas.hltri.logging.Logger;
import edu.utdallas.hltri.scribe.text.BaseDocument;
import edu.utdallas.hltri.scribe.text.Document;
import edu.utdallas.hltri.scribe.text.annotation.*;
import lingscope.algorithms.CrfAnnotator;
import lingscope.drivers.SentenceTagger;
import lingscope.structures.AnnotatedSentence;

import java.nio.file.Path;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
*
* @author travis
*/
abstract class LingScopeAnnotator<T extends BaseDocument, K extends Annotation<K>> implements Annotator<T> {
  private static final Logger       log = Logger.get(LingScopeAnnotator.class);

  public static final String ANNOTATION_SET = "lingscope";

  private final CrfAnnotator crf = new CrfAnnotator("B", "I", "O");
  private final Function<Document<? extends T>, ? extends Iterable<Sentence>> sentenceExtractor;
  private final Function<Sentence, ? extends Iterable<Token>>                 tokenExtractor;
  private final AnnotationType<K>                                             type;

  protected LingScopeAnnotator(Path path,
                            Function<Document<? extends T>, ? extends Iterable<Sentence>> sentenceExtractor,
                            Function<Sentence, ? extends Iterable<Token>> tokenExtractor,
                            AnnotationType<K> annotationType) {
    crf.loadAnnotator(path.toAbsolutePath().toString());
    this.sentenceExtractor = sentenceExtractor;
    this.tokenExtractor = tokenExtractor;
    this.type = annotationType;
  }

  @Override public <B extends T> void annotate(final Document<B> document) {
    for (final Sentence sentence : sentenceExtractor.apply(document)) {
      List<Token> tokens = Lists.newArrayList(tokenExtractor.apply(sentence));
      final AnnotatedSentence annotations = SentenceTagger.tag(crf, tokens.stream().collect(Collectors.joining(" ")), true);
      List<String> words = annotations.getWords();  // LingScope "tokens"
      List<String> tags = annotations.getTags();    // LingScope IOB tags
      assert tags.size() == words.size() : String.format("Tags: (%d)%s do not correspond to words: (%d)%s", tags.size(),
          tags, words.size(), words);
      assert tags.size() == tokens.size() : String.format("Tags: (%d)%s do not correspond to tokens: (%d)%s", tags.size(),
          tags, tokens.size(), tokens);

      int start = 0;
      for (int w = 0, t = 0; w < words.size() && t < tokens.size(); t++, w++) {
        Token token = tokens.get(t);
        int spaces = CharMatcher.WHITESPACE.countIn(token);

        switch (tags.get(w)) {
          case "B-S":
            start = t;
            break;
          case "O":
            if (start > 0) {
              type.create(document,
                          "lingscope",
                          tokens.get(start).get(AbstractAnnotation.StartOffset),
                          tokens.get(t).get(AbstractAnnotation.EndOffset));
              start = 0;
            }
            break;
          case "I-S":
            break;
          default:
            log.warn("Unexpected tag {}.", tags.get(w));
        }
        w += spaces;
      }
    }
  }
}
