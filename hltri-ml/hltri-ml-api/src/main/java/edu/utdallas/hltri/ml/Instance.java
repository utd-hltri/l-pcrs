package edu.utdallas.hltri.ml;

import com.google.common.base.*;
import com.google.common.collect.*;

import java.util.*;

/**
 * Created with IntelliJ IDEA.
 * User: bryan
 * Date: 12/16/12
 * Time: 10:25 AM
 */
public class Instance<C> implements Iterable<Feature<?>> {
  private String id;
  private String source;
  private C target;
  private List<Feature<?>> features = new ArrayList<>();

  public String id() {
      return id;
  }

  public void id(String id) {
      this.id = id;
  }

  public String source() {
      return source;
  }

  public void source(String source) {
      this.source = source;
  }

  public List<Feature<?>> features() {
      return features;
  }

  public List<Feature<?>> getFeatures(String featureName) {
    List<Feature<?>> kept = new ArrayList<>();
    for (Feature<?> feature : features) {
      if (feature.name().equals(featureName)) {
        kept.add(feature);
      }
    }
    return kept;
  }

  public void features(List<? extends Feature<?>> features) {
      this.features = Lists.newArrayList(features);
  }

  public void addFeature(Feature<?> feature) {
      features.add(feature);
  }

  public C target() {
    return target;
  }

  public void target(C target) {
    this.target = target;
  }

  @Override
  public Iterator<Feature<?>> iterator() {
    return features.iterator();
  }

  public static class TargetFunction<C> implements Function<Instance<C>,C> {
    @Override
    public C apply(Instance<C> instance) {
      return instance.target();
    }
  }
//
//  public static class SingleFeatureExtractor<C,F> implements FeatureExtractor<Instance<C>, Object> {
//    private String name;
//
//    public SingleFeatureExtractor(String featName) {
//      this.name = featName;
//    }
//
//    @Override
//    public Collection<? extends Feature<Object>> extract(Instance<C> instance) {
//      return Lists.newArrayList(Iterables.filter(instance.features(), new Predicate<Feature<Object>>() {
//        @Override
//        public boolean apply(edu.utdallas.hltri.ml.Feature<Object> feat) {
//          return feat.name().equals(name);
//        }
//      }));
//    }
//  }
//
//  public static class AllFeaturesExtractor<C,F> implements FeatureExtractor<Instance<C>, Object> {
//    private Predicate<? super Feature> keepTest = null;
//
//    public AllFeaturesExtractor() {  }
//
//    public AllFeaturesExtractor(Predicate<? super Feature> keepTest) {
//      this.keepTest = keepTest;
//    }
//
//    @Override
//    public Collection<? extends Feature<Object>> extract(Instance<C> instance) {
//      if (keepTest == null) {
//        return instance.features();
//      } else {
//        return Collections2.filter(instance.features(), keepTest);
//      }
//    }
//  }
}
