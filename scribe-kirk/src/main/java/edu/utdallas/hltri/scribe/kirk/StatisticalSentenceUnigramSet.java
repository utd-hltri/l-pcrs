package edu.utdallas.hltri.scribe.kirk;

import edu.utdallas.hlt.features.span.StatisticalTextSet;
import edu.utdallas.hlt.ml.feature.Feature;
import edu.utdallas.hlt.text.*;
import edu.utdallas.hltri.scribe.util.TieredHashing;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Created by rmm120030 on 9/30/15.
 */
public class StatisticalSentenceUnigramSet<T extends edu.utdallas.hlt.text.Text> extends StatisticalTextSet<T> {
  /**
   * Creates a new <code>StatisticalSentenceUnigramSet</code> for the given
   * {@link LexicalType} and {@link Feature}.
   */
  public StatisticalSentenceUnigramSet(final LexicalType lexicalType,
                                       final Feature<T,String> feature) {
    super(lexicalType, feature, "SentenceUnigram");
  }

  /**
   * Returns unigrams for the given {@link Text}.
   */
  @Override
  public Collection<String> getValues(final T text) {
    final Sentence sentence = text.getOnlySuper(Sentence.class);
    if (sentence == null) {
      final Document document = text.getDocument();
      System.out.println("Failed Document /" + TieredHashing.getHashDirsAsString(document.getDocumentID()) + "/" +
          document.getDocumentID() + ": " + document.toString());
      for (final Sentence s : document.getAnnotations(Sentence.class)) {
        System.out.println(s);
      }
      throw new IllegalArgumentException("Not in a sentence: " + text);
    }

    final Collection<String> values = new ArrayList<String>();
    for (final Token token : sentence.getTokens()) {
      values.add(lexicalType.value(token));
    }
    return values;
  }
}
