package edu.utdallas.hltri.scribe.gate;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import java.io.File;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import edu.utdallas.hltri.logging.Logger;
import edu.utdallas.hltri.util.Unsafe;
import gate.Annotation;
import gate.AnnotationSet;
import gate.DataStore;
import gate.Document;
import gate.Factory;
import gate.FeatureMap;
import gate.Gate;
import gate.LanguageAnalyser;
import gate.Resource;
import gate.corpora.DocumentContentImpl;
import gate.creole.AbstractLanguageAnalyser;
import gate.creole.AbstractProcessingResource;
import gate.creole.ExecutionException;
import gate.creole.ResourceInstantiationException;
import gate.persist.PersistenceException;
import gate.persist.SerialDataStore;
import gate.util.GateException;
import gate.util.OffsetComparator;

/**
 *
 * @author bryan
 */
public class GateUtils {

  private static Logger log = Logger.get(GateUtils.class);

  private static boolean isInitialized = false;

  public static synchronized void init() {
    if (!isInitialized) {
      try {
        final File gateHome = new File(System.getenv("GATE_HOME"));
        try {
          Gate.setGateHome(gateHome);
        } catch (IllegalStateException e) {
          if (!Gate.getGateHome().equals(gateHome)) {
            log.warn("Failed to change Gate home from {} to {}", Gate.getGateHome(), gateHome);
          }
        }
        Gate.init();
        Gate.getCreoleRegister().registerDirectories(new File(Gate.getPluginsHome(), "ANNIE").toURI().toURL());
        Gate.getCreoleRegister().registerDirectories(new File(Gate.getPluginsHome(), "OpenNLP").toURI().toURL());
        Gate.getCreoleRegister().registerDirectories(new File(Gate.getPluginsHome(), "Tagger_MetaMap").toURI().toURL());
        Gate.getCreoleRegister().registerDirectories(new File(Gate.getPluginsHome(), "Format_PubMed").toURI().toURL());
        Gate.getCreoleRegister().registerDirectories(new File(Gate.getPluginsHome(), "GENIA").toURI().toURL());
        isInitialized = true; //pigdog
      } catch (IOException | GateException e) {
        throw new RuntimeException(e);
      }
    }
  }

  public static class ResourceBuilder<T> {
    private final Class<T> clazz;
    private final String className;
    private FeatureMap params = Factory.newFeatureMap();
    private FeatureMap feats = Factory.newFeatureMap();

    private ResourceBuilder (Class<T> clazz) {
      this(clazz, clazz.getName());
    }

    private ResourceBuilder (Class<T> clazz, String name) {
      this.clazz = clazz;
      this.className = name;
    }

    public ResourceBuilder<T> params(Object value1, Object value2, Object... values) {
      params = gate.Utils.featureMap(value1, value2, values);
      return this;
    }

    public <K, V> ResourceBuilder<T> params(Map<K, V> map) {
      params = gate.Utils.toFeatureMap(map);
      return this;
    }

    public <K, V> ResourceBuilder<T> param(K key, V value) {
      params.put(key, value);
      return this;
    }

    public ResourceBuilder<T> feats(Object value1, Object value2, Object... values) {
      feats = gate.Utils.featureMap(value1, value2, values);
      return this;
    }

    public <K, V> ResourceBuilder<T> feats(Map<K, V> map) {
      feats = gate.Utils.toFeatureMap(map);
      return this;
    }

    public <K, V> ResourceBuilder<T> feat(K key, V value) {
      feats.put(key, value);
      return this;
    }

    public T build() {
      try {
        return clazz.cast(Factory.createResource(className, params, feats));
      } catch (ResourceInstantiationException e) {
        throw new RuntimeException(e);
      }
    }

    public T build(String name) {
      try {
        return clazz.cast(Factory.createResource(className, params, feats, name));
      } catch (ResourceInstantiationException e) {
        throw new RuntimeException(e);
      }
    }
  }

  public static <T> ResourceBuilder<T> loadResource(Class<T> clazz) {
    return new ResourceBuilder<>(clazz);
  }

  public static <T> ResourceBuilder<T> loadResource(Class<T> clazz, String name) {
    return new ResourceBuilder<>(clazz, name);
  }

  public static List<Annotation> getSubspansSorted(Document doc, Annotation annot, String type) {
    assert doc != null : "Null document";
    return getSubspansSorted(doc.getAnnotations(), annot, type);
  }

  public static List<Annotation> getSubspansSorted(AnnotationSet annotations, Annotation annot, String type) {
    List<Annotation> annots = new ArrayList<>();
    annots.addAll(annotations.get(type, annot.getStartNode().getOffset(), annot.getEndNode().getOffset()));
    Collections.sort(annots, new OffsetComparator());
    return annots;
  }

  public static AnnotationSet getSubspans(Document doc, Annotation annot, String type) {
    return doc.getAnnotations().getContained(annot.getStartNode().getOffset(), annot.getEndNode().getOffset()).get(type);
  }

  public static AnnotationSet getSubspans(Document doc, Annotation annot) {
    return doc.getAnnotations().getContained(annot.getStartNode().getOffset(), annot.getEndNode().getOffset());
  }

  public static List<Annotation> getIntersectingSpans(Document doc, Annotation annot, String type) {
    List<Annotation> annots = new ArrayList<>();
    annots.addAll(doc.getAnnotations().get(type, annot.getStartNode().getOffset(), annot.getEndNode().getOffset()));
    return annots;
  }

  public static List<Annotation> getIntersectingSpans(AnnotationSet set, Annotation annot, String type) {
    List<Annotation> annots = new ArrayList<>();
    annots.addAll(set.get(type, annot.getStartNode().getOffset(), annot.getEndNode().getOffset()));
    return annots;
  }

   public static List<Annotation> getCongruentSpans(Document doc, Annotation annot, String type) {
     return getCongruentSpans(doc, annot.getStartNode().getOffset(), annot.getEndNode().getOffset(), type);
   }

   public static List<Annotation> getCongruentSpans(Document doc, long start, long end, String type) {
    List<Annotation> annots = new ArrayList<>();
    for (Annotation annot2 : doc.getAnnotations().getCovering(type, start, end)) {
      if (annot2.getStartNode().getOffset() == start && annot2.getEndNode().getOffset() == end) {
        annots.add(annot2);
      }
    }
    return annots;
  }

  public static List<Annotation> getSubspansSorted(Document doc, Annotation annot) {
    return getSubspansSorted(doc, annot, null);
  }

  public static List<Annotation> getSubspansSorted(Document doc, String type) {
    return sort(doc.getAnnotations().get(type));
  }

  public static List<Annotation> getSubspansSorted(AnnotationSet annots, String type) {
    return sort(annots.get(type));
  }

  public static Annotation getShortestSuperspan(Document doc, Annotation span, String type) {
    Annotation shortest = null;
    long shortestLen = Long.MAX_VALUE;
    for (Annotation annot : doc.getAnnotations().get(type)) {
      if (span.withinSpanOf(annot) && ! span.equals(annot)) {
        long len = annot.getEndNode().getOffset() - annot.getStartNode().getOffset();
        if (shortest == null || (len < shortestLen)) {
          shortestLen = len;
          shortest = annot;
        }
      }
    }
    return shortest;
  }

  public static AnnotationSet annotationSetFor(Document doc, Annotation annot) {
    return doc.getAnnotations().get(annot.getStartNode().getOffset(), annot.getEndNode().getOffset());
  }

  public static boolean isIntersecting(Document doc, Annotation annot, String type) {
    return annotationSetFor(doc, annot).get(type).size() > 0;
  }

  public static long getLength(Annotation annot) {
    return annot.getEndNode().getOffset() - annot.getStartNode().getOffset();
  }

  public static FeatureMap newFeatureMap(Object... args) {
    return gate.Utils.featureMap(args);
  }

  public static FeatureMap newFeatureMap(Map<?, ?> map) {
    return gate.Utils.toFeatureMap(map);
  }

//  public static FeatureMap newFeatureMap(scala.collection.Map<?, ?> map) {
//    return gate.Utils.toFeatureMap(scala.collection.JavaConverters.mapAsJavaMapConverter(map).asJava());
//  }

  public static void annotate(Document doc, LanguageAnalyser... annotators) {
    try {
      for (LanguageAnalyser annotator : annotators) {
        annotator.setDocument(doc);
        annotator.execute();
      }
    } catch (ExecutionException e) {
      throw new RuntimeException(e);
    }
  }

  public static void annotate(Document doc, String className, String... args) {
    try {
      Resource resource;
      if (args.length > 0) {
        resource = Factory.createResource(className, newFeatureMap((Object) args));
      } else {
        resource = Factory.createResource(className);
      }
      ((AbstractLanguageAnalyser) resource).setDocument(doc);
      ((AbstractProcessingResource) resource).execute();
    } catch (ResourceInstantiationException | ExecutionException e) {
      throw new RuntimeException(e);
    }
  }

  public static String text(Annotation annot, Document doc) {
    if (annot == null) { throw new IllegalArgumentException("Null annotation"); }
    int start = annot.getStartNode().getOffset().intValue();
    int end = annot.getEndNode().getOffset().intValue();
    if (start < 0) { throw new RuntimeException("Start offset < 0"); }
    if (end < 0) { throw new RuntimeException("End offset < 0"); }
    if (start > end) { throw new RuntimeException("Start comes after end: " + annot); }
    return ((DocumentContentImpl) doc.getContent()).getOriginalContent()
            .substring(start, end);
  }

  public static String text(final Document doc) {
    return ((DocumentContentImpl) doc.getContent()).getOriginalContent();
  }

  public static List<Annotation> sort(Iterable<Annotation> annots) {
    List<Annotation> sorted = Lists.newArrayList(annots);
    Collections.sort(sorted, new OffsetComparator());
    return sorted;
  }

  public static void addAnnotationToDataStore(DataStore store, String annotationClass) {
    GateUtils.init();
    try {
      store.open();
      System.err.println("Opened store");
      final List<String> ids = Unsafe.cast(store.getLrIds("gate.corpora.DocumentImpl"));
      System.err.println("Got " + ids.size() + " doc ids. Loading...");
      final LanguageAnalyser annotator = loadResource(LanguageAnalyser.class, annotationClass).build();
      for (String id : ids) {
        Document doc = (Document) store.getLr("gate.corpora.DocumentImpl", id);
        annotator.setDocument(doc);
        annotator.execute();
        doc.sync();
      }
      store.close();
    } catch (PersistenceException | ExecutionException e) {
      throw new RuntimeException(e);
    }
    System.err.println("Done annotating");
  }

  /**
   * MUST OPEN THE STORE FIRST!!!
   */
//  public static Iterable<Document> iterableForDataStore(DataStore store)  {
//    try {
//      GateUtils.init();
//      final List<String> ids = (List<String>) store.getLrIds("gate.corpora.DocumentImpl");
//      //System.err.println(ids);
//      return Iterables.transform(ids, new ResourceIDToDocumentFunction(store));
//    } catch (PersistenceException pe) {
//      throw new RuntimeException(pe);
//    }
//  }

//  public static List<Document> loadDocuments(File path)  {
//    try {
//      SerialDataStore store = new SerialDataStore();
//      store.setStorageDir(path);
//      store.open();
//      GateUtils.init();
//      final List<String> ids = (List<String>) store.getLrIds("gate.corpora.DocumentImpl");
//      //System.err.println(ids);
//      List<Document> docs = new ArrayList<>(Lists.transform(ids, new ResourceIDToDocumentFunction(store)));
//      store.close();
//      return docs;
//    } catch (PersistenceException pe) {
//      throw new RuntimeException(pe);
//    }
//  }

  public static long length(Annotation annot) {
    return annot.getEndNode().getOffset() - annot.getStartNode().getOffset();
  }

  public static Annotation getLongest(Iterable<Annotation> annots) {
    Annotation longest = null;
    long maxLength = 0;
    for (Annotation annot : annots) {
      long length = length(annot);
      if (length > maxLength) {
        maxLength = length;
        longest = annot;
      }
    }
    return longest;
  }

  public static Annotation getLastToken(AnnotationSet annots) {
    List<Annotation> sorted = sort(annots.get("Token"));
    if (sorted.isEmpty()) { return null; }
    return Iterables.getLast(sorted);
  }

  public static Annotation getLastToken(Document doc, Annotation annot) {
    List<Annotation> tokens = sort(GateUtils.getIntersectingSpans(doc, annot, "Token"));
    if (tokens.isEmpty()) { return null; }
    return tokens.get(tokens.size()-1);
  }


  private static String uniqueSep = "C,I+d`JB<6";  // hey, that's the combination on my luggage!
  public static String uniqueIdentifier(Iterable<? extends Document> docs) {
     final MessageDigest digest;
    try {
      digest = MessageDigest.getInstance("MD5");
    }
    catch (NoSuchAlgorithmException nsae) {
      throw new RuntimeException("No MD5 algorithm exists");
    }
    for (Document doc : docs) {
      digest.update(((DocumentContentImpl) doc.getContent()).getOriginalContent().getBytes());
      digest.update(uniqueSep.getBytes());
      List<Annotation> annotations = Lists.newArrayList(doc.getAnnotations());
      Collections.sort(annotations, new Comparator<Annotation>() {
        @Override
        public int compare(Annotation o1, Annotation o2) {
          return o1.hashCode() - o2.hashCode();
        }
      });
      for (Annotation annot : annotations) {
        digest.update(annot.getType().getBytes());
        digest.update(uniqueSep.getBytes());
        if (annot.getFeatures() != null) {
          List<Entry<Object,Object>> entries = Lists.newArrayList(annot.getFeatures().entrySet());
          Collections.sort(entries, new Comparator<Entry<Object,Object>>() {

            @Override
            public int compare(Entry<Object, Object> o1, Entry<Object, Object> o2) {
              return o1.getKey().toString().compareTo(o2.getKey().toString());
            }
          });
          for (Entry<Object,Object> entry : entries) {
            digest.update(entry.getKey().toString().getBytes());
            digest.update(uniqueSep.getBytes());
            if (entry.getValue() != null) {
              digest.update(entry.getValue().toString().getBytes());
              digest.update(uniqueSep.getBytes());
            }
          }
        }
      }
    }

    final byte[] md5sum = digest.digest();
    StringBuilder builder = new StringBuilder();
    for (int i = md5sum.length-1; i >= 0; i--) {
      String hex = Integer.toHexString(md5sum[i] & 0xff);
      if (hex.length() < 2) { hex = "0" + hex; }
      builder.append(hex.substring(hex.length() - 2));

    }
    return builder.toString();
  }

  public static String uniqueIdentifier(Document doc) {
    return uniqueIdentifier(Lists.newArrayList(doc));
  }

//  public static void saveToNewCorpus(Iterable<Document> docs, File file) {
//    try {
//      SerialDataStore store = new SerialDataStore();
//      store.setStorageDir(file);
//      store.create();
//      store.open();
//
//      for (Document doc : docs) {
//        doc.setDataStore(store);
//        doc.sync();
//      }
//
//      store.close();
//    } catch (PersistenceException | SecurityException e) {
//      throw new RuntimeException(e);
//    }
//  }

  public static SerialDataStore createEmptyCorpus(File file) {
    try {
      SerialDataStore store = new SerialDataStore();
      store.setStorageDir(file);
      store.create();
      store.open();

      return store;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

//  public static Annotation findDeepestParseNode(Document doc, List<Annotation> syntaxNodes) {
//    Annotation deepest = null;
//    int maxDepth = -2;
//    for (Annotation annotation : syntaxNodes) {
//      int depth = StanfordUtils.depthOf(doc, annotation);
//      //System.err.println("Depth: " + depth + "  " + annotation);
//      if (deepest == null || depth > maxDepth) {
//        deepest = annotation;
//        maxDepth = depth;
//      }
//    }
//    return deepest;
//  }

  private GateUtils() {
  }
}
