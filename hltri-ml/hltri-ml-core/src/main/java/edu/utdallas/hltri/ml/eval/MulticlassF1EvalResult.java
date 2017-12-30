package edu.utdallas.hltri.ml.eval;

import com.google.common.collect.Maps;

import java.util.Map;
import java.util.Set;

/**
 * Created by rmm120030 on 9/9/16.
 */
public class MulticlassF1EvalResult<K,T> {
  private final Map<K, F1EvaluationResult<T>> map;

  public MulticlassF1EvalResult() {
    map = Maps.newHashMap();
  }

  public void truePositive(K key, T instance) {
    final F1EvaluationResult<T> f1er = map.containsKey(key) ? map.get(key) : new F1EvaluationResult<>();
    f1er.truePositive(instance);
    map.put(key, f1er);
  }
  public void trueNegative(K key, T instance) {
    final F1EvaluationResult<T> f1er = map.containsKey(key) ? map.get(key) : new F1EvaluationResult<>();
    f1er.trueNegative(instance);
    map.put(key, f1er);
  }
  public void falsePositive(K key, T instance) {
    final F1EvaluationResult<T> f1er = map.containsKey(key) ? map.get(key) : new F1EvaluationResult<>();
    f1er.falsePositive(instance);
    map.put(key, f1er);
  }
  public void falseNegative(K key, T instance) {
    final F1EvaluationResult<T> f1er = map.containsKey(key) ? map.get(key) : new F1EvaluationResult<>();
    f1er.falseNegative(instance);
    map.put(key, f1er);
  }
  public MulticlassF1EvalResult<K,T> incorporate(MulticlassF1EvalResult<K,T> other) {
    other.classes().forEach(key -> {
      final F1EvaluationResult<T> f1er = map.containsKey(key) ? map.get(key) : new F1EvaluationResult<>();
      f1er.incorporate(other.get(key));
      map.put(key, f1er);
    });
    return this;
  }

  public F1EvaluationResult<T> get(K key) {
    return map.get(key);
  }

  public Set<K> classes() {
    return map.keySet();
  }

  public double macroF1() {
    return map.keySet().stream().map(map::get).mapToDouble(F1EvaluationResult::getF1).average().getAsDouble();
  }

  public double weightedMacroF1() {
    double numerator = 0.0, denominator = 0.0;
    for (K key : map.keySet()) {
      final F1EvaluationResult<T> f1er = map.get(key);
      numerator += f1er.getF1() * f1er.countSupport();
      denominator += f1er.countSupport();
    }
    return numerator / denominator;
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder();
    for (K key : map.keySet()) {
      sb.append("\n       ").append(key.toString()).append(map.get(key).toString());
    }
    sb.append("\n Macro F1: ").append(macroF1());
    sb.append("\n Macro F1 weighted by support: ").append(weightedMacroF1());
    return sb.toString();
  }

  public String toCsvString() {
    final StringBuilder sb = new StringBuilder();
    sb.append("value,tp,fp,tn,fn,prec,rec,f1\n");
    for (K k : map.keySet()) {
      final F1EvaluationResult<T> r = map.get(k);
      sb.append(k.toString()).append(",").append(r.toCsvString()).append("\n");
    }
    return sb.toString();
  }
}
