package edu.utdallas.hlt.trecmed.offline;

import com.google.common.base.CharMatcher;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import edu.utdallas.hlt.text.Annotation;
import edu.utdallas.hlt.text.AnnotationType;
import edu.utdallas.hlt.text.Document;
import edu.utdallas.hlt.text.Sentence;
import edu.utdallas.hlt.text.Text;
import edu.utdallas.hlt.text.Token;
import edu.utdallas.hlt.text.annotator.Annotator;
import edu.utdallas.hlt.util.Log;
import lingscope.algorithms.CrfAnnotator;
import lingscope.drivers.SentenceTagger;
import lingscope.structures.AnnotatedSentence;

/**
*
* @author travis
*/
public abstract class LingScopeAnnotator extends Annotator {
  private static final Log log = new Log(LingScopeAnnotator.class);
  private final CrfAnnotator crf = new CrfAnnotator("B", "I", "O");

  public LingScopeAnnotator(String path) {
    crf.loadAnnotator(path);
  }

  protected abstract Annotation getSpan(Text text);

  /**
   * Returns {@link Sentence#TYPE}.
   */
  @Override
  public Set<AnnotationType> getPrerequisites() {
    return Collections.singleton(Sentence.TYPE);
  }

  /**
   * {inheritDoc}
   */
  @Override
  public void annotateDocument(Document document) {
    for (Sentence sentence : document.getSub(Sentence.class)) {
      AnnotatedSentence annotations = SentenceTagger.tag(crf, sentence.asTokenizedString(), true);
      List<String> words = annotations.getWords();  // LingScope "tokens"
      List<String> tags = annotations.getTags();    // LingScope IOB tags
      List<Token> tokens = sentence.getTokens();    // Kirk's tokens

      int start = 0;
      for (int w = 0, t = 0; w < words.size() && t < tokens.size(); t++, w++) {
        Token token = tokens.get(t);
        int spaces = CharMatcher.WHITESPACE.countIn(token.asRawString());

        switch (tags.get(w)) {
          case "B-S":
            start = t;
            break;
          case "O":
            if (start > 0) {
              Annotation span = getSpan(tokens.get(start).union(tokens.get(t)));
              span.setProvider("lingscope");
              span.attach();
            }
            break;
          case "I-S":
            break;
          default:
            log.warning("Unexpected tag {0}.", tags.get(w));
        }
        w += spaces;
      }
    }
  }
}
