package edu.utdallas.hlt.sim.corpus;

import edu.utdallas.hlt.io.ExternalBinarySearch;
import edu.utdallas.hlt.io.StusMagicLargeFileReader;
import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * TRY: stronger tfidf idf discounting (sqrt instead of log?), try increasing the max_occur after this
 * @author Bryan
 */
public class MostSimilarFinder {
  StusMagicLargeFileReader contextReader;

  int NUM_MOST_SIMILAR = 200;

  public MostSimilarFinder(File contextsFile) throws IOException {
    contextReader = new StusMagicLargeFileReader(contextsFile);
  }

  public void getContextCounts(String word, List<String> primaryContexts, List<Integer> primaryCounts) {
    for (String contextLine : ExternalBinarySearch.binarySearch(contextReader, word + ' ')) {
      int index = contextLine.lastIndexOf(' ');
      primaryContexts.add(contextLine.substring(contextLine.indexOf(' ')+1, index));
      primaryCounts.add(Integer.parseInt(contextLine.substring(index+1)));
    }
  }

//  public TObjectIntHashMap getUnigramCounts(File file) {
//    TObjectIntHashMap counts = new TObjectIntHashMap();
//    for (String line : IOUtils.iterateLines(file)) {
//
//    }
//    return counts;
//  }

//  public List<WordPair> findMostSimilar(String primaryWord) {
//    List<String> primaryContexts = new ArrayList<String>();
//    List<Integer> primaryCounts = new ArrayList<Integer>();
//    getContextCounts(primaryWord, primaryContexts, primaryCounts);
//    //System.err.println("Primaries: " + primaryContexts);
//
//    TObjectIntHashMap unigramCounts = ContextCollectorEMS.
//            readUnigramCounts(new File("/shared/hltdir3/disk1/bryan/sim/pmc2011_unigrams_lower_by_frequency.txt"));
//
//    PriorityQueue<WordPair> mostSimilar = new PriorityQueue<WordPair>();
//    CosineDistanceCPConsumer sim = new CosineDistanceCPConsumer();
//    //ContextPairConsumer sim = new JensenShannonCPConsumer();
//    //ContextPairConsumer sim = new FisherCPConsumer();
//
//
//
//    String line = null;
//    contextReader.seek(0);
//    String previous = null;
//
//    ExternalBinarySearch.seekTo(contextReader, "a ");
//    int MIN_OCCUR = 0;
//    int MAX_OCCUR = 1000000;
//    int JOINT_MIN_OCCUR = 15;
//    int TFIDF_TOP_N = 1500;
//
//    Map<String,Integer> contextIndex = new HashMap<String,Integer>();
//    PriorityQueue<WordPair> primaryTFIDF = new PriorityQueue<WordPair>();
//    for (int i = 0; i < primaryCounts.size(); i++) {
//      contextIndex.put(primaryContexts.get(i), i);
//      if (primaryContexts.get(i).equals(primaryWord) || primaryContexts.get(i).length() == 1 || unigramCounts.get(primaryContexts.get(i)) < MIN_OCCUR ||
//                  unigramCounts.get(primaryContexts.get(i)) >= MAX_OCCUR ||
//                  primaryCounts.get(i) < JOINT_MIN_OCCUR) {
//        System.err.println("FILTERING: " + primaryContexts.get(i));
//        continue;
//      }
//      double idf = Math.log(50310404.0/(unigramCounts.get(primaryContexts.get(i))+1));
//      double tfidf = primaryCounts.get(i)*idf*idf;
//      System.err.printf("TFIDF: %30s %f\n", primaryContexts.get(i), tfidf);
//      if (primaryTFIDF.size() < TFIDF_TOP_N || primaryTFIDF.peek().similarity < tfidf) {
//        primaryTFIDF.add(new WordPair(primaryWord, primaryContexts.get(i), tfidf));
//        if (primaryTFIDF.size() > TFIDF_TOP_N) {
//          primaryTFIDF.remove();
//        }
//      }
//    }
//
//    Set<String> contextWordsToUse = new HashSet<String>();
//    for (WordPair pair : primaryTFIDF) {
//      sim.precomputeLeft(pair.similarity);
//      contextWordsToUse.add(pair.word2);
//    }
//
//    double lastPercent = 0;
//    while ((line = contextReader.readLine()) != null) {
//      double percent = contextReader.getPosition() / (double) contextReader.getLength();
//      if (percent*100 > 0.99 + lastPercent) {
//        lastPercent += 1;
//        System.err.println(lastPercent + "%");
//      }
//      if (line.equals("")) { continue; }
//      int sepIndex = line.lastIndexOf(' ');
//      int firstSep = line.indexOf(' ');
//      String word = line.substring(0, firstSep);
//      String contextWord = line.substring(firstSep+1, sepIndex);
//      int count = Integer.parseInt(line.substring(sepIndex+1));
//      //System.err.println("LINE: " + line);
//      if (previous != null && !word.equals(previous)) {
//        double similarity = 0;
//        if (contextWordsToUse.contains(word)) {
//          Integer idx = contextIndex.get(word);
//          double idf = Math.log(50310404.0/(unigramCounts.get(primaryContexts.get(idx))+1));
//          similarity = sim.getSimilarity(primaryCounts.get(idx)*idf*idf);
//        } else {
//          similarity = sim.getSimilarity();
//        }
//        //System.out.printf("%30s %30s %f\n", primaryWord, previous, similarity);
//        if (mostSimilar.size() < NUM_MOST_SIMILAR || mostSimilar.peek().similarity < similarity) {
//          mostSimilar.add(new WordPair(primaryWord, previous, similarity));
//          if (mostSimilar.size() > NUM_MOST_SIMILAR) {
//            mostSimilar.remove();
//          }
//        }
//        sim.reset();
//      }
//
//
//      if (contextWordsToUse.contains(contextWord) && ! primaryWord.equals(contextWord) && contextWord.length() > 1 && ! word.equals(contextWord)) {
//        Integer primaryIdx = contextIndex.get(contextWord);
//        double contextTF = unigramCounts.get(contextWord);
//        double idf = Math.log(50310404.0/(contextTF+1));
//        if (primaryIdx == null) {
//          if (contextTF >= MIN_OCCUR && contextTF < MAX_OCCUR) {
//            sim.visitContextPair(0, (count)*idf*idf);
//          }
//        } else {
//          //System.err.println("EQ: " + primaryContexts.get(primaryIndex) + "  " + contextWord);
//          if (contextTF >= MIN_OCCUR && contextTF < MAX_OCCUR && primaryCounts.get(primaryIdx) >= JOINT_MIN_OCCUR) {
//            double idfSqr = idf*idf;
//            sim.visitContextPair((primaryCounts.get(primaryIdx))*idfSqr, (count)*idfSqr);
//          }
//        }
//      }
//
//      previous = word;
//    }
//
//    List<WordPair> results = new ArrayList<WordPair>();
//    while ( ! mostSimilar.isEmpty()) {
//      results.add(mostSimilar.poll());
//    }
//    return results;
//  }

  public static class WordPair implements Comparable<WordPair> {
    public String word1, word2;
    public double similarity;

    public WordPair(String word1, String word2, double similarity) {
      this.word1 = word1;
      this.word2 = word2;
      this.similarity = similarity;
    }

    public int compareTo(WordPair other) {
      return Double.compare(similarity, other.similarity);
    }

  }

//  public static void main(String... args) throws Exception {
//    for (WordPair wp : new MostSimilarFinder(new File(args[0])).findMostSimilar(args[1])) {
//       System.out.printf("%30s %30s   %f\n", wp.word1, wp.word2, wp.similarity);
//    }
//  }
}
