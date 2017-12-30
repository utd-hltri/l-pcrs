package edu.utdallas.hltri.scribe.annotators;

import edu.utdallas.hltri.conf.Config;
import edu.utdallas.hltri.scribe.text.BaseDocument;
import edu.utdallas.hltri.scribe.text.Document;
import edu.utdallas.hltri.scribe.text.annotation.HedgeSpan;
import edu.utdallas.hltri.scribe.text.annotation.NegationSpan;
import edu.utdallas.hltri.scribe.text.annotation.Sentence;
import edu.utdallas.hltri.scribe.text.annotation.Token;

import java.util.function.Function;

/**
* Annotates a {@link HedgeSpan} using LingScope.
* @author travis
*/
public class LingScopeNegationSpanAnnotator<T extends BaseDocument> extends LingScopeAnnotator<T, NegationSpan> {
  public LingScopeNegationSpanAnnotator(Function<Document<? extends T>, ? extends Iterable<Sentence>> sentenceExtractor,
                                        Function<Sentence, ? extends Iterable<Token>> tokenExtractor) {
    super(Config.load("scribe.annotators.lingscope").getPath("negation-model-path"),
          sentenceExtractor,
          tokenExtractor,
          NegationSpan.TYPE);
  }
}
