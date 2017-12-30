package edu.utdallas.hlt.rdf;

import com.clarkparsia.fourstore.sesame.FourStoreSailRepository;
import com.google.common.base.Throwables;
import edu.utdallas.hlt.text.Token;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import org.openrdf.query.*;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;

/**
 *
 * @author bryan
 */
public class Query4Store {

  private FourStoreSailRepository repository;
  private RepositoryConnection con;

  public Query4Store(URL repositoryURL) {
    try {
      repository = new FourStoreSailRepository(repositoryURL);
      repository.initialize();
      con = repository.getConnection();
    } catch (RepositoryException ex) {
      throw Throwables.propagate(ex);
    }
  }

  public TupleQueryResult query(String sparql) {

    try {

      TupleQuery tupleQuery = con.prepareTupleQuery(QueryLanguage.SPARQL, sparql);
      TupleQueryResult result = tupleQuery.evaluate();
      return result;
//      while (result.hasNext()) {
//
//        BindingSet binding = result.next();
//        Map<String, Token> bindings = new HashMap<String, Token>();
//        Iterator<Binding> it = binding.iterator();
//
//        while (it.hasNext()) {
//          Binding bind = it.next();
//          System.err.println(bind.getName() + " = " + bind.getValue().stringValue());
//        }
//      }

    } catch (RepositoryException | MalformedQueryException | QueryEvaluationException ex) {
      throw new RuntimeException("Error, sparql: " + sparql, ex);
    }
  }

  public void close() {
    try {
      con.close();
      repository.shutDown();
    } catch (RepositoryException ex) {
      throw Throwables.propagate(ex);
    }
  }

  public static void main(String... args) throws Exception {
    FourStoreSailRepository repository = new FourStoreSailRepository(new URL(args[0]));
    repository.initialize();
    RepositoryConnection con = repository.getConnection();
    TupleQuery tupleQuery = con.prepareTupleQuery(QueryLanguage.SPARQL, args[1]);
    TupleQueryResult result = tupleQuery.evaluate();
        while (result.hasNext()) {

    BindingSet binding = result.next();
    Map<String, Token> bindings = new HashMap<>();
    Iterator<Binding> it = binding.iterator();
    System.err.println();
    while (it.hasNext()) {
        Binding bind = it.next();
        System.err.println(bind.getName() + " = " + bind.getValue().stringValue());
      }
    }
    con.close();
    repository.shutDown();
  }
}
