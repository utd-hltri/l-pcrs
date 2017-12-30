package edu.utdallas.hlt.io;

import com.google.common.collect.Ordering;
import gnu.trove.map.hash.TObjectLongHashMap;
import gnu.trove.procedure.TObjectLongProcedure;
import java.io.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

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
public class ExternalCountWriter {

  // Count by Int's because Long's would half the number of things we can keep
  // in memory
  private TObjectLongHashMap memoryCountCache = new TObjectLongHashMap();
  private char SEPARATOR = '\t';
  private long MAX_MEMORY_USAGE = 8000000000l; // 8 GB
  private double MINIMUM_FREE_MEMORY_FRACTION = 0.1;
  private int sizeToFlush = -1;
  private File unsortedFile;
  private File sortedFile;
  private File outputFile;

  private boolean flushNeeded = false;

  private static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger(ExternalCountWriter.class);

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
  };

  public ExternalCountWriter(File outputFile) {
    this.unsortedFile = new File(outputFile.getParent(), outputFile.getName() + ".unsorted");
    this.sortedFile = new File(outputFile.getParent(), outputFile.getName() + ".sorted");
    this.outputFile = outputFile;
  }

  public ExternalCountWriter(File outputFile, boolean delete) {
    this(outputFile);

    if (delete) {
      try {
        if (unsortedFile.exists()) {
          LOGGER.info("Deleting existing unsorted file at {}", unsortedFile);
          unsortedFile.delete();
          unsortedFile.createNewFile();
        }

        if (sortedFile.exists()) {
          LOGGER.info("Deleting existing sorted file at {}", sortedFile);
          sortedFile.delete();
          sortedFile.createNewFile();
        }

        if (outputFile.exists()) {
          LOGGER.info("Deleting existing output file at {}", outputFile);
          outputFile.delete();
          outputFile.createNewFile();
        }
      }
      catch (IOException ex) {
        LOGGER.error("Unable to create new file", ex);
        throw new RuntimeException(ex);
      }
    }
  }

  private StringBuilder countBuilder = new StringBuilder();
  private Runtime runtime = Runtime.getRuntime();
  public void addCount(String key, long count) {
    countBuilder.setLength(0); // Reuse the builder to reduce memory churn
    countBuilder.append(key);
    String pair = countBuilder.toString();
    long oldCount =  memoryCountCache.get(pair);
    long newCount = oldCount+count;
    memoryCountCache.put(pair, newCount);
    flushNeeded = true;
//    if (oldCount == 0) {
//
//      estimatedMemoryUsage += ((key.length() * 2 + 32) + 4)*2;
//      if (memoryCountCache.size() % 10000 == 0) {
//        System.err.println("Esimated memory: " + estimatedMemoryUsage/1000);
//      }
//    }
//   if (estimatedMemoryUsage > MAX_MEMORY_USAGE) {
//    if (memoryCountCache.size() % 50000 == 0) {
//       System.err.println("Memory usage: " + (runtime.totalMemory() / (double) runtime.maxMemory()));
//    }
    boolean aboveMemThresh = (runtime.maxMemory() - runtime.totalMemory()) /
            (double) runtime.maxMemory() < MINIMUM_FREE_MEMORY_FRACTION;
    if ((sizeToFlush < 0 && aboveMemThresh) || (memoryCountCache.size() > sizeToFlush && sizeToFlush > 0)) {
      if (sizeToFlush < 0) { sizeToFlush = (int) (memoryCountCache.size() * 0.95); }
      flushCounts();
    }
  }

  public void flushCounts() {
    if (flushNeeded) {
      writeCountMap(memoryCountCache, unsortedFile, SEPARATOR);
      flushNeeded = false;
    }
  }

  protected static void writeCountMap(TObjectLongHashMap counts, File outFile, final char separator) {
    try {
      try (BufferedWriter writer = new BufferedWriter(new FileWriter(outFile, true))) {
        counts.forEachEntry(new TObjectLongProcedure() {
          @Override
          public boolean execute(Object o, long i) {
            try {
              writer.append((String) o);
              writer.append(separator);
              writer.append(Long.toString(i));
              writer.newLine();
              return true;
            } catch (Exception e) {
              throw new RuntimeException(e);
            }
          }
        });
      }
      counts.clear();
      System.gc();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public void close() throws IOException {
    flushCounts();
    memoryCountCache = null;
    LOGGER.info("Sorting file and saving intermediary output to {}.", sortedFile);
    ExternalMergeSort.sortFile(unsortedFile, sortedFile, comparator);
    reduceSortedFile(sortedFile, outputFile, SEPARATOR);
    unsortedFile.delete();
    sortedFile.delete();
  }

  /**
   * Assumes everything but the last field make up the key.
   * @param sortedFile
   * @param outFile
   * @param separator
   * @throws IOException 
   */
  public static void reduceSortedFile(File sortedFile, File outFile, char separator) throws IOException {
    LOGGER.info("Reducing sorted file and saving to {}.", outFile);
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

  public static void writeEntry(Writer writer, String key, long sum, char sep) throws IOException {
    writer.write(key);
    writer.write(sep);
    writer.write(Long.toString(sum));
    writer.write('\n');
  }

  public static void main(String... args) throws Exception {
    switch (args[0]) {
      case "sort":
        {
          File unsortedFile = new File(args[1]);
          File sortedFile = new File(args[2]);
          ExternalMergeSort.sortFile(unsortedFile, sortedFile, new KeyComparator('\t'));
          break;
        }
      case "reduce":
        {
          File sortedFile = new File(args[1]);
          File outFile = new File(args[2]);
          ExternalCountWriter.reduceSortedFile(sortedFile, outFile, '\t');
          break;
        }
      case "merge":  // or just use "LANG=C sort -m [sortedFiles] -o [outFile]"
        File outFile = new File(args[1]);
        List<File> sortedFiles = new ArrayList<>();
        for (int a = 2; a < args.length; a++) {
          sortedFiles.add(new File(args[a]));
        }
        ExternalMergeSort.mergeSortedFiles(sortedFiles, outFile,Ordering.<String>natural().nullsLast());
        break;
      default:
        System.err.println("Unknown command: " + args[0]);
        break;
    }
  }
  
}
