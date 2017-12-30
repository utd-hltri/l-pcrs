package edu.utdallas.hltri.scribe.annotators;

import edu.utdallas.hltri.scribe.text.Attribute;
import edu.utdallas.hltri.scribe.text.BaseDocument;
import edu.utdallas.hltri.scribe.text.Document;
import edu.utdallas.hltri.scribe.text.annotation.Token;
import edu.utdallas.hltri.util.InquirerPolarityChecker;

import java.util.Collection;
import java.util.function.Function;

/**
 * Created by rmm120030 on 7/1/16.
 */
public class InquirerPolarityAnnotator<D extends BaseDocument> implements Annotator<D> {
  public static final Attribute<Token, InquirerPolarityChecker.Polarity> polarity = Attribute.inferred("polarity");

  private final InquirerPolarityChecker checker;
  private final Function<Document<?>, Collection<Token>> tokenProvider;

  public InquirerPolarityAnnotator(final String source, final Function<Document<?>, Collection<Token>> tokenProvider) {
    this.checker = new InquirerPolarityChecker(source);
    this.tokenProvider = tokenProvider;
  }

  @Override
  public <B extends D> void annotate(Document<B> document) {
    for (final Token token : tokenProvider.apply(document)) {
      final InquirerPolarityChecker.Polarity pol = checker.getPolarity(token.asString());
      token.set(polarity, pol);
    }
  }
}
