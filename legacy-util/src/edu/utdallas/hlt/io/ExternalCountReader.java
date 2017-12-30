package edu.utdallas.hlt.io;

import gnu.trove.map.hash.TObjectIntHashMap;
import java.io.File;
import java.io.IOException;

/**
 * Class for reading in counts from a sorted file
 * Usage:
 * // first create a count file using ExternalCountWriter
 * ExternalCountReader reader = new ExternalCountReader(myFile);
 * TObjectIntHashMap counts = reader.readDoubleKeyCounts("gnutella");
 * System.out.println(counts.get("chocolate"));
 * reader.close();
 *
 * @author Bryan
 */
public class ExternalCountReader {

  private final StusMagicLargeFileReader contextReader;
  private final char SEPARATOR;

  public ExternalCountReader(File file, char separator) throws IOException {
    contextReader = new StusMagicLargeFileReader(file);
    this.SEPARATOR = separator;
  }

  public long getCount(String key) {
    long prevPos = contextReader.getPosition();
    ExternalBinarySearch.seekTo(contextReader, key);
    String line;

    while((line = contextReader.readLine()) != null && line.startsWith(key)) {
      int lastSep = line.lastIndexOf(SEPARATOR);
      long count = Long.parseLong(line.substring(lastSep+1));
      contextReader.seek(prevPos);
      return count;
    }
    contextReader.seek(prevPos);
    return 0;

//    List<String> lines = ExternalBinarySearch.binarySearch(contextReader, key);
//    String result = null;
//    for (String line : lines) {
//      if (line.indexOf(SEPARATOR) == line.lastIndexOf(SEPARATOR)) {
//        if (result != null) {
//          throw new IllegalStateException("More than one result when retrieving count for: " + key);
//        } else {
//          result = line;
//        }
//      }
//    }
//    if (result == null) { return 0; }
//    return Long.parseLong(result.substring(result.lastIndexOf(SEPARATOR)+1));
//    if (result == null) { return 0; }
//    if (results.size() > 1)  {
//      throw new IllegalStateException("More than one result when retrieving count for: " + key);
//    } else if (results.isEmpty()) {
//      return 0;
//    } else {
//      return Long.parseLong(results.get(0).substring(results.get(0).lastIndexOf(SEPARATOR)+1));
//    }
  }

  public void close() throws IOException {
    contextReader.close();
  }

  public void visitDoubleKeyCounts(String firstKey, CountVisitor visitor) {
    String keyAndSep = firstKey + SEPARATOR;
    long prevPos = contextReader.getPosition();
    ExternalBinarySearch.seekTo(contextReader, keyAndSep);
    String line;

    while((line = contextReader.readLine()) != null && line.startsWith(keyAndSep)) {
      int lastSep = line.lastIndexOf(SEPARATOR);
      long count = Long.parseLong(line.substring(lastSep+1));
      visitor.visit(line.substring(0, lastSep), count);
    }
    contextReader.seek(prevPos);

  }

  /**
   * Reads all the counts for the second key, given the first key
   * @param firstKey
   * @return TObjectIntHashMap
   */
  public TObjectIntHashMap readDoubleKeyCounts(String firstKey) {
    final TObjectIntHashMap counts = new TObjectIntHashMap();
    visitDoubleKeyCounts(firstKey, new CountVisitor() {
      @Override
      public void visit(String key, long count) {
        int delim = key.indexOf(SEPARATOR);
        if (delim < 0) { return; }
        String secondKey = key.substring(key.indexOf(delim)+1);
        counts.put(secondKey, (int) count);
      }
    });

    return counts;
  }

  public static interface CountVisitor {
    public void visit(String key, long count);
  }
}
