package edu.utdallas.hltri.io;

import com.google.common.base.Splitter;
import com.google.common.collect.Lists;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.List;

import edu.utdallas.hltri.util.Unsafe;

/**
 *  Common IO use cases.
 * @author bryan
 */
public abstract class IOUtils {

  protected IOUtils() { /* Do nothing */ }

  /**
   * By default File#delete fails for non-empty directories, it works like "rm".
   * We need something a little more brutual - this does the equivalent of "rm -r"
   *
   * @param path Root File Path
   * @return true iff the file and all sub files/directories have been removed
   */
  public static void deleteRecursive(File path) {
    if (!path.exists()){
      throw new RuntimeException(new FileNotFoundException(path.getAbsolutePath()));
    }
    boolean ret = true;
    if (path.isDirectory()) {
      for (File f : path.listFiles()) {
        IOUtils.deleteRecursive(f);
      }
    }
    path.delete();
  }

  public Iterable<String> readLines(InputStream istream) {
    return readLines(new BufferedReader(new InputStreamReader(istream)));
  }

  public Iterable<String> readLines(String path) {
    return readLines(Paths.get(path));
  }

  public Iterable<String> readLines(File file) {
    try {
      return readLines(new FileInputStream(file));
    } catch (FileNotFoundException e) {
      throw new RuntimeException(e);
    }
  }

  public Iterable<String> readLines(Path path) {
    try {
      return readLines(Files.newBufferedReader(path, Charset.defaultCharset()));
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public abstract Iterable<String> readLines(BufferedReader reader);

  public Iterable<List<String>> readTSV(final String path, final int fields) {
    return readTSV(Paths.get(path), fields);
  }

  public Iterable<List<String>> readTSV(final File file, final int fields) {
    try {
      return readTSV(new FileInputStream(file), fields);
    } catch (FileNotFoundException e) {
      throw new RuntimeException(e);
    }
  }

  public Iterable<List<String>> readTSV(final Path file, final int fields) {
    try {
      return readTSV(Files.newBufferedReader(file, Charset.defaultCharset()), fields);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public Iterable<List<String>> readTSV(final InputStream istream, final int fields) {
    return readTSV(new BufferedReader(new InputStreamReader(istream)), fields);
  }

  public Iterable<List<String>> readTSV(final BufferedReader reader, final int fields) {
    return new Iterable<List<String>>() {
      @Override
      public Iterator<List<String>> iterator() {
        return new TSVIterator(fields, readLines(reader).iterator());
      }
    };
  }


  protected static FileFilter NULL = new FileFilter() {
    @Override public boolean accept(File pathname) { return true; }
  };

  protected static class SuffixFileFilter implements FileFilter {
    private final String suffix;

    private SuffixFileFilter(String suffix) {
      this.suffix = suffix;
    }

    @Override public boolean accept(File pathname) {
      return pathname.getName().endsWith(suffix);
    }
  }


  public Iterable<File> iterate(File file) {
    return iterateWithFilter(file, NULL);
  }

  public Iterable<File> iterate(String path) {
    return iterate(new File(path));
  }

  public Iterable<File> iterateWithSuffix(File file, String suffix) {
    return iterateWithFilter(file, new SuffixFileFilter(suffix));
  }

  public Iterable<File> iterateWithSuffix(String path, String suffix) {
    return iterateWithSuffix(new File(path), suffix);
  }

  public abstract Iterable<File> iterateWithFilter(File file, FileFilter filter);

  public static final IOUtils eager = new EagerIOUtils();

  public static final LazyIOUtils lazy = new LazyIOUtils();

  /**
   * Serialize the given object into the given stream
   */
  public static void serialize(Serializable obj, ByteArrayOutputStream bout) {
    try {
      ObjectOutputStream out = new ObjectOutputStream(bout);
      out.writeObject(obj);
      out.close();
    } catch (IOException e) {
      throw new IllegalStateException("Could not serialize " + obj, e);
    }
  }

  /**
   * Save a single object to a file.  That object could be a container if multiple
   * objects need to be saved.
   * @param <T> Type of the object being saved.
   * @param file
   * @param obj
   */
  public static <T extends Serializable> void saveObject(File file, T obj) {
    try (ObjectOutput out = new ObjectOutputStream(new FileOutputStream(file))) {
      out.writeObject(obj);
    } catch (IOException ioe) {
      throw new RuntimeException(ioe);
    }
  }

  public static <T extends Serializable> void saveObject(Path path, T obj) {
    try (ObjectOutput out = new ObjectOutputStream(Files.newOutputStream(path))) {
      out.writeObject(obj);
    } catch (IOException ioe) {
      throw new RuntimeException(ioe);
    }
  }

  /**
   * Saves and objects like saveObject, but turns off Java's caching mechanism
   * which can lead to out of memory exceptions.
   * @param <T>
   * @param file
   * @param obj
   */
  public static <T extends Serializable> void saveObjectNoCaching(File file, T obj) {
    try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(file))) {
      out.writeUnshared(obj);
    } catch (IOException ioe) {
      throw new RuntimeException(ioe);
    }
  }

  /**
   * Reads a single object out of a file.  This works in conjunction with saveObject
   */
  public static <T extends Serializable> T readObject(File file) {
    try (ObjectInput in = new ObjectInputStream(new FileInputStream(file))) {
      T obj = Unsafe.cast(in.readObject());
      return obj;
    } catch (Exception ioe) {
      throw new RuntimeException(ioe);
    }
  }

  /**
   * Iterator for tab-separated value files.
   */
  protected static class TSVIterator implements Iterator<List<String>> {
    final private Iterator<String> lineIt;
    final private Splitter splitter = Splitter.on('\t');
    final private int fieldCount;

    public TSVIterator(int fieldCount, Iterator<String> lineIt) {
      this.lineIt = lineIt;
      this.fieldCount = fieldCount;
    }

    @Override
    public boolean hasNext() {
      return lineIt.hasNext();
    }

    @Override
    public List<String> next() {
      List<String> fields = Lists.newArrayList(splitter.split(lineIt.next()));
      if (fields.size() != fieldCount) {
        System.err.println("File has " + fields.size() + " fields");
      }
      return fields;
    }

    @Override
    public void remove() {
      throw new UnsupportedOperationException("Not supported.");
    }
  }

  /**
   * Remove the file extension from a given filename
   * @param filePath String representation of a filename or path
   * @return filename or path without extension
   */
  public static String removeExtension(String filePath) {
    // These first few lines the same as Justin's
    File f = new File(filePath);

    // if it's a directory, don't remove the extension
    if (f.isDirectory()) return filePath;

    String name = f.getName();

    // Now we know it's a file - don't need to do any special hidden
    // checking or contains() checking because of:
    final int lastPeriodPos = name.lastIndexOf('.');
    if (lastPeriodPos <= 0) {
      // No period after first character - return name as it was passed in
      return filePath;
    } else {
      // Remove the last period and everything after it
      File renamed = new File(f.getParent(), name.substring(0, lastPeriodPos));
      return renamed.getPath();
    }
  }

  /**
   * Remove the file extension from a given filename
   * @param filePath String representation of a filename or path
   * @return filename or path without extension
   */
  public static String removeAllExtensions(String filePath) {
    // These first few lines the same as Justin's
    File f = new File(filePath);

    // if it's a directory, don't remove the extension
    if (f.isDirectory()) return filePath;

    String name = f.getName();

    // Now we know it's a file - don't need to do any special hidden
    // checking or contains() checking because of:
    final int lastPeriodPos = name.indexOf('.');
    if (lastPeriodPos <= 0) {
      // No period after first character - return name as it was passed in
      return filePath;
    } else {
      // Remove the last period and everything after it
      File renamed = new File(f.getParent(), name.substring(0, lastPeriodPos));
      return renamed.getPath();
    }
  }


}
