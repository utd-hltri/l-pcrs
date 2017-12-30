package edu.utdallas.hltri.eeg;

import com.google.common.collect.Sets;

import java.io.*;
import java.util.Collection;
import java.util.Iterator;
import java.util.Set;

/**
 * Created by ramon on 2/16/16.
 */
public class SerializedSet implements AutoCloseable, Set<String> {
  private final Set<String> set;
  private BufferedWriter writer = null;

  public SerializedSet(final String outFile) throws IOException {
    set = Sets.newHashSet();
    final File file = new File(outFile);
    if (!file.exists()) file.createNewFile();
    try (final BufferedReader reader = new BufferedReader(new FileReader(outFile))) {
      String line;
      while ((line = reader.readLine()) != null) {
        set.add(line);
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    writer = new BufferedWriter(new FileWriter(outFile, true));
  }

  @Override
  public int size() {
    return set.size();
  }

  @Override
  public boolean isEmpty() {
    return set.isEmpty();
  }

  @Override
  public boolean contains(Object o) {
    return set.contains(o);
  }

  @Override
  public Iterator<String> iterator() {
    return set.iterator();
  }

  @Override
  public Object[] toArray() {
    return set.toArray();
  }

  @Override
  public <T1> T1[] toArray(T1[] a) {
    return set.toArray(a);
  }

  @Override
  public boolean add(String t) {
    if (!set.contains(t)) {
      try {
        writer.write(t);
        writer.newLine();
        writer.flush();
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }
    return set.add(t);
  }

  @Override
  public boolean remove(Object o) {
    return set.remove(o);
  }

  @Override
  public boolean containsAll(Collection<?> c) {
    return set.containsAll(c);
  }

  @Override
  public boolean addAll(Collection<? extends String> c) {
    return set.addAll(c);
  }

  @Override
  public boolean retainAll(Collection<?> c) {
    return set.retainAll(c);
  }

  @Override
  public boolean removeAll(Collection<?> c) {
    return set.removeAll(c);
  }

  @Override
  public void clear() {
    set.clear();
  }

  @Override
  public void close() throws IOException {
    writer.close();
  }
}
