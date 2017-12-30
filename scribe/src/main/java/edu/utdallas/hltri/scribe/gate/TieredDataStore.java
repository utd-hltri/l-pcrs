package edu.utdallas.hltri.scribe.gate;

import com.google.common.base.Throwables;
import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Stream;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import edu.utdallas.hltri.io.IOUtils;
import edu.utdallas.hltri.logging.Logger;
import edu.utdallas.hltri.scribe.text.BaseDocument;
import edu.utdallas.hltri.scribe.text.Document;
import edu.utdallas.hltri.scribe.util.TieredHashing;
import edu.utdallas.hltri.util.Unsafe;
import gate.Corpus;
import gate.Gate;
import gate.LanguageResource;
import gate.corpora.SerialCorpusImpl;
import gate.creole.ResourceData;
import gate.event.DatastoreEvent;
import gate.persist.GateAwareObjectInputStream;
import gate.persist.PersistenceException;
import gate.persist.SerialDataStore;

/**
 * Created by travis on 2/20/15.
 */
public class TieredDataStore extends SerialDataStore {

  private static final long serialVersionUID = 1l;

  private static final Logger log = Logger.get(TieredDataStore.class);

  public static TieredDataStore at(String path) {
    return at(new File(path));
  }

  public static TieredDataStore at(File path) {
    GateUtils.init();
    TieredDataStore store = new TieredDataStore();
    store.setStorageDir(path);
    try {
      if (!path.exists()) {
        store.create();
        log.info("Created a new datastore at {}", path);
      } else {
        log.info("Opened existing datastore at {}", path);
      }
      store.open();
    } catch (PersistenceException e) {
      throw Throwables.propagate(e);
    }
    return store;
  }

  public static File getResourceFileFromId(File resourceTypeDirectory, String lrPersistenceId) {
    return TieredHashing.tieredFile(resourceTypeDirectory.getAbsolutePath(), lrPersistenceId,
                                    ".ser");
  }

  public void saveAndDelete(gate.Document doc) {
    try {
      if (doc.getDataStore() != this) {
        doc.setDataStore(this);
      }
      doc.sync();
    } catch (PersistenceException e) {
      throw Throwables.propagate(e);
    } finally {
      gate.Factory.deleteResource(doc);
    }
  }

  public void saveAndDelete(Document<? extends BaseDocument> doc) {
    saveAndDelete(doc.asGate());
  }

  @Override public List<String> getLrIds(String lrType) throws PersistenceException {
    return Lists.newArrayList(getLrIdIterator(lrType));
  }

  public long size() {
    try {
      return Files.walk(this.getStorageDir().toPath()).parallel()
            .filter(f -> !(Files.isDirectory(f)))
            .count();
    } catch (IOException e) {
      throw Throwables.propagate(e);
    }
  }

  public Stream<String> getDocumentIds(int shards, int shard) throws PersistenceException  {
    try {
      return Files.list(storageDir.toPath().resolve("gate.corpora.DocumentImpl"))
          .filter(f -> Integer.parseInt(f.getFileName().toString(), 16) % shards == shard - 1)
          .flatMap(f -> {
            try {
              return Files.walk(f);
            } catch (IOException e) {
              throw Throwables.propagate(e);
            }
          })
          .filter(f -> f.toString().endsWith(".ser"))
          .map(f -> {
            String s = f.getFileName().toString();
            return s.substring(0, s.length() - 4);
          });
    } catch (IOException e) {
      throw new PersistenceException(e);
    }
  }

  public Stream<String> getDocumentIdStream() throws PersistenceException  {
    try {
      return Files.list(storageDir.toPath().resolve("gate.corpora.DocumentImpl"))
          .flatMap(f -> { try { return Files.walk(f); } catch (IOException e) { throw Throwables.propagate(e); } })
          .filter(f -> f.toString().endsWith(".ser"))
          .map(f -> { String s = f.getFileName().toString(); return s.substring(0, s.length() - 4); });
    } catch (IOException e) {
      throw new PersistenceException(e);
    }
  }

  public Iterator<String> getDocumentIds() throws PersistenceException  {
    return getLrIdIterator("gate.corpora.DocumentImpl");
  }



  public Iterator<String> getLrIdIterator(String lrType) throws PersistenceException {
    final File resourceTypeDir = new File(storageDir, lrType);
    final Iterator<File> fileIt =  IOUtils.lazy.iterateWithSuffix(resourceTypeDir, ".ser").iterator();
    return Iterators.transform(fileIt, file -> IOUtils.removeExtension(file.getName()));
  }

  @Override public void sync(LanguageResource lr) throws PersistenceException {
    // check that this LR is one of ours (i.e. has been adopted)
    if(lr.getDataStore() == null || ! lr.getDataStore().equals(this))
      throw new PersistenceException(
          "LR " + lr.getName() + " has not been adopted by this DataStore"
      );

    // find the resource data for this LR
    ResourceData lrData =
        Gate.getCreoleRegister().get(lr.getClass().getName());

    // create a subdirectory for resources of this type if none exists
    File resourceTypeDirectory = new File(storageDir, lrData.getClassName());
    if(
        (! resourceTypeDirectory.exists()) ||
        (! resourceTypeDirectory.isDirectory())
        ) {
      // try to create the directory, throw an exception if it does not
      // exist after this attempt.  It is possible for mkdir to fail and exists
      // still to return true if another thread managed to sneak in and
      // create the directory in the meantime
      if(! resourceTypeDirectory.mkdirs() && ! resourceTypeDirectory.exists())
        throw new PersistenceException("Can't write " + resourceTypeDirectory);
    }

    // create an indentifier for this resource
    String lrName = null;
    Object lrPersistenceId = null;
    lrName = lr.getName();
    lrPersistenceId = lr.getLRPersistenceId();

    if(lrName == null)
      lrName = lrData.getName();
    if(lrPersistenceId == null) {
      lrPersistenceId = constructPersistenceId(lrName);
      lr.setLRPersistenceId(lrPersistenceId);
    }

    //we're saving a corpus. I need to save its documents first
    if (lr instanceof Corpus) {
      //check if the corpus is the one we support. CorpusImpl cannot be saved!
      if (! (lr instanceof SerialCorpusImpl))
        throw new PersistenceException("Can't save a corpus which " +
                                       "is not of type SerialCorpusImpl!");
      SerialCorpusImpl corpus = (SerialCorpusImpl) lr;
      //this is a list of the indexes of all newly-adopted documents
      //which will be used by the SerialCorpusImpl to update the
      //corresponding document IDs
      for (int i = 0; i < corpus.size(); i++) {
        //if the document is not in memory, there's little point in saving it
        if ( (!corpus.isDocumentLoaded(i)) && corpus.isPersistentDocument(i))
          continue;
        log.debug("Saving document at position " + i);
        log.debug("Document in memory " + corpus.isDocumentLoaded(i));
        log.debug("is persistent? " + corpus.isPersistentDocument(i));
        log.debug("Document name at position" + corpus.getDocumentName(i));
        gate.Document doc = corpus.get(i);
        try {
          //if the document is not already adopted, we need to do that first
          if (doc.getLRPersistenceId() == null) {
            log.debug("Document adopted" + doc.getName());
            doc = (gate.Document) this.adopt(doc);
            this.sync(doc);
            log.debug("Document sync-ed");
            corpus.setDocumentPersistentID(i, doc.getLRPersistenceId());
          } else{
            //if it is adopted, just sync it
            this.sync(doc);
            log.debug("Document sync-ed");
          }
          // store the persistent ID. Needs to be done even if the document was
          //already adopted, in case the doc was already persistent
          //when added to the corpus
          corpus.setDocumentPersistentID(i, doc.getLRPersistenceId());
          log.debug("new document ID " + doc.getLRPersistenceId());
        } catch (Exception ex) {
          throw new PersistenceException("Error while saving corpus: "
                                         + corpus
                                         + "because of an error storing document "
                                         + ex.getMessage(), ex);
        }
      }//for loop through documents
    }

    // create a File to store the resource in
    File resourceFile = getResourceFileFromId(resourceTypeDirectory, (String) lrPersistenceId);

    // dump the LR into the new File
    try (OutputStream os = new BufferedOutputStream(new GZIPOutputStream(new FileOutputStream(resourceFile)));
         ObjectOutputStream oos = new ObjectOutputStream(os)) {
      oos.writeObject(lr);
    } catch(IOException e) {
      throw new PersistenceException("Couldn't write to storage file: " + e.getMessage(),e);
    }

    // let the world know about it
    fireResourceWritten(
        new DatastoreEvent(
            this, DatastoreEvent.RESOURCE_WRITTEN, lr, lrPersistenceId
        )
    );
  }

  public File getResourceFileFromId(String lrPersistenceId) {
    return getResourceFileFromId(new File(storageDir, "gate.corpora.DocumentImpl"), lrPersistenceId);
  }

  public <T extends LanguageResource> T load(Class<T> clazz, Object lrPersistenceId) {
    try {
      return Unsafe.cast(getLr(clazz.getName(), lrPersistenceId));
    } catch (PersistenceException e) {
      throw Throwables.propagate(e);
    }
  }


  public gate.Document getDocument(Object lrid) throws PersistenceException {
    return (gate.Document) getLr("gate.corpora.DocumentImpl", lrid);
  }

  @Override
  public LanguageResource getLr(String lrClassName, Object lrPersistenceId)
      throws PersistenceException, SecurityException {


    // find the subdirectory for resources of this type
    File resourceTypeDirectory = new File(storageDir, lrClassName);
    if(
        (! resourceTypeDirectory.exists()) ||
        (! resourceTypeDirectory.isDirectory())
        ) {
      throw new PersistenceException("Can't find " + resourceTypeDirectory);
    }

    // create a File to representing the resource storage file
    File resourceFile = getResourceFileFromId(resourceTypeDirectory, lrPersistenceId.toString());
    if(! resourceFile.exists() || ! resourceFile.isFile())
      throw new PersistenceException("Can't find file " + resourceFile);

    // try and read the file and deserialise it
    LanguageResource lr = null;
    try (InputStream is = new BufferedInputStream(new GZIPInputStream(new FileInputStream(resourceFile)));
         ObjectInputStream ois = new GateAwareObjectInputStream(is)) {
      lr = (LanguageResource) ois.readObject();
    } catch(IOException e) {
      throw new PersistenceException("Couldn't read file "+resourceFile+": "+e);
    } catch(ClassNotFoundException ee) {
      throw new PersistenceException("Couldn't find class "+lrClassName+": "+ee);
    }

    // set the dataStore property of the LR (which is transient and therefore
    // not serialised)
    lr.setDataStore(this);
    lr.setLRPersistenceId(lrPersistenceId);

    log.trace("LR read in memory: " + lr);

    return lr;
  }
}
