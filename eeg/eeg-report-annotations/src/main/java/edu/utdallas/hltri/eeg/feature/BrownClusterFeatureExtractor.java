package edu.utdallas.hltri.eeg.feature;

import edu.utdallas.hltri.conf.Config;
import edu.utdallas.hltri.ml.Feature;
import edu.utdallas.hltri.ml.FeatureExtractor;
import edu.utdallas.hltri.scribe.text.annotation.Token;
import edu.utdallas.hltri.scribe.util.BrownClusters;
import edu.utdallas.hltri.util.Lazy;

import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * Created by rmm120030 on 11/3/16.
 */
public class BrownClusterFeatureExtractor implements FeatureExtractor<Token, String> {
  private final static Config conf = Config.load("eeg.features");
  private final static Supplier<BrownClusters> bcSupplier = Lazy.lazily(() ->
      new BrownClusters(conf.getPath("bc-kirk").resolve("gigaword.cna_eng-c1000").toString(),
          conf.getPath("bc-kirk").resolve("gigaword.xin_eng-c100").toString(),
          conf.getPath("bc-eeg").resolve("v0.6.0-c1000").toString(),
          conf.getPath("bc-eeg").resolve("v0.6.0-c100").toString()));

  @Override
  public Stream<? extends Feature<String>> apply(Token token) {
    final BrownClusters brownClusters = bcSupplier.get();
    final BrownClusters.ClusterMap clusterMap = brownClusters.getClusters(token.get(Token.Lemma).toLowerCase());
    if (clusterMap == null) {
      return Stream.empty();
    }
    return Stream.of(
        Feature.stringFeature("bc:i2b2-c100", clusterMap.get("i2b2-c100")),
        Feature.stringFeature("bc:xin-c100-p2", clusterMap.getPrefix("gigaword.xin_eng-c100", 2)),
        Feature.stringFeature("bc:cna-c1000-p6", clusterMap.getPrefix("gigaword.cna_eng-c1000", 6)),
        Feature.stringFeature("bc:v060-1000", clusterMap.get("v0.6.0-c1000")),
        Feature.stringFeature("bc:v060-100", clusterMap.get("v0.6.0-c100")),
        Feature.stringFeature("bc:v060-1000-p2", clusterMap.getPrefix("v0.6.0-c1000", 2)),
        Feature.stringFeature("bc:v060-100-p2", clusterMap.getPrefix("v0.6.0-c100", 2)),
        Feature.stringFeature("bc:v060-1000-p4", clusterMap.getPrefix("v0.6.0-c1000", 4)),
        Feature.stringFeature("bc:v060-100-p4", clusterMap.getPrefix("v0.6.0-c100", 4))
    ).filter(f -> !f.value().equals("null"));
  }
}
