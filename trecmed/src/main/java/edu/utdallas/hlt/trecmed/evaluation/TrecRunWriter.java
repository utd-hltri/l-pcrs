/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.utdallas.hlt.trecmed.evaluation;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import edu.utdallas.hlt.trecmed.Topic;
import edu.utdallas.hltri.inquire.lucene.LuceneResult;
import edu.utdallas.hltri.scribe.text.Identifiable;

/**
 *
 * @author travis
 */
public class TrecRunWriter {
   public void writeQRels(final Path output,
                          final Iterable<Topic> queries, final Map<Topic, ? extends List<? extends LuceneResult<? extends Identifiable>>> results,
                          final String runtag) {
    try (PrintWriter writer = new PrintWriter(Files.newBufferedWriter(output, Charset.defaultCharset()))) {
      for (final Topic topic : queries) {
        for (int i = 0; i < 1000 && i < results.get(topic).size(); i++) {
          writer.printf("%s Q0 %s %d %f %s%n",
                        topic.getId(),                                  // Topic id
                        results.get(topic).get(i).getValue().getId(),   // Visit id
                        i + 1,                                          // Rank
                        results.get(topic).get(i).getScore(),           // Score
                        runtag);                                        // Runtag
        }
      }
    } catch (IOException ex) {
      throw new RuntimeException(ex);
    }
  }
}
