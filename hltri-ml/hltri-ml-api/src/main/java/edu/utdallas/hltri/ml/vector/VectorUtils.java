package edu.utdallas.hltri.ml.vector;

import com.google.common.base.Strings;
import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Created by travis on 11/3/16.
 */
public class VectorUtils {
  public static String toSvmRankVector(String label, String qid, FeatureVector<Number> vector) {
    return toSvmRankVector(label, qid, vector, null);
  }

  public static String toSvmRankVector(String label, String qid, FeatureVector<Number> vector, String comment) {
    final StringBuilder sb = new StringBuilder()
        .append(label)
        .append(" qid:")
        .append(qid);
    vector.forEachWithId((value, id) -> {
      sb.append(' ').append(id + 1).append(':').append(value);
    });
    if (!Strings.isNullOrEmpty(comment)) {
      sb.append(" # ").append(comment);
    }
    return sb.toString();
  }

  public static String toSvmRankVector(int label, int qid, FeatureVector<Number> vector, String comment) {
    return toSvmRankVector(Integer.toString(label), Integer.toString(qid), vector, comment);
  }

  public static String toSvml(SparseFeatureVector<?> fv) {
    final int[] ids = fv.ids();
    Arrays.sort(ids);
    return Arrays.stream(ids).mapToObj(i -> i+1 + ":" + Objects.toString(fv.getById(i))).collect(Collectors.joining(" "));
  }

  public static String toSvmlWithId(SparseFeatureVector<?> fv, String id) {
    return toSvml(fv) + " #" + id;
  }

  public static String toZeroIndexedSvml(SparseFeatureVector<?> fv) {
    final int[] ids = fv.ids();
    Arrays.sort(ids);
    return Arrays.stream(ids).mapToObj(i -> i + ":" + Objects.toString(fv.getById(i))).collect(Collectors.joining(" "));
  }

  public static String toZeroIndexedSvmlWithId(SparseFeatureVector<?> fv, String id) {
    return toZeroIndexedSvml(fv) + " #" + id;
  }
}
