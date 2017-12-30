package edu.utdallas.hlt.medical.onto;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.Writer;


/**
 *
 * @author bryan
 */
public class SNOMEDAnatomicalRelationOutputter {

  public static void main(String... args) throws Exception {
    File snomedRoot= new File(args[0]);
    File cacheRoot= new File(args[1]);
    SNOMEDManager manager = new SNOMEDManager(snomedRoot, cacheRoot);
    try (Writer writer = new BufferedWriter(new FileWriter(new File(args[2])))) {
      for (Long id : manager.conceptIDS()) {
        int count = 0;
        for (String syn : manager.getSynonyms(id)) {
          if (count++ > 0) { writer.write("\t"); }
          writer.write(syn);
        }
        writer.append("|");
        for (Long site : manager.getRelatedConceptIDs(id, SNOMEDManager.SNOMEDRelationshipType.FINDING_SITE, 1, SNOMEDManager.SNOMEDRelationshipDirection.TARGET)) {
          for (String syn : manager.getSynonyms(site)) {
            if (count++ > 0) { writer.write("\t"); }
            writer.write(syn);
          }
        }
        writer.write("\n");
      }
    }
  }
}
