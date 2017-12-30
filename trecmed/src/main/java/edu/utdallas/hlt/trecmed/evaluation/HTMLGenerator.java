package edu.utdallas.hlt.trecmed.evaluation;

import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.RuntimeConstants;
import org.slf4j.MDC;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import edu.utdallas.hlt.trecmed.Topic;
import edu.utdallas.hlt.trecmed.Visit;
import edu.utdallas.hlt.util.Config;
import edu.utdallas.hltri.logging.Logger;

/**
 *
 * @author travis
 */
public class HTMLGenerator {
  private static final long serialVersionUID = 1L;

  private static final Logger         log   = Logger.get(HTMLGenerator.class);
  private final        Properties     props = new Properties();
  private final        VelocityEngine ve;
  private final Path                                 directory;
  private final Evaluator                            evaluator;
  private final Iterable<Topic>                      questions;
  private final List<String>                         measures;



  public HTMLGenerator(Iterable<Topic> questions,
                       Path directory,
                       Evaluator evaluator,
                       List<String> measures) {
    props.put(RuntimeConstants.RESOURCE_LOADER, "class");
    props.put("class.resource.loader.class", "org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader");

    this.ve = new VelocityEngine(props);
    this.questions = questions;
    this.directory = directory;
    this.evaluator = evaluator;
    this.measures = measures;
  }

  public void generate() {
    ve.init();
    final VelocityContext context = new VelocityContext();
    context.put("questions", questions);
    context.put("eval", evaluator);
    context.put("measures", measures);

    final Template template = ve.getTemplate("templates/index.vm");

    log.info("Generating HTML to {}", directory);
    try (InputStream css = this.getClass().getResourceAsStream("default.css");
         InputStream jquery = this.getClass().getResourceAsStream("jquery-1.7.2.min.js");
         InputStream js = this.getClass().getResourceAsStream("main.js")) {
      final StandardCopyOption[] options = { StandardCopyOption.REPLACE_EXISTING };
      Files.createDirectories(directory.resolve("css"));
      Files.createDirectories(directory.resolve("img"));
      Files.createDirectories(directory.resolve("js").resolve("lib"));
      Files.copy(css, directory.resolve(Paths.get("css", "default.css")), options);
      Files.copy(jquery, directory.resolve(Paths.get("js", "lib", "jquery-1.7.2.min.js")), options);
      Files.copy(js, directory.resolve(Paths.get("js", "main.js")), options);
      template.merge(context, Files.newBufferedWriter(directory.resolve("index.html"), Charset.defaultCharset()));
    } catch (IOException ex) {
      throw new RuntimeException(ex);
    }

    for (final Topic topic : questions) {
      MDC.put("Q", "Q" + topic.getId() + " ");
      generateQuestion(topic);
    }
    MDC.clear();
  }

  public void generateQuestion(final Topic topic) {
    final List<Visit> visits = evaluator.getVisits(topic.getId(), Config.get(this.getClass(), "NUM_VISITS").toInteger());
    final VelocityContext context = new VelocityContext();
    final Template template = ve.getTemplate("templates/question.vm");

    context.put("query", topic);
    context.put("eval", evaluator);
    context.put("visits", visits);

    try (final BufferedWriter writer = Files.newBufferedWriter(
            directory.resolve(topic.getId() + ".html"),
            Charset.defaultCharset())) {
      template.merge(context, writer);
    } catch (IOException ex) {
      throw new RuntimeException(ex);
    }

    final LuceneQueryHighlighter highlighter = new LuceneQueryHighlighter(topic);
    final ExecutorService service = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
    for (final Visit visit : visits) {
      service.submit(() -> generateVisit(topic, visit, highlighter));
    }
    service.shutdown();
    try {
      service.awaitTermination(1, TimeUnit.DAYS);
    } catch (InterruptedException ex) {
      throw new RuntimeException(ex);
    }
  }

  public void generateVisit(final Topic topic, final Visit visit, final LuceneQueryHighlighter highlighter) {
    final VelocityContext context = new VelocityContext();
    final Template template = ve.getTemplate("templates/visit.vm");

    context.put("query", topic);
    context.put("visit", visit);
    context.put("highlighter", highlighter);

    Path path = directory.resolve(topic.getId());
    try {
      Files.createDirectories(path);
      try (final BufferedWriter writer = Files.newBufferedWriter(path.resolve(visit.getEncodedId() + ".html"), Charset.defaultCharset())) {
        template.merge(context, writer);
      }
    }  catch (IOException ex) {
      throw new RuntimeException(ex);
    }
  }
}
