package edu.utdallas.hltri.scribe.io;

import com.google.common.base.Throwables;
import edu.utdallas.hltri.logging.Logger;
import edu.utdallas.hltri.scribe.text.BaseDocument;
import edu.utdallas.hltri.scribe.text.Document;
import edu.utdallas.hltri.scribe.util.TieredHashing;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

/**
 * Created by rmm120030 on 1/7/16.
 */
public abstract class AnaphoraCorpus<D extends BaseDocument> extends Corpus<D> {
  private static final Logger log = Logger.get(AnaphoraCorpus.class);
  protected final String textPath;
  protected final String annotationPath;
  protected boolean tiered = false;

  protected AnaphoraCorpus(final String textPath, final String annotationPath) {
    this.textPath = textPath;
    this.annotationPath = annotationPath;
  }

  @Override
  protected Document<D> loadDocument(String id) {
    try (final BufferedReader textReader = new BufferedReader(new FileReader(getTextDir(id).resolve(id + ".txt").toFile()))) {
      log.trace("Document: {}", id);
      final StringBuilder sb = new StringBuilder();
      String line;
      for (int i = 0; (line = textReader.readLine()) != null; i++) {
        if (i > 0) {
          sb.append("\n");
        }
        sb.append(line);
      }
      final Document<D> document = Document.fromString(sb.toString());
      document.set(BaseDocument.id, id);
      log.debug("Attaching annotations to doc {}", id);
      attachAnnotations(document);

      return document;
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public void save(Document<D> document) {
    final File textFile = getTextDir(document).resolve(document.get(BaseDocument.id) + ".txt").toFile();
    if (!textFile.exists()) {
      try (final BufferedWriter writer = new BufferedWriter(new FileWriter(textFile))) {
        writer.write(document.asString());
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }
    writeAnnotations(document);
  }

  @Override
  public Stream<String> getIdStream() {
    try {
      log.info("Reading ids from {}", textPath);
      return Files.list(Paths.get(textPath))
          .flatMap(f -> {
            try {
              return Files.walk(f);
            } catch (IOException e) {
              throw Throwables.propagate(e);
            }
          })
          .filter(f -> !f.toString().contains("~"))
          .map(f -> {
            if (f.getFileName().toString().endsWith(".txt"))
              return f.getFileName().toString().substring(0, f.getFileName().toString().length() - 4);
            else
              return f.getFileName().toString();
          });
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public boolean canLoad(String id) {
    return Files.exists(getTextDir(id).resolve(id + ".txt"))
        && Files.exists(getAnnotationDir(id).resolve(id + ".xml"));
  }

  protected Path getTextDir(final Document<D> document) {
    return getTextDir(document.get(BaseDocument.id));
  }

  protected Path getTextDir(final String id) {
    if (tiered) {
      final Path dir = Paths.get(textPath, TieredHashing.getHashDirsAsString(id));
      if (!dir.toFile().exists()) {
        dir.toFile().mkdirs();
        dir.toFile().mkdir();
      }
      return dir;
    }
    return Paths.get(textPath);
  }

  protected Path getAnnotationDir(final Document<D> document) {
    return getAnnotationDir(document.get(BaseDocument.id));
  }

  protected Path getAnnotationDir(final String id) {
    if (tiered) {
      final Path dir = Paths.get(annotationPath, TieredHashing.getHashDirsAsString(id));
      if (!dir.toFile().exists()) {
        dir.toFile().mkdirs();
        dir.toFile().mkdir();
      }
      return dir;
    }
    return Paths.get(annotationPath);
  }

  protected abstract void writeAnnotations(Document<D> document);

  protected abstract void attachAnnotations(Document<D> document);
}
