package edu.utdallas.hltri.eeg.feature;

import com.google.common.collect.Lists;
import edu.utdallas.hltri.ml.Feature;
import edu.utdallas.hltri.ml.FeatureExtractor;
import edu.utdallas.hltri.scribe.text.annotation.Annotation;
import edu.utdallas.hltri.scribe.text.annotation.Section;

import java.util.List;
import java.util.stream.Stream;

/**
 * Created by rmm120030 on 11/3/16.
 */
public class SectionFeatureExtractor<A extends Annotation<A>> implements FeatureExtractor<A, String> {
  @Override
  public Stream<? extends Feature<String>> apply(A token) {
    return sectionFeatures(token).stream();
  }

  private List<Feature<String>> sectionFeatures(A ann) {
    final List<Feature<String>> list = Lists.newArrayList();
    final List<Section> coveringSections = ann.getCovering("regex-eeg", Section.TYPE);
    if (coveringSections.size() > 0) {
      final String sectionTitle = coveringSections.get(0).get(Section.title).toLowerCase();
      list.add(Feature.stringFeature("section", sectionTitle));
      if (sectionTitle.contains("description")) {
        list.add(Feature.stringFeature("section_match", "DECSRIPTION"));
      }
      if (sectionTitle.contains("history")) {
        list.add(Feature.stringFeature("section_match", "HISTORY"));
      }
      if (sectionTitle.contains("medication")) {
        list.add(Feature.stringFeature("section_match", "MEDICATIONS"));
      }
      if (sectionTitle.contains("intro")) {
        list.add(Feature.stringFeature("section_match", "INTRO"));
      }
      if (sectionTitle.contains("impression")) {
        list.add(Feature.stringFeature("section_match", "IMPRESSION"));
      }
      if (sectionTitle.contains("correlation")) {
        list.add(Feature.stringFeature("section_match", "CORRELATION"));
      }
    }
    return list;
  }

}
