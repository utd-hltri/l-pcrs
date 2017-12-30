package edu.utdallas.hltri.eeg.feature;

import com.google.common.collect.Sets;
import edu.utdallas.hltri.eeg.annotation.EegActivity;
import edu.utdallas.hltri.ml.FeatureExtractor;
import edu.utdallas.hltri.ml.label.EnumLabel;
import edu.utdallas.hltri.ml.label.IoLabel;
import edu.utdallas.hltri.ml.label.IobLabel;
import edu.utdallas.hltri.scribe.text.Attribute;
import edu.utdallas.hltri.scribe.text.annotation.Annotation;
import edu.utdallas.hltri.scribe.text.annotation.AnnotationType;
import edu.utdallas.hltri.scribe.text.annotation.Token;

import java.util.*;
import java.util.function.Function;

/**
 * Feature utilities
 * Created by ramon on 2/14/16.
 */
public class FeatureUtils {

  public static Collection<FeatureExtractor<Token, ?>> boundaryDetectionFeatureExtractors() {
    final String sentAnnset = "opennlp";
    return Arrays.asList(
        new GeniaFeatureExtractor(sentAnnset),
        new BrownClusterFeatureExtractor(),
        new UmlsFeatureExtractor(),
        new SectionFeatureExtractor<Token>()
    );
  }

  public static <A extends Annotation<A>> Function<Token, IobLabel> boundaryLabeler(final String annset,
                                                                                    final AnnotationType<A> type) {
    return token -> {
      final List<A> goldCover = token.getCovering(annset, type);
      if (goldCover.isEmpty()) {// || goldCover.get(0).get(Event.type).toLowerCase().contains("technique)) {
        return IobLabel.O;
      } else if (Objects.equals(goldCover.get(0).get(Annotation.StartOffset), token.get(Annotation.StartOffset))) {
        return IobLabel.B;
      } else {
        return IobLabel.I;
      }
    };
  }

  public static <A extends Annotation<A>> Function<Token, IoLabel> ioBoundaryLabeler(final String annset,
                                                                                    final AnnotationType<A> type) {
    return token -> {
      final List<A> goldCover = token.getCovering(annset, type);
      if (goldCover.isEmpty()) {// || goldCover.get(0).get(Event.type).toLowerCase().contains("technique)) {
        return IoLabel.O;
      } else {
        return IoLabel.I;
      }
    };
  }

  @SafeVarargs
  public static <A extends Annotation<A>> Collection<FeatureExtractor<A,?>> attributeFeatureExtractors(
      String actAnnset, Attribute<A, String>... otherAttributes) {
    if (otherAttributes.length > 0) {
      return Arrays.asList(
          new ConceptSpanFeatureExtractor<>(),
          new ContextFeatureExtractor<>(actAnnset),
          new SectionFeatureExtractor<>(),
          new OtherAttributeFeatureExtractor<>(otherAttributes)
      );
    } else {
      return Arrays.asList(
          new ConceptSpanFeatureExtractor<>(),
          new ContextFeatureExtractor<>(actAnnset),
          new SectionFeatureExtractor<>()
      );
    }
  }

  public static Collection<FeatureExtractor<EegActivity,?>> attributeFeatureExtractors(String actAnnset,
                                                                                       String attrName) {
    final Set<String> independentAttributes = Sets.newHashSet("morphology", "activity_modality",
        "activity_polarity");
    if (independentAttributes.contains(attrName)) {
      return attributeFeatureExtractors(actAnnset);
    } else if ("band".equals(attrName)) {
      return attributeFeatureExtractors(actAnnset, EegActivity.morphology);
    } else if ("hemisphere".equals(attrName)) {
      return attributeFeatureExtractors(actAnnset, EegActivity.morphology, EegActivity.band);
    } else if ("dispersal".equals(attrName)) {
      return attributeFeatureExtractors(actAnnset, EegActivity.morphology, EegActivity.band, EegActivity.hemisphere);
    } else if ("recurrence".equals(attrName)) {
      return attributeFeatureExtractors(actAnnset, EegActivity.morphology, EegActivity.band);
    } else if ("magnitude".equals(attrName)) {
      return attributeFeatureExtractors(actAnnset, EegActivity.morphology, EegActivity.band);
    } else if ("in_background".equals(attrName)) {
      return attributeFeatureExtractors(actAnnset, EegActivity.morphology, EegActivity.band, EegActivity.hemisphere,
          EegActivity.dispersal, EegActivity.recurrence, EegActivity.magnitude);
    } else if (Arrays.stream(EegActivity.Location.values()).map(EnumLabel::asString).anyMatch(attrName::equals)) {
      return attributeFeatureExtractors(actAnnset, EegActivity.morphology, EegActivity.hemisphere, EegActivity.dispersal);
    } else {
      throw new RuntimeException("Unrecognized attribute type: " + attrName);
    }
  }
}
