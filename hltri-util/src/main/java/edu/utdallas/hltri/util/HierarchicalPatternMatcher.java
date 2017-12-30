package edu.utdallas.hltri.util;

import com.google.common.base.Throwables;
import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;

import com.googlecode.concurrenttrees.radix.node.concrete.DefaultCharArrayNodeFactory;
import com.googlecode.concurrenttrees.radixinverted.ConcurrentInvertedRadixTree;
import com.googlecode.concurrenttrees.radixinverted.InvertedRadixTree;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import edu.utdallas.hltri.logging.Logger;

/**
 * Created by travis on 8/2/16.
 */
@SuppressWarnings({"unused", "WeakerAccess"})
public abstract class HierarchicalPatternMatcher {
  private final static Logger log = Logger.get(HierarchicalPatternMatcher.class);

  protected final InvertedRadixTree<Collection<String>> patterns = new ConcurrentInvertedRadixTree<>(new DefaultCharArrayNodeFactory());
  protected final Collection<String> labels = Sets.newConcurrentHashSet();

  private int size = 0;

  public void addPattern(String pattern, Collection<String> path) {
    patterns.put(formatContext(pattern), path);
    labels.addAll(path);
    size++;
  }


  protected static String formatContext(CharSequence context) {
    return '|' + PatternMatcher.formatter.trimAndCollapseFrom(context, '|') + '|';
  }

  public boolean hasMatchWithin(CharSequence context) {
      return !Iterables.isEmpty(patterns.getKeysContainedIn(formatContext(context)));
  }

  public Iterable<String> getMatchesWithin(CharSequence context) {
    return StreamSupport.stream(patterns.getKeysContainedIn(formatContext(context)).spliterator(), false).map(s -> PatternMatcher.unformatter.trimAndCollapseFrom(s, ' ')).collect(Collectors.toList());
  }

  public int getNumMatchesWithin(CharSequence context) {
    return Iterables.size(patterns.getKeysContainedIn(formatContext(context)));
  }

  public int getNumLabelsWithin(CharSequence context) {
    return Iterables.size(getLabelsWithin(context));
  }

  public Iterable<String> getLabelsWithin(CharSequence context) {
    return Iterables.concat(patterns.getValuesForKeysContainedIn(formatContext(context)));
  }

  public final void toTSV(Path file) {
    final List<String> lines = StreamSupport.stream(patterns.getKeyValuePairsForKeysStartingWith("|").spliterator(), false)
                                            .map(p -> PatternMatcher.unformatter.trimAndCollapseFrom(p.getKey(), ' ')
                                                + '\t' + p.getValue().stream().collect(Collectors.joining(" ")))
                                            .collect(Collectors.toList());
    try {
      Files.write(file, lines);
    } catch (IOException e) {
      throw Throwables.propagate(e);
    }
  }
}
