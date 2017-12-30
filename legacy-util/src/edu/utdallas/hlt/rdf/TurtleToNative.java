package edu.utdallas.hlt.rdf;

import java.io.File;
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.sail.SailRepository;
import org.openrdf.rio.RDFFormat;
import org.openrdf.sail.nativerdf.NativeStore;

/**
 *
 * @author bryan
 */
public class TurtleToNative {
  public static void main(String... args) throws Exception {
    NativeStore store = new org.openrdf.sail.nativerdf.NativeStore(new File(args[1]));
    Repository repository = new SailRepository( store);
    repository.initialize();
    RepositoryConnection con = repository.getConnection();
    System.err.println("Adding");
    con.add(new File(args[0]), "http://hlt.utdallas.edu/", RDFFormat.TURTLE);
    System.err.println("Done");
    con.close();
    repository.shutDown();
  }
}
