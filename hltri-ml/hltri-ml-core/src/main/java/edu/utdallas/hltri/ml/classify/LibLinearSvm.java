package edu.utdallas.hltri.ml.classify;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import com.google.common.collect.Ordering;
import de.bwaldvogel.liblinear.*;
import edu.utdallas.hltri.logging.Logger;
import edu.utdallas.hltri.ml.label.Label;
import edu.utdallas.hltri.ml.vector.FeatureVector;
import edu.utdallas.hltri.struct.Pair;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Wrapper for LibLinear's SVM
 * Created by ramon on 2/25/16.
 */
public class LibLinearSvm {
  private static final Logger log = Logger.get(LibLinearSvm.class);
  public static final Parameter DEFAULT_MULTICLASS_PARAMS = new Parameter(SolverType.MCSVM_CS, 1, 0.1);
  public static final Parameter DEFAULT_BINARY_PARAMS = new Parameter(SolverType.L2R_L2LOSS_SVC, 1, 0.1);


  private final Model model;

  private LibLinearSvm(final Model model) {
    this.model = model;
  }

  public static LibLinearSvm load(final String modelPath) {
    try {
      log.info("Initializing svm model from {}...", modelPath);
      final Model model = Model.load(new File(modelPath));
      return new LibLinearSvm(model);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public static <L extends Label, FV extends FeatureVector<Number>> LibLinearSvm train(final Parameter params,
                                                                                       final List<Pair<L, FV>> featureVectors) {
    return train(params, featureVectors, false);
  }

  public static <L extends Label, FV extends FeatureVector<Number>> LibLinearSvm train(final Parameter params,
                                                                                       final List<Pair<L, FV>> featureVectors,
                                                                                       boolean weightByClass) {
    assert featureVectors.size() > 0 : "Empty list of feature vectors.";
    final Problem problem = new Problem();
    problem.l = featureVectors.size();
    final Feature[][] features = new FeatureNode[problem.l][];
    double[] labels = new double[problem.l];
    int exampleIdx = 0;
    final Multiset<Integer> classDistribution = HashMultiset.create();
    for (final Pair<L, FV> fv : featureVectors) {
      final double label = fv.first().asInt();
      classDistribution.add((int)label);
      final Feature[] lsvmVector = convertFeatureVector(fv.second());
      assert Ordering.<Feature>from((f1, f2) -> Integer.compare(f1.getIndex(), f2.getIndex())).isOrdered(Arrays.asList(lsvmVector))
          : Arrays.toString(lsvmVector);
      features[exampleIdx] = lsvmVector;
      labels[exampleIdx] = label;
      exampleIdx++;
    }
    problem.x = features;
    problem.n = featureVectors.get(0).second().size();
    problem.y = labels;

    log.info("Training Liblinear model on {} examples with {} features...", problem.l, problem.n);
    if (weightByClass) {
      final int numClasses = classDistribution.elementSet().size();
      double[] weights = new double[numClasses];
      int[] weightLabels = new int[numClasses];
      final Iterator<Integer> it = classDistribution.elementSet().iterator();
      for (int i = 0; it.hasNext(); i++) {
        final Integer label = it.next();
        weightLabels[i] = label;
        weights[i] = ((double)classDistribution.size()) / (double)classDistribution.count(label);
      }
      log.info("Classes: {}; Weights: {}", Arrays.toString(weightLabels), Arrays.toString(weights));
      params.setWeights(weights, weightLabels);
    }
    final Model model = Linear.train(problem, params);
    return new LibLinearSvm(model);
  }

  private static Feature[] convertFeatureVector(final FeatureVector<Number> fv) {
    int[] ids = fv.ids();
    assert (ids.length > 0) : "Feature vector of size 0.";
    final FeatureNode[] vector = new FeatureNode[ids.length];
//    log.info(Arrays.toString(ids));
    Arrays.sort(ids);
//    log.info("Sorted: {}", Arrays.toString(ids));
    for (int i = 0; i < ids.length; i++) {
      vector[i] = new FeatureNode(ids[i] + 1, fv.getById(ids[i]).doubleValue());
    }
    return vector;
  }

  public LibLinearSvm save(final Path modelFile) {
    try {
      log.info("Saving mode to {}", modelFile);
      model.save(modelFile.toFile());
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    return this;
  }

  public LibLinearSvm save(final String modelFile) {
    return save(Paths.get(modelFile));
  }

  public double classify(final FeatureVector<Number> vector) {
    return Linear.predict(model, convertFeatureVector(vector));
  }

  /**
   * @param vector feature vector to be classified
   * @return the vector of label weights associated with each classification label for this feature vector
   */
  public double[] labelWeights(final FeatureVector<Number> vector, int numLabels) {
    double[] weights = new double[numLabels];
    Linear.predictValues(model, convertFeatureVector(vector), weights);
    return weights;
  }

  /**
   * If the underlying model is probabilistic, it will return Linear.predictProbability() which gives the vector of
   * class probabilities.
   * If not, it will scale the class weights with MaxAbsScaling, then convert the scaled weights into a probability
   * distribution.
   * @param vector feature vector to be classified
   * @param numLabels number of labels
   * @return the vector of label probabilities associated with each classification label for this feature vector
   */
  public double[] labelProbabilities(final FeatureVector<Number> vector, int numLabels) {
    if (model.isProbabilityModel()) {
      double[] weights = new double[numLabels];
      Linear.predictProbability(model, convertFeatureVector(vector), weights);
      return weights;
    }
    else {
      double[] weights = new double[numLabels];
      Linear.predictValues(model, convertFeatureVector(vector), weights);
      double min = Double.MAX_VALUE, max = Double.MIN_VALUE;
      for (double weight : weights) {
        if (weight < min) {
          min = weight;
        }
        if (weight > max) {
          max = weight;
        }
      }
      // MaxAbsScaling from sklearn.preprocessing
      for (int i = 0; i < weights.length; i++) {
        weights[i] = (weights[i] - min) / (max - min);
      }
      // convert to probability
      final double probabilityMass = Arrays.stream(weights).sum();
      for (int i = 0; i < weights.length; i++) {
        weights[i] = weights[i] / probabilityMass;
      }
      return weights;
    }
  }
}
