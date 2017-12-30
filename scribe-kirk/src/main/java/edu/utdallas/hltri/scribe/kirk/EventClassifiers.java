package edu.utdallas.hltri.scribe.kirk;

import edu.utdallas.hlt.features.span.*;
import edu.utdallas.hlt.i2b2.feature.token.GeniaFeatures;
import edu.utdallas.hlt.kiwi.feature.event.ConceptFeatures;
import edu.utdallas.hlt.kiwi.feature.event.EventFeatures;
import edu.utdallas.hlt.kiwi.feature.token.I2B2Features;
import edu.utdallas.hlt.kiwi.i2b2.event.CRFEventRecognizer;
import edu.utdallas.hlt.kiwi.i2b2.event.SVMEventModalityClassifier;
import edu.utdallas.hlt.kiwi.i2b2.event.SVMEventPolarityClassifier;
import edu.utdallas.hlt.kiwi.i2b2.event.SVMEventTypeClassifier;
import edu.utdallas.hlt.ml.TrainingListener;
import edu.utdallas.hlt.ml.feature.Feature;
import edu.utdallas.hlt.text.Event;
import edu.utdallas.hlt.text.LexicalType;
import edu.utdallas.hlt.text.Token;
import edu.utdallas.hlt.util.Place;
import edu.utdallas.hlt.util.nlp.BrownClusters;
import edu.utdallas.hlt.util.stats.StatisticalSet;
import edu.utdallas.hltri.logging.Logger;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by rmm120030 on 9/29/15.
 */
public class EventClassifiers {
  private static final Logger log = Logger.get(EventClassifiers.class);

  public static CRFEventRecognizer boundaryClassifier() {
    return new CRFEventRecognizer() {
      @Override
      protected List<Feature<Token,?>> getFeatures() {
        if (features == null) {
          features = new ArrayList<>();

          final BrownClusters i2b2100Clusters;
          final BrownClusters xin100Clusters;
//      final BrownClusters wiki100Clusters;
          final BrownClusters trecmed1000Clusters;
          final BrownClusters cna1000Clusters;
          try {
            i2b2100Clusters = new BrownClusters(
//            Place.fromFile("/shared/aifiles/disk1/kirk/data/brownClusters/i2b2-c100-p1.out/paths"),
                Place.fromFile("/home/rmm120030/working/eeg/kirk/brownClusters/i2b2-c100"),
                Place.fromFile("/home/rmm120030/working/eeg/kirk/brownClusters/i2b2-c100-p1.cache"),
                "i2b2.c100");
            xin100Clusters = new BrownClusters(
//            Place.fromFile("/shared/aifiles/disk1/kirk/data/brownClusters/gigaword.xin_eng-c100-p1.out/paths"),
                Place.fromFile("/home/rmm120030/working/eeg/kirk/brownClusters/neph25/alldocs-c1000-p1.out/paths"),
                Place.fromFile("/home/rmm120030/working/eeg/kirk/brownClusters/gigaword.xin_eng-c100-p1.cache"),
                "gigaword.xin_eng.c100");
//        wiki100Clusters = new BrownClusters(
//            Place.fromFile("/shared/aifiles/disk1/kirk/data/brownClusters/wikipedia-c100-p1.out/paths"),
//            Place.fromFile("/home/kirk/projects/kiwi/data/brownClusters/wikipedia-c100-p1.cache"),
//            "wikipedia.c100");
            trecmed1000Clusters = new BrownClusters(
//            Place.fromFile("/shared/aifiles/disk1/kirk/data/brownClusters/trecmed2012-c1000-p1.out/paths"),
                Place.fromFile("/home/rmm120030/working/eeg/kirk/brownClusters/trecmed2012-c1000"),
                Place.fromFile("/home/rmm120030/working/eeg/kirk/brownClusters/trecmed2012-c1000-p1.cache"),
                "trecmed2012.c1000");
            cna1000Clusters = new BrownClusters(
//            Place.fromFile("/shared/aifiles/disk1/kirk/data/brownClusters/gigaword.cna_eng-c1000-p1.out/paths"),
                Place.fromFile("/home/rmm120030/working/eeg/kirk/brownClusters/neph25/alldocs-c1000-p1.out/paths"),
                Place.fromFile("/home/rmm120030/working/eeg/kirk/brownClusters/gigaword.cna_eng-c1000-p1.cache"),
                "gigaword.cna_eng.c1000");
          } catch (IOException ioe) {
            throw new RuntimeException(ioe);
          }

          // FS: 78.654, CV-5: 78.25633
          features.add(new edu.utdallas.hlt.features.token.ContextFeatures.StemmedPrevWordFeature());
          features.add(new edu.utdallas.hlt.features.token.BrownClusterFeatures.BrownClusterFeature(trecmed1000Clusters));
          features.add(new GeniaFeatures.NextGeniaTokenFeature(GeniaFeatures.Value.POS));
//      features.add(new UMLSFeatures.UMLSConceptCategoryIOBFeature(2));
          features.add(new I2B2Features.PrevWhitespaceFeature());
          features.add(new edu.utdallas.hlt.features.token.BrownClusterFeatures.BrownClusterPrefixFeature(cna1000Clusters, 6));
          features.add(new GeniaFeatures.NextGeniaTokenFeature(GeniaFeatures.Value.PCHUNK));
          features.add(new GeniaFeatures.GeniaContextFeature(GeniaFeatures.Value.STEM, 1));
          features.add(new edu.utdallas.hlt.features.token.BrownClusterFeatures.BrownClusterPrefixFeature(xin100Clusters, 2));
          features.add(new edu.utdallas.hlt.features.token.BrownClusterFeatures.BrownClusterFeature(i2b2100Clusters));
        }

        log.info("CRF Boundary Classifier: {} features", features.size());

        return features;
      }
    };
  }

  public static SVMEventTypeClassifier typeClassifier() {
    return new SVMEventTypeClassifier() {
      @Override
      protected List<Feature<Event,?>> getFeatures() {
        if (features == null) {
          features = new ArrayList<>();

          //// FS: 88.5  2012-07-11
          //final NGramFeatures ngramFeatures = new NGramFeatures();
          //final WordFeatures wordFeatures = new WordFeatures();
          //final ContextFeatures contextFeatures = new ContextFeatures();
          //final SpanFeatures spanFeatures = new SpanFeatures();
          //final POSFeatures posFeatures = new POSFeatures();
          //features.add(ngramFeatures.new UnigramFeature(NGramFeatures.Type.UNCASED));
          //features.add(contextFeatures.new PrevWordFeature());
          //features.add(new EventFeatures.PrevEventTypeFeature());
          //features.add(spanFeatures.new LastTokenStemFeature());
          //features.add(contextFeatures.new UncasedPrevBigramFeature());
          //features.add(wordFeatures.new RawTextFeature());
          //features.add(posFeatures.new POSUnigramReplaceFeature(",", ".", "''", ":", "``", "$"));
          //features.add(contextFeatures.new NextNextWordFeature());

          // FS: 90.039  2012-08-06
          final StatisticalUnigramSet<Event> unigramSet =
              new StatisticalUnigramSet<>(LexicalType.UNCASED_AS_STRING,
                  getTargetFeature());
          final StatisticalSentenceUnigramSet<Event> sentSet =
              new StatisticalSentenceUnigramSet<>(LexicalType.UNCASED_AS_STRING,
                  getTargetFeature());
          addTrainingListener(new TrainingListener<Event>() {
            @Override
            public void featureExtractionStart() {
              unigramSet.clear();
              sentSet.clear();
            }
            @Override
            public void preFeatureExtraction(final Event event) {
              unigramSet.add(event);
              sentSet.add(event);
            }

            @Override
            public void featureExtractionFinish() {
              unigramSet.calculatePMI();
              sentSet.calculatePMI();
            }
          });
          final BrownClusters trecmed1000Clusters;
          final BrownClusters pmc100Clusters;
          try {
//        log.severe("Reading statistical sets...");
            unigramSet.read(Place.fromFile("/home/kirk/projects/kiwi/unigrams.pmi"), StatisticalSet.Type.PMI);
            unigramSet.calculatePMI();
            sentSet.read(Place.fromFile("/home/kirk/projects/kiwi/sent_unigrams.pmi"), StatisticalSet.Type.PMI);
            sentSet.calculatePMI();
//        log.severe("Unigram size: {0}; Sentence size: {1}", unigramSet.getValues().size(), sentSet.getValues().size());

            trecmed1000Clusters = new BrownClusters(
                Place.fromFile("/home/rmm120030/working/eeg/kirk/brownClusters/trecmed2012-c1000"),
                Place.fromFile("/home/rmm120030/working/eeg/kirk/brownClusters/trecmed2012-c1000-p1.cache"),
                "trecmed2012.c1000");
            pmc100Clusters = new BrownClusters(
                Place.fromFile("/home/rmm120030/working/eeg/kirk/brownClusters/pmc-c100"),
                Place.fromFile("/home/rmm120030/working/eeg/kirk/brownClusters/pmc-c100-p1.cache"),
                "pmc.c100");
          }
          catch (IOException ioe) {
            throw new RuntimeException(ioe);
          }
          final StatisticalSetFeatures statSetFeatures = new StatisticalSetFeatures();
          final BrownClusterFeatures brownClusterFeatures = new BrownClusterFeatures();
          final ContextFeatures contextFeatures = new ContextFeatures();
          final SpanFeatures spanFeatures = new SpanFeatures();
          final WordFeatures wordFeatures = new WordFeatures();
          //final NGramFeatures ngramFeatures = new NGramFeatures();
          //final POSFeatures posFeatures = new POSFeatures();

          features.add(statSetFeatures.new AboveStatisticalSetFeature(unigramSet, 0.0, StatisticalSet.Type.PMI));
          features.add(brownClusterFeatures.new BrownClusterFeature(trecmed1000Clusters));
          features.add(statSetFeatures.new StrongestSumStatisticalSetFeature(sentSet, StatisticalSet.Type.PMI));
          features.add(contextFeatures.new PrevWordFeature());
          features.add(spanFeatures.new LastTokenCaselessFeature());
          features.add(new ConceptFeatures.StrictConceptTypeFeature());
          features.add(contextFeatures.new UncasedPrevBigramFeature());
          features.add(wordFeatures.new UncasedTextFeature());
          features.add(brownClusterFeatures.new BrownClusterFeature(pmc100Clusters));
          features.add(new EventFeatures.PrevEventTypeFeature());
          features.add(new EventFeatures.ProviderEventPolarityFeature("evita"));
          features.add(brownClusterFeatures.new BrownClusterPrefixFeature(trecmed1000Clusters, 4));
          features.add(wordFeatures.new PuncFeature());
//          features.add(new UMLSFeatures.UMLSConceptCategoryFeature(5));
//          features.add(new UMLSFeatures.UMLSConceptFeature());
        }
        return features;
      }
    };
  }

  public static SVMEventPolarityClassifier polarityClassifier() {
    return new SVMEventPolarityClassifier() {
      @Override
      protected List<Feature<Event,?>> getFeatures() {
        if (features == null) {
          features = new ArrayList<>();

          // FS: 97.8  2012-07-11
          final ContextFeatures contextFeatures = new ContextFeatures();
          final NGramFeatures ngramFeatures = new NGramFeatures();
          features.add(contextFeatures.new IndexedStemmedPrevWordFeature());
          features.add(new EventFeatures.PrevEventPolarityFeature());
          features.add(ngramFeatures.new UnigramFeature(NGramFeatures.Type.NORMAL));
        }
        return features;
      }
    };
  }

  public static SVMEventModalityClassifier modalityClassifier() {
    return new SVMEventModalityClassifier() {
      @Override
      protected List<Feature<Event,?>> getFeatures() {
        if (features == null) {
          features = new ArrayList<Feature<Event,?>>();

          // FS: 96.9  2012-07-11
          final ContextFeatures contextFeatures = new ContextFeatures();
          features.add(new EventFeatures.PrevEventModalityFeature());
          features.add(contextFeatures.new StemmedPrevWordFeature());
          features.add(contextFeatures.new StemmedPrevPrevWordFeature());
        }
        return features;
      }
    };
  }
}
