package edu.utdallas.hltri.io;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Taken from: http://stackoverflow.com/questions/736556/binary-search-in-a-sorted-memory-mapped-file-in-java
 * @author bryan
 *
 * If you're using this as a quick read-only tsv-backed Key-Value store, make sure you
 * binary search for key + "\t" otherwise you'll get all the entries for everything
 * that starts with key.
 */
public class ExternalBinarySearch {

  public static List<String> binarySearch(File file, String string) {
    try {
      StusMagicLargeFileReader raf = new StusMagicLargeFileReader(file);
      List<String> result = binarySearch(raf, string);
      raf.close();
      return result;
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Returns all of the lines that start with the given <var>string</var>.  If no lines match, an empty list is returned
   * @param raf
   * @param string
   * @return
   */
  synchronized public static List<String> binarySearch(StusMagicLargeFileReader raf, String string) {

    seekTo(raf, string);

    List<String> result = new ArrayList<>();
    while (true) {
      String line = raf.readLine();
      if (line == null || !line.startsWith(string)) {
        break;
      }
      result.add(line);
    }

    return result;
  }

  synchronized public static void seekTo(StusMagicLargeFileReader raf, String string) {


    long low = 0;
    long high = raf.getLength();

    long p = -1;
    while (low < high) {
      long mid = (low + high) / 2;
      p = mid;
      while (p >= 0) {
        raf.seek(p);

        char c = (char) raf.readByte();
        //System.out.println(p + "\t" + c);
        if (c == '\n') {
          break;
        }
        p--;
      }
      if (p < 0) {
        raf.seek(0);
      }
      String line = raf.readLine();
      //System.out.println("-- " + mid + " " + line);
      if (line.compareTo(string) < 0) {
        low = mid + 1;
      } else {
        high = mid;
      }
    }

    p = low;
    while (p >= 0) {
      raf.seek(p);
      if (((char) raf.readByte()) == '\n') {
        break;
      }
      p--;
    }

    if (p < 0) {
      raf.seek(0);
    }

  }

  public static void main(String... args) {
    List<String> results = ExternalBinarySearch.binarySearch(new File(args[0]), args[1]);
    for (String result : results) {
      System.out.println(result);
    }
    System.err.println("Got " + results.size() + " results");
  }
}
