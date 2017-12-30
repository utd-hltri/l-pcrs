package edu.utdallas.hltri.ml.eval;

import com.google.common.collect.Lists;
import edu.utdallas.hltri.logging.Logger;
import edu.utdallas.hltri.struct.Pair;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Created by rmm120030 on 8/3/16.
 */
public class CrossValidationSplit<D> {
  private static final Logger log = Logger.get(CrossValidationSplit.class);

  private final List<D> docs;
  private final int numSplits;
  private final List<List<Integer>> splits;

  private CrossValidationSplit(List<D> docs, int numSplits) {
    this.docs = docs;
    this.numSplits = numSplits;
    final int splitSize = docs.size() / numSplits;
    splits = Lists.newArrayList();
    int[] sizes = new int[numSplits];
    for (int i = 0; i < numSplits; i++) {
      sizes[i] = splitSize;
      splits.add(IntStream.range(i*splitSize, (i+1)*splitSize).boxed().collect(Collectors.toList()));
    }
    if (docs.size() % numSplits != 0) {
      splits.get(numSplits - 1).addAll(IntStream.range(numSplits * splitSize, docs.size()).boxed().collect(Collectors.toList()));
    }
    sizes[numSplits - 1] = splits.get(numSplits - 1).size();
    log.info("Split docs into {} splits with sizes: {}", numSplits, sizes);
  }

  public static <D> CrossValidationSplit<D> of(List<D> docs, int numSplits) {
    return new CrossValidationSplit<>(docs, numSplits);
  }

  public Pair<List<D>,List<D>> getSplits(int i) {
    return Pair.of(getTrain(i), getTest(i));
  }

  public List<D> getTrain(int splitNum) {
    final List<D> train = Lists.newArrayList();
    for (int i = 0; i < numSplits; i++) {
      if (i != splitNum) {
        train.addAll(splits.get(i).stream().map(docs::get).collect(Collectors.toList()));
      }
    }
    return train;
  }

  public List<D> getTest(int splitNum) {
    return splits.get(splitNum).stream().map(docs::get).collect(Collectors.toList());
  }
}
