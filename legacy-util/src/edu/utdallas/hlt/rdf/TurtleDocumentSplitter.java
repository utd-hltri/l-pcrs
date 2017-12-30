package edu.utdallas.hlt.rdf;

import java.io.*;

/**
 *
 * @author travis
 */
public class TurtleDocumentSplitter {

  public static final String DOC_START = "@base";
  public static final String HEADER_PREFIX = "@prefix";
  private File source, dest;
  private int last = 0;
  private BufferedWriter writer;
  private String name;

  public TurtleDocumentSplitter(String source, String dest) {
    this.source = new File(source);
    this.dest = new File(dest);
    this.dest.mkdirs();
    this.name = this.source.getName();
  }

  private void createNext() throws IOException {
    if (writer != null) { writer.close(); }

    String[] t = name.split("\\.");
    File f = new File(dest.getAbsolutePath(), t[0] + "_" + last + "." + t[1]);
    writer = new BufferedWriter(new FileWriter(f));
    System.err.println("Creating output file " + f + ".");
    last++;
  }

  private TurtleDocumentSplitter name(String name) {
    this.name = name;
    return this;
  }

  public void split(int size) throws FileNotFoundException, IOException {
    BufferedReader reader = new BufferedReader(new FileReader(source));
    StringBuilder sb = new StringBuilder();
    String line, header;

    // Collect prefix information
    while ((line = reader.readLine()) != null && line.startsWith(HEADER_PREFIX)) {
      sb.append(line).append('\n');
    }
    header = sb.toString();

    // Split document
    int count = size + 1;
    while ((line = reader.readLine()) != null) {
      if (line.startsWith(DOC_START)) {
        if (count > size) {
          createNext();
          writer.append(header);
          writer.newLine();
          count = 0;
        }
        writer.append(line);
        writer.newLine();
        count++;
      } else {
        writer.append(line);
        writer.newLine();
      }
    }

    writer.close();
  }

  public static void main(String... args) throws FileNotFoundException, IOException {
    if (args.length == 4) {
      new TurtleDocumentSplitter(args[0], args[1]).name(args[4]).split(Integer.parseInt(args[2]));
    } else if (args.length == 3) {
      new TurtleDocumentSplitter(args[0], args[1]).split(Integer.parseInt(args[2]));
    } else {
      System.err.println("Usage: " + TurtleDocumentSplitter.class.getName() + "path/to/input path/to/output/dir outputFileSize [outputFileName.outputFileExt]" );
    }
  }
}
