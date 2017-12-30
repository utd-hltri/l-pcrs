package edu.utdallas.hltri.mercury.scripts;

import com.google.common.base.Splitter;
import com.google.common.collect.Maps;
import edu.utdallas.hltri.inquire.eval.TrecRunWriter;
import edu.utdallas.hltri.logging.Logger;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Created by ramon on 5/27/17.
 */
public class InterleaveQrels {
  private static final Logger log = Logger.get(InterleaveQrels.class);

  public static void main(String... args) {
    final Set<String> queryIds = new HashSet<>();
    final Map<String, Set<Qrel>> topResults = Maps.newHashMap();
    final Map<String, Set<Qrel>> randomResults = Maps.newHashMap();

    assert args.length > 2 : "Usage: <number of good/bad results per model> <output qrel file> [<input qrel file>]";

    final Iterator<String> it = Arrays.stream(args).iterator();
    final int numTopResultsPerRelevanceModel = Integer.parseInt(it.next());
    final Path qrelsOut = Paths.get(it.next());
    while(it.hasNext()) {
      final Map<String, List<Qrel>> qrelMap = readQrels(it.next());
      for (Map.Entry<String, List<Qrel>> entry : qrelMap.entrySet()) {
        final String qid = entry.getKey();
        queryIds.add(qid);
        final List<Qrel> list = entry.getValue();
        final Set<Qrel> topResults_ = topResults.containsKey(qid) ? topResults.get(qid) : new HashSet<>();
        addTopQrelsToSet(numTopResultsPerRelevanceModel, list, topResults_);
        topResults.put(qid, topResults_);

        final Set<Qrel> randResults_ = randomResults.containsKey(qid) ? randomResults.get(qid) : new HashSet<>();
        addRandomQrelsToSet(numTopResultsPerRelevanceModel, list, randResults_, topResults_);
        randomResults.put(qid, randResults_);
      }
    }

    log.info("Writing qrels from {} queries to {}...", topResults.keySet().size(), qrelsOut);
    try (TrecRunWriter writer = new TrecRunWriter(qrelsOut, "Merged")) {
      for (String qid : queryIds) {
        final List<Qrel> list = new ArrayList<>(topResults.get(qid));
        list.sort(Comparator.reverseOrder());
        list.addAll(randomResults.get(qid));
        for (int i = 0; i < list.size(); i++) {
          final Qrel qrel = list.get(i);
          writer.writeResultWithTag(qrel.qid, qrel.did, i+1, qrel.score, qrel.runName);
        }
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private static Map<String, List<Qrel>> readQrels(String qrelFile) {
    try (BufferedReader reader = new BufferedReader(new FileReader(qrelFile))) {
      final Map<String, List<Qrel>> map = new HashMap<>();
      String line;
      String prevQid = null;
      List<Qrel> list = null;
      while ((line = reader.readLine()) != null) {
        final Qrel qrel = new Qrel(line);

        if (!qrel.qid.equals(prevQid)) {
          if (prevQid != null) {
            map.put(prevQid, list);
          }
          list = new ArrayList<>();
          prevQid = qrel.qid;
        }
        list.add(qrel);
      }
      map.put(prevQid, list);
      log.info("Loaded qrels for {} queries from {}", map.keySet().size(), qrelFile);

      return map;
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private static void addTopQrelsToSet(int numToAdd, List<Qrel> sortedQrels, Set<Qrel> set) {
    final int stop = set.size() + numToAdd;
    final Iterator<Qrel> it = sortedQrels.iterator();
    while(set.size() < stop && it.hasNext()) {
      set.add(it.next());
    }
  }

  private static void addRandomQrelsToSet(int numToAdd, List<Qrel> sortedQrels, Set<Qrel> set, Set<Qrel> badSet) {
    final int stop = set.size() + numToAdd;
    while(set.size() < stop) {
      final Qrel qrel = sortedQrels.get(ThreadLocalRandom.current().nextInt(0, sortedQrels.size()));
      if (!badSet.contains(qrel)) {
        set.add(qrel);
      }
    }
  }

  private static class Qrel implements Comparable<Qrel> {
    private static final Splitter splitter = Splitter.on("\t").omitEmptyStrings();

    private final String qid; // query id
    private final String idk; // ???
    private final String did; // doc id
    private final int rank;   // rank
    private final double score;   // score
    private final String runName; // run name

    public Qrel(String line) {
      final List<String> list = splitter.splitToList(line);
      qid = list.get(0);
      idk = list.get(1);
      did = list.get(2);
      rank = Integer.parseInt(list.get(3));
      score = Double.parseDouble(list.get(4));
      runName = list.get(5);
    }

    @Override
    public String toString() {
      return qid + "\t" + idk  + "\t" + did  + "\t" + rank  + "\t" + score  + "\t" + runName;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;

      Qrel qrel = (Qrel) o;

      if (rank != qrel.rank) return false;
      if (Double.compare(qrel.score, score) != 0) return false;
      if (!qid.equals(qrel.qid)) return false;
      if (!idk.equals(qrel.idk)) return false;
      if (!did.equals(qrel.did)) return false;
      return runName.equals(qrel.runName);

    }

    @Override
    public int hashCode() {
      int result;
      long temp;
      result = qid.hashCode();
      result = 31 * result + idk.hashCode();
      result = 31 * result + did.hashCode();
      result = 31 * result + rank;
      temp = Double.doubleToLongBits(score);
      result = 31 * result + (int) (temp ^ (temp >>> 32));
      result = 31 * result + runName.hashCode();
      return result;
    }

    @Override
    public int compareTo(Qrel o) {
      return Double.compare(score, o.score);
    }
  }
}
