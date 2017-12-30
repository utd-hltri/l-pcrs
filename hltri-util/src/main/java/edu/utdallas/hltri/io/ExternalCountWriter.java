package edu.utdallas.hltri.io;

import com.google.code.externalsorting.ExternalSort;
import edu.utdallas.hltri.logging.Logger;
import gnu.trove.map.hash.TObjectIntHashMap;
import gnu.trove.procedure.TObjectIntProcedure;

import java.io.*;
import java.util.Comparator;

/**
 * A class to count string occurences, spilling over to disk if necessary
 *
 * Usage:
 * ExternalCounter counter = new ExternalCounter(myFile);
 * ...
 * counter.addCount("ponies", 5);
 * counter.addCount("rice a roni", 4);
 * counter.addCount("ponies", 2);
 * ...
 * counter.close(); // IMPORTANT!
 *
 * @author Bryan Rink bryan@hlt.utdallas.edu
 */
@SuppressWarnings("unused")
public class ExternalCountWriter implements AutoCloseable {

  // Count by Int's because Long's would half the number of things we can keep
  // in memory
  private TObjectIntHashMap<String> memoryCountCache             = new TObjectIntHashMap<>();
  private char                      SEPARATOR                    = '\t';
  private long                      MAX_MEMORY_USAGE             = 8_000_000_000l; // 8 GB
  private double                    MINIMUM_FREE_MEMORY_FRACTION = 0.25;
  private int                       sizeToFlush                  = -1;
  private File unsortedFile;
  private File sortedFile;
  private File outputFile;

  private boolean flushNeeded = false;

  private static final Logger log = Logger.get(ExternalCountWriter.class);

  Comparator<String> comparator = new KeyComparator(SEPARATOR);

  protected static class KeyComparator implements Comparator<String> {
    private char SEPARATOR = '\t';

    public KeyComparator(char SEPARATOR) {
      this.SEPARATOR = SEPARATOR;
    }

    @Override
    public int compare(String r1, String r2) {
      int i = 0;

      char v1;
      char v2;
      while (true) {

        v1 = r1.charAt(i);
        v2 = r2.charAt(i);

        if (v1 != v2) {
          return v1 - v2;
        }

        if (v1 == SEPARATOR)
          break;

        i++;
      }

      return r1.substring(i, r1.lastIndexOf(SEPARATOR))
               .compareTo(r2.substring(i, r2.lastIndexOf(SEPARATOR)));
    }
  }

  public ExternalCountWriter(File outputFile) {
    this.unsortedFile = new File(outputFile.getParent(), outputFile.getName() + ".unsorted");
    this.sortedFile = new File(outputFile.getParent(), outputFile.getName() + ".sorted");
    this.outputFile = outputFile;
  }

  @SuppressWarnings("ResultOfMethodCallIgnored")
  public ExternalCountWriter(File outputFile, boolean delete) {
    this(outputFile);

    if (delete) {
      try {
        if (unsortedFile.exists()) {
          log.info("Deleting existing unsorted file at {}", unsortedFile);
          unsortedFile.delete();
          unsortedFile.createNewFile();
        }

        if (sortedFile.exists()) {
          log.info("Deleting existing sorted file at {}", sortedFile);
          sortedFile.delete();
          sortedFile.createNewFile();
        }

        if (outputFile.exists()) {
          log.info("Deleting existing output file at {}", outputFile);
          outputFile.delete();
          outputFile.createNewFile();
        }
      }
      catch (IOException ex) {
        log.error("Unable to create new file", ex);
        throw new RuntimeException(ex);
      }
    }
  }

  private StringBuilder countBuilder = new StringBuilder();

  public void addCount(final String key, final int count) {
    countBuilder.setLength(0); // Reuse the builder to reduce memory churn
    countBuilder.append(key);
    addCount(countBuilder, count);
  }

  // Convenience method for calculating the free memory
  private static final Runtime runtime = Runtime.getRuntime();
  private static double freeMemoryRatio() {
    double ratio = (runtime.maxMemory() - runtime.totalMemory() + runtime.freeMemory()) / (double) runtime.maxMemory();
    log.debug("Free memory ratio is {}", ratio);
    return ratio;
  }

  public void addCount(final StringBuilder key, final int count) {
    final String pair = key.toString();
    final int oldCount =  memoryCountCache.get(pair);
    final int newCount = oldCount + count;
    memoryCountCache.put(pair, newCount);
    flushNeeded = true;

    // If we haven't flushed yet, calculate free memory ratio every 100 entries and remember the size of the map
    // We are abusing short-circuiting here to only calculate the freeMemory every 100 entries IF we haven't calculated sizeToFlush
    if (sizeToFlush < 0 && memoryCountCache.size() % 100 == 0 && freeMemoryRatio() < MINIMUM_FREE_MEMORY_FRACTION) {
      sizeToFlush = (int) (memoryCountCache.size() * 0.9);
      log.info("Setting flush threshold to {} entries", sizeToFlush);
    }

    // For future flushes, just compare the map size (as the memory is still allocated)
    // We don't want to flush until sizeToFlush has been calculated
    if (sizeToFlush >= 0 && memoryCountCache.size() >= sizeToFlush) {
      log.info("Flushing current counts...");
      flushCounts();
    }
  }

  public void flushCounts() {
    if (flushNeeded) {
      writeCountMap(memoryCountCache, unsortedFile, SEPARATOR);
      flushNeeded = false;
    }
  }

  protected static void writeCountMap(final TObjectIntHashMap<String> counts, final File outFile, final char separator) {
    try {
      try (final BufferedWriter writer = new BufferedWriter(new FileWriter(outFile, true))) {
        counts.forEachEntry(new TObjectIntProcedure<String>() {
          @Override
          public boolean execute(final String o, final int i) {
            try {
              writer.append(o);
              writer.append(separator);
              writer.append(Integer.toString(i));
              writer.newLine();
              return true;
            } catch (Exception e) {
              throw new RuntimeException(e);
            }
          }
        });
      counts.clear();
      System.gc();
      }
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @Override public void close() throws IOException {
    flushCounts();
    memoryCountCache = null;
    log.info("Sorting file and saving intermediary output to {}", sortedFile);
    ExternalSort.mergeSortedFiles(ExternalSort.sortInBatch(unsortedFile, comparator), sortedFile, comparator);
    log.info("Reducing sorted file to {}", outputFile);
    reduceSortedFile(sortedFile, outputFile, SEPARATOR);
  }

  public static void reduceSortedFile(File sortedFile, File outFile, char separator) throws IOException {
    log.info("Reducing sorted file and saving to {}", outFile);
    String lastKey = null;
    long sum = 0;
    try (Writer writer = new BufferedWriter(new FileWriter(outFile))) {
      BufferedReader reader = new BufferedReader(new FileReader(sortedFile));
      String line;
      while ((line = reader.readLine()) != null) {
        int sep = line.lastIndexOf(separator);
        String key = line.substring(0, sep);
        long value = Long.parseLong(line.substring(sep+1));
        if (lastKey != null &&  ! key.equals(lastKey)) {
          writeEntry(writer, lastKey, sum, separator);
          lastKey = key;
          sum = value;
        } else {
          sum += value;
          lastKey = key;
        }
      }
      writeEntry(writer, lastKey, sum, separator);
      reader.close();
    }
  }

  public static void writeEntry(final Writer writer, final String key, final long sum, final char sep) throws IOException {
    writer.append(key)
    .append(sep)
    .append(Long.toString(sum))
    .append('\n');
  }

  public static void main(String... args) throws Exception {
    switch (args[0]) {
      case "sort":
        {
          File unsortedFile = new File(args[1]);
          File sortedFile = new File(args[2]);
          Comparator<String> comparator = new KeyComparator('\t');
          ExternalSort.mergeSortedFiles(ExternalSort.sortInBatch(unsortedFile, comparator), sortedFile, comparator);
          break;
        }
      case "merge":
        {
          File sortedFile = new File(args[1]);
          File outFile = new File(args[2]);
          ExternalCountWriter.reduceSortedFile(sortedFile, outFile, '\t');
          break;
        }
      default:
        System.err.println("Unknown command: " + args[0]);
        break;
    }
  }
}
