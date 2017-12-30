package edu.utdallas.hltri.util;

import com.google.common.base.CharMatcher;
import com.google.common.base.Splitter;
import com.google.common.base.Throwables;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import edu.utdallas.hltri.logging.Logger;

/**
 * Assigns indexes to items, starting from zero.
 * Created with IntelliJ IDEA.
 * User: bryan
 * Date: 12/17/12
 * Time: 6:05 PM
 */
public class IntIdentifier<T> implements Serializable {
  private static final Logger log = Logger.get(IntIdentifier.class);
  private static final long serialVersionUID = 0;

  protected volatile boolean isImmutable;

  /** Efficient thread-safety */
  protected final ReadWriteLock readWriteLock = new ReentrantReadWriteLock();

  protected final Lock read  = readWriteLock.readLock();
  protected final Lock write = readWriteLock.writeLock();

  /** Stores the mapping from IDs to items */
  protected final List<T> items;

  /** Stores the mapping from items to IDs */
//  protected final TObjectIntMap<T> ids;
  protected final Object2IntMap<T> ids;

//  public IntIdentifier(List<T> items, TObjectIntMap<T> ids) {
//    this.items = items;
//    this.ids = ids;
//    this.isImmutable = false;
//  }

  public IntIdentifier(List<T> items, Object2IntMap<T> ids) {
    this.items = items;
    this.ids = ids;
    this.isImmutable = false;
  }

  /** Create a new mapping of items to IDs.  The index of each item in the list
   * will be its ID.
   * @param items
   */
  public IntIdentifier(ArrayList<T> items) {
//    this(items, new TObjectIntHashMap<T>());
    this(items, new Object2IntOpenHashMap<T>());
    for (int id = 0; id < items.size(); id++) {
      ids.put(items.get(id), id);
    }
  }

  /** Items will be added later through getOrAdd() */
  public IntIdentifier() {
//    this(new ArrayList<T>(), new TObjectIntHashMap<T>());
    this(new ArrayList<T>(), new Object2IntOpenHashMap<T>());
  }

  public ImmutibleIntIdenitifer<T> lock() {
    isImmutable = true;
    return new ImmutibleIntIdenitifer<>(items, ids);
  }

  public void set(T item, int id) {
    if (!isImmutable) {
      write.lock();
      try {
        ids.put(item, id);
        for (int i = items.size(); i <= id; i++) {
          items.add(null);
        }
        items.set(id, item);
      } finally {
        write.unlock();
      }
    } else {
      throw new UnsupportedOperationException("Attempt to mutate a locked IntIdentifier.");
    }
  }

//  public IntIdentifier<T> set(T key, int value) {
//    write.lock();
//    try {
//      if (value == items.size()) {
//        items.add(key);
//      } else if (value < items.size()) {
//        items.set(value, key);
//      }
//      ids.put(key, value);
//    } finally {
//      write.unlock();
//    }
//    return this;
//  }

  /** {@inheritDoc} */
  public T get(int id) {
    read.lock();
    try {
      return items.get(id);
    } finally {
      read.unlock();
    }
  }

  /** {@inheritDoc} */
  public int getID(T item) {
    read.lock();
    try {
      if (!ids.containsKey(item)) {
        return -1;
      }
      return ids.get(item);
    } finally {
      read.unlock();
    }
  }

  @Deprecated
  public int getID(T item, boolean addIfAbsent) {
    if (addIfAbsent) {
      return getIDOrAdd(item);
    } else {
      return getID(item);
    }
  }

  /** Retrieves the id for an item, or assigns a new id to the item and returns
   * that (thread-safe).
   * @param item
   * @return
   */
  public int getIDOrAdd(T item) {
    read.lock();
    try {
      if (ids.containsKey(item)) {
        return ids.get(item);
      }
    } finally {
      read.unlock();
    }
    return add(item);
  }

  public int addIfMissing(T item) {
    read.lock();
    try {
      if (ids.containsKey(item)) {
        //TODO: find fastutil equivalent of Constants.DEFAULT_INT_NO_ENTRY_VALUE
        return -1;
      }
    } finally {
      read.unlock();
    }
    return add(item);
  }

  public int add(T item) {
    if (!isImmutable) {
      write.lock();
      try {
        final int id = items.size();
        ids.put(item, id);
        items.add(item);
        return id;
      } finally {
        write.unlock();
      }
    } else {
      throw new UnsupportedOperationException("Attempt to mutate a locked IntIdentifier.");
    }
  }

  public Set<T> getItems() {
    read.lock();
    try {
      final Set<T> items = new HashSet<>();
      ids.keySet().forEach(items::add);
      return items;
    } finally {
      read.unlock();
    }
  }

  public boolean isLocked() {
    return this.isImmutable;
  }

  /**
   * The number of items currently being tracked by the identifier.  This
   * can change over time.
   */
  public int size() {
    read.lock();
    try {
      return items.size();
    } finally {
      read.unlock();
    }
  }

  public void toFile(final Path path) {
    toFile(path, false);
  }

  public void toFile(final Path path, boolean startAtOne) {
    try (final BufferedWriter writer = Files.newBufferedWriter(path)) {
      for (int i = 0; i < items.size(); i++) {
        final T item = items.get(i);
        if (item == null) {
          log.warn("Feature {} was NULL!", i);
        }
        writer.append(Integer.toString(startAtOne ? i + 1 : i))
            .append('\t')
            .append(Objects.toString(item));
        writer.newLine();
      }
//      ids.forEachEntry((key, id) -> {
//        try {
//          writer.append(Integer.toString(id));
//          writer.append('\t');
//          writer.append(key.toString());
//          writer.newLine();
//        } catch (IOException e) {
//          Throwables.propagate(e);
//        }
//        return true;
//      });
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public void toFile(final String file) {
    toFile(Paths.get(file));
  }

  public static IntIdentifier<String> fromFile(final Path intIdentifierFile) {
    final IntIdentifier<String> identifier = new IntIdentifier<>();
    final Splitter splitter = Splitter.on(CharMatcher.WHITESPACE).omitEmptyStrings();
    final File file = intIdentifierFile.toFile();
    if (file.exists()) {
      try (final BufferedReader reader = new BufferedReader(new FileReader(file))) {
        String line;
        while ((line = reader.readLine()) != null) {
          if (line.length() > 0) {
            final List<String> list = splitter.splitToList(line);
            if (list.size() > 1) {
              identifier.set(list.get(1), Integer.parseInt(list.get(0)));
            }
          }
        }
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }
    else {
      throw new IllegalArgumentException("No IntIdentifier map file at " + intIdentifierFile);
    }
    log.info("Initialized IntIdentifier of size {} from map at {}", identifier.size(), intIdentifierFile);
    return identifier;
  }

  public static IntIdentifier<String> fromFile(final String intIdentifierFile) {
    return fromFile(Paths.get(intIdentifierFile));
  }
}
