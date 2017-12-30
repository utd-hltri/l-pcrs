package edu.utdallas.hltri.eeg.feature;

import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import edu.utdallas.hltri.eeg.annotation.EegActivity;
import edu.utdallas.hltri.logging.Logger;
import edu.utdallas.hltri.ml.Feature;
import edu.utdallas.hltri.ml.FeatureExtractor;
import edu.utdallas.hltri.scribe.text.annotation.Annotation;
import edu.utdallas.hltri.scribe.text.annotation.Token;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Extracts features from a clinical concept's span, disregarding context
 * Created by rmm120030 on 11/3/16.
 */
public class ConceptSpanFeatureExtractor<A extends Annotation<A>> implements FeatureExtractor<A,String> {
  private static final Logger log = Logger.get(ConceptSpanFeatureExtractor.class);
  private static final String tokenAnnset = "genia";

  @Override
  public Stream<? extends Feature<String>> apply(A ann) {
    final List<Feature<String>> list = Lists.newArrayList();
    list.add(Feature.stringFeature("raw_span", ann.toString().toLowerCase().replaceAll("\\s", "_")));
//    log.debug("Made raw span feature: {}", list);
    final List<Token> activityTokens = ann.getContained(tokenAnnset, Token.TYPE);
    if (activityTokens.size() > 0) {
      list.addAll(attributeMatchingFeatures(activityTokens).stream()
          .map(label -> Feature.stringFeature("matches", label)).collect(Collectors.toList()));
      activityTokens.forEach(t -> list.add(Feature.stringFeature("in_span", GeniaFeatureExtractor.lemmatizer.apply(t))));
    } else {
      log.warn("No tokens in {}", ann.describe());
      list.add(Feature.stringFeature("token-B", ann.toString().toLowerCase()));
    }

    //token features in span
    list.addAll(tokensInSpanFeatures(activityTokens));
    return list.stream();
  }

  // provides a feature for each attribute name that matches a token in the passed list
  private static Set<String> attributeMatchingFeatures(final List<Token> tokens) {
    final Set<String> matchingLabels = Sets.newHashSet();
    for (final Token token : tokens) {
      for (final EegActivity.Band label : EegActivity.Band.values()) {
        if (!matchingLabels.contains(label.name())) {
          matchAttr(token, label, matchingLabels);
        }
      }
      for (final EegActivity.Morphology label : EegActivity.Morphology.values()) {
        if (!matchingLabels.contains(label.name())) {
          matchAttr(token, label, matchingLabels);
        }
      }
      for (final EegActivity.Hemisphere label : EegActivity.Hemisphere.values()) {
        if (!matchingLabels.contains(label.name())) {
          matchAttr(token, label, matchingLabels);
        }
      }
      for (final EegActivity.Location label : EegActivity.Location.values()) {
        if (!matchingLabels.contains(label.name())) {
          matchAttr(token, label, matchingLabels);
        }
      }
      for (final EegActivity.Dispersal label : EegActivity.Dispersal.values()) {
        if (!matchingLabels.contains(label.name())) {
          matchAttr(token, label, matchingLabels);
        }
      }
      for (final EegActivity.Recurrence label : EegActivity.Recurrence.values()) {
        if (!matchingLabels.contains(label.name())) {
          matchAttr(token, label, matchingLabels);
        }
      }
      for (final EegActivity.Magnitude label : EegActivity.Magnitude.values()) {
        if (!matchingLabels.contains(label.name())) {
          matchAttr(token, label, matchingLabels);
        }
      }
    }

    return matchingLabels;
  }

  private static final Splitter splitter = Splitter.on("_");
  static boolean matchAttr(final Token token, final Enum label, final Set<String> matchingLabels) {
    if (label.toString().equals("NA") || label.toString().equals("NONE")) return false;

    final List<String> list = splitter.splitToList(label.name());
    final String lemma = token.get(Token.Lemma).toUpperCase();
    for (final String keyword : list) {
      if (lemma.equals(keyword)) {
        if (matchingLabels != null) {
          matchingLabels.add(keyword);
        }
        return true;
      }
      if (keyword.endsWith("S") && lemma.equals(keyword.substring(0, keyword.length()-2))) {
        if (matchingLabels != null) {
          matchingLabels.add(keyword);
        }
        return true;
      }
      if (matchingLabels != null && lemma.length() > 2 && keyword.length() > 2) {
        if (lemma.contains(keyword) || keyword.contains(lemma)) {
          matchingLabels.add("partial-" + keyword);
        }
      }
    }
    return false;
  }

  private static Collection<Feature<String>> tokensInSpanFeatures(final List<Token> tokens) {
    final List<Feature<String>> list = Lists.newArrayList();
    for (final Token token : tokens) {
      String featureLabelPrefix = "token-";
      if (token.equals(tokens.get(0))) {
        featureLabelPrefix += "B";
      } else {
        featureLabelPrefix += "I";
      }
      final List<Feature<String>> tokenList = Arrays.asList(
          UmlsFeatureExtractor.umlsIobFeature(featureLabelPrefix, token),
          Feature.stringFeature(featureLabelPrefix, token.asString().toLowerCase()),
          Feature.stringFeature(featureLabelPrefix + "-lemma", token.get(Token.Lemma).toLowerCase()),
          Feature.stringFeature(featureLabelPrefix + "-pos", token.get(Token.PoS).toLowerCase())
      );
      list.addAll(tokenList);
    }
    return list;
  }
}
