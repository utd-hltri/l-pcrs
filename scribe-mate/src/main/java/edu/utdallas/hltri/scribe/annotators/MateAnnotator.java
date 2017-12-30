package edu.utdallas.hltri.scribe.annotators;

import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.io.Files;
import edu.utdallas.hltri.concurrent.BlockPolicy;
import edu.utdallas.hltri.logging.Logger;
import edu.utdallas.hlt.medbase.pred.PredBank;
import edu.utdallas.hlt.medbase.pred.RoleSet;
import edu.utdallas.hltri.scribe.mate.*;
import edu.utdallas.hltri.scribe.text.Attribute;
import edu.utdallas.hltri.scribe.text.BaseDocument;
import edu.utdallas.hltri.scribe.text.Document;
import edu.utdallas.hltri.scribe.text.annotation.Annotation;
import edu.utdallas.hltri.scribe.text.annotation.Sentence;
import edu.utdallas.hltri.scribe.text.annotation.Token;
import edu.utdallas.hltri.scribe.text.documents.HasTitle;
import edu.utdallas.hltri.scribe.text.relation.Dependency;
import edu.utdallas.hltri.scribe.text.relation.DependencyGraph;
import edu.utdallas.hltri.scribe.text.relation.Relation;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Function;

/**
 * Created with IntelliJ IDEA.
 * User: Ramon
 * Date: 7/17/14
 * Time: 3:21 PM
 *
 * TODO: ensure ARGUMENT_HEAD actually points to the arg head, not the first noun
 *
 * Adds Predicate Annotations and Argument Annotations.
 * Attaches a DependencyGraph<Token> to each sentence.
 */
public class MateAnnotator<D extends BaseDocument> implements Annotator<D> {
  private static final Logger log = Logger.get(MateAnnotator.class);
  private static final Set<String> UNRECOGNIZED_SET = Sets.newHashSet();
  private static final String ANNOTATION_SET = "mate";
  private final Function<Annotation<?>, Iterable<Token>> tokFun;
  private final Function<Document<? extends D>, Iterable<Sentence>> senFun;
  private final boolean clear;
  private final String tokenAnnSet;

  public static final Attribute<Token, Boolean> IS_PRED = Attribute.typed("is-predicate", Boolean.class);

  // for setting output directory for parallel debugging
  private int parallel_dir = -1;
  public void setNum(int num) {
    parallel_dir = num;
  }

  private MateAnnotator(Builder<D> builder) {
    if (builder.tokFun == null) {
      log.warn("No token function provided, defaulting to genia...");
      tokFun = (s -> s.getContained("genia", Token.TYPE));
    }
    else {
      tokFun = builder.tokFun;
    }
    if (builder.tokFun == null) {
      log.warn("No sentence function provided, defaulting to opennlp...");
      senFun = (d -> d.get("opennlp", Sentence.TYPE));
    }
    else {
      senFun = builder.senFun;
    }
    clear = builder.clear;
    if (builder.tokenAnnSet != null) {
      tokenAnnSet = builder.tokenAnnSet;
    }
    else {
      throw new IllegalStateException("no token annotation set specified.");
    }
  }

  public static class Builder<D extends BaseDocument> {
    protected Function<Annotation<?>, Iterable<Token>> tokFun;
    protected Function<Document<? extends D>, Iterable<Sentence>> senFun;
    protected boolean clear = false;
    protected String tokenAnnSet;

    public Builder(){}

    public Builder<D> useTokens(Function<Annotation<?>, Iterable<Token>> tokFun) {
      this.tokFun = tokFun;
      return this;
    }

    public Builder<D> useSentences(Function<Document<? extends D>, Iterable<Sentence>> senFun) {
      this.senFun = senFun;
      return this;
    }

    public Builder<D> tokenAnnotationSet(final String tokenAnnSet) {
      this.tokenAnnSet = tokenAnnSet;
      return this;
    }

    public Builder<D> clear() {
      this.clear = true;
      return this;
    }

    public MateAnnotator<D> build() {
      return new MateAnnotator<>(this);
    }
  }

  @Override
  public <B extends D> void annotateAll(Iterable<Document<B>> documents) {
    PredBank.initAll();

//    final String tokensOut = "/users/rmm120030/working/clinarr/mate/neph/test";
//    final String mateOut = "/users/rmm120030/working/clinarr/mate/neph/test-p";
//    final File tempIn = new File(tokensOut);
//    final File tempOut = new File(mateOut);
//    tempIn.mkdirs();
//    tempOut.mkdirs();

    final File tempIn = Files.createTempDir();
    tempIn.deleteOnExit();
    final File tempOut = Files.createTempDir();
    tempOut.deleteOnExit();

    writeDocTextToDir1WordPerLine(tempIn.getAbsolutePath(), documents, tokFun, senFun);

    log.info("Running Mate...");
    //Populate tempOut with tagged versions of the documents from MateWrapper
    try {
      MateWrapper.annotate(tempIn.getAbsolutePath(), tempOut.getAbsolutePath());
    } catch (Exception e) {
      e.printStackTrace();
    }

    log.info("Mate done.");

    //For each file in the tempDir populated by MateWrapper, find the corresponding Document,
    //parse the file, and add annotations to the document
    for (final File file : tempOut.listFiles()) {/*new File("/users/rmm120030/working/clinarr/srl/parsed.new").listFiles())*/
      log.debug("Processing {}", file.getName());
      try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
        Document<B> document = null;
        for (Document<B> doc : documents) {
          if (doc.get(BaseDocument.id).equals(file.getName())) {
            document = doc;
            break;
          }
        }

        if (document != null) {
          if (clear) document.clear("mate");

          final Iterator<Sentence> sentenceIterator = senFun.apply(document).iterator();
          List<CONLL09Line> sentence = new ArrayList<>();

          String line;
          while ((line = reader.readLine()) != null) {
            if (line.length() == 0) {
              addAnnotationsFromSentence(sentence, document, sentenceIterator.next());
//            log.debug("Count: {}", count);
              sentence = new ArrayList<>();
            } else {
              sentence.add(new CONLL09Line(line));
            }
          }

//          final BufferedWriter writer = new BufferedWriter(
//              new FileWriter(String.format("/home/rmm120030/working/clinarr/mate/annotated/%d/%s.ann", parallel_dir, document.getName())));
//          final StringBuilder sb = new StringBuilder();
//          sb.append(document.asString()).append("\n");
//          for (Sentence s : senFun.apply(document) {
//            sb.append("------------------\n").append(s.asString()).append("\n");
//            sb.append(s.getUnsafeAnnotations(DEPENDENCIES)).append("\n");
//            for (Token t : tokFun.apply(s) {
//              final Predicate predicate = t.getUnsafeAnnotations(PREDICATE);
//              if (predicate != null) {
//                sb.append("Pred: ").append(predicate.toString()).append("\n");
//              }
//            }
//          }
//          writer.write(sb.toString());
//          writer.close();
//          final BufferedWriter writer2 = new BufferedWriter(
//              new FileWriter(String.format("/home/rmm120030/working/clinarr/mate/annotated/%d/%s.txt", parallel_dir, document.getUnsafeAnnotations(Document.title))));
//          writer2.write(document.describe());
//          writer2.close();
        }
        else {
          log.warn("Document '{}' not found in store", file.getName());
        }
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }
  }

  @Override
  public <B extends D> void annotate(Document<B> document) {
    log.warn("MateAnnotator is designed for batch processing of many documents at a time. If possible, use MateAnnotator.annotateAll(Iterable<D extends Document>");
    this.annotateAll(Collections.singletonList(document));
  }

  /**
   * Closes this stream and releases any system resources associated
   * with it. If the stream is already closed then invoking this
   * method has no effect.
   */
  @Override
  public void close() {

  }

  public void test(String inFile) {
    List<CONLL09Line> lines = Lists.newArrayList();

    try (BufferedReader reader = new BufferedReader(new FileReader(inFile))) {
      String line;
      while ((line = reader.readLine()) != null) {
        if (line.isEmpty()) {
          testAddAnnotations(lines);
          lines = Lists.newArrayList();
        }
        else {
          lines.add(new CONLL09Line(line));
        }
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private void testAddAnnotations(List<CONLL09Line> lines) {
    final List<CONLL09Line> predicates = new ArrayList<>();
    final Map<CONLL09Line, List<CONLL09Line>> predMap = Maps.newHashMap();

    //Find all the predicates and make annotations for them
    //Also, add edges to the dependency graph
    int index = 0;
    for (CONLL09Line word : lines) {
//      log.trace(token.describe());
//      log.trace("Mate token: {}", word.getForm());
      //add predicate
      RoleSet roleset;
      String source;
      if (word.isPred()) {
//        //if noun, search nombank
//        if (word.getPOS().startsWith("NN")) {
//          roleset = PredCorpus.NOMBANK.getRoleSet(word.getPred());
//          source = "NOMBANK";
//        }
//        //else search propbank
//        else {
//          roleset = PredCorpus.PROPBANK.getRoleSet(word.getPred());
//          source = "PROPBANK";
//        }

        predicates.add(word);
//        log.debug("Found Predicate: {}-{}", index, word.toString());
      }

      //add dependency to graph
      if (word.getHead() >= 0)
//        log.debug("Dependency: ({}, parent:{}-{}, child:{}-{})", word.getDepRel(), word.getHead(),
//            lines.getUnsafeAnnotations(word.getHead()).getForm(), index, word.getForm());
      index++;
    }

    //Add arguments to the predicate annotations found
    for (CONLL09Line word : lines) {
      //if this word is the head of an argument
      if (word.isArg()) {
        //normally do annotation stuff to make an argument ann

        //then do pred stuff to add the ann to the pred
        for (int i = 0; i < predicates.size(); i++) {
          final CONLL09Line predicate = predicates.get(i);
          //if the word is an arg for predicate i, add it to the predicate
          if (word.isArg(i)) {
            List<CONLL09Line> list = (predMap.containsKey(predicate)) ? predMap.get(predicate) : Lists.newArrayList();
            list.add(word);
            predMap.put(predicate, list);
          }
        }
      }
    }

    for (CONLL09Line pred : predMap.keySet()) {
      log.info("Pred: {}", pred.toString());
      for (CONLL09Line arg : predMap.get(pred)) {
        log.info("Arg: {}", arg.toString());
      }
      log.info("----------------------------");
    }
  }

  /**
   *
   * @param mateSentence a list of lines of output from the MateTools SRL system comprising a single sentence
   * @param doc the Document containing the sentence in question
   * @param sentence the Sentence annotation corresponding to the sentence in question
   */
  private <B extends D> void addAnnotationsFromSentence(List<CONLL09Line> mateSentence, Document<B> doc, Sentence sentence) {
//    log.trace(sentence.describe());
    final DependencyGraph graph = DependencyGraph.on(Lists.newArrayListWithCapacity(mateSentence.size() + 1));
    final List<Predicate> predicates = Lists.newArrayList();
    final List<Token> sentenceTokens = Lists.newArrayList(tokFun.apply(sentence));
    Token token;

//    sentenceTokens.stream().filter(t -> t.getUnsafeAnnotations(Annotation.id) == null).forEach(t -> t.set(Annotation.id, t.hashCode()));

    //Find all the predicates and make annotations for them
    //Also, add edges to the dependency graph
    Iterator<Token> it = sentenceTokens.iterator();
    for (CONLL09Line word : mateSentence) {
      token = it.next();
      token.set(IS_PRED, false);
//      log.trace(token.describe());
//      log.trace("Mate token: {}", word.getForm());
      assert (word.getForm().equals(token.asString())) : String.format("{%s} not equal to expected token: {%s}", word.getForm(), token.asString());
      //add predicate
      RoleSet roleset;
      String source;
      Predicate predicate;
      if (word.isPred()) {
        token.set(IS_PRED, true);
        //if noun, search nombank
        if (word.getPOS().startsWith("NN")) {
          roleset = PredBank.NOMBANK.getRoleSet(word.getPred());
          source = "NOMBANK";
        }
        //else search propbank
        else {
          roleset = PredBank.PROPBANK.getRoleSet(word.getPred());
          source = "PROPBANK";
        }

        predicate = Predicate.TYPE.create(doc, "mate", token.get(Token.StartOffset), token.get(Token.EndOffset));
        predicate.set(Predicate.Token_id, token.getId());
        predicate.set(Predicate.Name, word.getForm());
        if (roleset != null) {
          predicate.set(Predicate.RoleSet, roleset);
          predicate.set(Predicate.Source, source);
        }
        else {
          predicate.set(Predicate.RoleSet, RoleSet.NULL_ROLESET);
          predicate.set(Predicate.Source, "NONE");
        }

        predicates.add(predicate);
      }

      //create dependency relation and add it to the dependency graph
      if (word.getHead() >= 0) {
        final Relation<Dependency,Token,Token> dependency = Relation.create(Dependency.TYPE, sentenceTokens.get(word.getHead()), token, tokenAnnSet);
        dependency.set(Dependency.Label, word.getDepRel());
        graph.addEdge(dependency);
//        log.info("Added dependency {}.", dependency.describe());
//        log.info("Parent: {}", dependency.parent().describe());
//        log.info("Child: {}", dependency.child().describe());
      }
    }

    it = sentenceTokens.iterator();

    //Add arguments to the predicate annotations found
    for (final CONLL09Line word : mateSentence) {
      final Token argHead = it.next();
      assert (word.getForm().equals(argHead.asString())) : String.format("{%s} not equal to expected token: {%s}", word.getForm(), argHead.asString());

      //if this word is the head of an argument
      if (word.isArg()) {
        // find the offsets for this argument
        long start = Long.MAX_VALUE, end = Long.MIN_VALUE;
        final Collection<Token> argTokens = graph.getSubGraph(argHead).asJungGraph().getVertices();
        if (argTokens.size() > 0) {
          for (final Token argTok : argTokens) {
            start = (argTok.get(Token.StartOffset) < start) ? argTok.get(Token.StartOffset) : start;
            end = (argTok.get(Token.EndOffset) > end) ? argTok.get(Token.EndOffset) : end;
          }
        }
        else {
          start = argHead.get(Token.StartOffset);
          end = argHead.get(Token.EndOffset);
        }
        for (int i = 0; i < predicates.size(); i++) {
          //if the word is an arg for predicate i, add it to the predicate
          if (word.isArg(i)) {
            final Predicate predicate = predicates.get(i);
            log.trace("Creating argument at [{}, {}]", start, end);
            final Argument argument = Argument.TYPE.create(doc, "mate", start, end);
            argument.set(Argument.Token_id, argHead.getId());
            argument.set(Argument.Role, predicate.getRoleForArgument(word.getArg(i)));
            Relation.create(PredArgRelation.TYPE, predicate, argument, ANNOTATION_SET);
          }
        }
      }
    }
  }

  public static <D extends BaseDocument, B extends D> void writeDocTextToDir1WordPerLine(final String outDir,
                                                                            final Iterable<Document<B>> documents,
                                                                            final Function<Annotation<?>, Iterable<Token>> tokFun,
                                                                            final Function<Document<? extends D>, Iterable<Sentence>> senFun) {
    documents.forEach(doc -> {
      log.trace("Writing {} to {}...", doc.get(BaseDocument.id), outDir);
      try (BufferedWriter writer = new BufferedWriter(new FileWriter(outDir + "/" + doc.get(BaseDocument.id)))) {
        for (Sentence sentence : senFun.apply(doc)) {
          for (Token token : tokFun.apply(sentence)) {
            writer.write("_\t" + token.asString());
            writer.newLine();
          }
          writer.newLine();
        }
        log.trace("Wrote {}", doc.get(BaseDocument.id));

//        doc.close();
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    });

    log.info("Finished writing to {}", outDir);
  }

  /**
   *
   */
  public static <D extends BaseDocument, B extends D> void writeDocTextToDir1WordPerLineParallel(final String outDir,
                                                                                    final Iterable<Document<B>> documents,
                                                                                    final Function<Annotation<?>, Iterable<Token>> tokFun,
                                                                                    final Function<Document<? extends D>, Iterable<Sentence>> senFun) {
    final int threads = Runtime.getRuntime().availableProcessors();
    final ThreadPoolExecutor executor = new ThreadPoolExecutor(threads, threads, 0L, TimeUnit.MILLISECONDS,
        new LinkedBlockingQueue<>(threads *2), new BlockPolicy());

    documents.forEach(doc -> executor.execute(() -> {
      try (BufferedWriter writer = new BufferedWriter(new FileWriter(outDir + "/" + doc.get(BaseDocument.id)))) {
        for (Sentence sentence : senFun.apply(doc)) {
          for (Token token : tokFun.apply(sentence)) {
            writer.write("_\t" + token.asString());
            writer.newLine();
          }
          writer.newLine();
        }
        log.trace("Wrote {}", doc.get(BaseDocument.id));
//        doc.close();
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }));

    log.info("Finished writing to {}", outDir);

    executor.shutdown();
    try {
      executor.awaitTermination(1, TimeUnit.DAYS);
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
  }

  @SuppressWarnings("unused")
  private static class CONLL09Line {
    private List<String> fields;

    public CONLL09Line(String line) {
      Splitter splitter = Splitter.on("\t");
      fields = splitter.splitToList(line);
    }

    public String getNum() {
      return fields.get(0).trim();
    }

    public String getForm() {
      return fields.get(1).trim();
    }

    public String getLemma() {
      return fields.get(2).trim();
    }

    public String getPOS() {
      return fields.get(4).trim();
    }

    public int getHead() {
      return Integer.parseInt(fields.get(8).trim()) - 1;
    }

    public String getDepRel() {
      return fields.get(10).trim();
    }

    public boolean isPred() {
      return fields.get(12).charAt(0) == 'Y';
    }

    public String getPred() {
      return fields.get(13).replace("..",".").trim();
    }

    public boolean isArg(int index) {
      return !fields.get(index + 14).equals("_");
    }

    public boolean isArg() {
      for (int i=14; i<fields.size(); i++) {
        if (!fields.get(i).equals("_"))
          return true;
      }

      return false;
    }

    public String getArg(int index) {
      return fields.get(index + 14).trim();
    }

    public String toString() {
      final StringBuilder builder = new StringBuilder();
      int i = 0;
      for(String field : fields) {
        if (!field.trim().equals("_")) {
          builder.append("[").append(i++).append(": ");
          builder.append(field);
          builder.append("], ");
        }
      }
      return builder.toString();
    }
  }

  public static void addUnrecognizedArg(String argName) {
    UNRECOGNIZED_SET.add(argName);
  }

  public static void writeUnrecognizedArgs(String outFile) {
    try (BufferedWriter writer = new BufferedWriter(new FileWriter(outFile))) {
      String out = UNRECOGNIZED_SET.stream()
          .reduce("",
              (prev, curr) -> prev + curr + "\n",
              (prev, curr) -> prev + curr + "\n");
      writer.write(out);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
