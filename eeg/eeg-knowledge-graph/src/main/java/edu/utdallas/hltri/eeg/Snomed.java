package edu.utdallas.hltri.eeg;

import edu.utdallas.hlt.medbase.snomed.SNOMEDManager;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Created by rmm120030 on 5/24/17.
 */
public class Snomed {
  private static final Map<String, Optional<String>> normCache = new HashMap<>();

  public static void main(String... args) {
    final Path path = Paths.get(args[0]);
    final SNOMEDManager snomed = new SNOMEDManager();
    try {
//      snomed.writeTriplesTransXStyle(path);
      snomed.writeTriplesSimple(path, Snomed::simpleNormalize);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private static Optional<String> metamapLiteNormalize(String s) {
    if (s == null) {
      return Optional.empty();
    }
    if (!normCache.containsKey(s)) {
      normCache.put(s, ConceptNormalization.normalizeConcept(s));
    }
    return normCache.get(s);
  }

  private static Optional<String> simpleNormalize(String s) {
    if (s == null) {
      return Optional.empty();
    }
    if (!normCache.containsKey(s)) {
      normCache.put(s, Optional.of(s.replaceAll("\\s", "_")));
    }
    return normCache.get(s);
  }
}
