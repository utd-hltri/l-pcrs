
package edu.utdallas.hlt.trecmed.framework;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.Lists;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ExecutionException;

import edu.utdallas.hlt.genia_wrapper.GeniaTokenReaderAndWriter;
import edu.utdallas.hlt.i2b2.Concept;
import edu.utdallas.hlt.text.Document;
import edu.utdallas.hlt.text.Gender;
import edu.utdallas.hlt.text.io.XMLDocumentReader;
import edu.utdallas.hlt.text.io.xml.HedgeSpanReaderAndWriter;
import edu.utdallas.hlt.text.io.xml.NegationSpanReaderAndWriter;
import edu.utdallas.hlt.trecmed.Topic;
import edu.utdallas.hlt.util.Config;
import edu.utdallas.hlt.util.Place;
import edu.utdallas.hltri.logging.Logger;

/**
 *
 * @author travis
 */
public class App {
  private static final Logger log = Logger.get(App.class);

  public static String[] init (String... args) {
    try {
      return Config.init(App.class.getPackage().getName(), args);
    } catch (IOException ex) {
      throw new RuntimeException(ex);
    }
  }

  public static void clearReportCache() {
    reports.invalidateAll();
  }

  private final static LoadingCache<Place, Document> reports = CacheBuilder.newBuilder()
      .concurrencyLevel(16)
    .maximumSize(1000)
    .build(
      new CacheLoader<Place, Document>() {
        private final XMLDocumentReader reader = new XMLDocumentReader(
                Concept.getXMLAnnotationReader(),
                Gender.getXMLAnnotationReader(),
                new HedgeSpanReaderAndWriter(),
                new NegationSpanReaderAndWriter()
        );
        @Override public Document load(Place place) {
          try {
            return reader.read(place);
          } catch (IOException ex) {
            log.error("Failed to read document: " + ex);
            return new Document("");
          }
        }
      });

  public static Document readReport(Place place) {
    try {
      return reports.get(place);
    } catch (ExecutionException ex) {
      log.error("Failed to read document: " + ex);
      return new Document("");
    }
  }

  public static List<Topic> readQuestion(String path) {
    App.init();
    try {
      Collection<Document> docs = new XMLDocumentReader(
              Concept.getXMLAnnotationReader(),
              Gender.getXMLAnnotationReader(),
              new GeniaTokenReaderAndWriter(),
              new HedgeSpanReaderAndWriter(),
              new NegationSpanReaderAndWriter())
              .readAll(Place.fromFile(new File(path)));
      List<Topic> topics = Lists.newArrayList();
      for (Document doc : docs) {
        topics.add(new Topic(doc));
      }
      return topics;
    } catch (IOException ex) {
      log.error("Failed to read document: " + ex);
      throw new RuntimeException(ex);
    }
  }
}
