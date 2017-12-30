package edu.utdallas.hltri.ml;

import com.google.common.collect.*;
import java.util.*;

/**
 * Holds training and testing data with a predetermined split into training and testing
 * @author bryan
 */
public class TrainTestSplit<I> {

  private List<I> train;
  private List<I> test;

  public TrainTestSplit(Iterable<I> train, Iterable<I> test) {
    this.train = Lists.newArrayList(train);
    this.test = Lists.newArrayList(test);
  }

//  public TrainTestSplit(Iterable<I> train, Iterable<I> test) {
//    this.train = new ArrayList<>(train);
//    this.test = new ArrayList<>(test);
//  }

  /**
   * Randomly assign each instance to training or testing with probability <var>trainProportion</var>
   * @param allData
   * @param trainProportion
   */
  public TrainTestSplit(Iterable<? extends I> allData, double trainProportion) {
    train = new ArrayList();
    test = new ArrayList();
    Random random = new Random(1342);
    for (I t : allData) {
      if (random.nextDouble() < trainProportion) {
        train.add(t);
      } else {
        test.add(t);
      }
    }
  }

  /**
   * Empty at first, add items through addTrain and addTest
   */
  public TrainTestSplit() {
    train = new ArrayList<>();
    test = new ArrayList<>();
  }

  public void addTrain(I instance) {
    train.add(instance);
  }

  public void addTest(I instance) {
    test.add(instance);
  }

  /**
   * Automatically create a <var>numFolds</var>-way folding of the data for things like cross-validation.
   * @param allData
   * @param numFolds
   * @param <I>
   * @return
   */
  public static <I> List<TrainTestSplit<I>> createFolds(List<I> allData, int numFolds) {
    List<I> shuffledData = new ArrayList<I>(allData);
    Collections.shuffle(shuffledData);
    List<TrainTestSplit<I>> folds = Lists.newArrayList();
    double fraction = 1.0 / numFolds;
    for (int fold = 0; fold < numFolds; fold++) {
      int testStart = (int) (fraction * shuffledData.size() * fold);
      int testEnd = (int) (fraction * shuffledData.size() * (fold+1));
      Iterable<I> test = shuffledData.subList(testStart, testEnd);
      Iterable<I> training = Iterables.concat(shuffledData.subList(0, testStart),
          shuffledData.subList(testEnd, shuffledData.size()));
      folds.add(new TrainTestSplit<I>(Lists.newArrayList(training), Lists.newArrayList(test)));


    }
    return folds;
  }

  public List<I> getTest() {
    return test;
  }

  public List<I> getTrain() {
    return train;
  }

  public Iterable<I> getBoth() {
    return Iterables.concat(train, test);
  }

}