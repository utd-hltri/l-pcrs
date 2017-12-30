package edu.utdallas.hltri.mercury;

import com.google.common.base.CharMatcher;
import com.google.common.base.Splitter;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;

/**
 * Created by rmm120030 on 11/19/16.
 */
public class TrecEval {
  private final Map<String, Double> ndcgs, bprefs, maps;

  private TrecEval(Path path) {
    ndcgs = Maps.newHashMap();
    bprefs = Maps.newHashMap();
    maps = Maps.newHashMap();
    final Splitter splitter = Splitter.on(CharMatcher.whitespace()).limit(3).omitEmptyStrings();
    try (BufferedReader reader = Files.newBufferedReader(path, Charset.defaultCharset())) {
      for (String line; (line = reader.readLine()) != null; ) {
        String[] fields = Iterables.toArray(splitter.split(line), String.class);
        switch (fields[0]) {
          case "map": maps.put(fields[1], Double.parseDouble(fields[2]));
            break;
          case "ndcg": ndcgs.put(fields[1], Double.parseDouble(fields[2]));
            break;
          case "bpref": bprefs.put(fields[1], Double.parseDouble(fields[2]));
            break;
          default: break;
        }
      }
    } catch (IOException ex) {
      throw new RuntimeException(ex);
    }
  }

  public static TrecEval fromFile(Path path) {
    return new TrecEval(path);
  }

  public Double getNdcg(String qid) {
    return Optional.ofNullable(ndcgs.get(qid)).orElse(-1.0);
  }

  public Double getBpref(String qid) {
    return Optional.ofNullable(bprefs.get(qid)).orElse(-1.0);
  }

  public Double getMap(String qid) {
    return Optional.ofNullable(maps.get(qid)).orElse(-1.0);
  }
}
