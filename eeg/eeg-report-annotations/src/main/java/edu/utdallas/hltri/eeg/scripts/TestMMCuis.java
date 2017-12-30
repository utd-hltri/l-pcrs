package edu.utdallas.hltri.eeg.scripts;

import edu.utdallas.hlt.medbase.metamaplite.MetaMapLiteWrapper;

import java.util.Optional;
import java.util.Scanner;

/**
 * Created by rmm120030 on 2/10/17.
 */
public class TestMMCuis {

  public static void main(String... args) {
    Scanner scanner = new Scanner(System.in);
    final MetaMapLiteWrapper mml = MetaMapLiteWrapper.getInstance();
    String line = "no u";
    while (!line.trim().equalsIgnoreCase("done")) {
      System.out.println("Enter something you want cuis for (enter \"done\" when done):");
      line = scanner.nextLine();
      final Optional<String> cui = mml.getBestCuiForEntireSpan(line.trim());
      if (cui.isPresent()) {
        System.out.printf("Cui for %s: %s\n", line, cui.get());
      } else {
        System.out.println("No cuis found for " + line);
      }
    }
  }
}
