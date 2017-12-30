package edu.utdallas.hltri.eeg.feature;

import com.google.common.collect.Lists;
import edu.utdallas.hltri.eeg.annotation.EegActivity;
import edu.utdallas.hltri.ml.Feature;
import edu.utdallas.hltri.ml.FeatureExtractor;

import java.util.List;
import java.util.stream.Stream;

/**
 * Created by rmm120030 on 11/3/16.
 */
public class GoldFeatureExtractor implements FeatureExtractor<EegActivity,String> {
  @Override
  public Stream<? extends Feature<String>> apply(EegActivity eegActivity) {
    final List<Feature<String>> features = Lists.newArrayList();
//      features.add(Feature.stringFeature("morphology", activity.get(EegActivity.morphology)));
//      features.add(Feature.stringFeature("hemisphere", activity.get(EegActivity.hemisphere)));
//      features.add(Feature.stringFeature("magnitude", activity.get(EegActivity.magnitude)));
//      features.add(Feature.stringFeature("recurrence", activity.get(EegActivity.recurrence)));
    features.add(Feature.stringFeature("dispersal", eegActivity.get(EegActivity.dispersal)));
//      features.add(Feature.stringFeature("frequency", activity.get(EegActivity.band)));
//      features.add(Feature.stringFeature("background", activity.get(EegActivity.in_background)));
//      features.add(Feature.stringFeature("modality", activity.get(EegActivity.modality)));
//      features.add(Feature.stringFeature("polarity", activity.get(EegActivity.polarity)));
//      activity.getLocations().forEach(l -> features.add(Feature.stringFeature(l.name(), "yes")));
    return features.stream();
  }
}
