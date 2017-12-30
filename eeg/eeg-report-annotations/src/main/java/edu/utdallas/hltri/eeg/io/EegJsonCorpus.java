package edu.utdallas.hltri.eeg.io;

import edu.utdallas.hlt.medbase.umls.UMLSLexicon;
import edu.utdallas.hltri.eeg.EegNote;
import edu.utdallas.hltri.scribe.text.BaseDocument;
import edu.utdallas.hltri.scribe.text.annotation.Section;
import edu.utdallas.hltri.eeg.annotators.RegExEegAnnotator;
import edu.utdallas.hltri.scribe.annotators.GeniaAnnotator;
import edu.utdallas.hltri.scribe.annotators.OpenNLPSentenceAnnotator;
import edu.utdallas.hltri.scribe.annotators.StanfordCoreAnnotator;
import edu.utdallas.hltri.scribe.io.JsonCorpus;
import edu.utdallas.hltri.scribe.text.Document;
import edu.utdallas.hltri.scribe.text.annotation.Chunk;
import edu.utdallas.hltri.scribe.text.annotation.Sentence;
import edu.utdallas.hltri.scribe.text.annotation.Token;
import edu.utdallas.hltri.scribe.text.relation.Dependency;
import edu.utdallas.hltri.util.Lazy;

import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Created by rmm120030 on 2/18/16.
 */
public class EegJsonCorpus extends JsonCorpus<EegNote> {

  private static final Supplier<OpenNLPSentenceAnnotator<BaseDocument>> openNlp = Lazy.lazily(OpenNLPSentenceAnnotator::new);
  private static final Supplier<GeniaAnnotator<BaseDocument>> genia = Lazy.lazily(() ->
      GeniaAnnotator.tokenAnnotator(doc -> doc.get(OpenNLPSentenceAnnotator.ANNOTATION_SET_NAME, Sentence.TYPE), false));
  private static final Supplier<UMLSLexicon> umls = Lazy.lazily(UMLSLexicon::instance);
  private static final Supplier<StanfordCoreAnnotator<BaseDocument>> stanford = Lazy.lazily(() ->
      new StanfordCoreAnnotator.Builder<>().all().clear().build());
  private static final Function<String, RegExEegAnnotator> sectioner = RegExEegAnnotator::<BaseDocument>sectionAnnotator;
  private final boolean doPreprocessing;

  private EegJsonCorpus(final JsonCorpus.Builder<EegNote> builder, final boolean doPreprocessing) {
    super(builder);
    this.doPreprocessing = doPreprocessing;
  }

  @SuppressWarnings("unused")
  public static EegJsonCorpus newCorpus(final String jsonPath, final boolean tiered, final String... annsets) {
    return newCorpus(jsonPath, tiered, false, annsets);
  }

  @SuppressWarnings("WeakerAccess")
  public static EegJsonCorpus newCorpus(final String jsonPath, final boolean tiered, final boolean doPreprocessing, final String... annsets) {
    final Builder<EegNote> builder = JsonCorpus.<EegNote>at(jsonPath)
        .annotationSets(annsets, "genia", "opennlp", "umls", "stanford", "regex-eeg");
    if (tiered) {
      builder.tiered();
    }
    return new EegJsonCorpus(builder, doPreprocessing);
  }

  @Override
  public Document<EegNote> loadDocument(final String id) {
    final Document<EegNote> document = super.loadDocument(id);
    if (doPreprocessing) {
      preprocess(document);
      save(document);
    }
    return document;
  }

  public static void preprocess(final Document<EegNote> document) {
    if (document.get("opennlp", Sentence.TYPE).isEmpty()) {
      openNlp.get().annotate(document);
    }
    if (document.get("genia", Token.TYPE).isEmpty()) {
      genia.get().annotate(document);
    }
    if (document.get("umls", Chunk.TYPE).isEmpty()) {
      umls.get().annotate(document);
    }
    if (document.get("stanford", Chunk.TYPE).isEmpty() || document.getRelations("stanford", Dependency.TYPE).isEmpty()) {
      stanford.get().annotate(document);
    }
    if (document.get("regex-eeg", Section.TYPE).isEmpty()) {
      sectioner.apply("opennlp").annotate(document);
    }
  }
}
