package edu.utdallas.hltri.scribe.annotators.semafor;

import com.google.common.collect.ImmutableMultiset;
import com.google.common.collect.Multiset;
import com.google.common.collect.Multisets;
import com.google.common.collect.TreeMultiset;
import com.google.common.io.Files;

import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import edu.utdallas.hltri.conf.Config;
import edu.utdallas.hltri.logging.Logger;
import edu.utdallas.hltri.scribe.annotators.Annotator;
import edu.utdallas.hltri.scribe.text.Attribute;
import edu.utdallas.hltri.scribe.text.BaseDocument;
import edu.utdallas.hltri.scribe.text.Document;
import edu.utdallas.hltri.scribe.text.annotation.Sentence;

/**
 * Created with IntelliJ IDEA.
 * User: Ramon
 * Date: 9/5/14
 * Time: 11:10 AM
 */
public class SemaforFrameNetAnnotator<D extends BaseDocument> implements Annotator<D> {
  private static final Logger log = Logger.get(SemaforFrameNetAnnotator.class);
  private static final SAXBuilder builder = new SAXBuilder();
  private static final Multiset<String> frameCounts = TreeMultiset.create();
  private final Config conf = Config.load("scribe.annotator.semafor");

  public static final Attribute<Sentence, List<Frame>>
      Frames = Attribute.inferred("frames");

  public SemaforFrameNetAnnotator() {
    //TODO: establish comminication with MST Server
  }

  public void report() {
    log.info("Total number of distinct frames: {}", frameCounts.elementSet().size());
    final ImmutableMultiset<String> sorted = Multisets.copyHighestCountFirst(frameCounts);
    final Iterator<String> it = sorted.elementSet().iterator();
    for (int i=0; i<25; i++) {
      final String frame = it.next();
      log.info(String.format("%-20s %5d", frame, frameCounts.count(frame)));
    }
  }

  @Override
  public <B extends D> void annotate(Document<B> document) {
    log.info("Processing {}", document.get(BaseDocument.id));

    //initialize a temporary directory for semafor to read from and write to
    final File tempDir = Files.createTempDir();
    final String inFile = tempDir.getAbsolutePath() + document.get(BaseDocument.id);
    final String outFile = inFile + ".new";

    //write the document to inFile one sentence per line
    try (final BufferedWriter writer = new BufferedWriter(new FileWriter(inFile))) {
      for (Sentence sentence : document.get("genia", Sentence.TYPE)) {
        writer.write(sentence.asString());
        writer.newLine();
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

    try {
      //start the FN parser
      final ProcessBuilder pb = new ProcessBuilder("./fnParserDriver.sh", inFile, outFile);
      String semaforDir = conf.getString("semafor-path");
      pb.directory(new File(semaforDir));
      Process p = pb.start();

      //empty the output buffer for the process and wait for its completion
      BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
      String line;
      while ((line = reader.readLine()) != null) {
        if (line.contains("nitial") || line.contains("Finished")) {
          log.debug(line);
        }
      }
      p.waitFor();

      final File file = new File(outFile);
      if (!file.exists()) {
        log.warn("{} not found", document.get(BaseDocument.id));
      }
      else {
        final Iterator<Sentence> it = document.get("genia", Sentence.TYPE).iterator();
        log.info(document.get("genia", Sentence.TYPE).toString());
        List<Frame> frames;
        Sentence sent;

        for (final Element documents : builder.build(file).getRootElement().getChildren()) {
          log.trace("-Name: {}", documents.getName());
          if (documents.getName().equals("documents")) {
            for (final Element doc : documents.getChildren()) {
              log.trace("--Name: {}", doc.getName());
              if (doc.getName().equals("document")) {
                for (final Element paragraphs : doc.getChildren()) {
                  if (paragraphs.getName().equals("paragraphs")) {
                    for (final Element paragraph : paragraphs.getChildren()) {
                      if (paragraph.getName().equals("paragraph")) {
                        for (final Element sentences : paragraph.getChildren()) {
                          if (sentences.getName().equals("sentences")) {
                            for (final Element sentence : sentences.getChildren()) {
                              if (sentence.getName().equals("sentence")) {
                                sent = it.next();
                                frames = new ArrayList<>();
                                log.trace("Sentence found: {}", sent.toString());
                                final String semSentence = sentence.getChild("text").getValue();
                                for (final Element annSets : sentence.getChildren()) {
                                  if (annSets.getName().equals("annotationSets")) {
                                    log.trace("AnnSets found");
                                    for (final Element annSet : annSets.getChildren()) {
                                      if (annSet.getName().equals("annotationSet")) {
                                        log.info("Ann Set found: ", annSet.getAttributeValue("frameName"));
                                        frames.add(new Frame(annSet, sent, semSentence));
                                        frameCounts.add(annSet.getAttributeValue("frameName"));
                                      }
                                    }
                                  }
                                }

                                sent.set(Frames, frames);
                              }
                            }
                          }
                        }
                      }
                    }
                  }
                }
              }
            }
          }
        }
      }
    } catch (JDOMException | IOException | InterruptedException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Closes this stream and releases any system resources associated
   * with it. If the stream is already closed then invoking this
   * method has no effect.
   * <p>
   * <p> As noted in {@link AutoCloseable#close()}, cases where the
   * close may fail require careful attention. It is strongly advised
   * to relinquish the underlying resources and to internally
   * <em>mark</em> the {@code Closeable} as closed, prior to throwing
   * the {@code IOException}.
   *
   * @throws java.io.IOException if an I/O error occurs
   */
  @Override
  public void close() {
    //TODO: shutdown MST Server
  }
}
