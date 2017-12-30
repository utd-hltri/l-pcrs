package edu.utdallas.hltri.eeg.feature;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import edu.utdallas.hltri.eeg.annotation.EegActivity;
import edu.utdallas.hltri.logging.Logger;
import edu.utdallas.hltri.ml.Feature;
import edu.utdallas.hltri.ml.FeatureExtractor;
import edu.utdallas.hltri.ml.StringFeature;
import edu.utdallas.hltri.scribe.text.annotation.Annotation;
import edu.utdallas.hltri.scribe.text.annotation.Sentence;
import edu.utdallas.hltri.scribe.text.annotation.Token;
import edu.utdallas.hltri.scribe.text.relation.Dependency;
import edu.utdallas.hltri.scribe.text.relation.DependencyGraph;
import edu.utdallas.hltri.util.CharMatchers;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Extracts context features for activities/events
 * Created by rmm120030 on 11/3/16.
 */
public class ContextFeatureExtractor<A extends Annotation<A>> implements FeatureExtractor<A,Number> {
  private static final Logger log = Logger.get(ContextFeatureExtractor.class);
  private final String actAnnset;
  public static final String tokenAnnset = "stanford", sentenceAnnset = "stanford";

  public ContextFeatureExtractor(String actAnnset) {
    this.actAnnset = actAnnset;
  }

  @Override
  public Stream<? extends Feature<Number>> apply(A ann) {
    final List<Feature<?>> list = Lists.newArrayList();
    final List<Sentence> coveringSentences = ann.getCovering(sentenceAnnset, Sentence.TYPE);
    final List<Token> activityTokens = ann.getContained(tokenAnnset, Token.TYPE);
    if (activityTokens.size() > 0) {
      Sentence sentence = null;
      if (coveringSentences.size() > 0) {
        sentence = coveringSentences.get(0);
      }
      else {
        log.warn("{} not contained in sentence.", ann.describe());
        // use the longest overlapping sentence if it's not contained within a single sentence
        final List<Sentence> overlappingSentences = ann.getOverlapping(sentenceAnnset, Sentence.TYPE);
        if (overlappingSentences.size() > 0) {
          for (Sentence os : overlappingSentences) {
            if (sentence == null || os.length() > sentence.length()) {
              sentence = os;
            }
          }
        }
      }
      final Set<StringFeature> contextFeatures = new HashSet<>();
      if (sentence != null) {
        final List<Token> sentenceTokens = sentence.getContained(tokenAnnset, Token.TYPE);
        final Token startToken = activityTokens.get(0);
        final Token endToken = activityTokens.get(activityTokens.size()-1);
        contextFeatures.addAll(ngramFeatures(sentenceTokens, startToken, endToken, 3, tokenAnnset));
        contextFeatures.addAll(ngramFeatures(sentenceTokens, startToken, endToken, 2, tokenAnnset));
        contextFeatures.addAll(ngramFeatures(sentenceTokens, startToken, endToken, 4, tokenAnnset));
        contextFeatures.stream().map(Feature::toNumericFeature).forEach(list::add);
        final int SIDELOOK_SIZE = 3;
        for (int i = -SIDELOOK_SIZE;i <= SIDELOOK_SIZE;i++){
          if(i==0)
            continue;
          if( (i < 0 && sentenceTokens.indexOf(startToken) >= Math.abs(i)) ||
              (i > 0 && sentenceTokens.indexOf(activityTokens.get(activityTokens.size()-1)) + i < sentenceTokens.size())) {
            final Token theTok = sentenceTokens.get(sentenceTokens.indexOf(activityTokens.get(i < 0 ? 0 : activityTokens.size()-1))+i);
            list.add(Feature.stringFeature(String.format("context[%d]",i),theTok.get(Token.Lemma).toLowerCase()));
          }
        }

        final List<Token> context = sentenceTokens.stream().filter(t -> !activityTokens.contains(t)).collect(Collectors.toList());
        if (context.size() < 1) {
          log.warn("No context tokens for sentence [{}] containing concept [{}]", sentence, ann);
        }

        // get token pair features between the head of this activity and every 'important' word in the containing sentence
        for (Token token : context) {
          final Optional<String> lblOptional = isImportant(token);
          if (lblOptional.isPresent()) {
            list.addAll(tokenPairFeatures(getHead(ann, tokenAnnset), token, lblOptional.get(), sentence, tokenAnnset,
                DependencyGraph.of(sentence, sentenceAnnset), actAnnset));
          }
        }
      }
    }
    else {
      log.warn("No tokens in {}", ann.describe());
      list.add(Feature.stringFeature("token-B", ann.toString().toLowerCase()));
    }

    return list.stream().map(Feature::toNumericFeature);
  }

  private static Collection<Feature<?>> tokenPairFeatures(
      final Token govHead, final Token depHead, final String name, final Sentence sentence, final String tokenAnnset,
      final DependencyGraph dependencyGraph, final String actAnnset) {
    final List<Feature<?>> features = Lists.newArrayList();

    final List<Dependency> path = dependencyGraph.getPath(govHead, depHead);
    final List<Token> tokens = sentence.getContained(tokenAnnset, Token.TYPE);
    final List<EegActivity> activities = sentence.getContained(actAnnset, EegActivity.TYPE);

    // distance
//    features.add(Feature.categorical("distance-" + name,
//        () -> Math.abs(tokens.indexOf(govHead) - tokens.indexOf(depHead))).toLowlevelFeature());
    features.addAll(distanceThreshold(Math.abs(tokens.indexOf(govHead) - tokens.indexOf(depHead)), "distance-" + name));

    // path length
    features.add(Feature.binaryFeature("path_length-" + name + "=" + path.size(), true));
    features.addAll(distanceThreshold(path.size(), "path_length-" + name));

    //# of activities and punctuations between arguments
    final long start = govHead.get(Annotation.EndOffset);
    final long end = depHead.get(Annotation.StartOffset);
    final long eventsBetween = activities.stream()
        .filter(e -> e.get(Annotation.StartOffset) > start && e.get(Annotation.EndOffset) < end)
        .count();
    features.add(Feature.stringFeature("activities_between-" + name, ""+eventsBetween));
    final long punctuationsBetween = tokens.stream()
        .filter(e -> CharMatchers.PUNCTUATION.matchesAnyOf(e.asString()))
        .filter(e -> e.get(Annotation.StartOffset) > start && e.get(Annotation.EndOffset) < end)
        .count();
    features.add(Feature.stringFeature("punctuations_between-" + name, ""+punctuationsBetween));

    if (path.size() > 0) {
      // path of dependency labels between activities labels
//       features.add(Feature.stringFeature("path-" + name, getPathString(path)));

      final Set<Token> pathTokens = Sets.newHashSet();
      // each word in path along with label
      Token prevToken = govHead;
      for (final Dependency edge : path) {
        final Token tok = (prevToken.equals(edge.getDependant())) ? edge.getGovernor() : edge.getDependant();
        prevToken = tok;
        features.add(Feature.stringFeature("edge-in-path-" + name, edge.get(Dependency.Label)));
        features.add(Feature.stringFeature("lemma-in-path" + name, GeniaFeatureExtractor.lemmatizer.apply(tok)));
        pathTokens.add(edge.getGovernor());
        pathTokens.add(edge.getDependant());
      }
    } else {
      log.warn("No valid path from (%s) -> (%s).\n", govHead, depHead);
    }

    return features;
  }

  private static List<Feature<Boolean>> distanceThreshold(int i, String name) {
    final List<Feature<Boolean>> list = Lists.newArrayList();
    if (i < 3) {
      list.add(Feature.binaryFeature(name + "<3", true));
    }
    else if (i < 5) {
      list.add(Feature.binaryFeature(name + "<5", true));
    }
    else if (i < 10) {
      list.add(Feature.binaryFeature(name + "<10", true));
    }
    else if (i < 15) {
      list.add(Feature.binaryFeature(name + "<15", true));
    }
    else if (i < 20) {
      list.add(Feature.binaryFeature(name + "<20", true));
    }
    else {
      list.add(Feature.binaryFeature(name + ">=20", true));
    }
    return list;
  }

  private static Set<StringFeature> ngramFeatures(final List<Token> context, final Token startToken, final Token endToken,
                                                  final int n, final String prefix) {
    final Set<StringFeature> set = new HashSet<>();
    for (int i = context.indexOf(startToken) - n + 1; i <= context.indexOf(endToken); i++) {
      final int end = i + n;
      if (i >= 0 && end <= context.size()) {
        set.add(Feature.stringFeature(prefix + ":" + n + "-gram:",
            context.subList(i, end).stream().map(GeniaFeatureExtractor.lemmatizer).collect(Collectors.joining("_"))));
      }
    }
    return set;
  }

  private static <A extends Annotation<A>> Token getHead(final A event, final String tokenAnnSet) {
    List<Token> tokens = event.getContained(tokenAnnSet, Token.TYPE);
    if (tokens.size() == 0) {
      final List<Token> overlapping = event.getOverlapping(tokenAnnSet, Token.TYPE);
      if (overlapping.size() == 0) {
        return null;
      }
      else {
        tokens = overlapping;
      }
    }
    Token head = tokens.get(0);
    for (Token token : tokens) {
      if (token.get(Token.PoS).toUpperCase().contains("NN")) {
        return token;
      }
    }
    return head;
  }

  private static Optional<String> isImportant(final Token token) {
    for (final EegActivity.Band label : EegActivity.Band.values()) {
      if (ConceptSpanFeatureExtractor.matchAttr(token, label, null)) {
        return Optional.of(label.toString());
      }
    }
    for (final EegActivity.Morphology label : EegActivity.Morphology.values()) {
      if (ConceptSpanFeatureExtractor.matchAttr(token, label, null)) {
        return Optional.of(label.toString());
      }
    }
    for (final EegActivity.Hemisphere label : EegActivity.Hemisphere.values()) {
      if (ConceptSpanFeatureExtractor.matchAttr(token, label, null)) {
        return Optional.of(label.toString());
      }
    }
    for (final EegActivity.Location label : EegActivity.Location.values()) {
      if (ConceptSpanFeatureExtractor.matchAttr(token, label, null)) {
        return Optional.of(label.toString());
      }
    }
    for (final EegActivity.Dispersal label : EegActivity.Dispersal.values()) {
      if (ConceptSpanFeatureExtractor.matchAttr(token, label, null)) {
        return Optional.of(label.toString());
      }
    }
    for (final EegActivity.Recurrence label : EegActivity.Recurrence.values()) {
      if (ConceptSpanFeatureExtractor.matchAttr(token, label, null)) {
        return Optional.of(label.toString());
      }
    }
    for (final EegActivity.Magnitude label : EegActivity.Magnitude.values()) {
      if (ConceptSpanFeatureExtractor.matchAttr(token, label, null)) {
        return Optional.of(label.toString());
      }
    }
    return Optional.empty();
  }
}
