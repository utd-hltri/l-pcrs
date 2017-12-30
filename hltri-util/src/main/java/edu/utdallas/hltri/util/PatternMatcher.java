package edu.utdallas.hltri.util;

import com.google.common.base.CharMatcher;
import com.google.common.base.Splitter;
import com.google.common.base.Throwables;
import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;

import com.googlecode.concurrenttrees.radix.node.concrete.DefaultCharArrayNodeFactory;
import com.googlecode.concurrenttrees.radixinverted.ConcurrentInvertedRadixTree;
import com.googlecode.concurrenttrees.radixinverted.InvertedRadixTree;
import edu.utdallas.hltri.logging.Logger;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * Created by travis on 8/2/16.
 */
public class PatternMatcher {
  private final static Logger log = Logger.get(PatternMatcher.class);

  protected static final Splitter fieldSplitter = Splitter.on('\t').omitEmptyStrings().limit(2);

  protected static CharMatcher formatter = CharMatchers.PUNCTUATION.or(CharMatcher.WHITESPACE).precomputed();
  protected static CharMatcher unformatter = CharMatcher.is('|').precomputed();

  protected final InvertedRadixTree<String> patterns = new ConcurrentInvertedRadixTree<>(new DefaultCharArrayNodeFactory());
  protected final Set<String> labels = Sets.newConcurrentHashSet();

  private int size = 0;



  public void addPattern(String pattern) {
    addPattern(pattern, Integer.toString(size + 1));
  }

  public void addPattern(String pattern, String label) {
    patterns.put(formatContext(pattern), label);
    size++;
    labels.add(label);
  }


  public static PatternMatcher empty() {
    return new PatternMatcher();
  }

  public static PatternMatcher fromStrings(String... patterns) {
    return fromStrings(Arrays.asList(patterns));
  }

  public static PatternMatcher fromStrings(Iterable<String> patterns) {
    final PatternMatcher matcher = new PatternMatcher();
    for (String pattern : patterns) {
      matcher.addPattern(pattern);
    }
    return matcher;
  }

  public static PatternMatcher fromPath(Path path) {
      final PatternMatcher matcher = new PatternMatcher();
      try (BufferedReader reader = Files.newBufferedReader(path)) {
        for (String line; (line = reader.readLine()) != null; ) {
          if (line.charAt(0) != '#' && !line.isEmpty()) {
            int delim = line.indexOf('\t');
            if (delim < 0) {
              matcher.addPattern(line);
            } else {
              // Using a splitter instead of sub-strings allows us to handle consecutive tabs
              final List<String> fields = fieldSplitter.splitToList(line);
              final String key = fields.get(0);
              final String label = fields.get(1).trim();
              matcher.addPattern(key, label);
            }
          }
        }
        log.debug("Loaded {} patterns for {} labels from {}", matcher.size, matcher.labels.size(), path);
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
      return matcher;
    }

  public static PatternMatcher fromFile(File file) {
    return fromPath(file.toPath());
  }

  public static PatternMatcher fromFile(String file) {
    return fromPath(Paths.get(file));
  }

  protected static String formatContext(CharSequence context) {
    return '|' + formatter.trimAndCollapseFrom(context, '|') + '|';
  }

  public boolean hasMatchWithin(CharSequence context) {
      return !Iterables.isEmpty(patterns.getKeysContainedIn(formatContext(context)));
  }

  public Iterable<String> getMatchesWithin(CharSequence context) {
    return Iterables.transform(patterns.getKeysContainedIn(formatContext(context)), s -> unformatter.trimAndCollapseFrom(s, ' '));
  }

  public int getNumMatchesWithin(CharSequence context) {
    return Iterables.size(patterns.getKeysContainedIn(formatContext(context)));
  }

  public int getNumLabelsWithin(CharSequence context) {
    return Iterables.size(getLabelsWithin(context));
  }

  public Iterable<String> getLabelsWithin(CharSequence context) {
    return patterns.getValuesForKeysContainedIn(formatContext(context));
  }

  public final void toTSV(Path file) {
    final List<String> lines = StreamSupport.stream(patterns.getKeyValuePairsForKeysStartingWith("|").spliterator(), false)
                                            .map(p -> unformatter.trimAndCollapseFrom(p.getKey(), ' ') + '\t' + p.getValue())
                                            .collect(Collectors.toList());
    try {
      Files.write(file, lines);
    } catch (IOException e) {
      throw Throwables.propagate(e);
    }
  }
}
