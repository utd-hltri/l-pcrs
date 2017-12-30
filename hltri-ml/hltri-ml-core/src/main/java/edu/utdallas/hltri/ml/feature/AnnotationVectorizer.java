package edu.utdallas.hltri.ml.feature;

import com.google.common.base.Throwables;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import com.google.common.collect.Sets;
import edu.utdallas.hltri.framework.ProgressLogger;
import edu.utdallas.hltri.logging.Logger;
import edu.utdallas.hltri.ml.Feature;
import edu.utdallas.hltri.ml.FeatureExtractor;
import edu.utdallas.hltri.ml.NumericFeature;
import edu.utdallas.hltri.struct.Pair;
import edu.utdallas.hltri.util.ImmutibleIntIdenitifer;
import edu.utdallas.hltri.util.IntIdentifier;
import edu.utdallas.hltri.ml.label.EnumLabel;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Created by ramon on 1/29/16.
 */
@Deprecated
public class AnnotationVectorizer<A> {
  private static final Logger log = Logger.get(AnnotationVectorizer.class);

  private final Collection<FeatureExtractor<A, ?>> featureExtractors;
  private final Function<A, ? extends EnumLabel> labelExtractor;
  private IntIdentifier<String> featureIdentifier;
  protected String mappingExtension, featureExtension;
  protected Function<OldFeatureVector, String> stringFun;
  private int numBadAdds = 0;

  public AnnotationVectorizer(final Collection<FeatureExtractor<A, ?>> featureExtractors,
                              final Function<A, ? extends EnumLabel> labelExtractor,
                              final IntIdentifier<String> featureIdentifier) {
    this.featureIdentifier = featureIdentifier;
    this.featureExtractors = featureExtractors;
    this.mappingExtension = ".tsv";
    this.featureExtension = ".svml";
    this.stringFun = OldFeatureVector::toString;
    log.debug("Configured {} feature extractors", featureExtractors.size());
    this.labelExtractor = labelExtractor;
    log.info("Initialized feature vectorizer with {} pre-existing annotations.", featureIdentifier.size());
  }

//  public AnnotationVectorizer(final Collection<FeatureExtractor<A, Number>> featureExtractors,
//                              final Function<A, ? extends EnumLabel> labelExtractor,
//                              final IntIdentifier<String> featureIdentifier, boolean b) {
//    Collection<FeatureExtractor<A, ?>> fes = featureExtractors.stream().map(fe -> (FeatureExtractor<A,?>)fe).collect(Collectors.toList());
//    featureExtractors = fes;
//
//  }

  public void writeFeatureMap(final String mappingPath) {
    log.info("Writing {} feature IDs to {}{}....", featureIdentifier.size(), mappingPath, mappingExtension);
    featureIdentifier.toFile(mappingPath + mappingExtension);
  }

  public IntIdentifier<String> getFeatureIdentifier() {
    return featureIdentifier;
  }

  public <D> Stream<OldFeatureVector> vectorizeCorpus(final Stream<D> documents,
                                                      final Function<D, List<List<A>>> annotationSupplier) {
    return documents.flatMap(document -> vectorizeDocument(document, annotationSupplier));
  }

  public <D> Stream<OldFeatureVector> vectorizeDocument(final D document,
                                                        final Function<D, List<List<A>>> annotationSupplier) {
    return annotationSupplier.apply(document).stream().flatMap(Collection::stream).map(this::vectorizeAnnotation);
  }

  public OldFeatureVector vectorizeAnnotation(A annotation) {
    final List<Feature<?>> features = featureExtractors.stream()
        .flatMap(fe -> fe.apply(annotation)).collect(Collectors.toList());

    // Wait for each feature extractor to finish
//    CompletableFuture.allOf(features.stream().map(Feature::future).toArray(CompletableFuture[]::new)).join();

    // Write the label
    final OldFeatureVector fv = new OldFeatureVector(labelExtractor.apply(annotation));
    for (final Feature<?> feature : features) {
      final String fname = feature.name().replaceAll("\n", " ");
      if (!featureIdentifier.isLocked() || featureIdentifier.getID(fname) > -1) {
        final int id = featureIdentifier.getIDOrAdd(fname);
        fv.addFeature(id, feature);
      } else {
        if (numBadAdds % 100 == 0) {
          log.warn("Immutable IntIdentifier can't add features. Skipping feature: |{}|. {} skipped so far.", feature, numBadAdds);
        }
        numBadAdds++;
      }
    }
    return fv;
  }

  public void replaceFeatureIdentifier(final IntIdentifier<String> iid) {
    this.featureIdentifier = iid;
  }

  public <D> void writeFeatureVectors(final BufferedWriter writer, final D document,
                                      final Function<D, List<List<A>>> annotationSupplier) {
    try {
      for (final List<A> sequence : annotationSupplier.apply(document)) {
        for (final A annotation : sequence) {
          final OldFeatureVector fv = vectorizeAnnotation(annotation);

          try {
            writer.write(stringFun.apply(fv));
            writer.newLine();
          } catch (IOException ex) {
            log.error("Failed to vectorize annotation.", ex);
          }
        }
        writer.newLine();
      }
      writer.flush();
    } catch (IOException ex) {
      throw Throwables.propagate(ex);
    }
  }

  public <D> void vectorizeCorpusAndWrite(final Stream<D> documents,
                                          final String featurePath,
                                          final String mappingPath,
                                          final Function<D, List<List<A>>> annotationSupplier) {
    log.info("Starting vectorization of documents...");
    log.info("Writing feature vectors to {}{}...", featurePath, featureExtension);
    try (final BufferedWriter writer = Files.newBufferedWriter(Paths.get(featurePath + featureExtension));
         final ProgressLogger qlog = ProgressLogger.indeterminateSize("Parsing documents", 1, TimeUnit.SECONDS)) {
      documents.forEach(document -> {
        writeFeatureVectors(writer, document, annotationSupplier);
        qlog.update("parsed document");
      });
    } catch (IOException ex) {
      throw Throwables.propagate(ex);
    }

    writeFeatureMap(mappingPath);
    log.info("Done!");
  }

  public static Pair<List<OldFeatureVector>, ImmutibleIntIdenitifer<String>> removeUncommonFeatures(
      List<OldFeatureVector> fvs, final int n) {
    final IntIdentifier<String> iid = new IntIdentifier<>();
    final Multiset<String> ms = HashMultiset.create();
    fvs.stream().flatMap(fv -> fv.indices().stream().map(fv::getById).map(Feature::name)).forEach(ms::add);
    final Set<String> featureSet = ms.elementSet().stream().filter(s -> ms.count(s) > n).collect(Collectors.toSet());
    final List<OldFeatureVector> ret = new ArrayList<>();
    fvs.forEach(fv -> {
      final OldFeatureVector nfv = new OldFeatureVector(fv.label());
      for (Integer idx : fv.indices()) {
        final String fname = fv.getById(idx).name();
        if (featureSet.contains(fname)) {
          final int nidx = iid.getIDOrAdd(fname);
          nfv.addFeature(nidx, fv.getById(idx));
        }
      }
      ret.add(nfv);
    });
    return Pair.of(ret, iid.lock());
  }
}
