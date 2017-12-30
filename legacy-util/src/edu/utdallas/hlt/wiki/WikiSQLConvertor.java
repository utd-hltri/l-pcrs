package edu.utdallas.hlt.wiki;

import java.io.*;
import java.io.InputStreamReader;
import java.util.*;
import java.util.zip.*;

/**
 * http://download.wikimedia.org/enwiki/20100622/enwiki-20100622-redirect.sql.gz
 * @author bryan
 */
public class WikiSQLConvertor {

  public static void main(String... args) throws Exception {
    InputStreamReader reader = new InputStreamReader(new GZIPInputStream(new FileInputStream(new File(args[0]))));
    BufferedReader buffReader = new BufferedReader(reader);

    List<String> fields = new ArrayList<String>();
    StringBuilder field = new StringBuilder();

    String line;
    Set<Integer> indexesToPrint = new HashSet<Integer>();
    indexesToPrint.add(0);
    indexesToPrint.add(1);
    indexesToPrint.add(2);
    while ((line = buffReader.readLine()) != null) {
      System.err.print("*");
      if (line.startsWith("INSERT")) {
        int offset = line.indexOf("(");
        boolean newLine = false;
        while ( ! newLine) {
          fields.clear();
          field.setLength(0);
          boolean insideString = false;
          while (true) {
            offset++;
            if (line.charAt(offset) == '\'') {
              insideString = ! insideString;
            } else if (line.charAt(offset) == '\\') {
              field.append(line.charAt(offset+1));
              offset++;
            } else if (insideString) {
              field.append(line.charAt(offset));
            } else if (line.charAt(offset) == ')') {
              fields.add(field.toString());
              int i = 0;
              int index = 0;
              for (String value : fields) {
                if ( ! indexesToPrint.contains(index++)) { continue; }
                if (i > 0) { System.out.print("\t"); }
                System.out.print(value);
                i++;
              }
              System.out.println();
              if (line.charAt(offset+1) == ';') {
                newLine = true;
              } else {
                offset += 2;
              }
              break;
            } else if (line.charAt(offset) == ',') {
              fields.add(field.toString());
              field.setLength(0);
            } else {
              field.append(line.charAt(offset));
            }
          }
        }
      }
    }
  }
}
