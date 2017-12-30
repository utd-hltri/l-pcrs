package edu.utdallas.hltri.io;

import com.google.common.base.Splitter;
import com.google.common.collect.Multiset;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Utilities for reading/writing to/from text files!
 */
@SuppressWarnings({"unused", "WeakerAccess"})
public abstract class TextFiles {
  private TextFiles() {}

  public static Iterable<CSVRecord> loadCsv(Path path, boolean firstLineIsHeader) {
    final CSVFormat parser = (firstLineIsHeader) ? CSVFormat.DEFAULT.withFirstRecordAsHeader() : CSVFormat.DEFAULT;
    try (final BufferedReader reader = Files.newBufferedReader(path)) {
      return parser.parse(reader);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  // ???
  public static Map<String,String> loadMapFromTSV(String path, Iterable<String> lines) {
    Splitter splitter = Splitter.on('\t');
    Map<String,String> map = new HashMap<>();
    for (String line : lines) {
      Iterator<String> fields = splitter.split(line).iterator();
      map.put(fields.next(), fields.next());
    }
    return map;
  }

  /**
   * Loads each line from the file into a Set.
   * If a delimiter is provided, only take the content of a line up to the first occurrence of the delimiter.
   * @param file file whose lines are to be loaded into set
   * @param delimiter only save the substring of each line up to this delimiter; null if all lines should be added
   *                  in their entirety
   * @return set of line strings from file
   */
  public static Set<String> loadSet(String file, String delimiter) {
    try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
      final Set<String> set = new HashSet<>();
      String line;
      while ((line = reader.readLine()) != null) {
        if (delimiter == null || !line.contains(delimiter)) {
          set.add(line);
        }
        else {
          set.add(line.substring(0, line.indexOf(delimiter)));
        }
      }
      return set;
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Loads each line from the file into a Set.
   * @param file file whose lines are to be loaded into set
   * @return set of line strings from file
   */
  public static Set<String> loadSet(String file) {
    return loadSet(file, null);
  }

  /**
   * Saves contents of set to the file at the path provided, one set element per line.
   * @param set set to be saved
   * @param file full path to the file to write the set to
   * @param <T> type
   */
  public static <T> void saveSet(final Set<T> set, final String file) {
    try {
      Files.write(Paths.get(file), set.stream().map(Object::toString).collect(Collectors.toList()));
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Writes the contents of a multiset in descending order by count. Each line takes the form:
   *  <thing>\t<count>
   * @param ms the multiset
   * @param file file to write to
   * @param <T> type; should have working toString() method
   */
  public static <T> void writeMultisetSortedDescending(final Multiset<T> ms, final Path file) {
    writeMultisetSorted(ms, file, Comparator.<T>comparingDouble(ms::count).reversed());
  }

  /**
   * Writes the contents of a multiset in ascending order by count. Each line takes the form:
   *  <thing>\t<count>
   * @param ms the multiset
   * @param file file to write to
   * @param <T> type; should have working toString() method
   */
  public static <T> void writeMultisetSortedAscending(final Multiset<T> ms, final Path file) {
    writeMultisetSorted(ms, file, Comparator.comparingDouble(ms::count));
  }

  /**
   * Writes the contents of a multiset in sorted order using the passed comparator. Each line takes the form:
   *  <thing>\t<count>
   * @param ms the multiset
   * @param file file to write to
   * @param sorter used to define the sort ordering
   * @param <T> type; should have working toString() method
   */
  private static <T> void writeMultisetSorted(final Multiset<T> ms, final Path file, Comparator<T> sorter) {
    final ArrayList<T> list = new ArrayList<>(ms.elementSet());
    list.sort(sorter);
    try {
      Files.write(file, list.stream().map(s -> String.format("%6d\t%s", ms.count(s), s)).collect(Collectors.toList()));
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
