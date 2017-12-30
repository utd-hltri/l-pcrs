
package edu.utdallas.hlt.dataset.googngrams;

import java.io.*;
import java.util.zip.GZIPInputStream;

/**
 * Iterates over n-grams from the Google n-grams data set.
 * Data is located at: /shared/hltdir3/disk1/bryan/LDC2006T13/data/
 * @author bryan
 */
public class NGramIterator {

  private File dataDir;
  private int gramSize;

  public NGramIterator(File dataDir, int gramSize) {
    this.dataDir = dataDir;
    this.gramSize = gramSize;
  }

  public void iterate(NGramVisitor visitor) throws IOException {
    File[] files = dataDir.listFiles();
    for (int f = 0; f < files.length; f++) {
      File file = files[f];
      System.err.printf("%s %.0f%%\n", file.getName(), f*100.0 / files.length);
      if (file.getName().endsWith(".gz")) {
        Reader reader = new InputStreamReader(new GZIPInputStream(new FileInputStream(file)), "UTF-8");
        int chr;
        String[] ngram = new String[gramSize];
        StringBuilder currentGram = new StringBuilder();
        int gramNum = 0;
        while ((chr = reader.read()) != -1) {
          if (chr == ' ' || chr == '\t') {
            ngram[gramNum] = currentGram.toString();
            currentGram.setLength(0);
            gramNum++;
          } else if (chr == '\n') {
            visitor.visit(ngram, currentGram.toString());
            currentGram.setLength(0);
            gramNum = 0;
          } else {
            currentGram.append((char) chr);
          }
        }
        if (gramNum != 0) {
          visitor.visit(ngram, currentGram.toString());
        }
      }
      //break;
    }

  }

  public static void main(String... args) throws Exception {
    NGramIterator it = new NGramIterator((new File(args[0])), Integer.parseInt(args[1]));
    it.iterate(new NGramVisitor() {

      public void visit(String[] ngram, String count) {
        System.out.println(ngram[0] + " " + ngram[1] + " " + ngram[2] + "\t" + count);
      }
    });

  }
}
