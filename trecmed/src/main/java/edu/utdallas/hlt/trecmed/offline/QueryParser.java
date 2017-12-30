package edu.utdallas.hlt.trecmed.offline;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import edu.utdallas.hlt.trecmed.Topic;

/**
 *
 * @author travis
 */
public class QueryParser {
  public static List<Topic> fromPath(Path file) {
    try {
      List<String> lines = Files.readAllLines(file, Charset.defaultCharset());
      switch (lines.get(0)) {
        default:
        case "<top>": return fromTREC(lines);
      }
    } catch (IOException ex) {
      throw new RuntimeException(ex);
    }
  }

  public static List<Topic> fromTREC(List<String> lines) {
    final Pattern p = Pattern.compile("<num> Number: (\\d+)");
    final List<Topic> queries = new ArrayList<>();
    String id = null, text = null;
    for (int i = 0; i < lines.size(); i++) {
      Matcher m = p.matcher(lines.get(i));
      if (m.matches()) {
        id = m.group(1);
      }
      if (lines.get(i).trim().equals("</top>")) {
        text = lines.get(i - 1).trim();
        queries.add(new Topic(id, text));
      }
    }
    return queries;
  }

  public static void main (String... args) {
    for (Topic q : fromPath(Paths.get(args[0]))) {
      System.out.println(q);
    }
  }
}
