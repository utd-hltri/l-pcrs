
package edu.utdallas.hlt.util;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import edu.utdallas.hlt.framework.Logger;
import edu.utdallas.hlt.framework.LoggerFactory;
import edu.utdallas.hlt.util.ANSIColors.Color;
import edu.utdallas.hlt.util.ANSIColors.Style;
import java.io.*;
import java.util.*;

/**
 *
 * @author travis
 */
public class PhraseMatcher {
  private static final Joiner JOINER = Joiner.on(' ');
  final static Logger log = LoggerFactory.getLogger();

  List<PhraseCollection> phraseCollections = Lists.newArrayList();

  private static class PhraseCollection implements Iterable<String>{
    final Set<String> collection;
    final String name;

    public PhraseCollection(Set<String> collection, String name) {
      this.collection = collection;
      this.name = name;
    }

    @Override public Iterator<String> iterator() {
      return collection.iterator();
    }

    public boolean contains(String phrase) {
      return collection.contains(phrase);
    }
  }

  public PhraseMatcher addSource(String source) {
    return addSource(new File(source));
  }
  public PhraseMatcher addSource(File source) {
    return addSource(source, source.getName());
  }

  public PhraseMatcher addSource(File source, String name) {
    try {
      log.info("Adding phrase collection {} from {}...", name, source);
      Set<String> phrases = Sets.newHashSet();
      BufferedReader reader = new BufferedReader(new FileReader(source));
      for (String line; (line = reader.readLine()) != null; ) {
        phrases.add(line);
      }
      phraseCollections.add(new PhraseCollection(phrases, name));
      log.info("Added phrase collection {} with {} phrases.", name, phrases.size());
      return this;
    } catch (IOException ex) {
      throw new RuntimeException(ex);
    }
  }

  public PhraseMatcher addSource(String source, String name) {
    return addSource(new File(source), name);
  }

  public PhraseMatcher addSource(Iterable<String> source, String name) {
    phraseCollections.add(new PhraseCollection(Sets.newHashSet(source), name));
    return this;
  }

  public static class PhraseMatch {
    final public List<String> phrase;
    final public int startIndex;
    final public int endIndex;
    final public String name;
    final public String source;

    public PhraseMatch(List<String> phrase, String source) {
      this.phrase = phrase;
      this.source = source;
      this.name = JOINER.join(phrase);
      this.startIndex = 0;
      this.endIndex = 0;
    }

    public PhraseMatch(List<String> phrase, String source, int start, int end) {
      this.phrase = phrase;
      this.source = source;
      this.name = JOINER.join(phrase);
      this.startIndex = start;
      this.endIndex = end;
    }
  }

  public Collection<PhraseMatch> getLongestMatches(String... input) {
    return getLongestMatches(Arrays.asList(input));
  }

  public Collection<PhraseMatch> getLongestMatches(List<String> input) {
    Collection<PhraseMatch> results = new HashSet<>();
    for (int end = input.size(); end > 0; end--) {
      for (int start = 0; start < end; start++) {
        PhraseMatch  match = checkMatch(input.subList(start, end), start, end);
        if (match != null) {
          results.add(match);
          end = start;
          start = -1;
        }
      }
    }
    return results;
  }


  public PhraseMatch getLongestMatch(String... phrase) {
    return getLongestMatch(Arrays.asList(phrase));
  }


  public PhraseMatch getLongestMatch(List<String> input) {
    List<String> tokens = Lists.newArrayList(input);
    while (!tokens.isEmpty()) {
      PhraseMatch phraseMatch = checkMatch(tokens);
      if (phraseMatch != null) { return phraseMatch; }
      tokens.remove(0);
    }
    return new PhraseMatch (Collections.EMPTY_LIST, "", 0, 0);
  }

  public PhraseMatch checkMatch(List<String> tokens, int start, int end) {
    String match = JOINER.join(tokens);
    for (PhraseCollection collection : phraseCollections) {
      if (collection.contains(match)) {
        return new PhraseMatch(tokens, collection.name, start, end);
      }
    }
    return null;
  }

  public PhraseMatch checkMatch(List<String> tokens) {
    String match = JOINER.join(tokens);
    for (PhraseCollection collection : phraseCollections) {
      if (collection.contains(match)) {
        return new PhraseMatch(tokens, collection.name, 0, 0);
      }
    }
    return null;
  }

  public static void main(String... args) {
    PhraseMatcher matcher = new PhraseMatcher();
    for (int i = 0; i < args.length; i ++) {
      matcher.addSource(args[i]);
    }
    Console cons = System.console();
    String line;
    while (!(line = cons.readLine(ANSIColors.color(Style.BRIGHT, Color.GREEN, "phrase>"))).equals("exit")) {
      for (PhraseMatch match : matcher.getLongestMatches(line.split(" "))) {
        cons.printf(ANSIColors.color(Style.NORMAL, Color.BLUE, "Match: ") + match.name + "\n");
        cons.printf(ANSIColors.color(Style.NORMAL, Color.BLUE, "Phrase: ") + match.phrase + "\n");
        cons.printf(ANSIColors.color(Style.NORMAL, Color.MAGENTA, "Source: ") + match.source + "\n");
      }
    }
  }
}
