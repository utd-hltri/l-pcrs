package edu.utdallas.hltri.scribe.io;

import java.io.InputStream;
import java.util.Map;

import edu.utdallas.hltri.scribe.Annotation;
import edu.utdallas.hltri.scribe.Document;

/**
 * Created by trg19 on 8/27/2016.
 */
public abstract class DocumentReader {

  public abstract String readText(long numericId);

  public abstract Map<String, ?> readFeatures(long numericId);

  public abstract Iterable<? extends  Annotation> readAnnotations(long numericId);

  public abstract boolean canRead(long numericId);

  public abstract boolean hasAnnotationSets(long numericId, String... annotationSets);

  public abstract boolean hasAnnotations(long numericId, String annotationSets, Class<? extends Annotation> annotationType);

  public abstract boolean hasAnnotations(long numericId, Class<? extends Annotation> annotationType);

  public abstract <T extends Document>

  public <T extends Document> T read(long numericId) {



  }

}
