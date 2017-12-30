package edu.utdallas.hltri.scribe.util;

import com.google.common.base.Throwables;
import com.google.common.collect.Lists;

import java.io.File;
import java.util.List;

import edu.utdallas.hltri.logging.Logger;
import edu.utdallas.hltri.scribe.gate.GateUtils;
import edu.utdallas.hltri.scribe.text.BaseDocument;
import edu.utdallas.hltri.scribe.text.Document;
import edu.utdallas.hltri.util.Unsafe;
import gate.persist.PersistenceException;
import gate.persist.SerialDataStore;

public class EagerCorpus {

  private static final Logger log = Logger.get(Corpus.class);

  public static class SerializationException extends RuntimeException {
    static final long serialVersionUID = 1l;

    /**
     * Constructs a new runtime exception with {@code null} as its
     * detail message.  The cause is not initialized, and may subsequently be
     * initialized by a call to {@link #initCause}.
     */
    public SerializationException() {
    }

    /**
     * Constructs a new runtime exception with the specified detail message.
     * The cause is not initialized, and may subsequently be initialized by a
     * call to {@link #initCause}.
     *
     * @param message the detail message. The detail message is saved for
     *                later retrieval by the {@link #getMessage()} method.
     */
    public SerializationException(String message) {
      super(message);
    }

    /**
     * Constructs a new runtime exception with the specified detail message and
     * cause.  <p>Note that the detail message associated with
     * {@code cause} is <i>not</i> automatically incorporated in
     * this runtime exception's detail message.
     *
     * @param message the detail message (which is saved for later retrieval
     *                by the {@link #getMessage()} method).
     * @param cause   the cause (which is saved for later retrieval by the
     *                {@link #getCause()} method).  (A <tt>null</tt> value is
     *                permitted, and indicates that the cause is nonexistent or
     *                unknown.)
     * @since 1.4
     */
    public SerializationException(String message, Throwable cause) {
      super(message, cause);
    }

    /**
     * Constructs a new runtime exception with the specified cause and a
     * detail message of <tt>(cause==null ? null : cause.toString())</tt>
     * (which typically contains the class and detail message of
     * <tt>cause</tt>).  This constructor is useful for runtime exceptions
     * that are little more than wrappers for other throwables.
     *
     * @param cause the cause (which is saved for later retrieval by the
     *              {@link #getCause()} method).  (A <tt>null</tt> value is
     *              permitted, and indicates that the cause is nonexistent or
     *              unknown.)
     * @since 1.4
     */
    public SerializationException(Throwable cause) {
      super(cause);
    }

    /**
     * Constructs a new runtime exception with the specified detail
     * message, cause, suppression enabled or disabled, and writable
     * stack trace enabled or disabled.
     *
     * @param message            the detail message.
     * @param cause              the cause.  (A {@code null} value is permitted,
     *                           and indicates that the cause is nonexistent or unknown.)
     * @param enableSuppression  whether or not suppression is enabled
     *                           or disabled
     * @param writableStackTrace whether or not the stack trace should
     *                           be writable
     * @since 1.7
     */
    public SerializationException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
      super(message, cause, enableSuppression, writableStackTrace);
    }
  }

  public <T extends BaseDocument> List<Document<T>> fromDataStore(final String file) {
    return fromDataStore(new File(file));
  }

  public <T extends BaseDocument> List<Document<T>> fromDataStore(final File file) {
    GateUtils.init();

    try {
      log.debug("Opening serial data store at {}...", file);
      SerialDataStore store = new SerialDataStore();
      store.setStorageDir(file);
      store.open();

      final List<String> ids = Unsafe.cast(store.getLrIds("gate.corpora.DocumentImpl"));
      final List<Document<T>> docs = Lists.newArrayListWithCapacity(ids.size());
      for (String id : ids) {
        docs.add(Document.fromGate((gate.Document) store.getLr("gate.corpora.DocumentImpl", id)));
      }
      store.close();
      log.info("Loaded {} documents from {}", docs.size(), file);
      return docs;
    } catch (PersistenceException ex) {
      throw new SerializationException(ex);
    } catch (SecurityException ex) {
      throw Throwables.propagate(ex);
    }
  }
}
