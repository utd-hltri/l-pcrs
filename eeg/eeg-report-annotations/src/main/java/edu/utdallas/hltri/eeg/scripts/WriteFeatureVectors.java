package edu.utdallas.hltri.eeg.scripts;

import edu.utdallas.hltri.eeg.Data;
import edu.utdallas.hltri.eeg.TensorflowUtils;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Created by rmm120030 on 9/19/16.
 */
public class WriteFeatureVectors {

  public static void main(String... args) {
    if ("activity".equals(args[0])) {
      TensorflowUtils.writeActivityVectorsOneLocation(Data.activeLearning(), args[1], "gold");
    }
    else if ("event".equals(args[0])) {
      TensorflowUtils.writeEventVectors(Data.activeLearning(), args[1], "gold");
    }
    else if ("boundary".equals(args[0])) {
      TensorflowUtils.writeBoundaryVectors(Data.activeLearning(), args[1], "gold");
    }
    else if ("al-boundary".equals(args[0])) {
      writeAllAlRunsBoundary(Paths.get(args[1]));
    }
    else {
      throw new RuntimeException("First argument to this class must be in [activity, event, boundary] and denotes what" +
          " kind of feature vectors you want.");
    }
  }

  private static void writeAllAlRunsBoundary(final Path outDir) {
    for (int i = 0; i < 10; i++) {
      final Path rundir = outDir.resolve("run" + i);
      rundir.toFile().mkdir();
      TensorflowUtils.writeBoundaryVectors(Data.run(i), rundir.toString(), "gold");
    }
  }
}
