package edu.utdallas.hlt.trecmed.offline;

import gnu.trove.iterator.TObjectIntIterator;
import gnu.trove.map.hash.TObjectIntHashMap;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.zip.GZIPInputStream;

import edu.utdallas.hltri.io.ExternalCountWriter;
import edu.utdallas.hltri.logging.Logger;

/**
 *
 * @author travis
 */
public class CooccurrenceCounter {

  private static final Logger log = Logger.get(CooccurrenceCounter.class);

  private static final int DEFAULT_STOPWORDS = 50;
  private static final long serialVersionUID = 5L;
  private final TrieNode root;
  private final Set<String> keywords;
  private final Path corpus;
  private Set<String> stopwords;
  private int numDocs;


  public CooccurrenceCounter(Path corpus) {
    this.root = new TrieNode();
    this.stopwords = new HashSet<>();
    this.keywords = new TreeSet<>();
    this.numDocs = 0;
    this.corpus = corpus;
  }

  public void setKeywords(Iterable<String> keywords) {
    for (String keyword : keywords) {
      TrieNode node = root;

      // Split each keyword into individual key words
      for (String token : keyword.toLowerCase().split(" ")) {
        node = node.getOrAdd(token);
      }

      // Mark the end of each keyword
      node.isKeyword = true;
      node.keyword = keyword;

      this.keywords.add(keyword);
    }
  }

  public List<String> tokenize(String line) throws IOException {
    final StringBuilder sb = new StringBuilder();
    List<String> tokens = new ArrayList<>(128);
    try (BufferedReader sr = new BufferedReader(new StringReader(line))) {
      tokens.clear();
      int current;
      while ((current = sr.read()) != -1) {
        if (Character.isWhitespace(current)) {
          tokens.add(sb.toString());
          sb.setLength(0);
        } else {
          sb.append(Character.toLowerCase((char) current));
        }
      }
      tokens.add(sb.toString());
    }

    return tokens;
  }

  public Set<String> generateStopWords() throws IOException {
    return generateStopWords(DEFAULT_STOPWORDS);
  }

  public Set<String> generateStopWords(int n) throws IOException {
    stopwords = new HashSet<>();
    final TObjectIntHashMap<String> wordFrequencies = new TObjectIntHashMap<>();
    log.debug("Generating stopwords from {}", corpus);

    // Iterate across the documents & count each token
    String line;
    int lines = 0;
    try (BufferedReader reader = new BufferedReader(
            new InputStreamReader(
            new GZIPInputStream(
            Files.newInputStream(corpus))))) {
      while ((line = reader.readLine()) != null) {
        for (final String token : tokenize(line)) {
          wordFrequencies.adjustOrPutValue(token, 1, 1);
        }
        lines++;
        System.out.printf("\r * Processed document %,d", lines);
      }
      System.out.println("\r ** Processed " + lines + " documents.");

      // Generate sorted array of most frequent words
      int[] frequencies = wordFrequencies.values();
      Arrays.sort(frequencies);

      // Get our threshold
      int T = frequencies[Math.max(0, frequencies.length - n - 1)];
      log.debug("Generated stopword threshold of {}.", T);

      // Filter everything over threshold Q1
      for (TObjectIntIterator<String> it = wordFrequencies.iterator(); it.hasNext();) {
        it.advance();
        if (it.value() >= T && stopwords.size() < n) {
          stopwords.add(it.key());
        }
      }
    }
    log.info("Generated {} stopwords.", stopwords.size());
    return Collections.unmodifiableSet(stopwords);
  }

  public void saveStopWords(Path path) throws IOException {
    Files.write(path, new TreeSet<>(stopwords), Charset.defaultCharset());
    log.info("Saved {} stopwords to {}.", stopwords.size(), path);
  }

  public Set<String> loadStopWords(Path path) throws IOException {
    stopwords = new HashSet<>(Files.readAllLines(path, Charset.defaultCharset()));
    log.info("Loaded {} stopwords from {}.", stopwords.size(), path);
    return Collections.unmodifiableSet(stopwords);
  }

  public void generate(int gramSize, final Path output) throws IOException {
    final ExternalCountWriter writer = new ExternalCountWriter(output.toFile(), true);

    Set<String> foundWords = new HashSet<>();
    Set<String> foundKeywords = new HashSet<>();
    List<String> tokens;
    try (BufferedReader reader = new BufferedReader(
            new InputStreamReader(
            new GZIPInputStream(
            Files.newInputStream(corpus))))) {
      for (String line; (line = reader.readLine()) != null;) {
        // Manually tokenize each line
        tokens = tokenize(line);
        foundWords.clear();
        foundKeywords.clear();

        // Iterate over each token
        StringBuilder token = new StringBuilder();
        String tokenString;
        TrieNode node;
        for (int i = 0; i < tokens.size(); i++) {
          token.setLength(0);
          for (int t = i; t < gramSize + i && t < tokens.size(); t++) {
            // Add a space between tokens
            if (t > i) {
              token.append(" ");
            }
            boolean isStopword = stopwords.contains(tokens.get(t));

            // End if the gram begins with a stopword
            if (isStopword && t == i) {
              break;
            }

            // Add this token to the n-gram
            token.append(tokens.get(t));

            // Do not remember this gram if it ends in a stopword
            if (isStopword) {
              continue;
            }

            // Store the gram & count its contexts
            tokenString = token.toString();
            if (foundWords.add(tokenString)) {
              writer.addCount(tokenString, 1);
            }
          }

          // Use the trie to find all getKeywords that start with this token
          node = root;
          int t = i;
          do {
            node = node.getChild(tokens.get(t++));
            if (node == null) {
              break;
            }
            if (node.isKeyword && foundKeywords.add(node.keyword) && !foundWords.
                contains(node.keyword)) {
              writer.addCount(node.keyword, 1);
            }
          } while (node.children.size() > 0 && t < tokens.size());
        }

        // Iterate over all getKeywords <-> word pairs
        for (String keyword : foundKeywords) {
          for (String word : foundWords) {
            writer.addCount(keyword + '\t' + word, 1);
          }
        }

        numDocs++;
        if (numDocs % 10 == 0) {
          System.out.printf("\r * Processed document %,d", numDocs);
        }
      }
      System.out.printf("\r * Processed %,d documents.%n", numDocs);
    }
    writer.addCount("__FILE_LENGTH", numDocs);
    writer.close();

  }

  public long getNumDocs() {
    return this.numDocs;
  }

  private static class TrieNode implements java.io.Serializable {

    private static final long serialVersionUID = 1L;

    final Map<String, TrieNode> children = new HashMap<>();
    boolean isKeyword = false;
    public String keyword;

    public void addChild(String str, TrieNode node) {
      children.put(str, node);
    }

    public TrieNode getChild(String str) {
      TrieNode ret = children.get(str);
      assert ret != this;
      return ret;
    }

    public TrieNode getOrAdd(String str) {
      if (getChild(str) == null) {
        TrieNode node = new TrieNode();
        addChild(str, node);
        return node;
      }
      return getChild(str);
    }
  }


  public static void main(String[] args) throws IOException {
    String command = args[0];
    CooccurrenceCounter gen = new CooccurrenceCounter(Paths.get(args[1]));
    switch (command) {
      case "stopwords": {
        gen.generateStopWords(Integer.parseInt(args[2]));
        gen.saveStopWords(Paths.get(args[3]));
        break;
      }
      case "generate": {
        Path stopwords = Paths.get(args[2]);
        List<String> keywords = Files.readAllLines(Paths.get(args[3]), Charset.defaultCharset());
        int gram = Integer.parseInt(args[4]);
        Path output = Paths.get(args[5]);
        gen.setKeywords(keywords);
        gen.loadStopWords(stopwords);
        gen.generate(gram, output);
        break;
      }
    }
  }
}
