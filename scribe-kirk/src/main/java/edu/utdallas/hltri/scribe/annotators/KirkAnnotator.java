package edu.utdallas.hltri.scribe.annotators;

import com.google.common.collect.Lists;
import edu.utdallas.hlt.genia_wrapper.GeniaTokenReaderAndWriter;
import edu.utdallas.hlt.text.io.XMLDocumentWriter;
import edu.utdallas.hlt.util.Place;
import edu.utdallas.hltri.logging.Logger;
import edu.utdallas.hltri.scribe.gate.GateUtils;
import edu.utdallas.hltri.scribe.io.JsonCorpus;
import edu.utdallas.hltri.scribe.kirk.KirkDocument;
import edu.utdallas.hltri.scribe.text.BaseDocument;
import edu.utdallas.hltri.scribe.text.Document;
import edu.utdallas.hltri.scribe.text.annotation.Sentence;
import edu.utdallas.hltri.scribe.text.annotation.Token;

import java.io.IOException;
import java.util.List;
import java.util.function.Function;

/**
 * Created by rmm120030 on 6/30/15.
 *
 * Abstract Kirk Annotator providing basic builder functions for adding tokens and sentences.
 */
@SuppressWarnings("unused")
public abstract class KirkAnnotator<D extends BaseDocument> implements Annotator<D> {
  private static final Logger log = Logger.get(KirkAnnotator.class);

  protected edu.utdallas.hlt.text.annotator.Annotator annotator;
  protected final Function<Sentence, ? extends Iterable<Token>> tokenProvider;
  protected final Function<Document<? extends D>, ? extends Iterable<Sentence>> sentenceProvider;

  protected KirkAnnotator(Builder<D,? extends Builder<D,?>> builder) {
    GateUtils.init();
    tokenProvider = builder.tokenProvider;
    sentenceProvider = builder.sentenceProvider;
  }

  public static abstract class Builder<D extends BaseDocument, T extends Builder<D,T>> extends Annotator.Builder<D,T> {
    protected Function<Sentence, ? extends Iterable<Token>> tokenProvider;
    protected Function<Document<? extends D>, ? extends Iterable<Sentence>> sentenceProvider;

    public T useTokens(Function<Sentence, ? extends Iterable<Token>> f) {
      tokenProvider = f;
      return self();
    }

    public T useSentences(Function<Document<? extends D>, ? extends Iterable<Sentence>> f) {
      sentenceProvider = f;
      return self();
    }
  }

  public static void main(String... args) {
    final List<Document<BaseDocument>> documents = JsonCorpus.builder(args[0])
        .featPath(args[1])
        .annPath(args[1])
        .annotationSets("opennlp", "genia")
        .tiered()
        .build()
        .getDocumentList();

    final List<edu.utdallas.hlt.text.Document> kirkDocs = Lists.newArrayListWithCapacity(documents.size());
    for (final Document<BaseDocument> document : documents) {
      kirkDocs.add(new KirkDocument.ToKirkBuilder<>(document)
          .tokens(a -> a.getContained("genia", Token.TYPE))
          .sentences(d -> d.get("opennlp", Sentence.TYPE))
          .build());
    }

    try {
      final XMLDocumentWriter writer = new XMLDocumentWriter(new GeniaTokenReaderAndWriter());
      writer.writeAll(kirkDocs, Place.fromFile(args[3]));
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
