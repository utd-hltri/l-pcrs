//package edu.utdallas.hltri.scribe.gate;
//
//import edu.utdallas.hltri.io.IOUtils;
//import edu.utdallas.hltri.scribe.annotators.AbstractAnnotator;
//import gate.Annotation;
//import gate.Document;
//import gate.Node;
//
//import java.io.File;
//import java.io.IOException;
//import java.nio.file.Files;
//import java.nio.file.Path;
//import java.util.ArrayList;
//import java.util.List;
//
//public class GENIATagger extends AbstractAnnotator {
//
//  private final int BATCH_SIZE = 1_000;
//
//  private final Path input, output;
//
//  private final ProcessBuilder genia;
//
//  public GENIATagger() {
//    try {
//      input = Files.createTempFile("genia", ".in");
//      output = Files.createTempFile("genia", ".out");
//    } catch (IOException ex) {
//      throw new RuntimeException(ex);
//    }
//
//    genia = new ProcessBuilder("geniatagger");
//  }
//
//
//  public String[] getCommand(File inputFolder) {
//    return new String[]{"/users/bryan/apps/share/geniatagger-3.0.1/script",
//                        new File(inputFolder, "input.txt").getAbsolutePath(), new File(inputFolder, "output.txt").getAbsolutePath()};
//  }
//
//
//    @Override
//    public void attachAnnotations(List<String> result, Document doc, Annotation sentence) {
//      int tokNum = 0;
//      Node chunkStart = null;
//      String chunkType = null;
////    System.err.println("Sentence: " + GATEUtils.text(sentence, doc));
////    System.err.println("Result: " + result);
//      for (Annotation token : GATEUtils.getSubspansSorted(doc, sentence, "Token")) {
//        if (token.getFeatures().getUnsafeAnnotations("string").equals("")) { continue; }
//        String[] data = result.getUnsafeAnnotations(tokNum).split("\t");
//        if ( ! token.getFeatures().getUnsafeAnnotations("string").equals(data[0])) {
//          System.err.println("Sentence: " + GATEUtils.text(sentence, doc));
//          System.err.println("Result: ");
//          for (String line : result) {
//            System.err.println("  " + line);
//          }
//          System.err.println(GATEUtils.text(token, doc));
//          throw new RuntimeException("Token mismatch: " + token + "   " + data[0]);
//        }
//        token.getFeatures().put("category", data[2]);
//        if (data[3].startsWith("B") || (data[3].startsWith("I"))) {
//          if (data[3].startsWith("B") || ! data[3].substring(2).equals(chunkType)) {
//            if (data[3].startsWith("I") &&  !data[3].substring(2).equals(chunkType)) {
//              System.err.println("chunk types don't match: " + result.getUnsafeAnnotations(tokNum));
//            }
//            if (chunkStart != null) {
//              doc.getAnnotations().add(chunkStart, token.getStartNode(), "Chunk", GATEUtils.newFeatureMap("chunkType", chunkType));
//            }
//            chunkStart = token.getStartNode();
//            chunkType = data[3].substring(2);
//          }
//        } else if (data[3].startsWith("O")) {
//          if (chunkStart != null) {
//            doc.getAnnotations().add(chunkStart, token.getStartNode(), "Chunk", GATEUtils.newFeatureMap("chunkType", chunkType));
//          }
//          chunkStart = null;
//        } else {
//          throw new RuntimeException("Unexpected IOB character: " + data[3]);
//        }
//        tokNum++;
//      }
//      if (tokNum != result.size()) {
//        throw new RuntimeException("Token mismatch: \n" + result + "\n" + GATEUtils.text(sentence, doc));
//      }
//    }
//  }
//}
