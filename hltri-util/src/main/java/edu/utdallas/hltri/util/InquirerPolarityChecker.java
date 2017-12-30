package edu.utdallas.hltri.util;

import java.io.BufferedReader;
import java.io.Console;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author trevor
 */
public class InquirerPolarityChecker {

  // Initialize our hash map to store 11,788 keys
  private Map<String, Integer> polarity = new HashMap<>(11788);

  public static enum Polarity {
    NEUTRAL,
    POSITIVE,
    NEGATIVE
  };

  private int polarity(int p) {
    if (p > 0) {
      return 1;
    }
    if (p < 0) {
      return 2;
    }
    return 0;
  }

  public InquirerPolarityChecker(String source) {
    try {
      BufferedReader reader = new BufferedReader(new FileReader(source));

      String line, last = null, next;
      String[] fields;

      final String POSITIVE = "Positiv";
      final String NEGATIVE = "Negativ";

      // Skip first line
      reader.readLine();

      int sum = 0;
      while ((line = reader.readLine()) != null) {
        fields = line.split("\t", 5);

        next = fields[0].toLowerCase().replaceAll("#[0-9]+", "");
        if (!next.equals(last)) {
          polarity.put(last, polarity(sum));
          sum = 0;

          if (Math.random() < 1e-4) {
            System.out.printf("Put %s as %d.%n", last, polarity.get(last));
          }
        }

        if (fields[2].equals(POSITIVE)) {
          sum += 1;
        } else if (fields[3].equals(NEGATIVE)) {
          sum -= 1;
        }

        last = next;
      }

      polarity.put(last, polarity(sum));
    } catch (IOException ex) {
      throw new RuntimeException(ex);
    }
  }

  public Polarity getPolarity(String word) {
    Integer val = polarity.get(word.toLowerCase());

    if (val == null) {
      val = 0;
    }

    return Polarity.values()[val];
  }

  public static void main(String... args) {
    InquirerPolarityChecker checker = new InquirerPolarityChecker(args[0]);

    Console cons = System.console();
    String line;
    while (!(line = cons.readLine("Input: ")).equals("exit")) {
      cons.printf("Polarity: %s%n%n", checker.getPolarity(line));
    }
  }
}
