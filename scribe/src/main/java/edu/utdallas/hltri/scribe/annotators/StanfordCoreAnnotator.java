package edu.utdallas.hltri.scribe.annotators;

import com.google.common.collect.Lists;
import edu.stanford.nlp.dcoref.CorefChain;
import edu.stanford.nlp.dcoref.CorefCoreAnnotations;
import edu.stanford.nlp.ie.util.RelationTriple;
import edu.stanford.nlp.ling.CoreAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.naturalli.NaturalLogicAnnotations;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.semgraph.SemanticGraph;
import edu.stanford.nlp.semgraph.SemanticGraphCoreAnnotations;
import edu.stanford.nlp.semgraph.SemanticGraphEdge;
import edu.stanford.nlp.time.TimeAnnotations;
import edu.stanford.nlp.time.TimeAnnotator;
import edu.stanford.nlp.util.CoreMap;
import edu.utdallas.hltri.conf.Config;
import edu.utdallas.hltri.logging.Logger;
import edu.utdallas.hltri.scribe.text.Attribute;
import edu.utdallas.hltri.scribe.text.BaseDocument;
import edu.utdallas.hltri.scribe.text.Document;
import edu.utdallas.hltri.scribe.text.DuplicateAnnotationException;
import edu.utdallas.hltri.scribe.text.annotation.*;
import edu.utdallas.hltri.scribe.text.relation.Dependency;
import edu.utdallas.hltri.scribe.text.relation.OpenRelation;

import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.function.Function;

/**
 * runJava bsr064000.nlp.gate.StanfordCoreAnnotator corpora/trainCorpus0
 *
 * @author bryan
 */
public class StanfordCoreAnnotator<D extends BaseDocument> implements Annotator<D> {

  private static final Logger log = Logger.get(StanfordCoreAnnotator.class);
  public static final String ANNOTATION_SET = "stanford";

  private final StanfordCoreNLP pipeline;
  private final boolean pos;
  private final boolean lemma;
  private final boolean ssplit;
  private final boolean tokenize;
  private final boolean coref;
  private final boolean depparse;
  private final boolean parse;
  private final boolean ner;
  private final boolean time;
  private final boolean openie;
  private final Function<Document<? extends D>, String> docTimeGetter;

  private final boolean clear;

  private StanfordCoreAnnotator(final Builder<D> builder) {
    // u m8???
    PrintStream err = System.err;
    System.setErr(new PrintStream(new OutputStream() {
      @Override public void write(int b) { /* Do nothing */ }
    }));
    System.setErr(err);

    pos = builder.pos;
    lemma = builder.lemma;
    ssplit = builder.ssplit;
    tokenize = builder.tokenize;
    coref = builder.coref;
    depparse = builder.depparse;
    parse = builder.constParse;
    ner = builder.ner;
    time = builder.time;
    clear = builder.clear;
    openie = builder.openie;
    docTimeGetter = builder.docTimeGetter;

    pipeline = new StanfordCoreNLP(builder.props);
    if (time) {
      pipeline.addAnnotator(new TimeAnnotator("sutime", new Properties()));
    }
  }

  public enum NerLabelType {
    ALL_3_CLASS("edu.stanford.nlp.models.ner.english.all.3class.distsim.crf.ser.gz"),
    CONLL_4_CLASS("edu.stanford.nlp.models.ner.english.conll.4class.distsim.crf.ser.gz"),
    MUC_7_CLASS("edu.stanford.nlp.models.ner.english.muc.7class.distsim.crf.ser.gz");

    private final String model;
    NerLabelType(String model) {
      this.model = model;
    }
  }

  public static class Builder<D extends BaseDocument> extends Annotator.Builder<D,Builder<D>> {
    private final Properties props = new Properties();
    private String annotators = "tokenize";
    private boolean pos = false;
    private boolean lemma = false;
    private boolean ssplit = false;
    private boolean tokenize = true;
    private boolean coref = false;
    private boolean depparse = false;
    private boolean constParse = false;
    private boolean ner = false;
    private NerLabelType nerLabelType = NerLabelType.ALL_3_CLASS;
    private boolean clear = false;
    private boolean openie = false;
    private boolean useShiftReduceParser = false;
    private boolean time = false;
    private Function<Document<? extends D>, String> docTimeGetter = null;

    public Builder<D> tokenize() {
      if (annotators.length() > 0) {
        annotators += ", ";
      }
      annotators += "tokenize";
      tokenize = true;
      return self();
    }

    public Builder<D> ssplit() {
      if (!tokenize) {
        annotators += "tokenize";
        tokenize = true;
      }
      annotators += ", ssplit";
      ssplit = true;
      return self();
    }

    public Builder<D> pos() {
      if (!ssplit) {
        ssplit();
      }
      annotators += ", pos";
      pos = true;
      return self();
    }

    public Builder<D> lemma() {
      if (!pos) {
        pos();
      }
      annotators += ", lemma";
      lemma = true;
      return self();
    }

    public Builder<D> depParse() {
      if (!pos) {
        pos();
      }
      annotators += ", depparse";
      depparse = true;
      return self();
    }

    private Builder<D> ner() {
      if (!lemma) {
        lemma();
      }
      annotators += ", ner";
      ner = true;
      return self();
    }

    private Builder<D> ner(NerLabelType nerLabelType) {
      if (!lemma) {
        lemma();
      }
      this.annotators += ", ner";
      this.nerLabelType = nerLabelType;
      this.ner = true;
      return self();
    }

    private Builder<D> constParse(boolean useShiftReduceParser) {
      if(!ssplit) {
        ssplit();
      }
      this.useShiftReduceParser = useShiftReduceParser;
      annotators += ", parse";
      constParse = true;
      return self();
    }

    public Builder<D> time(final Function<Document<? extends D>, String> docTimeGetter) {
      if (!ner) {
        ner();
      }
      this.docTimeGetter = docTimeGetter;
      this.time = true;
      return self();
    }

    public Builder<D> coref() {
      if (!constParse) {
        constParse(false);
      }
      if (!ner) {
        ner();
      }
      annotators += ", dcoref";
      coref = true;
      return self();
    }

    public Builder<D> openie() {
      if (!constParse) {
        constParse(false);
      }
      if (!lemma) {
        lemma();
      }
      annotators += ", natlog, openie";
      openie = true;
      return self();
    }

    public Builder<D> clear() {
      this.clear = true;
      return self();
    }

    public Builder<D> all() {
      annotators = "tokenize, ssplit, pos, lemma, depparse, parse, ner, dcoref, natlog, openie";
      tokenize = true;
      ssplit = true;
      pos = true;
      lemma = true;
      depparse = true;
      constParse = true;
      coref = true;
      openie = true;
      return self();
    }

    @Override
    protected Builder<D> self() {
      return this;
    }

    @Override
    public StanfordCoreAnnotator<D> build() {
      if (useShiftReduceParser) {
        props.setProperty("parse.model", Config.load("scribe.annotators.stanford").getString("shift-reduce-model"));
      }
      props.setProperty("parse.maxlen", "80");
      props.setProperty("pos.maxlen", "80");
//      props.setProperty("pos.nthreads", "2");
//      props.setProperty("depparse.testThreads", "2");
      props.setProperty("depparse.sentenceTimeout", "10000");
      props.setProperty("depparse.extradependencies", "ref_only_uncollapsed");
      props.setProperty("annotators", annotators);
      return new StanfordCoreAnnotator<D>(self());
    }
  }

  @Override public void close() {

  }

  private static int getStart(List<CoreLabel> stanfordTokens) {
    return stanfordTokens.get(0).beginPosition();
  }

  private static int getEnd(List<CoreLabel> stanfordTokens) {
    return stanfordTokens.get(stanfordTokens.size() - 1).endPosition();
  }

  @Override public <B extends D> void annotate(final Document<B> document) {
    final Annotation result = new Annotation(document.asString());
    // set the DocumentTime if looking for SUTime annotations
    if (docTimeGetter != null) {
      final String docTime = docTimeGetter.apply(document);
      if (docTime != null) {
        result.set(CoreAnnotations.DocDateAnnotation.class, docTime);
      }
    }

    pipeline.annotate(result);

    if (clear) {
      document.clear(ANNOTATION_SET);
    }

    if (tokenize) {
      for (CoreMap stanfordToken : result.get(CoreAnnotations.TokensAnnotation.class)) {
        long start = (stanfordToken.get(CoreAnnotations.CharacterOffsetBeginAnnotation.class)).longValue();
        long end = (stanfordToken.get(CoreAnnotations.CharacterOffsetEndAnnotation.class)).longValue();
        final Token token = Token.TYPE.create(document, ANNOTATION_SET, start, end);
        if (pos) {
          token.set(Token.PoS, stanfordToken.get(CoreAnnotations.PartOfSpeechAnnotation.class));
        }
        if (lemma) {
          token.set(Token.Lemma, stanfordToken.get(CoreAnnotations.LemmaAnnotation.class));
        }
//        if (ner) {
//          final String neLabel = stanfordToken.get(CoreAnnotations.NamedEntityTagAnnotation.class);
//          log.info("{}\\\\{}", token, neLabel);
//        }
        log.trace("Annotated {}", token.describe());
      }
    }

    if (time) {
      for (CoreMap time : result.get(TimeAnnotations.TimexAnnotations.class)) {
        List<CoreLabel> tokens = time.get(CoreAnnotations.TokensAnnotation.class);
        final Timex3 timex = Timex3.TYPE.create(document, ANNOTATION_SET,
            tokens.get(0).get(CoreAnnotations.CharacterOffsetBeginAnnotation.class),
            tokens.get(tokens.size() - 1).get(CoreAnnotations.CharacterOffsetEndAnnotation.class));
        final edu.stanford.nlp.time.Timex stanTimex = time.get(TimeAnnotations.TimexAnnotation.class);
        setIfNotNull(timex, Timex3.tid, stanTimex.tid());
        setIfNotNull(timex, Timex3.value, stanTimex.value());
        setIfNotNull(timex, Timex3.altvalue, stanTimex.altVal());
        setIfNotNull(timex, Timex3.type, stanTimex.timexType());
        setIfNotNull(timex, Timex3.beginPoint, stanTimex.beginPoint());
        setIfNotNull(timex, Timex3.endPoint, stanTimex.endPoint());
      }
    }

    if (ssplit) {
      for (CoreMap stanfordSentence : result.get(CoreAnnotations.SentencesAnnotation.class)) {
        int start = (stanfordSentence.get(CoreAnnotations.CharacterOffsetBeginAnnotation.class));
//                .longValue();
        int end = (stanfordSentence.get(CoreAnnotations.CharacterOffsetEndAnnotation.class));
        final Sentence sentence = Sentence.TYPE.create(document, ANNOTATION_SET, start, end);
        log.trace("Annotated {}", sentence.describe());

        final List<Token> tokens = sentence.getContained(ANNOTATION_SET, Token.TYPE);
        if (depparse) {
          final SemanticGraph dependencies =
              stanfordSentence.get(SemanticGraphCoreAnnotations.CollapsedCCProcessedDependenciesAnnotation.class);
          for (SemanticGraphEdge edge : dependencies.edgeIterable()) {
            final Token parent = tokens.get(edge.getGovernor().index() - 1);
            final Token child = tokens.get(edge.getDependent().index() - 1);
            final Dependency dependency = Dependency.TYPE.create(ANNOTATION_SET, parent, child);
            dependency.set(Dependency.Label, edge.getRelation().getShortName());
            log.trace("Adding edge '{}'", dependency.describe());
            log.trace("Parent: {}", parent.describe());
            log.trace("Child: {}", child.describe());
          }
        }

        //TODO: add constituent parse annotations

        if (openie) {
          final Collection<RelationTriple> stanfordRelations =
              stanfordSentence.get(NaturalLogicAnnotations.RelationTriplesAnnotation.class);

          for (final RelationTriple stanfordRelation : stanfordRelations) {
            final int subjectStart = getStart(stanfordRelation.subject);
            final int subjectEnd = getEnd(stanfordRelation.subject);
            final Chunk subject = Chunk.TYPE.createOrWrap(document, ANNOTATION_SET, subjectStart, subjectEnd);
            subject.set(Chunk.headTokenId, tokens.get(stanfordRelation.subjectHead().index() - 1).getGateId());

            final int objectStart = getStart(stanfordRelation.object);
            final int objectEnd = getEnd(stanfordRelation.object);
            final Chunk object = Chunk.TYPE.createOrWrap(document, ANNOTATION_SET, objectStart, objectEnd);
            object.set(Chunk.headTokenId, tokens.get(stanfordRelation.objectHead().index() - 1).getGateId());

            final StringBuilder relationStringBuilder = new StringBuilder();
            final List<Integer> tokenIds = Lists.newArrayListWithCapacity(stanfordRelation.relation.size());
            for (final CoreLabel cl : stanfordRelation.relation) {
              assert cl != null : "cl is null. panic.";
              if (tokens.get(cl.index() - 1) == null) {
                System.out.printf("Doc %s: relation - [%s]\nBad token: %s.\nSentence length: %d", document.get(BaseDocument.id),
                    stanfordRelation.relation.toString(), cl.toString(), tokens.size());
                throw new RuntimeException();
              }
              relationStringBuilder.append(cl.toString(CoreLabel.OutputFormat.VALUE)).append(" ");
              tokenIds.add(tokens.get(cl.index() - 1).getGateId());
            }
            final OpenRelation relation = OpenRelation.TYPE.create(ANNOTATION_SET, subject, object);
            relation.set(OpenRelation.relation, relationStringBuilder.toString());
            relation.set(OpenRelation.relationTokenIds, tokenIds);
          }
        }
      }
    }

    if (coref) {
      final List<Sentence> sentences = document.get(ANNOTATION_SET, Sentence.TYPE);
      final Map<Integer, CorefChain> chains = result.get(CorefCoreAnnotations.CorefChainAnnotation.class);
      if (chains == null) {
        log.warn("No coref annotations in doc {}", document.get(BaseDocument.id));
      }
      else {
        for (final CorefChain chain : chains.values()) {
          for (final CorefChain.CorefMention mention : chain.getMentionsInTextualOrder()) {
            final Sentence sentence = sentences.get(mention.sentNum - 1);
            final List<Token> tokens = sentence.getContained(ANNOTATION_SET, Token.TYPE);
            final long start = tokens.get(mention.startIndex - 1).get(Token.StartOffset);
            final long end = tokens.get(mention.endIndex - 2).get(Token.EndOffset);
            final Token head = tokens.get(mention.headIndex - 1);
            try {
              final CorefMention cm = CorefMention.TYPE.create(document, ANNOTATION_SET, start, end)
                  .set(CorefMention.ClusterId, mention.corefClusterID)
                  .set(CorefMention.ClusterHeadId, head.getGateId());
              log.trace("Annotated coref mention: {}", cm.describe());
            } catch (DuplicateAnnotationException e) {
              throw new RuntimeException(e);
            }
          }
        }
      }
    }
  }

  private <T> void setIfNotNull(Timex3 timex, Attribute<Timex3, T> attr, T val) {
    if (val != null) {
      timex.set(attr, val);
    }
  }
}
