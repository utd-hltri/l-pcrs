package edu.utdallas.hltri.scribe;

import com.google.common.base.Throwables;

import org.reflections.Reflections;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by trg19 on 8/27/2016.
 */
public interface FeatureBearer extends Serializable {
  default Map<String, ?> getFeatureMap() {
    final Map<String, Object> featureMap = new HashMap<>();
    final Reflections self = new Reflections(this.getClass());
    for (final Field featureField : self.getFieldsAnnotatedWith(Feature.class)) {
      featureField.setAccessible(true);
      featureField.setAccessible(true);
      final Feature feature = featureField.getAnnotation(Feature.class);
      try {
        featureMap.put(feature.value(), featureField.get(this));
      } catch (IllegalAccessException ex) {
        throw Throwables.propagate(ex);
      }
    }

    return featureMap;
  }
}
