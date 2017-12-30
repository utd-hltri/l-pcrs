package edu.utdallas.hltri.scribe.util;

import edu.utdallas.hltri.scribe.Stopwords;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

/**
 * Simple stopwords class that reads from a text file with one stopword per line.
 */
public class SimpleStopwords extends Stopwords {
  public SimpleStopwords(final String stopWordFile) {
    try (final BufferedReader reader = new BufferedReader(new FileReader(stopWordFile))) {
      String line;
      while((line = reader.readLine()) != null) {
        delegate.add(line.trim());
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
