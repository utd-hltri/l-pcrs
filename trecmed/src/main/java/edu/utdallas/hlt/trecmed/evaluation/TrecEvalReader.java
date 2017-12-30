/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.utdallas.hlt.trecmed.evaluation;

import com.google.common.base.Splitter;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Iterables;
import com.google.common.collect.Table;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Pattern;

import edu.utdallas.hltri.logging.Logger;

/**
 *
 * @author travis
 */
public class TrecEvalReader {
  final private Logger log = Logger.get(TrecEvalReader.class);
  final Table<String, String, Double> evaluations = HashBasedTable.create();
  private final Path path;

  public TrecEvalReader(Path path) {
    this.path = path;
    parse();
  }

  private void parse() {
    final Splitter splitter = Splitter.on(Pattern.compile("\\s+"));
    String[] fields;
    try (BufferedReader reader = Files.newBufferedReader(path, Charset.defaultCharset())) {
      for (String line; (line = reader.readLine()) != null;) {
         fields = Iterables.toArray(splitter.split(line), String.class);
         try {
          if (evaluations.put(fields[1], fields[0], Double.parseDouble(fields[2])) != null) {
            log.warn("Previous evaluation found for {} {}: {}", fields[1], fields[0], evaluations.get(fields[1], fields[0]));
          }
         } catch (NumberFormatException ex) {
           log.trace("Failed to store evaluation from line {}", line);
         }
      }
    } catch (IOException ex) {
      throw new RuntimeException(ex);
    }
  }

  public double get(String questionId, String measure) {
    try {
      return evaluations.get(questionId, measure);
    } catch (NullPointerException ex) {
      log.warn("Question {} had no value for measure {}", questionId, measure);
      return Double.NaN;
    }
  }
}
