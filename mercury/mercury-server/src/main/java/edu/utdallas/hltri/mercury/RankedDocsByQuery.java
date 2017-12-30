package edu.utdallas.hltri.mercury;

import com.google.common.base.CharMatcher;
import com.google.common.base.Splitter;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * Created by rmm120030 on 11/19/16.
 */
public class RankedDocsByQuery {
  private final Map<String, List<String>> docsByQuery;

  private RankedDocsByQuery(Path path) {
    docsByQuery = Maps.newHashMap();
    final Splitter splitter = Splitter.on(CharMatcher.whitespace()).limit(5).omitEmptyStrings();
    try (BufferedReader reader = Files.newBufferedReader(path, Charset.defaultCharset())) {
      for (String line; (line = reader.readLine()) != null; ) {
        String[] fields = Iterables.toArray(splitter.split(line), String.class);
        final List<String> rankedDids = docsByQuery.containsKey(fields[0]) ? docsByQuery.get(fields[0])
            : Lists.newArrayListWithCapacity(10);
        if (rankedDids.size() < 10) {
          rankedDids.add(fields[2]);
          docsByQuery.put(fields[0], rankedDids);
        }
      }
    } catch (IOException ex) {
      throw new RuntimeException(ex);
    }
  }

  public static RankedDocsByQuery fromFile(Path path) {
    return new RankedDocsByQuery(path);
  }

  public List<String> getBestDocs(String qid) {
    return docsByQuery.get(qid);
  }
}
