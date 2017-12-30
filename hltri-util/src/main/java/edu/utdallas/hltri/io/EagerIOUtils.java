package edu.utdallas.hltri.io;

import com.google.common.collect.Lists;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.stream.Collectors;

/**
 * Created by travis on 7/18/14.
 */
public class EagerIOUtils extends IOUtils {
  @Override public Iterable<String> readLines(BufferedReader reader) {
    final Collection<String> lines = Lists.newArrayList();
    try {
      for (String line; ((line = reader.readLine()) != null); ) {
        lines.add(line);
      }
    } catch (IOException ex) {
      throw new RuntimeException(ex);
    }
    return lines;
  }

  protected static class FileFilterAdaptor implements DirectoryStream.Filter<Path> {
    private final FileFilter filter;

    public FileFilterAdaptor(FileFilter filter) {
      this.filter = filter;
    }

    @Override public boolean accept (Path entry)throws IOException {
      return filter.accept(entry.toFile());
    }
  }

  @Override public Iterable<File> iterateWithFilter(final File file, final FileFilter filter) {
    try {
      return Files.walk(file.toPath()).map(Path::toFile).filter(filter::accept).collect(
          Collectors.toList());
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
