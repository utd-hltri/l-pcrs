package edu.utdallas.hltri.scribe.annotators;

import edu.utdallas.hltri.logging.Logger;
import edu.utdallas.hltri.scribe.gate.GateUtils;
import edu.utdallas.hltri.scribe.text.BaseDocument;
import edu.utdallas.hltri.scribe.text.Document;
import edu.utdallas.hltri.scribe.text.annotation.Sentence;
import gate.LanguageAnalyser;

/**
 * Created by rmm120030 on 7/8/15.
 */
public class OpenNLPSentenceAnnotator<D extends BaseDocument> implements Annotator<D> {
  private final static Logger log = Logger.get(GeniaAnnotator.class);
  public static final String ANNOTATION_SET_NAME = "opennlp";

  private final LanguageAnalyser onlp;
  private boolean clear = false;

  public OpenNLPSentenceAnnotator() {
    GateUtils.init();
    log.info("Opening OpenNLP Sentence Splitter...");
    onlp = GateUtils.loadResource(LanguageAnalyser.class, "gate.opennlp.OpenNlpSentenceSplit")
        .param("annotationSetName", ANNOTATION_SET_NAME)
        .build();
  }

  public OpenNLPSentenceAnnotator<D> clear() {
    clear = true;
    return this;
  }

  @Override
  public <B extends D> void annotate(Document<B> document) {
    if (clear || document.get("opennlp", Sentence.TYPE).isEmpty()) {
      log.trace("Annotating OpenNLP sentences on {}", document.get(BaseDocument.id));
      document.clear("opennlp", Sentence.TYPE);
      onlp.setDocument(document.asGate());
      try {
        onlp.execute();
        log.trace("OpenNLP: split {}", document.get(BaseDocument.id));
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }
  }

  @Override
  public void close() {
    gate.Factory.deleteResource(onlp);
    log.debug("OpenNLP sentence splitter closed.");
  }
}
