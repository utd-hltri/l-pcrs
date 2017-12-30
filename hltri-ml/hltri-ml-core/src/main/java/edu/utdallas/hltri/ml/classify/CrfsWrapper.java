package edu.utdallas.hltri.ml.classify;

import com.google.common.collect.Lists;
import edu.utdallas.hltri.conf.Config;
import edu.utdallas.hltri.logging.Logger;
import edu.utdallas.hltri.struct.Pair;
import edu.utdallas.hltri.util.IntIdentifier;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * Wrapper for CRF Suite.
 * Created by ramon on 10/22/2016.
 */
public class CrfsWrapper {
  private static final Logger log = Logger.get(CrfsWrapper.class);
  private static final Config config = Config.load("ml.crfs");

  public static List<List<Pair<String, Double>>> predict(final List<List<CrfsFeatureVector>> sequences,
                                                         final String modelPath, final Path outDir, final String name) {
    final String outFile = outDir.resolve(name + ".vec").toString();
    final String[] command = new String[]{"./crfsuite", "tag", "-i", "-m", modelPath, outFile};
    log.trace("Writing to {}", outFile);
    writeVectors(sequences, outFile);
    final ProcessBuilder pb = new ProcessBuilder(command);
    pb.directory(config.getFile("bin-path"));

    log.trace("Running CRFS command {}", Arrays.toString(command));
    try {
      // create a temporary file to store the CRFS output in
      final File crfsOutput = outDir.resolve(name + ".out").toFile();
      // tell the CRFS process to redirect output into the temp file
      pb.redirectOutput(crfsOutput);
      // run the process and wait for it to complete
      final Process process = pb.start();
      process.waitFor();
      log.trace("Reading CRFS output from {}", crfsOutput.toString());

      final BufferedReader reader = new BufferedReader(new FileReader(crfsOutput));
      String line;
      List<List<Pair<String, Double>>> predictions = Lists.newArrayListWithCapacity(sequences.size());
      List<Pair<String, Double>> seq = new ArrayList<>();
      while ((line = reader.readLine()) != null) {
        if (line.length() > 0) {
          final String[] split = line.split(":");
          seq.add(Pair.of(split[0], Double.parseDouble(split[1].trim())));
        }
        else {
          predictions.add(seq);
          seq = new ArrayList<>();
        }
      }
      reader.close();
      return predictions;
    } catch (IOException | InterruptedException e) {
      throw new RuntimeException(e);
    }
  }

  public static void train(final String modelFile, final String mappingFile, final IntIdentifier<String> featureMap,
                           final List<List<CrfsFeatureVector>> trainingSequences, final boolean... verbose) {
    try {
      final String vectorsOut = Files.createTempFile("vectors", ".vec").toString();
      log.info("Writing vectors to {}...", vectorsOut);
      writeVectors(trainingSequences, vectorsOut);
      featureMap.toFile(mappingFile);
      final String[] command = new String[]{"./crfsuite", "learn", "-m", modelFile, ""};
      command[4] = vectorsOut;
      log.info("CRFSuite command: {}", Arrays.toString(command));
      final ProcessBuilder pb = new ProcessBuilder(command);
      pb.directory(config.getFile("bin-path"));
      pb.redirectOutput(ProcessBuilder.Redirect.INHERIT);
      final long start = System.currentTimeMillis();
      log.info("Begin training CRF...");
      final Process process = pb.start();
      process.waitFor();
      final long end = System.currentTimeMillis();
      log.info("Done. Took {}.", String.format("%2.2f seconds", (end-start)/(float)1000));
    } catch (IOException | InterruptedException e) {
      throw new RuntimeException(e);
    }
  }

  private static void writeVectors(final List<List<CrfsFeatureVector>> trainingSequences, String vectorsOut) {
    try (BufferedWriter writer = new BufferedWriter(new FileWriter(vectorsOut))) {
      for (List<CrfsFeatureVector> sequence : trainingSequences) {
        for (CrfsFeatureVector fv : sequence) {
          writer.write(fv.toString());
          writer.newLine();
        }
        writer.newLine();
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
