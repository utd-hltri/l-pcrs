package edu.utdallas.hltri.scribe.text;

import it.unimi.dsi.fastutil.ints.IntCollection;

import java.nio.file.Path;
import java.util.Iterator;

import edu.utdallas.hltri.scribe.io.DocumentReader;
import edu.utdallas.hltri.scribe.io.DocumentWriter;

/**
 * Created by trg19 on 8/27/2016.
 */
public class Corpus<T> implements Iterable<T> {
  private int size;
  private IntCollection ids;

  private Path path;

  private DocumentReader reader;
  private DocumentWriter writer;

  @Override
  public Iterator<T> iterator() {
    return null;
  }

  public int size() {
    return -1;
  }
}
