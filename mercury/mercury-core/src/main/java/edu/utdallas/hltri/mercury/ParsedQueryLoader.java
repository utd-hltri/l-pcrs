package edu.utdallas.hltri.mercury;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import edu.utdallas.hltri.logging.Logger;

/**
 * Created by travis on 10/13/16.
 */
public enum ParsedQueryLoader {
  INSTANCE;

  private final Logger log = Logger.get(ParsedQuery.class);

  public Map<String, ParsedQuery> load(final Path parsedQueryPath) {
    final Map<String, ParsedQuery> parsedQueries = new HashMap<>();
    try(final BufferedReader reader = Files.newBufferedReader(parsedQueryPath)) {
      for (final CSVRecord fields : CSVFormat.DEFAULT.withFirstRecordAsHeader().parse(reader)) {
        log.debug("Reading line {}", fields);
        parsedQueries.put(fields.get(0), new ParsedQuery(fields.get(1), fields.get(2)));
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    return parsedQueries;
  }

  public Map<String, String> loadUnparsed(final Path unparsedQueryPath) {
    final Map<String, String> queries = new HashMap<>();
    try(final BufferedReader reader = Files.newBufferedReader(unparsedQueryPath)) {
      for (final CSVRecord fields : CSVFormat.DEFAULT.withFirstRecordAsHeader().parse(reader)) {
        log.debug("Reading line {}", fields);
        queries.put(fields.get(0), fields.get(1));
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    return queries;
  }
}
