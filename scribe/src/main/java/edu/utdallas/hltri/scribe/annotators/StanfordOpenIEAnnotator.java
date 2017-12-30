package edu.utdallas.hltri.scribe.annotators;

import com.google.common.collect.Lists;

import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.hash.TObjectIntHashMap;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import edu.stanford.nlp.ie.util.RelationTriple;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.ling.IndexedWord;
import edu.stanford.nlp.ling.Word;
import edu.stanford.nlp.naturalli.NaturalLogicAnnotations;
import edu.stanford.nlp.naturalli.NaturalLogicAnnotator;
import edu.stanford.nlp.naturalli.OpenIE;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.WordsToSentencesAnnotator;
import edu.stanford.nlp.process.CoreLabelTokenFactory;
import edu.stanford.nlp.semgraph.SemanticGraph;
import edu.stanford.nlp.semgraph.SemanticGraphCoreAnnotations;
import edu.stanford.nlp.semgraph.SemanticGraphFactory;
import edu.stanford.nlp.trees.GrammaticalRelation;
import edu.stanford.nlp.trees.GrammaticalStructure;
import edu.stanford.nlp.trees.TreeGraphNode;
import edu.stanford.nlp.trees.TypedDependency;
import edu.stanford.nlp.trees.UniversalEnglishGrammaticalStructure;
import edu.stanford.nlp.util.CoreMap;
import edu.uci.ics.jung.graph.DelegateForest;
import edu.utdallas.hltri.logging.Logger;
import edu.utdallas.hltri.scribe.text.BaseDocument;
import edu.utdallas.hltri.scribe.text.Document;
import edu.utdallas.hltri.scribe.text.annotation.AbstractAnnotation;
import edu.utdallas.hltri.scribe.text.annotation.Chunk;
import edu.utdallas.hltri.scribe.text.annotation.Sentence;
import edu.utdallas.hltri.scribe.text.annotation.Token;
import edu.utdallas.hltri.scribe.text.relation.Dependency;
import edu.utdallas.hltri.scribe.text.relation.DependencyGraph;
import edu.utdallas.hltri.scribe.text.relation.OpenRelation;
import edu.utdallas.hltri.scribe.text.relation.Relation;

/**
 * Created by travis on 9/3/15.
 */
public class StanfordOpenIEAnnotator<D extends BaseDocument> implements Annotator<D> {
  final static Logger log = Logger.get(StanfordOpenIEAnnotator.class);


  WordsToSentencesAnnotator sssplit = WordsToSentencesAnnotator.nonSplitter(false);
  NaturalLogicAnnotator snla = new NaturalLogicAnnotator();
  OpenIE soie = new OpenIE();

  @Override
  public <B extends D> void annotate(Document<B> document) {
    document.clear("stanford-openie");


    final CoreLabelTokenFactory coreLabelTokenFactory = new CoreLabelTokenFactory();
    final List<Sentence> sentences = document.get("stanford", Sentence.TYPE);
    for (int si = 0; si < sentences.size(); si++) {
      final Sentence sentence = sentences.get(si);
      final int sentenceStart = sentence.get(AbstractAnnotation.StartOffset).intValue();
      final List<Token> tokens = sentence.getContained("stanford", Token.TYPE);
      final TObjectIntMap<Token> tokenIdMap = new TObjectIntHashMap<>();

      final Annotation stanfordSentenceDocument = new Annotation(sentence.asString());

      // Collect stanford Tokens
      final List<CoreLabel> stanfordTokens = Lists.newArrayList();
      for (int ti = 0; ti < tokens.size(); ti++) {
        final Token token = tokens.get(ti);
        tokenIdMap.put(token, ti);

        final int start = token.get(AbstractAnnotation.StartOffset).intValue() - sentenceStart;
        final int length = token.get(AbstractAnnotation.EndOffset).intValue() - start - sentenceStart;
        final CoreLabel stanfordToken = coreLabelTokenFactory.makeToken(token.toString(), start, length);
        stanfordToken.setIndex(ti + 1);
        stanfordToken.setLemma(token.get(Token.Lemma));
        stanfordToken.setTag(token.get(Token.PoS));
        stanfordToken.setSentIndex(si + 1);
        stanfordToken.setDocID(document.get(BaseDocument.id));
        stanfordTokens.add(stanfordToken);
      }
      stanfordSentenceDocument.set(CoreAnnotations.TokensAnnotation.class, stanfordTokens);

      final IndexedWord root = new IndexedWord(new Word("ROOT"));
      final List<TypedDependency> stanfordDependencies = new ArrayList<>();

      // Collect stanford dependencies
      final DependencyGraph dependencies = DependencyGraph.of(sentence, "stanford");
      final DelegateForest<Token, Dependency>
          dependencyForest = new DelegateForest<>(dependencies.asJungGraph());
      for (final Dependency dependency : dependencyForest.getEdges()) {
        stanfordDependencies.add(new TypedDependency(GrammaticalRelation.valueOf(dependency.get(Dependency.Label)),
                                                     new IndexedWord(stanfordTokens.get(tokenIdMap.get(dependency.getGovernor()))),
                                                     new IndexedWord(stanfordTokens.get(tokenIdMap.get(dependency.getDependant())))));

        if (dependency.getDependant() == dependency.getGovernor()) {
          stanfordDependencies.add(new TypedDependency(GrammaticalRelation.ROOT,
                                                       root,
                                                       new IndexedWord(stanfordTokens.get(tokenIdMap.get(dependency.getDependant())))));
        }
      }

      for (final Token forestRoot : dependencyForest.getRoots()) {
        stanfordDependencies.add(new TypedDependency(GrammaticalRelation.ROOT,
                                                     root,
                                                     new IndexedWord(stanfordTokens.get(tokenIdMap.get(forestRoot)))));

      }

      final UniversalEnglishGrammaticalStructure
          gs = new UniversalEnglishGrammaticalStructure(stanfordDependencies, new TreeGraphNode(root));

      sssplit.annotate(stanfordSentenceDocument);

      final CoreMap
          stanfordSentence = stanfordSentenceDocument.get(CoreAnnotations.SentencesAnnotation.class).get(
          0);

      SemanticGraph
          deps = SemanticGraphFactory.makeFromTree(gs, SemanticGraphFactory.Mode.COLLAPSED,
                                                   GrammaticalStructure.Extras.REF_ONLY_UNCOLLAPSED, true, null),
          uncollapsedDeps = SemanticGraphFactory.makeFromTree(gs, SemanticGraphFactory.Mode.BASIC, GrammaticalStructure.Extras.REF_ONLY_UNCOLLAPSED, true, null),
          ccDeps = SemanticGraphFactory.makeFromTree(gs, SemanticGraphFactory.Mode.CCPROCESSED, GrammaticalStructure.Extras.REF_ONLY_UNCOLLAPSED, true, null);
      deps.resetRoots();
      uncollapsedDeps.resetRoots();

      stanfordSentence
          .set(SemanticGraphCoreAnnotations.CollapsedDependenciesAnnotation.class, deps);

      stanfordSentence.set(SemanticGraphCoreAnnotations.BasicDependenciesAnnotation.class,
                           uncollapsedDeps);

      stanfordSentence.set(
          SemanticGraphCoreAnnotations.CollapsedCCProcessedDependenciesAnnotation.class,
          ccDeps);


      snla.annotate(stanfordSentenceDocument);
      soie.annotate(stanfordSentenceDocument);

      final Collection<RelationTriple> stanfordRelations = stanfordSentenceDocument.get(CoreAnnotations.SentencesAnnotation.class).get(0).get(
          NaturalLogicAnnotations.RelationTriplesAnnotation.class);

      for (final RelationTriple stanfordRelation: stanfordRelations) {
        final int subjectStart = sentenceStart + stanfordRelation.subject.get(0).beginPosition();
        final int subjectEnd = sentenceStart + stanfordRelation.subject.get(stanfordRelation.subject.size() - 1).endPosition();
        final Chunk subject = Chunk.TYPE.createOrWrap(document, "stanford-openie", subjectStart,
                                                      subjectEnd);

        final int objectStart = sentenceStart + stanfordRelation.object.get(0).beginPosition();
        final int objectEnd = sentenceStart + stanfordRelation.object.get(stanfordRelation.object.size() - 1).endPosition();
        final Chunk object = Chunk.TYPE.createOrWrap(document, "stanford-openie", objectStart,
                                                     objectEnd);

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
        final OpenRelation relation = OpenRelation.TYPE.create("stanford-openie", subject, object);
        relation.set(OpenRelation.relation, relationStringBuilder.toString());
        relation.set(OpenRelation.relationTokenIds, tokenIds);
      }
    }
  }

  @Override
  public void close() {
    sssplit = null;
    snla = null;
    soie = null;
  }
}
