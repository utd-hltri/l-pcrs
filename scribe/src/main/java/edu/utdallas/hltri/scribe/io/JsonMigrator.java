package edu.utdallas.hltri.scribe.io;

/**
 * Created by travis on 9/10/15.
 */

import com.google.common.base.Throwables;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import edu.utdallas.hltri.framework.ProgressLogger;
import edu.utdallas.hltri.logging.Logger;

/**
 * Created by rmm120030 on 9/7/15.
 */
public class JsonMigrator {
  private static final Logger log = Logger.get(JsonMigrator.class);

  public static void main(String... args) {
    try(ProgressLogger plog = ProgressLogger.indeterminateSize("migr8", 1, TimeUnit.SECONDS)) {
      migrate(args[0]).forEach(id -> plog.update(id));
    }
  }

  private static Stream<String> migrate(final String annotationPath) {
    try {
      final File annPath = new File(annotationPath);
      final File old = new File(annotationPath + ".old");
      if (!annPath.renameTo(old)) {
        throw new RuntimeException("rename failed");
      }

      return Files.list(old.toPath())
          .flatMap(f -> {
            try {
              return Files.walk(f);
            } catch (IOException e) {
              throw Throwables.propagate(e);
            }
          })
          .filter(f -> f.toString().contains(".json"))
          .map(f -> {
            try {
              final File file = f.toFile();
              final String id = file.getParentFile().getName();
              final String hd2 = file.getParentFile().getParentFile().getName();
              final String hd1 = file.getParentFile().getParentFile().getParentFile().getName();
              final String annSet = file.getName().substring(0, file.getName().lastIndexOf(".json"));
              Paths.get(annotationPath, annSet, hd1, hd2).toFile().mkdirs();
              Files.move(f, Paths.get(annotationPath, annSet, hd1, hd2, id + ".json"));
//              log.info("{} -> {}", f, Paths.get(annotationPath, annSet, hd1, hd2, id + ".json"));
              return id;
            } catch (IOException e) {
              throw new RuntimeException(e);
            }
          });
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
