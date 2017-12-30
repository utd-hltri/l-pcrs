package edu.utdallas.hlt.medical.knowledge;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;

public class ICD9Resolver {

  // Cache for factory methods
  private static WeakHashMap<File, ICD9Resolver> cache = new WeakHashMap<>();

  /*
   * Expects a file of the format <ICD-9> <tab> <Text> Where an ICD-9 matches
   * [E|V]?\d+(\.\d+)?
   */
  public static ICD9Resolver getResolver(String path) {
    return getResolver(new File(path));
  }

  public static ICD9Resolver getResolver(File path) {
    if (!cache.containsKey(path)) {
      cache.put(path, new ICD9Resolver(path));
    }
    return cache.get(path);
  }
  private File path;
  private Map<String, String> icd9s = new HashMap<>();

  // Private constructor does nothing but save the path
  private ICD9Resolver(File path) {
    this.path = path;
  }

  // Lazy initializer
  private void init() {
    System.err.printf("Lazily initializing ICD-9 codes from %s.%n", path);
    try {
      icd9s = new HashMap<>();
      try (BufferedReader reader = new BufferedReader(new FileReader(path))) {
        String line;

        while ((line = reader.readLine()) != null) {
          String[] args = line.split("\\s+", 2);
          if (args.length == 2) {
            String name = args[0];
            String value = args[1];

            // Check for valid ICD-9 Code
            if (name.matches("[E|V]?\\d+(?:\\.\\d+)?")) {
              icd9s.put(name, value);
            }
          }
        }
      }
      System.err.printf("Loaded %,d ICD-9 mappings.%n", icd9s.size());
    } catch (IOException ex) {
      System.err.printf("Failed to initialize ICD-9 map from %s.", path);
      throw new RuntimeException(ex);
    }
  }

  protected String broaden(String ICD9) {
    if (icd9s.isEmpty()) {
      init();
    }

    ICD9 = ICD9.toUpperCase();
    ICD9 = ICD9.replaceAll("[^A-Z0-9.]", "");

    if (icd9s.get(ICD9) != null) {
      if (ICD9.indexOf(".") > -1) {
        return broaden(ICD9.substring(0, ICD9.length() - 1)) + " > " + icd9s.get(ICD9);
      } else {
        return icd9s.get(ICD9);
      }
    } else if (ICD9.indexOf(".") > -1) {
      return broaden(ICD9.substring(0, ICD9.length() - 1));
    }
    return null;
  }

  public String decode(String ICD9) {
    if (broaden(ICD9) == null) {
      if (broaden("0" + ICD9) == null) {
        if (broaden("00" + ICD9) == null) {
          return null;
        } else {
          return broaden("00" + ICD9);
        }
      } else {
        return broaden("0" + ICD9);
      }
    } else {
      return broaden(ICD9);
    }
  }

  public static void main(String... args) {
    ICD9Resolver r = ICD9Resolver.getResolver(args[0]);
    Console cons = System.console();
    String line;
    while ((line = cons.readLine("Decode: ")) != null && !line.equals("exit") && !line.equals("quit")) {
      cons.printf("Resolved %s to %s.", line, r.decode(line));
    }
  }
}