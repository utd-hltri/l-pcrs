package edu.utdallas.hltri.scribe.util;

import java.io.File;

import edu.utdallas.hltri.logging.Logger;
import edu.utdallas.hltri.scribe.text.BaseDocument;
import edu.utdallas.hltri.scribe.text.Document;
import gate.persist.PersistenceException;
import gate.persist.SerialDataStore;

/**
 * Created by travis on 7/16/14.
 */
@SuppressWarnings("unused")
public abstract class Corpus {
  protected static final Logger log = Logger.get(Corpus.class);

  /**
   * Contains eager-loading methods
   */
  public static EagerCorpus eager = new EagerCorpus();

  /**
   * Contains lazy-loading methods
   */
//  public static LazyCorpus lazy = new LazyCorpus();

  /**
   * Serializes an Iterable of Documents to the given path
   * @param documents Iterable of Documents to save (serialize)
   * @param path Path of the resultant SerialDataStore
   */
  public static void saveToNewDataStore(final Iterable<? extends Document<?>> documents, final String path) {
    saveToNewDataStore(documents, new File(path));
  }

  /**
   * Serializes an Iterable of Documents to the given path
   *
   * @param documents Iterable of Documents to save (serialize)
   * @param file      File for the resultant SerialDataStore
   */
  public static void saveToNewDataStore(final Iterable<? extends Document<?>> documents, final File file) {
    try {
      final SerialDataStore store = new SerialDataStore();
      store.setStorageDir(file);
      store.create();
      store.open();
      log.info("Created a new datastore at {}", file);

      gate.Document gateDoc;
      for (final Document<?> doc : documents) {
        gateDoc = doc.asGate();
        gateDoc.setDataStore(store);
        gateDoc.sync();
      }

      store.close();
    } catch (PersistenceException | SecurityException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Updates the persisted version of the given document to reflect any in-memory changes
   * These documents must either:
   *   (1) have originated from a data-store or
   *   (2) been previously saved to a data-store
   * @param documents Iterable of Documents to synchronize to the disk
   */
  public static <T extends BaseDocument> void synchronize(final Iterable<Document<T>> documents) {
    for (final Document<T> document: documents) {
      document.sync();
      log.trace("Synchronized {}", document.get(BaseDocument.id));
    }
  }
}

