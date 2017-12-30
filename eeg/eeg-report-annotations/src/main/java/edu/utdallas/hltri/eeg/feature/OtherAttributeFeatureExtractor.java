package edu.utdallas.hltri.eeg.feature;

import edu.utdallas.hltri.ml.Feature;
import edu.utdallas.hltri.ml.FeatureExtractor;
import edu.utdallas.hltri.scribe.text.Attribute;
import edu.utdallas.hltri.scribe.text.annotation.Annotation;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * Extracts a feature for each attribute passed if the annotation has a value for it
 * Created by rmm120030 on 8/15/17.
 */
public class OtherAttributeFeatureExtractor<A extends Annotation<A>> implements FeatureExtractor<A,Number> {
  private final Attribute<A, String>[] otherAttributes;

  @SafeVarargs
  OtherAttributeFeatureExtractor(@Nonnull Attribute<A, String>... otherAttributes) {
    this.otherAttributes = otherAttributes;
  }

  @Override
  public Stream<? extends Feature<Number>> apply(@Nonnull A ann) {
    return Arrays.stream(otherAttributes)
        .filter(attr -> Objects.nonNull(ann.get(attr)))
        .map(attr -> Feature.stringFeature(attr.name, ann.get(attr)))
        .map(Feature::toNumericFeature);
  }
}
