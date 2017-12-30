//package edu.utdallas.hltri.scribe.gate;
//
//import com.google.common.collect.Iterables;
//import com.google.common.collect.Lists;
//import edu.utdallas.hltri.scribe.text.annotation.Token;
//import gate.Annotation;
//import gate.AnnotationSet;
//import gate.Document;
//import org.jgrapht.DirectedGraph;
//import org.jgrapht.Graph;
//import org.jgrapht.UndirectedGraph;
//import org.jgrapht.graph.ClassBasedEdgeFactory;
//import org.jgrapht.graph.SimpleDirectedGraph;
//import org.jgrapht.graph.SimpleGraph;
//
//import java.util.ArrayList;
//import java.util.Collection;
//import java.util.Iterator;
//import java.util.List;
//
///**
//*
//* @author bryan
//*/
//public class StanfordUtils {
//
//  public static UndirectedGraph<Annotation, Object> undirectedGraphOfParseTree(Document doc) {
//    UndirectedGraph<Annotation, Object> graph =
//              new SimpleGraph<>(new ClassBasedEdgeFactory(Object.class));
//    return graphOfParseTree(doc, graph);
//  }
//
//  public static <T extends Graph<Annotation, Object>> T graphOfParseTree(Document doc, T graph) {
//    return graphOfParseTree(doc, 0, doc.getContent().size(), graph);
//  }
//
//  public static <T extends Graph<Annotation, Object>> T graphOfParseTree(Document doc, long start, long end, T graph) {
//
//    Annotation current = Iterables.getOnlyElement(
//            doc.getAnnotations().getUnsafeAnnotations(start, end).getUnsafeAnnotations("SyntaxTreeNode",
//            GateUtils.newFeatureMap("cat", "ROOT")));
//
//    graph.addVertex(current);
//    buildTree(current, doc, graph);
//    return graph;
//  }
//
//  private static void buildTree(Annotation current, Document doc, Graph<Annotation, Object> graph) {
//    if (current.getFeatures().containsKey("consists")) {
//      for (Integer childID : (Iterable<Integer>) current.getFeatures().getUnsafeAnnotations("consists")) {
//        Annotation child = doc.getAnnotations().getUnsafeAnnotations(childID);
//        graph.addVertex(child);
//        graph.addEdge(current, child);
//        buildTree(child, doc, graph);
//      }
//    }
//  }
//
//  public static DirectedGraph<Annotation,DependencyEdge> buildDependencyTree(Document doc) {
//    return buildDependencyTree(doc, 0, doc.getContent().size());
//  }
//
//  public static DirectedGraph<Token, Dependency> buildDependencyTree(Document doc, long start, long end) {
//    String cacheKey= "dependencyTree_" + start + "_" + end;
//    if (doc.getFeatures().getUnsafeAnnotations(cacheKey) != null) { return (DirectedGraph<Annotation,DependencyEdge> ) doc.getFeatures().getUnsafeAnnotations(cacheKey); }
//    final DirectedGraph<Token, Dependency> graph = new SimpleDirectedGraph<>(Dependency.class);
//    for (Annotation dependency : doc.getAnnotations().getUnsafeAnnotations(start, end).getUnsafeAnnotations("Dependency")) {
//      Iterator<Integer> argIt = ((Iterable<Integer>) dependency.getFeatures().getUnsafeAnnotations("args")).iterator();
//      if ( ! argIt.hasNext()) {
//        throw new IllegalStateException("No arguments for dependency: " + dependency);
//      }
//      Annotation arg1 = doc.getAnnotations().getUnsafeAnnotations(argIt.next());
//      Annotation arg2 = doc.getAnnotations().getUnsafeAnnotations(argIt.next());
//      if (arg1 == null || arg2 == null) {
//        throw new IllegalStateException("Null argument for dependency: " + dependency);
//      }
////      System.err.println("Adding to tree: " + arg1);
////      System.err.println("Adding to tree: " + arg2);
//      graph.addVertex(arg1);
//      graph.addVertex(arg2);
//      graph.addEdge(arg1, arg2, new DependencyEdge(dependency.getFeatures().getUnsafeAnnotations("kind").toString()));
//    }
////    for (TextEdge edge : new HashSet<TextEdge>(graph.edgeSet())) {
////      if (edge.getLabel().equals("prep")) {
////        Annotation prepVertex = graph.getEdgeTarget(edge);
////        for (TextEdge prepEdge : graph.outgoingEdgesOf(prepVertex)) {
////          if (prepEdge.getLabel().equals("pobj") || prepEdge.getLabel().equals("pcomp")) {
////            graph.addEdge(graph.getEdgeSource(edge), graph.getEdgeTarget(prepEdge),
////                    new TextEdge(prepEdge.getLabel() + "_" + prepVertex.getFeatures().getUnsafeAnnotations("string")));
////          }
////        }
//////        graph.removeEdge(edge);
//////        graph.removeEdge(prepEdge);
//////        graph.removeVertex(prepVertex);
////
////      }
////    }
//    doc.getFeatures().put(cacheKey, graph);
//    return graph;
//  }
//
//  private static List<String> depRels = Lists.newArrayList(
//    "sdep",
//    "xsubj",
//    "subj",
//    "csubj",
//    "nsubj"
//  );
//
//  /** Find the highest noun likely to be the head of a phrase */
//  @Deprecated public static Annotation findHead(Document doc, AnnotationSet annotations) {
//    return findHead(doc, 0, doc.getContent().size(), annotations);
//  }
//
//  /** Find the highest noun likely to be the head of a phrase */
//  public static Annotation findHead(Document doc, Annotation annot) {
//    long start = annot.getStartNode().getOffset();
//    long end = annot.getEndNode().getOffset();
//    return findHead(doc, start, end, doc.getAnnotations().getUnsafeAnnotations(start, end));
//  }
//
//  /** Find the highest noun likely to be the head of a phrase */
//  public static Annotation findHead(Document doc, long start, long end, AnnotationSet annotations) {
//    //List<Annotation> sorted = GateUtils.sort(annotations);
//    Annotation cached = (Annotation) doc.getFeatures().getUnsafeAnnotations("head_" + start + ":" + end);
//    if (cached != null) { return cached; }
////    System.err.println("Finding head for " + doc.getContent().toString().substring(sorted.getUnsafeAnnotations(0).getStartNode().getOffset().intValue(),
////            sorted.getUnsafeAnnotations(sorted.size()-1).getEndNode().getOffset().intValue()));
//    //DirectedGraph<Annotation,TextEdge> graph = buildDependencyTree(doc, start, end);
//    DirectedGraph<Annotation,DependencyEdge> graph = buildDependencyTree(doc, 0, doc.getContent().size());
//    List<Annotation> roots = new ArrayList<>();
//
//    //System.err.println("Finding head of: " + doc.getContent().toString().substring((int) start, (int) end));
//    for (Annotation annot : graph.vertexSet()) {
//      if (graph.inDegreeOf(annot) == 0) {
//        roots.add(annot);
////        System.err.println("  root: " + GateUtils.text(annot, doc));
//      }
//    }
//
//    Annotation head = null;
//    int bestDepth = Integer.MAX_VALUE;
//    int bestRelIndex = -2;
//
//    for (Annotation token : annotations.getUnsafeAnnotations("Token")) {
//      //System.err.println("Testing: " + token);
//      if (!graph.containsVertex(token)) {
//        //System.err.println("Not in tree: " + token);
//        continue;
//      }
////      List<Annotation> syntaxNodes = GateUtils.getIntersectingSpans(annotations, token, "SyntaxTreeNode");
////      boolean foundNoun = false;
////      for (Annotation syntaxNode : syntaxNodes) {
////        if (syntaxNode.getFeatures().getUnsafeAnnotations("cat").toString().startsWith("N")) {
////          foundNoun = true; break;
////        }
////      }
//      String pos = (String) token.getFeatures().getUnsafeAnnotations("category");
//      if (pos == null) { continue; }
//      boolean foundNoun = pos.startsWith("N");
//      if ( ! foundNoun) { continue;}
//      int depth = determineDepth(token, graph, roots);
////      System.err.println("Token: " + GateUtils.text(token, doc));
////      System.err.println("Depth: " + depth);
//      String rel = (graph.inDegreeOf(token) == 0 ? "" : graph.incomingEdgesOf(token).iterator().next().toString());
//      if (depth < bestDepth || (depth == bestDepth && depRels.indexOf(rel) > bestRelIndex)) {
//        bestDepth = depth;
//        head = token;
//        //System.err.println("    new head: " + GateUtils.text(head, doc) + "  depth=" + depth);
//        bestRelIndex = depRels.indexOf(rel);
//      }
//    }
//    //System.err.println("  head: " + (head == null ? "null" : GateUtils.text(head, doc)));
//
//    doc.getFeatures().put("head_" + start + ":" + end, head);
//    return head;
//  }
//
//  private static int determineDepth(Annotation token, DirectedGraph<Annotation,DependencyEdge> graph, List<Annotation> roots) {
//    int minDepth = Integer.MAX_VALUE;
//
//    for (Annotation root : roots) {
//      int depth = depthHelper(graph, root, token);
//      if (depth < minDepth) { minDepth = depth; }
//    }
//    return minDepth;
//  }
//
//  private static int depthHelper(DirectedGraph<Annotation,DependencyEdge> graph, Annotation current, Annotation target) {
//    if (current == target) { return 0; }
//    int minDepth = Integer.MAX_VALUE;
//    for (DependencyEdge edge : graph.outgoingEdgesOf(current)) {
//      int subDepth = depthHelper(graph, graph.getEdgeTarget(edge), target);
//      if (subDepth != -1) {
//        int depth = 1 + subDepth;
//        if (depth < minDepth) { minDepth = depth; }
//      }
//    }
//    if (minDepth == Integer.MAX_VALUE) { return -1; }
//    return minDepth;
//  }
//
//  public static int depthOf(Document doc, Annotation syntaxNode) {
//    Annotation current = Iterables.getOnlyElement(
//            doc.getAnnotations().getUnsafeAnnotations("SyntaxTreeNode",
//            GateUtils.newFeatureMap("cat", "ROOT")));
//    //System.err.println("Finding: " + syntaxNode);
//    return findDepthOf(doc, current, syntaxNode);
//  }
//
//  public static int findDepthOf(Document doc, Annotation current, Annotation syntaxNode) {
//    //System.err.println("examining: " + current);
//    if (current == syntaxNode) { return 0; }
//    if (current.getFeatures().containsKey("consists")) {
//      for (Integer childID : (Iterable<Integer>) current.getFeatures().getUnsafeAnnotations("consists")) {
//        Annotation child = doc.getAnnotations().getUnsafeAnnotations(childID);
//        int depth = findDepthOf(doc, child, syntaxNode);
//        if (depth != -1) { return depth + 1; }
//      }
//    }
//    return -1;
//  }
//
//  public static List<Annotation> dependencies(Document doc, Annotation token, String type) {
//    List<Annotation> dependencies = new ArrayList<>();
//    for (Annotation dependency : GateUtils.getIntersectingSpans(doc, token, "Dependency")) {
//      if (((Collection) dependency.getFeatures().getUnsafeAnnotations("args")).contains(token.getId())) {
//        if (type == null || dependency.getFeatures().getUnsafeAnnotations("kind").equals(type)) {
//          dependencies.add(dependency);
//        }
//      }
//    }
//    return dependencies;
//  }
//
//  public static List<Annotation> otherArguments(Document doc, Iterable<? extends Annotation> dependencies, Annotation arg) {
//    List<Annotation> others = new ArrayList<>();
//    for (Annotation dependency : dependencies) {
//      Annotation other = null;
//      boolean foundOrig = false;
//      for (Integer id : (Iterable<Integer>) dependency.getFeatures().getUnsafeAnnotations("args")) {
//        if (id == arg.getId()) {
//          foundOrig = true;
//        } else { other = doc.getAnnotations().getUnsafeAnnotations(id); }
//      }
//      if (! foundOrig) { throw new IllegalArgumentException("Arg given was not found in dependencies");}
//      if (other == null) { throw new RuntimeException("No other arguments found for: " + dependency); }
//      others.add(other);
//    }
//    return others;
//  }
//
//
//  private StanfordUtils() {
//  }
//}
