package edu.utdallas.hltri.scribe.annotators;

import com.google.common.base.CharMatcher;
import com.google.common.base.Splitter;

import EDU.oswego.cs.dl.util.concurrent.TimeoutException;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import edu.utdallas.hltri.conf.Config;
import edu.utdallas.hltri.logging.Logger;
import edu.utdallas.hltri.scribe.gate.GateUtils;
import edu.utdallas.hltri.scribe.text.Attribute;
import edu.utdallas.hltri.scribe.text.BaseDocument;
import edu.utdallas.hltri.scribe.text.Document;
import edu.utdallas.hltri.scribe.text.annotation.Entity;
import edu.utdallas.hltri.scribe.text.annotation.PhraseChunk;
import edu.utdallas.hltri.scribe.text.annotation.Sentence;
import edu.utdallas.hltri.scribe.text.annotation.Token;
import gate.Factory;
import gate.LanguageAnalyser;

/**
 * Created with IntelliJ IDEA.
 * User: Ramon
 * Date: 7/16/14
 * Time: 5:46 PM
 */
@SuppressWarnings("unused")
public class GeniaAnnotator<D extends BaseDocument> implements Annotator<D> {
  private final static Logger log = Logger.get(GeniaAnnotator.class);
  private static final CharMatcher matcher = CharMatcher.is('\n').precomputed();

  public static final String ANNOTATION_SET_NAME = "genia";

  private final Process process;
  private final BufferedWriter writer;
  private final BufferedReader reader;
  private final boolean annotateSentences;
  private final boolean annotateTokens;
  private final Function<Document<? extends D>, ? extends Iterable<Sentence>> sentenceProvider;
  private final boolean clear;
  private final LanguageAnalyser sentenceSplitter;

  @SuppressWarnings("WeakerAccess")
  public static Attribute<Token, String>       phraseIOB       = Attribute.typed("phrase", String.class);

  @SuppressWarnings("WeakerAccess")
  public static Attribute<Token, String>       entityIOB       = Attribute.typed("entity", String.class);

  @SuppressWarnings("WeakerAccess")
  public static Attribute<PhraseChunk, String> PhraseType      = Attribute.typed("type", String.class);

  @SuppressWarnings("WeakerAccess")
  public static Attribute<Entity, String>      GeniaEntityType = Attribute.typed("type", String.class);

  /**
   * A genia annotator that just does sentences
   */
  public static <D extends BaseDocument> GeniaAnnotator<D> sentenceAnnotator(final boolean clear) {
    final Builder<D> builder = new Builder<D>().annotateSentences();
    return (clear) ? builder.clear().build() : builder.build();
  }

  /**
   * Annotates both sentences and tokens
   */
  public static <D extends BaseDocument> GeniaAnnotator<D> sentenceAndTokenAnnotator(final boolean clear) {
    final Builder<D> builder = new Builder<D>().annotateSentences().annotateTokens();
    return (clear) ? builder.clear().build() : builder.build();
  }

  /**
   * Annotates documents
   * @param sentenceProvider uses the sentences provided by the passed provider
   */
  public static <D extends BaseDocument> GeniaAnnotator<D> tokenAnnotator(Function<Document<? extends D>, ? extends Iterable<Sentence>> sentenceProvider, final boolean clear) {
    final Builder<D> builder = new Builder<D>().annotateTokens().withSentences(sentenceProvider);
    return (clear) ? builder.clear().build() : builder.build();
  }

  /**
   * Starts the GENIA Tagger and establishes communication with it
   */
  private GeniaAnnotator(final Builder<D> builder) {
    // if not sentence provider was provided and we are going to annot8 sentences, make the created genia sentences
    // the provided ones
    if (builder.sentenceProvider == null && builder.annotateSentences) {
      this.sentenceProvider = (d -> d.get(ANNOTATION_SET_NAME, Sentence.TYPE));
    }
    else {
      this.sentenceProvider = builder.sentenceProvider;
    }
    this.annotateSentences = builder.annotateSentences;
    this.annotateTokens = builder.annotateTokens;
    this.clear = builder.clear;

    if (annotateSentences) {
      log.info("Genia Annotator annotating sentences. Initializing gate genia splitter...");
      GateUtils.init();
    }
    sentenceSplitter = (annotateSentences) ? GateUtils.loadResource(LanguageAnalyser.class, "gate.creole.genia.splitter.GENIASentenceSplitter")
        .param("splitterBinary", "/shared/aifiles/disk1/travis/software/geniass-1.00-amd64/geniass-64")
        .param("debug", true)
        .param("annotationSetName", ANNOTATION_SET_NAME)
        .build()
        : null;

    // initializes the Genia Tokenizer, opening streams to and from it
    if (annotateTokens) {
      log.info("Genia Annotator annotating tokens. Initializing Genia Tagger...");
      final Config conf = Config.load("scribe.annotators.genia");
      String geniaDir = System.getenv("GENIA_TAGGER_HOME");
      if (geniaDir == null) {
        geniaDir = String.format(conf.getString("tagger-path"), System.getProperty("os.arch"));
      }
      final ProcessBuilder pb = new ProcessBuilder("./geniatagger");
      pb.directory(new File(geniaDir));
      try {
        process = pb.start();
//    The following line copies Genia's output to the console
//      pb.redirectError(ProcessBuilder.Redirect.INHERIT);
        writer = new BufferedWriter(new OutputStreamWriter(process.getOutputStream()));
        reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
      } catch (IOException e) {
        throw new RuntimeException(e);
      } finally {
        log.debug("GENIA Tagger initialized at {}", geniaDir);
      }
    }
    else {
      process = null;
      reader = null;
      writer = null;
    }
  }

  public static class Builder<D extends BaseDocument> extends Annotator.Builder<D,Builder<D>> {
    private boolean annotateSentences = false;
    private boolean annotateTokens = false;
    private Function<Document<? extends D>, ? extends Iterable<Sentence>> sentenceProvider;
    private boolean clear = false;

    @Override
    protected Builder<D> self() {
      return this;
    }

    public Builder<D> clear() {
      this.clear = true;
      return self();
    }

    public Builder<D> withSentences(Function<Document<? extends D>, ? extends Iterable<Sentence>> sentenceProvider) {
      this.sentenceProvider = sentenceProvider;
      return self();
    }

    @SuppressWarnings("WeakerAccess")
    public Builder<D> annotateSentences() {
      this.annotateSentences = true;
      return self();
    }

    public Builder<D> annotateTokens() {
      this.annotateTokens = true;
      return self();
    }

    @Override
    public GeniaAnnotator<D> build() {
      return new GeniaAnnotator<>(self());
    }
  }

  /**
   *
   */
  @Override
  public <B extends D> void annotate(Document<B> document) {
    if (!annotateSentences && !annotateTokens) {
      throw new RuntimeException("Genia annotator isn't annotating anything. Set annotateSentences() or annotateTokens() in the builder");
    }
    if (annotateSentences) {
      annotateSentences(document);
    }
    if (annotateTokens) {
      annotateTokens(document);
    }
  }

  /**
   * Tokenizes each sentences in the Document, one at a time, providing the following annotations:
   * - tokens with POS and lemma
   * - phrase chunk
   * - named entity
   *
   * @param document the Document to be annotated; must have Sentences annotated
   */
  private synchronized <B extends D>  void annotateTokens(final Document<B> document) {
    try {
      final Splitter splitter = Splitter.on('\t');

      // Clear if needed
      if (!document.get(ANNOTATION_SET_NAME, Token.TYPE).isEmpty()) {
        if (clear) {
          log.debug("Clearing existing GENIA token and phrase chunk annotations on {}",
              document.get(BaseDocument.id));
          document.clear(ANNOTATION_SET_NAME, Token.TYPE);
          document.clear(ANNOTATION_SET_NAME, PhraseChunk.TYPE);
          document.clear(ANNOTATION_SET_NAME, Entity.TYPE);
        } else {
          log.warn("Document {} already has GENIA annotations", document.get(BaseDocument.id));
        }
      }

      // Skip if needed
      if (!document.get(ANNOTATION_SET_NAME, Token.TYPE).isEmpty()) {
        log.warn("Skipping document {} as it already has GENIA tokens", document.get(BaseDocument.id));
      }
      else {
        // Otherwise, add stuff
        log.trace("Annotating GENIA tokens, chunks, & entities on {}", document.get(BaseDocument.id));
        String line, chunk, chunkType = "", entity, entityType = "", token;
        int tokenStart, tokenEnd = 0, chunkStart = -1, chunkEnd = -1, entityStart = -1, entityEnd = -1;
        List<String> list;

        for (Sentence sentence : sentenceProvider.apply(document)) {
          writer.write(matcher.replaceFrom(sentence.asString(), ' '));
          writer.newLine();
          writer.flush();

          while ((line = reader.readLine()).length() > 0) {
            list = splitter.splitToList(line);
            token = list.get(0);

            tokenStart = document.asString().indexOf(token, tokenEnd);
            if (tokenStart < 0) {
              if (token.equals("``") || token.equals("''")) {
                token = "\"";
                tokenStart = document.asString().indexOf(token, tokenEnd);
              }
              if ((tokenStart < 0)) {
                log.error("Unable to locate token |{}| after offset {} in Document {}: {}", token,
                    tokenEnd,
                    document.get(BaseDocument.id),
                    document.asString().substring(tokenEnd));
                continue;
              }
            }
            tokenEnd = tokenStart + token.length();

            final Token tkn = Token.TYPE.create(document, ANNOTATION_SET_NAME, tokenStart, tokenEnd)
                .set(Token.Lemma, list.get(1))
                .set(Token.PoS, list.get(2))
                .set(GeniaAnnotator.phraseIOB, list.get(3))
                .set(GeniaAnnotator.entityIOB, list.get(4));

            chunk = list.get(3);
            switch (chunk.charAt(0)) {
              case 'B': {
                if (chunkStart != -1) {
                  PhraseChunk.TYPE.create(document, ANNOTATION_SET_NAME, chunkStart, chunkEnd)
                      .set(PhraseType, chunkType);
                }
                chunkType = chunk.substring(2);
                chunkStart = tokenStart;
                chunkEnd = tokenEnd;
                break;
              }

              case 'I': {
                chunkEnd = tokenEnd;
                break;
              }

              case 'O': {
                if (chunkStart != -1) {
                  PhraseChunk.TYPE.create(document, ANNOTATION_SET_NAME, chunkStart, chunkEnd)
                      .set(PhraseType, chunkType);
                  chunkStart = -1;
                }
                break;
              }

              default:
                throw new IOException("Encountered invalid IOB tag '" + chunk.charAt(0) + "'");
            }

            entity = list.get(4);
            switch (entity.charAt(0)) {
              case 'B': {
                if (entityStart != -1) {
                  Entity.TYPE.create(document, ANNOTATION_SET_NAME, entityStart, entityEnd).set(Entity.type, entityType);
                }
                entityType = entity.substring(2);
                entityStart = tokenStart;
                entityEnd = tokenEnd;
                break;
              }

              case 'I': {
                entityEnd = tokenEnd;
                break;
              }

              case 'O': {
                if (entityStart != -1) {
                  Entity.TYPE.create(document, ANNOTATION_SET_NAME, entityStart, entityEnd)
                      .set(GeniaEntityType, entityType);
                }
                entityStart = -1;
                break;
              }

              default:
                throw new IOException("Encountered invalid IOB tag '" + entity.charAt(0) + "'");
            }
          }
        }
      }
    }catch(IOException e){
      throw new RuntimeException(e);
    }
  }

  /**
   * Annotates sentences using the Genia sentence splitter from gate
   * @param document the Document to be annotated
   */
  private <B extends D> void annotateSentences(final Document<B> document) {
    if (clear || document.get(ANNOTATION_SET_NAME, Sentence.TYPE).isEmpty()) {
      document.clear(ANNOTATION_SET_NAME, Sentence.TYPE);
      sentenceSplitter.setDocument(document.asGate());
      try {
        sentenceSplitter.execute();
        log.debug("GENIA: split {}", document.getId());
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    } else {
      log.warn("Document {} already has genia annotations, skipping...", document.getId());
    }
  }

  /**
   * Closes this stream and releases any system resources associated
   * with it. If the stream is already closed then invoking this
   * method has no effect.
   */
  @Override
  public void close() {
    try {
      if (annotateTokens)  {
        writer.close();
        try {
          process.waitFor(1, TimeUnit.MINUTES);
        } catch(TimeoutException e) {
          process.destroyForcibly().waitFor();
        }
        process.exitValue();
        reader.close();
      }
      if (annotateSentences) {
        Factory.deleteResource(sentenceSplitter);
      }
    } catch (IOException | InterruptedException e) {
      throw new RuntimeException(e);
    } finally {
      log.debug("GENIA Tagger closed");
    }
  }
}
