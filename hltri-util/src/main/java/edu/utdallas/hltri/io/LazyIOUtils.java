package edu.utdallas.hltri.io;

import com.google.common.collect.UnmodifiableIterator;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.function.Consumer;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * Created by travis on 7/18/14.
 */
public class LazyIOUtils extends IOUtils {
  public Iterable<String> readBatchLines(String path, int batch) {
    return readBatchLines(Paths.get(path), batch);
  }

  @Override
  public Iterable<File> iterateWithFilter(File file, FileFilter filter) {
    try {
      //noinspection NullableProblems
      return Files.walk(file.toPath()).map(Path::toFile).filter(filter::accept)::iterator;
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public Iterable<String> readBatchLines(File file, int batch) {
    try {
      return readBatchLines(new FileInputStream(file), batch);
    } catch (FileNotFoundException e) {
      throw new RuntimeException(e);
    }
  }

  public Iterable<String> readBatchLines(Path path, int batch) {
    try {
      return readBatchLines(Files.newBufferedReader(path, Charset.defaultCharset()), batch);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public Iterable<String> readBatchLines(InputStream istream, int batch) {
    return readBatchLines(new BufferedReader(new InputStreamReader(istream)), batch);
  }

  public Iterable<String> readBatchLines(BufferedReader reader, int batch) {
    //noinspection NullableProblems
    return batchedLineStream(reader, batch)::iterator;
  }

  private Stream<String> batchedLineStream(BufferedReader br, int batch) {
    return StreamSupport.stream(new BufferedReaderSpliterator(br, batch), false).onClose(() -> {
      try { br.close(); } catch (IOException e) { throw new UncheckedIOException(e); }
    });
  }

  /**
   * Lazily reads lines from a file.  Useful for very large files which
   * cannot fit in memory.
   *
   * @param br bufferedreader to be read
   * @return lines
   */
  @Override public Iterable<String> readLines(final BufferedReader br) {
    //noinspection NullableProblems
    return streamLines(br)::iterator;
  }

  private Stream<String> streamLines(final BufferedReader br) {
    try {
      return br.lines().onClose(() -> {
        try {
          br.close();
        } catch (IOException e) {
          throw new UncheckedIOException(e);
        }
      });
    } catch (Error|RuntimeException e) {
      try {
        br.close();
      } catch (IOException ex) {
        try {
          e.addSuppressed(ex);
        } catch (Throwable ignore) {
        }
      }
      throw e;
    }
  }

  /**
   * Iterator over the lines from a BufferedReader.  Lines are read on demand.
   */
  protected static class LineIterator extends UnmodifiableIterator<String> {
    private final BufferedReader reader;
    String nextLine;

    public LineIterator(BufferedReader reader) {
      this.reader = reader;
      try {
        nextLine = reader.readLine();
      } catch (IOException ioe) {
        throw new RuntimeException(ioe);
      }
    }

    @Override public boolean hasNext() {
      return nextLine != null;
    }

    @Override  public String next() {
      final String retVal = nextLine;
      try {
        this.nextLine = reader.readLine();
      } catch (IOException ioe) {
        throw new RuntimeException(ioe);
      }
      return retVal;
    }
  }
}
