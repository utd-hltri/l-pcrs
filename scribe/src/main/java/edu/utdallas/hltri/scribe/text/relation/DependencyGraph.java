package edu.utdallas.hltri.scribe.text.relation;

import java.util.*;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import edu.uci.ics.jung.graph.*;
import edu.uci.ics.jung.graph.util.EdgeType;
import edu.utdallas.hltri.logging.Logger;
import edu.utdallas.hltri.scribe.text.Document;
import edu.utdallas.hltri.scribe.text.annotation.Sentence;
import edu.utdallas.hltri.scribe.text.annotation.Token;

/**
 * Created with IntelliJ IDEA.
 * User: Ramon
 * Date: 8/27/14
 * Time: 1:40 PM
 */
public class DependencyGraph {
  private static final Logger log = Logger.get(DependencyGraph.class);

  private final Collection<Dependency> dependencies;
  private DirectedOrderedSparseMultigraph<Token, Dependency> graph = null;
  private DelegateForest<Token, Dependency> forest = null;

  protected DependencyGraph (Collection<Dependency> deps) {
    dependencies = deps;
  }

  public static DependencyGraph on(Collection<Dependency> deps) {
    return new DependencyGraph(deps);
  }

  public void addEdge (final Dependency dependency) {
    if (graph == null) {
      graph = asJungGraph();
    }
    graph.addVertex(dependency.getGovernor());
    graph.addVertex(dependency.getDependant());
    graph.addEdge(dependency, dependency.getGovernor(), dependency.getDependant(), EdgeType.DIRECTED);
    dependencies.add(dependency);
  }

  public Collection<Dependency> getDependencies() {
    return dependencies;
  }

  public DirectedOrderedSparseMultigraph<Token, Dependency> asJungGraph() {
    if (graph == null) {
      graph = new DirectedOrderedSparseMultigraph<>();
      for (Dependency dependency : dependencies) {
        if (!dependency.getGovernor().equals(dependency.getDependant())) {
          graph.addVertex(dependency.getDependant());
          graph.addVertex(dependency.getGovernor());
          graph.addEdge(dependency, dependency.getGovernor(), dependency.getDependant(), EdgeType.DIRECTED);
        } else {
          dependency.getDocument().removeRelation("stanford", dependency);
        }
      }
    }

    return graph;
  }

  /**
   * Returns the ordered list of dependencies that comprise the undirected path from source to destination.
   * Each Dependency is an ordered edge, so the returned sequence might have a point where the edge directions change
   * (if the path from source to destination passes through the root).
   * e.g. source <- w1 <- w2 <- root -> w3 -> destination
   * @param source find path from this token to destination. The governor of the first Dependency in the returned list.
   * @param destination find the path to this token from source. The dependant of the last Dependency in the returned list.
   * @return the ordered list of dependencies that comprise the undirected path from source to destination.
   */
  public List<Dependency> getPath(final Token source, final Token destination) {
    if (forest == null) {
      forest = new DelegateForest<>(asJungGraph());
    }
//    System.out.println("Made forest from graph " + graph);
    if (!forest.containsVertex(source) || !forest.containsVertex(destination) || source.equals(destination)) {
      return Collections.emptyList();
    }

    final List<Token> spath = forest.getPath(source);
//    System.out.printf("Found path from %s: %s\n", source, spath);
    final List<Token> dpath = forest.getPath(destination);
//    System.out.printf("Found path from %s: %s\n", destination, dpath);
    final ArrayList<Dependency> path = Lists.newArrayList();
    if (spath.contains(destination)) {
      Token prevToken = source;
      for (int i = 1; i <= spath.indexOf(destination); i++) {
        Token t = spath.get(i);
        Dependency edge = forest.findEdge(prevToken, t);
        if (edge == null) edge = forest.findEdge(t, prevToken);
        assert edge != null : String.format("1: s(%s) d(%s) spath(%s)", source, destination, spath);
        path.add(edge);
        prevToken = t;
      }
    } else if (dpath.contains(source)) {
      Token prevToken = source;
      for (int i = dpath.indexOf(source); i >= 0; i--) {
        Token t = dpath.get(i);
        if (!t.equals(prevToken)) {
          Dependency edge = forest.findEdge(prevToken, t);
          if (edge == null) edge = forest.findEdge(t, prevToken);
          assert edge != null : String.format("4: s(%s) d(%s) dpath(%s)", source, destination, dpath);
          path.add(edge);
        }
        prevToken = t;
      }
    } else {
      Token prevToken = source;
      for (int i = 1; i < spath.size(); i++) {
        Token t = spath.get(i);
        Dependency edge = forest.findEdge(prevToken, t);
        if (edge == null) edge = forest.findEdge(t, prevToken);
        assert edge != null : String.format("2: s(%s) d(%s) spath(%s) dpath(%s) path(%s)", source, destination, spath,
            dpath, path);
        path.add(edge);
        prevToken = t;
        if (dpath.contains(prevToken)) break;
      }

      if (!dpath.contains(prevToken)) {
        prevToken = dpath.get(dpath.size() - 1);
      }
      for (int i = dpath.indexOf(prevToken) - 1; i >= 0; i--) {
        Token t = dpath.get(i);
        Dependency edge = forest.findEdge(prevToken, t);
        if (edge == null) edge = forest.findEdge(t, prevToken);
        assert edge != null : String.format("3: s(%s) d(%s) spath(%s) dpath(%s) path(%s)", source, destination, spath,
            dpath, path);
        path.add(edge);
        prevToken = t;
      }
    }
    return path;
  }

  public DependencyGraph getSubGraph(final Token head) {
    final List<Dependency> edges = Lists.newArrayList();
    final LinkedList<Token> vertexQueue = new LinkedList<>();
    final Set<Token> walkedVertices = Sets.newHashSet();
    vertexQueue.add(head);
    while (!vertexQueue.isEmpty()) {
      final Token root = vertexQueue.removeFirst();
      for (final Token child : graph.getSuccessors(root)) {
        if (!walkedVertices.contains(child) && !vertexQueue.contains(child)) {
          edges.add(graph.findEdge(root, child));
          vertexQueue.addLast(child);
        }
      }
      walkedVertices.add(root);
    }
    return DependencyGraph.on(edges);
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder();
    for (Dependency dep : dependencies) {
      sb.append("[").append(dep.describe()).append("] ");
    }
    return sb.toString();
  }

  /**
   * Creates and returns a dependency graph of the passed sentence
   * @param s the sentence whose dependency graph will be created
   * @param annotationSet the annotation set of the Tokens and Dependencies in the graph
   * @return a dependency graph of the passed sentence
   */
  public static DependencyGraph of(final Sentence s, final String annotationSet) {
    final Set<Dependency> dependencies = Sets.newHashSet();
    final Document<?> document = s.getDocument();

    for (final Token token : s.getContained(annotationSet, Token.TYPE)) {
      // return all of and only those Dependency relations which involve a token in the sentence
      dependencies.addAll(document.getRelations(token, annotationSet, Dependency.TYPE));
    }

    if (dependencies.size() == 0) {
      throw new RuntimeException("No deps in sentence: " + s.describe());
    }

    return DependencyGraph.on(dependencies);
  }
}
