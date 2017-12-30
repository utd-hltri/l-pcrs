package edu.utdallas.hltri.eeg.feature;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import edu.utdallas.hlt.medbase.umls.UMLSLexicon;
import edu.utdallas.hltri.ml.Feature;
import edu.utdallas.hltri.ml.FeatureExtractor;
import edu.utdallas.hltri.scribe.text.annotation.Annotation;
import edu.utdallas.hltri.scribe.text.annotation.Chunk;
import edu.utdallas.hltri.scribe.text.annotation.Token;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Created by rmm120030 on 11/3/16.
 */
public class UmlsFeatureExtractor implements FeatureExtractor<Token, String> {
  @Override
  public Stream<? extends Feature<String>> apply(Token token) {
    final List<Chunk> umlsCover = token.getCovering("umls", Chunk.TYPE);
    final Feature<String> iob = umlsIobFeature("", token);
    //TODO: normalize CUIs
    final List<Feature<String>> features = Lists.newArrayList();
    features.add(iob);
    if (!umlsCover.isEmpty()) {
//            log.info("Token {} is a part of cuis:[{}]{}", token, umlsCover.size(), umlsCover);
      final Set<String> finalCuis = Sets.newHashSet();
      for (final Chunk cui : umlsCover) {
        finalCuis.addAll(cui.get(UMLSLexicon.cuis));
      }
      for (final String cui : finalCuis) {
        features.add(Feature.stringFeature("umls:cui", cui));
      }
    }
    return features.stream();
  }

  public static Feature<String> umlsIobFeature(final String prefix, final Token token) {
    final List<Chunk> umlsCover = token.getCovering("umls", Chunk.TYPE);
    return Feature.stringFeature(prefix + "umls:iob", (umlsCover.isEmpty()) ? "O" :
          (Objects.equals(umlsCover.get(0).get(Annotation.StartOffset), token.get(Annotation.StartOffset))) ? "B" : "I"
    );
  }
}
