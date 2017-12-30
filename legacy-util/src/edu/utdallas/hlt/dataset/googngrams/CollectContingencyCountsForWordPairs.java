package edu.utdallas.hlt.dataset.googngrams;

import com.google.common.base.Joiner;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import edu.utdallas.hlt.io.ExternalCountWriter;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

/**
 *
 * @author bryan
 */
public class CollectContingencyCountsForWordPairs {

  public static final String WILDCARD = "__";
  final Joiner joiner = Joiner.on('\t');

  /**
   * The file input pairs should have two tokens per line, tab-separated
   * @param inputPairs
   * @param outputFile the file which will hold all of the contingency counts
   */
  public void collectCounts(File inputPairs, File outputFile) throws IOException {
    // {"word1\tword2"}
    final Multimap pairs = HashMultimap.create();
    for (String strPair : readLines(inputPairs)) {
      String[] pair = strPair.split("\t");
      assert pair.length == 2;
      pairs.put(pair[0], pair[1]);
    }

    final ExternalCountWriter countWriter = new ExternalCountWriter(outputFile);


    for (int ngramSize = 2; ngramSize <= 5; ngramSize++) {
      final int N = ngramSize;
      final String[] buffer = new String[ngramSize];
      for (int i = 0; i < buffer.length; i++) {
        buffer[i] = WILDCARD;
      }
      final String allWildcards = joiner.join(buffer);
      NGramIterator ngramIt = new NGramIterator(new File("/shared/hltdir3/disk1/bryan/LDC2006T13/data/" + ngramSize + "gms"), ngramSize);
      ngramIt.iterate(new NGramVisitor() {
        public void visit(String[] ngram, String strCount) {
          Collection<String> arg2s = pairs.get(ngram[0]);
          assert arg2s instanceof Set; // TODO: remove
          long count = Long.parseLong(strCount);
          if (arg2s.contains(ngram[N-1])) {
            // A1 P A2
            String strNgram = joiner.join(ngram);
            countWriter.addCount(strNgram, count);

            // A1 * A2
            buffer[0] = ngram[0];
            buffer[N-1] = ngram[N-1];
            countWriter.addCount(joiner.join(buffer), count);

          }
          // * * *
          countWriter.addCount(allWildcards, count);
        }
      });
    }

    countWriter.close();
  }


  private static List<String> readLines(File file) {
    List<String> lines = new ArrayList<>();
    BufferedReader reader = null;
    try {
      String line = null;
      reader = new BufferedReader(new FileReader(file));
      while ((line = reader.readLine()) != null) {
        lines.add(line);
      }
    } catch (IOException ex) {
      throw new RuntimeException(ex);
    } finally {
      try {
        reader.close();
      } catch (IOException ex) {
        throw new RuntimeException(ex);
      }
    }
    return lines;
  }

  public void collectInnerCounts(File firstPassOutput, File outputFile) throws IOException {
    Map<Integer,Set<String>> innerPhrases = new HashMap<>();
    innerPhrases.put(3, new HashSet<String>());
    innerPhrases.put(4, new HashSet<String>());
    innerPhrases.put(5, new HashSet<String>());

    StringBuilder buffer = new StringBuilder();
    for (String line : readLines(firstPassOutput)) {
      String[] fields = line.split("\t");
      int ngramSize = fields.length-1;
      buffer.setLength(0);
      for (int t = 1; t < fields.length-2; t++) {
        if (buffer.length() > 0) { buffer.append("\t"); }
        buffer.append(fields[t]);
      }
      innerPhrases.get(ngramSize).add(buffer.toString());
    }
  }

  public static void main(String... args) throws Exception {
    File inputPairsFile = new File(args[0]);
    File outputFile = new File(args[1]);
    new CollectContingencyCountsForWordPairs().collectCounts(inputPairsFile, outputFile);
    outputFile.setReadOnly();
  }

}
