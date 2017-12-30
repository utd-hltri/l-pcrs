package edu.utdallas.hltri.ml.io;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.*;
import com.google.common.collect.*;
import edu.utdallas.hltri.ml.Feature;
import edu.utdallas.hltri.ml.Instance;
import edu.utdallas.hltri.ml.NumericFeature;
import edu.utdallas.hltri.ml.StringFeature;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.*;
import java.util.*;

/**
 * Writes and reads training/testing data to and from JSON
 * Created with IntelliJ IDEA.
 * User: bryan
 * Date: 12/15/12
 * Time: 11:35 PM
 */
public class FeatureJSONIO {
//  public static void writeToJSONVerbose(Iterable<? extends Instance<?>> instances, Path outFile) {
//    ObjectMapper mapper = new ObjectMapper();
//    ArrayNode instanceNodes = mapper.createArrayNode();
//    for (Instance<?> instance : instances) {
//      ObjectNode instanceNode = mapper.createObjectNode();
//      instanceNodes.add(instanceNode);
//      instanceNode.put("id", instance.id());
//      instanceNode.put("source", instance.source());
//      ArrayNode featureNodes = mapper.createArrayNode();
//      instanceNode.put("features", featureNodes);
//      Multimap<String,Feature> featuresWithSameName = HashMultimap.create();
//      for (Feature<?> feature : instance) {
//        featuresWithSameName.put(feature.name(), feature);
//      }
//      for (String featName : featuresWithSameName.keySet()) {
//        ObjectNode featureNode = mapper.createObjectNode();
//        Collection<Feature> values = featuresWithSameName.get(featName);
//        if (values.size() == 1) {
//          featureNode.put("name", values.iterator().next().name());
//          if (values.iterator().next() instanceof NumericFeature) {
//            featureNode.put("value", ((Number) values.iterator().next().value()).doubleValue());
//          } else {
//            featureNode.put("value", values.iterator().next().value().toString());
//          }
//        } else {
//          ArrayNode valuesNode = mapper.createArrayNode();
//          for (Feature<?> feature : featuresWithSameName.get(featName)) {
////            if (feature instanceof StringFeature) {
////              featureNode.put("type", "string");
////            } else if (feature instanceof NumericFeature) {
////              featureNode.put("type", "numeric");
////            } else {
////              featureNode.put("type", feature.getClass().getName());
////            }
//            if (feature instanceof NumericFeature) {
//              valuesNode.add(((Number) feature.value()).doubleValue());
//            } else {
//              valuesNode.add(feature.value().toString());
//            }
//          }
//          featureNode.put("value", valuesNode);
//        }
//        featureNodes.add(featureNode);
//      }
//    }
//    try (BufferedWriter writer = Files.newBufferedWriter(outFile, Charset.defaultCharset())) {
//      //mapper.writeValue(writer, instanceNodes);
//      JsonGenerator generator = mapper.getFactory().createGenerator(writer);
//      generator.setPrettyPrinter(new DefaultPrettyPrinter());
//      mapper.writeTree(generator, instanceNodes);
//      //mapper.writerWithDefaultPrettyPrinter().writeTee(writer, instanceNodes);
//    } catch (IOException ioe) {
//      throw new RuntimeException(ioe);
//    }
//  }

  public static void writeToJSON(Iterable<? extends Instance<?>> instances, Path outFile) {
    ObjectMapper mapper = new ObjectMapper();
    ArrayNode instanceNodes = mapper.createArrayNode();
    for (Instance<?> instance : instances) {
      ObjectNode instanceNode = mapper.createObjectNode();
      instanceNodes.add(instanceNode);
      instanceNode.put("id", instance.id());
      instanceNode.put("source", instance.source());
      if (instance.target() instanceof Number) {
        instanceNode.put("target", ((Number) instance.target()).doubleValue());
      } else {
        instanceNode.put("target", (String) instance.target());
      }
      ObjectNode featureMap = mapper.createObjectNode();
      instanceNode.put("features", featureMap);
      Multimap<String,Feature<?>> featuresWithSameName = HashMultimap.create();
      for (Feature<?> feature : instance) {
        featuresWithSameName.put(feature.name(), feature);
      }
      for (String featName : Ordering.natural().sortedCopy(featuresWithSameName.keySet())) {
        Collection<Feature<?>> features = featuresWithSameName.get(featName);
        if (features.size() == 1) {
          if (features.iterator().next() instanceof NumericFeature) {
            featureMap.put(featName, ((Number) features.iterator().next().value()).doubleValue());
          } else {
            featureMap.put(featName, features.iterator().next().value().toString());
          }
        } else {
          ArrayNode valueNodes = mapper.createArrayNode();
          for (Feature<?> feature : featuresWithSameName.get(featName)) {
            if (feature instanceof NumericFeature) {
              valueNodes.add(((Number) feature.value()).doubleValue());
            } else {
              valueNodes.add(feature.value().toString());
            }
          }
          featureMap.put(featName,valueNodes);
        }
      }
    }

    // Write to file
    try (BufferedWriter writer = Files.newBufferedWriter(outFile, Charset.defaultCharset())) {
      //mapper.writeValue(writer, instanceNodes);
      JsonGenerator generator = mapper.getFactory().createGenerator(writer);
      generator.setPrettyPrinter(new DefaultPrettyPrinter());
      mapper.writeTree(generator, instanceNodes);
      //mapper.writerWithDefaultPrettyPrinter().writeTee(writer, instanceNodes);
    } catch (IOException ioe) {
      throw new RuntimeException(ioe);
    }
  }






  public static <C> List<Instance<C>> readFromJSON(Path file) {
    ObjectMapper mapper = new ObjectMapper();
    try (BufferedReader reader = Files.newBufferedReader(file, Charset.defaultCharset())) {
      JsonNode instanceNodes = mapper.readTree(reader);
      List<Instance<C>> instances = new ArrayList<>();
      for (int i = 0; i < instanceNodes.size(); i++) {
        JsonNode instNode = instanceNodes.get(i);
        Instance<C> instance;
        if (instNode.get("target").isDouble()) {
          Instance<Double> doubleInst = new Instance<Double>();
          instance = (Instance<C>) doubleInst;
          doubleInst.target(instNode.get("target").asDouble());
        } else {
          Instance<String> stringInst = new Instance<String>();
          stringInst.target(instNode.get("target").asText());
          instance = (Instance<C>) stringInst;
        }
        instances.add(instance);
        instance.id(instNode.get("id").asText());
        instance.source(instNode.get("source").asText());
        JsonNode featNode = instNode.get("features");
        Iterator<String> featNames = featNode.fieldNames();
        while (featNames.hasNext()) {
          String featName = featNames.next();
          JsonNode valueNode = featNode.get(featName);
          if ( ! valueNode.isArray()) {
            instance.addFeature(makeFeature(featName, valueNode));
          } else {
            Iterator<JsonNode> elems = valueNode.elements();
            while (elems.hasNext()) {
              instance.addFeature(makeFeature(featName, elems.next()));
            }
          }
        }
      }
      return instances;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private static Feature<?> makeFeature(String featName, JsonNode valueNode) {
    if (valueNode.isDouble()) {
      return Feature.numericFeature(featName, valueNode.doubleValue());
    } else {
      return Feature.stringFeature(featName, valueNode.textValue());
    }
  }
}
