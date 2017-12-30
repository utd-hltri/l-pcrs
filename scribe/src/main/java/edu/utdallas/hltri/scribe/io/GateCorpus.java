package edu.utdallas.hltri.scribe.io;

import java.io.File;
import java.util.stream.Stream;

import edu.utdallas.hltri.scribe.gate.TieredDataStore;
import edu.utdallas.hltri.scribe.text.BaseDocument;
import edu.utdallas.hltri.scribe.text.Document;
import gate.persist.PersistenceException;

/**
 * Created by rmm120030 on 8/18/15.
 */
public class GateCorpus<D extends BaseDocument> extends Corpus<D> {
  private final TieredDataStore tds;

  public GateCorpus(final String serialDataStore) {
    tds = TieredDataStore.at(serialDataStore);
  }

  public GateCorpus(final File serialDataStore) {
    tds = TieredDataStore.at(serialDataStore);
  }

  @Override
  public boolean canLoad(String id) {
    return tds.getResourceFileFromId(id).exists();
  }

  @Override
  protected Document<D> loadDocument(String id) {
    try {
      return Document.fromGate(tds.getDocument(id));
    } catch (PersistenceException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public void save(Document<D> document) {
    try {
      document.asGate().sync();
    } catch (PersistenceException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public Stream<String> getIdStream() {
    try {
      return tds.getDocumentIdStream();
    } catch (PersistenceException e) {
      throw new RuntimeException(e);
    }
  }
}
