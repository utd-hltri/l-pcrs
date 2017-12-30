package edu.utdallas.hlt.trecmed.offline;

import edu.utdallas.hlt.trecmed.framework.App;
import edu.utdallas.hltri.combinatorics.CooccurrenceMeasure;
import edu.utdallas.hltri.io.ExternalCountReader;
import edu.utdallas.hltri.logging.Logger;
import edu.utdallas.hltri.struct.Weighted;
import edu.utdallas.hltri.util.AbstractExpander;
import java.io.BufferedOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;
import java.util.TreeSet;

/**
 *
 * @author travis
 */
public class CooccurrenceMeasurer extends AbstractExpander<CharSequence, Weighted<String>>
    implements Serializable, Closeable {
  private static final long   serialVersionUID = 1L;
  private static       Logger log              = Logger.get(CooccurrenceMeasurer.class);

  private final transient CooccurrenceMeasure measure;
  private final transient ExternalCountReader count;
  private final transient char SEPARATOR = '\t';
  private final transient int N;

  private final Map<String, Collection<Weighted<String>>> expansions = new HashMap<>();

  private final Comparator<Weighted<String>> byWeight = Comparator.<Weighted<String>>comparingDouble(Weighted::getWeight).reversed();

  public CooccurrenceMeasurer(Path file, CooccurrenceMeasure measure) {
    super("Co-occurrence");
    this.measure = measure;
    try {
      this.count = new ExternalCountReader(file.toFile(), SEPARATOR);
    } catch (IOException ex) {
      throw new RuntimeException(ex);
    }

    this.N = (int) count.getCount("__FILE_LENGTH") + 1;
  }

  @SuppressWarnings("SameParameterValue")
  private List<Weighted<String>> expandKeyword(final String key, final int maxSize) {
    final Queue<Weighted<String>> expansion = new PriorityQueue<>(maxSize);
    final String keyword = key.toLowerCase();
    final long x = count.getCount(keyword);

    count.visitDoubleKeyCounts(keyword, (key1, xy) -> {
      final int delim = key1.indexOf(SEPARATOR);
      if (delim < 0) { return; }
      final String otherword = key1.substring(delim + 1);
      final long y = count.getCount(otherword);
      final double score = measure.measure(x, y, xy, N);
      final Weighted<String> word = Weighted.create(score, otherword);

      if (expansion.size() < maxSize) {
        expansion.add(word);
      }

      if (byWeight.compare(word, expansion.peek()) < 0) {
        expansion.poll();
        expansion.add(word);
      }

      log.trace("{} {}::{} = {} (counts of {}::{} with {} together from {} total)",
        measure.getClass().getSimpleName(),
        keyword,
        otherword,
        score,
        x,
        y,
        xy,
        N
      );
    });
    final ArrayList<Weighted<String>> result = new ArrayList<>(expansion);
    result.sort(byWeight);
    return result;
  }

  private Map<String, Collection<Weighted<String>>> expand(Collection<String> keywords) {
    for (String keyword : keywords) {
      expansions.put(keyword, expandKeyword(keyword, 200));
    }
    return Collections.unmodifiableMap(expansions);
  }

  @Override
  public Collection<Weighted<String>> getExpansions(CharSequence term) {
    return expansions.get(term.toString());
  }

  @Override
  public void close() throws IOException {
    this.count.close();
  }

  public static void main(String[] args) throws Exception {
    args = App.init(args);
    final Set<String> keywords = new TreeSet<>(Files.readAllLines(Paths.get(args[1]), Charset.defaultCharset()));
    log.info("Found {} getKeywords.", keywords.size());
    final Class<? extends CooccurrenceMeasure> clazz = Class.forName(args[2]).asSubclass(CooccurrenceMeasure.class);
    final CooccurrenceMeasurer expander = new CooccurrenceMeasurer(Paths.get(args[0]), clazz.newInstance());
    final Map<String, Collection<Weighted<String>>> expansions = expander.expand(keywords);
    try (ObjectOutputStream out =
          new ObjectOutputStream(
          new BufferedOutputStream(
          Files.newOutputStream(
          Paths.get(args[3]))))) {
        out.writeObject(expansions);
    }
  }

}
