package edu.utdallas.hltri.scribe.util;

import com.google.common.base.CharMatcher;
import com.google.common.base.Splitter;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multiset;
import com.google.common.collect.Sets;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

import edu.utdallas.hltri.conf.Config;
import edu.utdallas.hltri.logging.Logger;
import edu.utdallas.hltri.scribe.io.JsonCorpus;
import edu.utdallas.hltri.scribe.text.BaseDocument;
import edu.utdallas.hltri.scribe.text.annotation.Token;

/**
 * Created by rmm120030 on 1/25/16.
 */
public class BrownClusters {
  private static final Splitter splitter = Splitter.on(CharMatcher.WHITESPACE).omitEmptyStrings();
  private static final Logger log = Logger.get(BrownClusters.class);

  private final List<String> names;
  private final Map<String, ClusterMap> clusters;
  private final String[] clusterFiles;
  private boolean init = false;

  public static BrownClusters semeval16BrownClusters(final String kirkDir, final String thymeDir) {
    return new BrownClusters(
//        kirkDir + File.separator + "gigaword.afp_eng-c100",
//        kirkDir + File.separator + "gigaword.apw_eng-c100",
//        kirkDir + File.separator + "gigaword.cna_eng-c100",
        kirkDir + File.separator + "gigaword.cna_eng-c1000",
//        kirkDir + File.separator + "gigaword.ltw_eng-c100",
//        kirkDir + File.separator + "gigaword.ltw_eng-c1000",
//        kirkDir + File.separator + "gigaword.nyt_eng-c100",
        kirkDir + File.separator + "gigaword.xin_eng-c100",
//        kirkDir + File.separator + "gigaword.xin_eng-c1000",
        kirkDir + File.separator + "i2b2-c100",
//        kirkDir + File.separator + "i2b2-c1000",
//        kirkDir + File.separator + "pmc-c100",
//        kirkDir + File.separator + "trecmed2012-c100",
//        kirkDir + File.separator + "trecmed2012-c1000",
//        kirkDir + File.separator + "wikipedia-c100",
//        thymeDir + File.separator + "lowercase_lemmas-c100",
        thymeDir + File.separator + "thyme-c1000"
    );
  }

  /**
   * Initialize a group of brown clusters from files containing triples of <cluster> <word> <count> one per line.
   * @param clusterFiles absolute locations of brown cluster files
   */
  public BrownClusters(final String... clusterFiles) {
    this.clusterFiles = clusterFiles;
    clusters = Maps.newHashMap();
    names = Lists.newArrayListWithCapacity(clusterFiles.length);
  }

  private void init() {
    log.info("Initializing Brown Clusters...");
    for (final String clusterFile : clusterFiles) {
      final File file = new File(clusterFile);
      names.add(file.getName());
      initClusters(file);
    }
    log.info("Initializing Brown Clusters... Done.");
    init = true;
  }

  private void initClusters(final File inFile) {
    try (BufferedReader reader = new BufferedReader(new FileReader(inFile))) {
      String line;
      while ((line = reader.readLine()) != null) {
        final List<String> list = splitter.splitToList(line);
        final String word = list.get(1);
        final ClusterMap clusterMap = (clusters.containsKey(word)) ? clusters.get(word) : new ClusterMap(names);
        clusterMap.put(inFile.getName(), list.get(0));
        clusters.put(word, clusterMap);
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public String getCluster(final String string, final String name) {

    if (!init) {
      init();
    }
    if (clusters.containsKey(string)) {
      return clusters.get(string).get(name);
    }
    else {
      return "null";
    }
  }

  public ClusterMap getClusters(final String string) {
    if (!init) {
      init();
    }
    return clusters.containsKey(string) ? clusters.get(string) : new ClusterMap(names);
  }

  /**
   * Return the first <arg>prefix</arg> characters of the brown cluster for <var>string</var>
   * @param string the string to get brown cluster info for
   * @param prefix the length desired
   * @param name brown cluster identifier
   * @return the prefix of the brown cluster for the passed string
   */
  public String getPrefix(final String string, final int prefix, final String name) {
    if (!init) {
      init();
    }

    if (!clusters.containsKey(string)) {
      return "null";
    }
    else {
      return clusters.get(string).getPrefix(name, prefix);
    }
  }

  public static class ClusterMap {
    private final Map<String, String> map;

    private ClusterMap(List<String> names) {
      map = Maps.newHashMap();
      names.forEach(name -> map.put(name, "null"));
    }

    private void put(String name, String cluster) {
      map.put(name, cluster);
    }

    public String get(String name) {
      return map.getOrDefault(name, "null");
    }

    public String getPrefix(String name, final int prefix) {
      final String cluster = get(name);
      if (cluster.equals("null") || cluster.length() < prefix) {
        return cluster;
      }
      else {
        return cluster.substring(0, prefix);
      }
    }

    public Set<String> keyset() {
      return map.keySet();
    }
  }

  public static void main(String... args) {
    final Config config = Config.load("semeval16.vectorizer");
    final BrownClusters bc = BrownClusters.semeval16BrownClusters(config.getString("bc-kirk"), config.getString("bc-thyme"));
    final JsonCorpus<BaseDocument> json = JsonCorpus.at(args[0]).annotationSets("genia").build();
    final Set<String> uniqueTokens = Sets.newConcurrentHashSet();

    log.info("------------Totals------------");
    final Multiset<String> retreived = HashMultiset.create();
    final int totalTokens = json.mapEachDocument(doc -> {
      int tokens = 0;
      for (final Token token : doc.get("genia", Token.TYPE)) {
        uniqueTokens.add(token.get(Token.Lemma).toLowerCase());
        final ClusterMap clusterMap = bc.getClusters(token.get(Token.Lemma).toLowerCase());
        for (final String key : clusterMap.keyset()) {
          if (!clusterMap.get(key).equals("null")) {
            retreived.add(key);
          }
        }
        tokens++;
      }
      return tokens;
    }).reduce(0, (c1, c2) -> c1 + c2);
    for (final String clusterset : retreived.elementSet()) {
      log.info("{}: {}", clusterset, (double)retreived.count(clusterset)/(double)totalTokens);
    }

    log.info("------------Unique------------");
    final Multiset<String> ms = HashMultiset.create();
    for (final String token : uniqueTokens) {
      final ClusterMap clusterMap = bc.getClusters(token);
      for (final String key : clusterMap.keyset()) {
        if (!clusterMap.get(key).equals("null")) {
          ms.add(key);
        }
      }
    }
    for (final String clusterset : retreived.elementSet()) {
      log.info("{}: {}", clusterset, (double)ms.count(clusterset)/(double)uniqueTokens.size());
    }
  }
}
