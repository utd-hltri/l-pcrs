package edu.utdallas.hltri.ml.classify;

import edu.utdallas.hltri.framework.ProgressLogger;
import edu.utdallas.hltri.ml.Feature;
import edu.utdallas.hltri.ml.FeatureExtractor;
import edu.utdallas.hltri.ml.label.Label;
import edu.utdallas.hltri.ml.vector.CountingSparseFeatureVectorizer;
import edu.utdallas.hltri.ml.vector.SparseFeatureVector;
import edu.utdallas.hltri.ml.vector.SparseFeatureVectorizer;
import edu.utdallas.hltri.ml.vector.VectorUtils;
import edu.utdallas.hltri.struct.Pair;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Created by rmm120030 on 4/23/17.
 */
public class TfBoundaryDetection {

  static <T> void vectorize(final String outDir, String filename, Supplier<Collection<List<T>>> sequenceSupplier,
                            SparseFeatureVectorizer<Number> vectorizer, Collection<FeatureExtractor<T,Number>> fes,
                            Function<T, Label> labeler) {
    vectorize(outDir, filename, sequenceSupplier, vectorizer, fes, labeler, null);
  }

  static <T> void vectorize(final String outDir, final String filename, Supplier<Collection<List<T>>> sequenceSupplier,
                            SparseFeatureVectorizer<Number> vectorizer, Collection<FeatureExtractor<T,Number>> fes,
                            Function<T, Label> labeler, Function<T, String> tokenIdExtractor) {
    final ProgressLogger plog = ProgressLogger.indeterminateSize("vectorizing", 5, TimeUnit.SECONDS);
    final BiFunction<T, SparseFeatureVector<Number>, String> vector2String = tokenIdExtractor == null ?
        (token, vector) -> VectorUtils.toZeroIndexedSvml(vector) :
        (token, vector) -> VectorUtils.toZeroIndexedSvmlWithId(vector, tokenIdExtractor.apply(token));

    try (BufferedWriter writer = new BufferedWriter(new FileWriter(new File(outDir, filename + ".svml")))) {
      try {
        for (final List<T> sentence : sequenceSupplier.get()) {
          int seqlength = 0;
          for (final T token : sentence) {
            final SparseFeatureVector<Number> vector = vectorizer.vectorize(fes.stream().flatMap(fe -> fe.apply(token)));
            final Label label = labeler.apply(token);

            writer.write(label + " " + vector2String.apply(token, vector));
            writer.newLine();
            if (++seqlength > 19) {
              writer.newLine();
              seqlength = 0;
            }
          }
          writer.newLine();
        }
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
//      iid.toFile(Paths.get(outDir).resolve(filename + ".tsv"));
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  static <T> void vectorizeRemoveUncommon(final String outDir, final String filename,
                            Supplier<Collection<List<T>>> sequenceSupplier, Collection<FeatureExtractor<T,Number>> fes,
                            Function<T, Label> labeler, int featureThreshold) {
    final CountingSparseFeatureVectorizer<Number> vectorizer = new CountingSparseFeatureVectorizer<>();
    final List<List<Pair<Label, SparseFeatureVector<Number>>>> list = new ArrayList<>();
      for (final List<T> sentence : sequenceSupplier.get()) {
      List<Pair<Label, SparseFeatureVector<Number>>> sentList = new ArrayList<>();
      int seqlength = 0;
      for (final T token : sentence) {
        final SparseFeatureVector<Number> vector = vectorizer.vectorize(fes.stream().flatMap(fe -> fe.apply(token)));
        final Label label = labeler.apply(token);
        sentList.add(Pair.of(label, vector));
        if (++seqlength > 19) {
          list.add(sentList);
          sentList = new ArrayList<>();
          seqlength = 0;
        }
      }
      if (sentList.size() > 0) {
        list.add(sentList);
      }
    }
    vectorizer.lockAndRemoveUncommonFeatures(featureThreshold);
    try (BufferedWriter writer = new BufferedWriter(new FileWriter(new File(outDir, filename + ".svml")))) {
      for (List<Pair<Label, SparseFeatureVector<Number>>> sentence : list) {
        for (Pair<Label, SparseFeatureVector<Number>> pair : sentence) {
          writer.write(pair.first() + " " + VectorUtils.toZeroIndexedSvml(pair.second()));
          writer.newLine();
        }
        writer.newLine();
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

    vectorizer.getFeatureIdentifier().toFile(Paths.get(outDir).resolve(filename + ".tsv"));
  }
}
