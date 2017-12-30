package edu.utdallas.hltri.math;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import edu.utdallas.hltri.struct.Pair;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: bryan
 * Date: 5/25/13
 * Time: 3:38 PM
 * To change this template use File | Settings | File Templates.
 */
public class Vectors {

  public static double dot(double[] first, double[] second) {
    double sum = 0.0;
    for (int i = 0; i < first.length; i++) {
      sum += first[i] * second[i];
    }
    return sum;
  }

  public static Collection<Pair<Integer, Double>> averageVectors(Iterable<Collection<Pair<Integer, Double>>> vectors) {
    Multimap<Integer,Double> values = HashMultimap.create();
    for (Collection<Pair<Integer, Double>> vector : vectors) {
      for (Pair<Integer,Double> value : vector) {
        values.put(value.first(), value.second());
      }
    }
    List<Pair<Integer,Double>> newVec = new ArrayList<>();
    for (Integer index : values.keySet()) {
      double sum = 0;
      int count = 0;
      for (Double val : values.get(index)) {
        sum += val;
        count++;
      }
      newVec.add(Pair.of(index, sum/count));
    }
    return newVec;
  }

  public static double[] elementWiseSum(double[] vectorA, double[] vectorB) {
    double[] sum = new double[vectorA.length];
    for (int i = 0; i < vectorA.length; i++) {
      sum[i] += vectorA[i] + vectorB[i];
    }
    return sum;
  }

  public static double[] elementWiseDifference(double[] vectorA, double[] vectorB) {
    double[] sum = new double[vectorA.length];
    for (int i = 0; i < vectorA.length; i++) {
      sum[i] += vectorA[i] - vectorB[i];
    }
    return sum;
  }

  public static double cosineSimilarity(double[] vectorA, double[] vectorB) {
    double dotProduct = 0.0;
    double normA = 0.0;
    double normB = 0.0;
    for (int i = 0; i < vectorA.length; i++) {
      dotProduct += vectorA[i] * vectorB[i];
      normA += vectorA[i] * vectorA[i];
      normB += vectorB[i] * vectorB[i];
    }
    double distance = dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
    assert !Double.isNaN(distance) : String.format("NaN Cosine Similarity for\nvectorA: %s\nvectorB: %s\ndotProduct: %s" +
        "\nnormA: %s\nnormB: %s", Arrays.toString(vectorA), Arrays.toString(vectorB), dotProduct, normA, normB);
    return Math.min(1.0, Math.max(0.0, distance));
  }

  public static double angularDistance(double[] vectorA, double[] vectorB) {
    final double sim = cosineSimilarity(vectorA, vectorB);
    final double distance = Math.acos(sim) / Math.PI;
    assert !Double.isNaN(distance) : "NaN Angular Distance. Cosine Similarity: " + sim;
    return distance;
  }

  public static double angularSimilarity(double[] vectorA, double[] vectorB) {
    return 1.0 - angularDistance(vectorA, vectorB);
  }

  public static double l1Norm(double[] vector) {
    double sum = 0.0;
    for (double aVector : vector) {
      sum += Math.abs(aVector);
    }

    return (Double.isNaN(sum)) ? 0.0 : sum;
  }

  public static double l2Distance(double[] first, double[] second) {
    double sum = 0.0;
    for (int i = 0; i < first.length; i++) {
      sum += (pow(first[i], 2) - pow(second[i], 2));
    }

    double score =  Math.sqrt(sum);
    return (Double.isNaN(score)) ? 0.0 : score;
  }

  public static double l2Norm(double[] vector) {
    double sum = 0.0;
    for (double aVector : vector) {
      sum += pow(aVector, 2);
    }

    double score =  Math.sqrt(sum);
    return (Double.isNaN(score)) ? 0.0 : score;
  }

  private static double pow(double x, int p) {
    double r = Math.pow(x, p);
    return (Double.isNaN(r)) ? 0.0 : r;
  }
}
