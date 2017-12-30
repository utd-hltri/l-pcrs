package edu.utdallas.hltri.scribe.annotators;

import edu.utdallas.hltri.logging.Logger;
import edu.utdallas.hltri.scribe.gate.GateUtils;
import edu.utdallas.hltri.scribe.text.Attribute;
import edu.utdallas.hltri.scribe.text.BaseDocument;
import edu.utdallas.hltri.scribe.text.Document;
import edu.utdallas.hltri.scribe.text.annotation.AbstractAnnotation;
import edu.utdallas.hltri.scribe.text.annotation.Chunk;
import edu.utdallas.hltri.scribe.text.annotation.PhraseChunk;
import edu.utdallas.hltri.scribe.text.annotation.Token;
import gate.LanguageAnalyser;

/**
 * Created by rmm120030 on 7/8/15.
 */
public class OpenNLPChunker<D extends BaseDocument> implements Annotator<D> {
  private final static Logger log = Logger.get(GeniaAnnotator.class);
  public static final String ANNOTATION_SET_NAME = "opennlp";

  private final LanguageAnalyser onlp;
  private boolean clear = false;

  private static final Attribute<Token, String> phraseChunkIobTag =
      Attribute.typed("chunk", String.class);

  public static final Attribute<PhraseChunk, String> chunkType =
      Attribute.typed("chunk-type", String.class);

  public OpenNLPChunker() {
    GateUtils.init();
    log.info("Loading OpenNLP chunker...");
    onlp = GateUtils.loadResource(LanguageAnalyser.class, "gate.opennlp.OpenNlpChunker")
//        .param("annotationSetName", ANNOTATION_SET_NAME)
        .param("inputASName", ANNOTATION_SET_NAME)
        .param("outputASName", ANNOTATION_SET_NAME)
        .build();
  }

  public OpenNLPChunker<D> clear() {
    clear = true;
    return this;
  }

  @Override
  public <B extends D> void annotate(Document<B> document) {
    if (clear || document.get("opennlp", Chunk.TYPE).isEmpty()) {
      log.debug("Annotating OpenNLP chunks on {}", document.get(BaseDocument.id));
      document.clear("opennlp", Chunk.TYPE);
      onlp.setDocument(document.asGate());
      try {
        onlp.execute();
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
      String type = null;
      long start = 0L;
      long end = 0L;
      for (Token token : document.get("opennlp", Token.TYPE)) {
        final String tag = token.get(phraseChunkIobTag);
        if (tag.startsWith("B-")) {
          if (type != null) {
            PhraseChunk.TYPE.create(document, "opennlp", start, end)
                .set(chunkType, type);
          }
          type = tag.substring(2);
          start = token.get(AbstractAnnotation.StartOffset).intValue();
          end = token.get(AbstractAnnotation.EndOffset).intValue();
        } else if (tag.startsWith("I-")) {
          end = token.get(AbstractAnnotation.EndOffset).intValue();
          assert tag.substring(2).equals(type) : "Found {}" + tag + "after B-" + type;
        } else if (tag.equals("O")) {
          if (type != null) {
            PhraseChunk.TYPE.create(document, "opennlp", start, end)
                .set(chunkType, type);
          }
          type = null;
        } else {
          throw new InvalidIobTagException("found IOB tag |" + tag + "|");
        }
      }
    }
  }

  @Override
  public void close() {
    gate.Factory.deleteResource(onlp);
    log.debug("OpenNLP tokenizer closed.");
  }

  public static class InvalidIobTagException extends RuntimeException {
    public InvalidIobTagException(String message) {
      super(message);
    }
  }
}
