package edu.utdallas.hltri.combinatorics;

import com.google.common.base.CharMatcher;
import com.google.common.base.Splitter;
import edu.utdallas.hltri.io.IOUtils;

import java.io.BufferedReader;
import java.nio.file.Path;
import java.util.Iterator;

/**
 * Reads in text files where each line contains "item[tab]count"
 * @author bryan
 */
public class TextCountLoader {

  private Path countFile;
  private BufferedReader reader;
  private Splitter splitter = Splitter.on(CharMatcher.WHITESPACE);

  public TextCountLoader(Path countFile) {
    this.countFile = countFile;
  }

  public TextCountLoader(BufferedReader reader) {
    this.reader = reader;
  }

  /**
   * Populates the counter from the count file passed in to the constructor
   * @param counter
   * @return
   */
  public Counter<String> loadCounts(Counter<String> counter) {
    Iterable<String> lineIt;
    if (countFile != null) {
      lineIt = IOUtils.lazy.readLines(countFile);
    } else {
      lineIt = IOUtils.lazy.readLines(reader);
    }
    int count = 0;
    for (String line : lineIt) {
      Iterator<String> fields = splitter.split(line).iterator();
      counter.add(fields.next(), Long.parseLong(fields.next()));
      count++;
      assert ! fields.hasNext();
    }
    return counter;
  }
}
