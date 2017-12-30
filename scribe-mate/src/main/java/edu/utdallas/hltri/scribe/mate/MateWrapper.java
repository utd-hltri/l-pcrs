package edu.utdallas.hltri.scribe.mate;

import edu.utdallas.hltri.conf.Config;
import se.lth.cs.srl.CompletePipeline;
import se.lth.cs.srl.corpus.Sentence;
import se.lth.cs.srl.io.CoNLL09Writer;
import se.lth.cs.srl.io.SentenceWriter;
import se.lth.cs.srl.options.CompletePipelineCMDLineOptions;
import se.lth.cs.srl.util.ChineseDesegmenter;
import se.lth.cs.srl.util.FileExistenceVerifier;
import se.lth.cs.srl.util.Util;

import java.io.*;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Created with IntelliJ IDEA.
 * User: Ramon
 * Date: 7/15/14
 * Time: 5:49 PM
 */
public class MateWrapper {
  private static final Config conf = Config.load("scribe.annotator.mate");
  private static final Pattern WHITESPACE_PATTERN= Pattern.compile("\\s+");

  public static void main (String[] args) throws Exception{
      annotate(args[0], args[1]);
  }

  public static void annotate(String inDir, String outDir) throws Exception {
    String[] args = { "eng",
                      "-tagger", conf.getString("tagger"),
                      "-parser", conf.getString("parser"),
                      "-srl", conf.getString("srl"),
                      "-lemma", conf.getString("lemmatizer"),
                      "-test", inDir,
                      "-out", outDir};

    CompletePipelineCMDLineOptions options = new CompletePipelineCMDLineOptions();
    options.parseCmdLineArgs(args);
    String error = FileExistenceVerifier.verifyCompletePipelineAllNecessaryModelFiles(options);
    if (error != null) {
      System.err.println(error);
      System.err.println();
      System.err.println("Aborting.");
      System.exit(1);
    }

    CompletePipeline pipeline = CompletePipeline.getCompletePipeline(options);
//    BufferedReader in;// = new BufferedReader(new InputStreamReader(new FileInputStream(options.input), Charset.forName("UTF-8")));
    SentenceWriter writer;// = new CoNLL09Writer(options.output);
    long start = System.currentTimeMillis();
    int senCount;

    for (File file : options.input.listFiles()) {
      System.out.println("Processing " + file.getName());
      try (BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(file), Charset.forName("UTF-8")))) {
        writer = new CoNLL09Writer(new File(options.output.getAbsolutePath() + "/" + file.getName()));

        if (options.loadPreprocessorWithTokenizer) {
          senCount = parseNonSegmentedLineByLine(options, pipeline, in, writer);
        }
        else {
          senCount = parseCoNLL09(options, pipeline, in, writer);
        }

        writer.close();
      } catch (IOException e) {
        throw new RuntimeException(e);
      }

      long time = System.currentTimeMillis() - start;
      System.out.println(pipeline.getStatusString());
      System.out.println();
      System.out.println("Total parsing time (ms):  " + Util.insertCommas(time));
      System.out.println("Overall speed (ms/sen):   " + Util.insertCommas(time / senCount));
    }
  }

  private static int parseNonSegmentedLineByLine(CompletePipelineCMDLineOptions options, CompletePipeline pipeline, BufferedReader in, SentenceWriter writer)	throws IOException, Exception {
    int senCount=0;
    String str;

    while((str=in.readLine()) != null){
      Sentence s=pipeline.parse(str);
      writer.write(s);
      senCount++;
      if(senCount%100==0)
        System.out.println("Processing sentence "+senCount); //TODO, same as below.
    }


    return senCount;
  }

  private static int parseCoNLL09(CompletePipelineCMDLineOptions options,CompletePipeline pipeline, BufferedReader in, SentenceWriter writer)	throws IOException, Exception {
    List<String> forms=new ArrayList<>();
    forms.add("<root>");
    List<Boolean> isPred=new ArrayList<>();
    isPred.add(false);
    String str;
    int senCount=0;

    while ((str = in.readLine()) != null) {
      if(str.trim().equals("")){
        Sentence s;
        if(options.desegment){
          s=pipeline.parse(ChineseDesegmenter.desegment(forms.toArray(new String[0])));
        } else {
          s=options.skipPI ? pipeline.parseOraclePI(forms, isPred) : pipeline.parse(forms);
        }
        forms.clear();
        forms.add("<root>");
        isPred.clear();
        isPred.add(false); //Root is not a predicate
        writer.write(s);
        senCount++;
        if(senCount%100==0){ //TODO fix output in general, don't print to System.out. Wrap a printstream in some (static) class, and allow people to adjust this. While doing this, also add the option to make the output file be -, ie so it prints to stdout. All kinds of errors should goto stderr, and nothing should be printed to stdout by default
          System.out.println("Processing sentence "+senCount);
        }
      } else {
        String[] tokens=WHITESPACE_PATTERN.split(str);
        forms.add(tokens[1]);
        if(options.skipPI)
          isPred.add(tokens[12].equals("Y"));
      }
    }

    if(forms.size()>1){ //We have the root token too, remember!
      writer.write(pipeline.parse(forms));
      senCount++;
    }
    return senCount;
  }
}
