package edu.utdallas.hltri.eeg.feature;

import edu.utdallas.hltri.ml.Feature;
import edu.utdallas.hltri.ml.FeatureExtractor;
import edu.utdallas.hltri.ml.StringFeature;
import edu.utdallas.hltri.scribe.annotators.GeniaAnnotator;
import edu.utdallas.hltri.scribe.text.annotation.Sentence;
import edu.utdallas.hltri.scribe.text.annotation.Token;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Created by rmm120030 on 11/3/16.
 */
public class GeniaFeatureExtractor implements FeatureExtractor<Token, String> {
  public static final Function<Token, String> lemmatizer = t -> t.getOptional(Token.Lemma).orElse(t.toString()).toLowerCase();
  private final String sentAnnset;

  public GeniaFeatureExtractor(String sentAnnset) {
    this.sentAnnset = sentAnnset;
  }

  @Override
  public Stream<? extends Feature<String>> apply(Token token) {
    final List<Token> context = token.getCovering(sentAnnset, Sentence.TYPE).get(0).getContained("genia", Token.TYPE);
      final int index = context.indexOf(token);
      final List<Feature<String>> features = new ArrayList<>();
      features.add(Feature.stringFeature("genia:token", token.asString()));
      features.add(Feature.stringFeature("genia:token.lemma", lemmatizer.apply(token)));
      features.add(Feature.stringFeature("genia:prev-token.lemma", (index > 0) ? lemmatizer.apply(context.get(index - 1)) : "NULL"));
      features.add(Feature.stringFeature("genia:next-token.lemma", (index < context.size() - 1) ?
          lemmatizer.apply(context.get(index + 1)) : "NULL"));
      features.add(Feature.stringFeature("genia:token.pos", token.get(Token.PoS)));
      features.add(Feature.stringFeature("genia:prev-token.pos", (index > 0) ? context.get(index - 1).get(Token.PoS) : "NULL"));
      features.add(Feature.stringFeature("genia:next-token.pos", (index < context.size() - 1) ?
          context.get(index + 1).get(Token.PoS) : "NULL"));
      features.add(Feature.stringFeature("genia:token.chunk", token.get(GeniaAnnotator.phraseIOB)));
      features.add(Feature.stringFeature("genia:prev-token.chunk", (index > 0) ?
          context.get(index - 1).get(GeniaAnnotator.phraseIOB) : "NULL"));
      features.add(Feature.stringFeature("genia:next-token.chunk", (index < context.size() - 1) ?
          context.get(index + 1).get(GeniaAnnotator.phraseIOB) : "NULL"));
      // context features
      for (int i = 1; i < 4; i++) {
        features.add(Feature.stringFeature("genia:" + i + "-lemma-context", context.subList(Math.max(0, index - i),
            Math.min(context.size(), index + i + 1))
            .stream()
            .map(lemmatizer).collect(Collectors.joining("_"))));
      }
      final Set<StringFeature> contextFeatures = ngramFeatures(context, token, token, 3, "genia");
      contextFeatures.addAll(ngramFeatures(context, token, token, 2, "genia"));
      contextFeatures.stream();

      return features.stream().filter(f -> !f.value().equals("NULL"));
  }

  public static Set<StringFeature> ngramFeatures(final List<Token> context, final Token startToken, final Token endToken,
                                                 final int n, final String prefix) {
    final Set<StringFeature> set = new HashSet<>();
    for (int i = context.indexOf(startToken) - n + 1; i <= context.indexOf(endToken); i++) {
      final int end = i + n;
      if (i >= 0 && end <= context.size()) {
        set.add(Feature.stringFeature(prefix + ":" + n + "-gram:",
            context.subList(i, end).stream().map(lemmatizer).collect(Collectors.joining("_"))));
      }
    }
    return set;
  }
}
