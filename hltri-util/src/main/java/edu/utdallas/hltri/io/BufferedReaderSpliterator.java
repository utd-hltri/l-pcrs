package edu.utdallas.hltri.io;

import java.io.BufferedReader;
import java.util.function.Consumer;

/**
 * Class for batched line reading
 */
public class BufferedReaderSpliterator extends FixedBatchSpliteratorBase<String> {
  private final BufferedReader br;

  BufferedReaderSpliterator(BufferedReader cr, int batchSize) {
    super(IMMUTABLE | ORDERED | NONNULL, batchSize);
    if (cr == null) throw new NullPointerException("CSVReader is null");
    this.br = cr;
  }

  @Override public boolean tryAdvance(Consumer<? super String> action) {
    if (action == null) throw new NullPointerException();
    try {
      final String row = br.readLine();
      if (row == null) return false;
      action.accept(row);
      return true;
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @Override public void forEachRemaining(Consumer<? super String> action) {
    if (action == null) throw new NullPointerException();
    try {
      for (String row; (row = br.readLine()) != null;) action.accept(row);
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}
