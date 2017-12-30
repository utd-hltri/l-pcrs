package edu.utdallas.hltri.scribe.text;

import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import edu.utdallas.hltri.Describable;
import edu.utdallas.hltri.io.AC;
import edu.utdallas.hltri.logging.Logger;
import edu.utdallas.hltri.scribe.gate.GateUtils;
import edu.utdallas.hltri.scribe.io.Corpus;
import edu.utdallas.hltri.scribe.text.annotation.Annotation;
import edu.utdallas.hltri.scribe.text.annotation.AnnotationType;
import edu.utdallas.hltri.scribe.text.annotation.UnsafeAnnotation;
import edu.utdallas.hltri.scribe.text.relation.Relation;
import edu.utdallas.hltri.scribe.text.relation.RelationType;
import edu.utdallas.hltri.scribe.text.relation.UnsafeRelation;
import gate.AnnotationSet;
import gate.Factory;
import gate.FeatureMap;
import gate.creole.ResourceInstantiationException;
import gate.relations.RelationSet;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

/**
 * Created by travis on 2/20/15.
 */
public class Document<D extends BaseDocument> extends Text implements
    PropertyMap<D>,
    HasFeatureMap,
    Describable,
    Identifiable, AC,
    Serializable {

  private static final Logger log = Logger.get(Document.class);

  public final gate.FeatureMap features;

  protected final String string;

  protected final gate.Document gateDocument;

  protected final AtomicBoolean dirty = new AtomicBoolean();

  private transient Optional<Corpus<D>> corpus = Optional.empty();
  private transient boolean cleanFeats = true;

   protected Document(String string) {
    this.string = string;
    try {
      GateUtils.init();
      this.gateDocument = gate.Factory.newDocument(string);
      this.features = gateDocument.getFeatures();
    } catch (ResourceInstantiationException e) {
      throw new RuntimeException(e);
    }
  }

  protected Document(gate.Document gateDocument) {
    this.gateDocument = gateDocument;
    this.features = gateDocument.getFeatures();
    this.string = GateUtils.text(gateDocument);
  }

  public static <T extends BaseDocument> Document<T> fromGate(gate.Document gateDocument) {
    return new Document<>(gateDocument);
  }


  public static <T extends BaseDocument> Document<T> fromString(String string) {
    return new Document<>(string);
  }

  @SuppressWarnings("unchecked")
  public static <T extends BaseDocument> Document<T> fromStringWithMime(String string, String mimeType) {
    GateUtils.init();
    FeatureMap params = gate.Factory.newFeatureMap();
    params.put(gate.Document.DOCUMENT_STRING_CONTENT_PARAMETER_NAME, string);
    params.put(gate.Document.DOCUMENT_MIME_TYPE_PARAMETER_NAME, mimeType);
    try {
      return new Document<>((gate.Document) Factory.createResource("gate.corpora.DocumentImpl", params));
    } catch (ResourceInstantiationException e) {
      throw new RuntimeException(e);
    }
  }

  public static <T extends BaseDocument> Document<T> empty() {
    GateUtils.init();
    try {
      return new Document<>((gate.Document) Factory.createResource("gate.corpora.DocumentImpl"));
    } catch (ResourceInstantiationException e) {
      throw new RuntimeException(e);
    }
  }

  public static <T extends BaseDocument> Document<T> fromFile(String file) {
    return fromPath(Paths.get(file));
  }

  public static <T extends BaseDocument> Document<T> fromFile(File file) {
    return fromPath(file.toPath());
  }

  public static <T extends BaseDocument> Document<T> fromPath(Path path) {
    try {
      final Document<T> doc = fromString(new String(Files.readAllBytes(path)));
      doc.set(BaseDocument.path, path.toAbsolutePath().toString());
      doc.set(BaseDocument.fileName, path.getFileName().toString());
      doc.set(BaseDocument.url, path.toUri().toURL());
      doc.set(BaseDocument.id, path.getFileName().toString().replace(".txt", ""));
      return doc;
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public Document<D> setDirty() {
    this.dirty.set(true);
    return this;
  }

  public Document<D> clearDirty() {
    this.dirty.set(false);
    return this;
  }

  public Document<D> setCorpus(final Corpus<D> corpus) {
    this.corpus = Optional.of(corpus);
    return this;
  }

  /**
   * Synchronize this document with the underlying data-store
   * The document have either
   *   (1) originated from a data-store (e.g. Corpus.lazy.fromDataStore), or
   *   (2) already been saved to a data-store (e.g. Corpus.saveToDataStore)
   *
   * @return this
   */
  public void sync() {
    assert corpus.isPresent() : "No corpus to sync to.";
    corpus.get().save(this);
    clearDirty();
  }

  /**
   * EXPERT: Returns the underlying gate.Document this Document wraps
   * @return gate.Document corresponding to this Document
   */
  public gate.Document asGate() {
    return gateDocument;
  }

  /**
   * Returns the String content of this document in lower-case with excess white-space removed
   * @return simplified String
   */
  public String asSimpleString() {
    return asString().toLowerCase().trim();
  }

  public String asString() {
    return string;
  }

  // Annotation stuff

  public <T extends Annotation<T>> gate.AnnotationSet getAnnotationSet(final AnnotationType<T> type) {
    return gateDocument.getAnnotations().get(type.getName());
  }

  public <T extends Annotation<T>> gate.AnnotationSet getAnnotationSet(final String name, final AnnotationType<T> type) {
    return gateDocument.getAnnotations(name).get(type.getName());
  }

  public <T extends Annotation<T>> List<T> convertAnnotationSet(final gate.AnnotationSet annotationSet, final AnnotationType<T> type) {
    final List<T> annotations = Lists.newArrayList();
    for (final gate.Annotation gateAnnotation : gate.Utils.inDocumentOrder(annotationSet.get(type.getName()))) {
      annotations.add(type.wrap(this, gateAnnotation));
    }
    return AnnotationList.create(annotationSet, annotations);
  }

  public <T extends Annotation<T>> Optional<T> getAnnotationById(final int id, final AnnotationType<T> type) {
    for (String annSet : getAnnotationSets()) {
      for (T ann : get(annSet, type)) {
        if (ann.asGate().getId() == id) {
          return Optional.of(ann);
        }
      }
    }
    return Optional.empty();
  }

  public Optional<UnsafeAnnotation> getUnsafeAnnotationById(final int id) {
    for (String annSet : getAnnotationSets()) {
      for (UnsafeAnnotation ua : getUnsafeAnnotations(annSet)) {
        if (ua.getGateId() == id) {
          return Optional.of(ua);
        }
      }
    }
    return Optional.empty();
  }

  // AnnotationSet stuff

  /**
   * Get the annotations corresponding to the given AnnotationType inside the default annotation set
   * @param type AnnotationType to look for (e.g. Token.TYPE)
   * @param <T> type of Annotation to return (e.g. Token)
   * @return List of Annotations corresponding to the given AnnotationType
   * @see edu.utdallas.hltri.scribe.text.annotation.Annotation
   * @see edu.utdallas.hltri.scribe.text.annotation.AnnotationType
   */
  public <T extends Annotation<T>> List<T> get(final AnnotationType<T> type) {
    return convertAnnotationSet(gateDocument.getAnnotations(), type);
  }

  /**
   * Get the annotations corresponding to the given AnnotationType inside the given annotation set
   * @param annotationSet Name of the annotation set to search within
   * @param type AnnotationType to look for (e.g. Token.TYPE)
   * @param <T>  type of Annotation to return (e.g. Token)
   * @return List of Annotations corresponding to the given AnnotationType within the given annotation set
   * @see edu.utdallas.hltri.scribe.text.annotation.Annotation
   * @see edu.utdallas.hltri.scribe.text.annotation.AnnotationType
   */
  synchronized public <T extends Annotation<T>> List<T> get(final String annotationSet, final AnnotationType<T> type) {
    return convertAnnotationSet(gateDocument.getAnnotations(annotationSet), type);
  }

  /**
   * EXPERT: Get the unsafe, untyped annotations corresponding to the default annotation set
   *
   * @return List of UnsafeAnnotations corresponding to the default Annotation set
   * @see edu.utdallas.hltri.scribe.text.annotation.UnsafeAnnotation
   */
  @Deprecated
  public Collection<UnsafeAnnotation> get() {
    log.warn("Fetching annotations from default annotation set...");
    List<UnsafeAnnotation> annotations = Lists.newArrayList();
    for (final gate.Annotation gateAnnotation : gate.Utils.inDocumentOrder(gateDocument.getAnnotations())) {
      annotations.add(new UnsafeAnnotation(this, gateAnnotation));
    }
    return annotations;
  }

  public boolean hasAnnotationSet(final String annotationSet) {
    return !gateDocument.getAnnotations(annotationSet).isEmpty();
  }

  /**
   * EXPERT: Get the unsafe, untyped annotations corresponding to the given annotation set
   *
   * @return List of UnsafeAnnotations corresponding to the given Annotation set
   * @see edu.utdallas.hltri.scribe.text.annotation.UnsafeAnnotation
   */
  public List<UnsafeAnnotation> getUnsafeAnnotations(final String annotationSet) {
    List<UnsafeAnnotation> annotations = Lists.newArrayList();
    for (final gate.Annotation gateAnnotation : gate.Utils.inDocumentOrder(gateDocument.getAnnotations(annotationSet))) {
      annotations.add(new UnsafeAnnotation(this, gateAnnotation));
    }
    return annotations;
  }

  /**
   * Gets the set of annotation types for a specific annotation set from Gate
   * @param annotationSet the annotation set whose types will be returned
   * @return a set of annotation types for the annotation set passed
   */
  public Set<String> getAnnotationTypes(String annotationSet) {
    return asGate().getAnnotations(annotationSet).getAllTypes();
  }

  /**
   * Returns the set of Relation<R,A,B> of the given relation set and type
   * Will replace instances of UnsafeRelation in the RELATION_ID_MAP with an R instance using replaceUnsafeRelationsWithTyped
   * @param annSet the annotation set of the desired relations
   * @param type the relation type of the desired relations
   * @param <R> the class type of the desired relation
   * @return the set of Relation<R,A,B> of the given relation set and type
   */
  public <R extends Relation<R, G, Dep>, G extends Annotation<G>, Dep extends Annotation<Dep>> Set<R>
  getRelations(final String annSet, final RelationType<R,G,Dep> type) {
    return gateDocument.getAnnotations(annSet).getRelations().getRelations(type.getName()).stream()
        .map(gr -> type.wrap(this, gr, annSet)).collect(Collectors.toSet());
  }

  /**
   * @param annotation returns all the relations involving this annotation. Should be of type compatible with the passed
   *                   RelationType, type
   * @param annSet returns relations from this annset
   * @param type returns relations of this type
   * @return all the relations involving annotation
   */
  public <R extends Relation<R, G, Dep>, G extends Annotation<G>, Dep extends Annotation<Dep>> Set<R> getRelations(
      final Annotation<?> annotation, final String annSet, final RelationType<R,G,Dep> type) {
    final Set<R> relations = Sets.newHashSet();
    for (final gate.relations.Relation gateRel : asGate().getAnnotations(annSet).getRelations().getReferencing(annotation.getGateId())) {
      // if the gate relation is of the correct type
      if (gateRel.getType().equals(type.getName())) {
        // create a new Annotation of the dependant's gate annotation and add it to the set
        relations.add(type.wrap(this, gateRel, annSet));
      }
    }
    return relations;
  }

  public <R extends Relation<R, G, Dep>, G extends Annotation<G>, Dep extends Annotation<Dep>> Set<Dep> getDependants(
      final G governor, final RelationType<R,G,Dep> type, final String annSet) {
    final Set<Dep> dependants = Sets.newHashSet();
    // for each gate relation involving governor
    for (final gate.relations.Relation gateRel : gateDocument.getAnnotations(annSet).getRelations().getReferencing(governor.getGateId())) {
      // if the gate relation is of the correct type and has governor as it's governor
      if (gateRel.getType().equals(type.getName()) && (governor.getGateId() == gateRel.getMembers()[0])) {
        // create a new Annotation of the dependant's gate annotation and add it to the set
        dependants.add(type.dependantType().wrap(this, asGate().getAnnotations(annSet).get(gateRel.getMembers()[1])));
      }
    }
    return dependants;
  }

  public <R extends Relation<R, G, Dep>, G extends Annotation<G>, Dep extends Annotation<Dep>> Set<G> getGovernors(
      final Dep dependant,final RelationType<R, G, Dep> type,final String annSet) {
    final Set<G> governors = Sets.newHashSet();
    // for each gate relation involving dependant
    for (final gate.relations.Relation gateRel : gateDocument.getAnnotations(annSet).getRelations().getReferencing(dependant.getGateId())) {
      // if the gate relation is of the correct type and has dependant as it's dependant
      if (gateRel.getType().equals(type.getName()) && (dependant.getGateId() == gateRel.getMembers()[1])) {
        // create a new Annotation of the governor's gate annotation and add it to the set
        governors.add(type.governorType().wrap(this, asGate().getAnnotations(annSet).get(gateRel.getMembers()[0])));
      }
    }
    return governors;
  }

  /**
   * Returns all relations of the given relation set
   * @param annSet the annotation set of the desired relations
   * @return all relations of the given relation set
   */
  public Set<UnsafeRelation> getUnsafeRelations(final String annSet) {
    final Set<UnsafeRelation> relations = Sets.newHashSet();
    for (gate.relations.Relation relation : asGate().getAnnotations(annSet).getRelations()) {
      relations.add(UnsafeRelation.wrap(this, annSet, relation));
    }
    return relations;
  }

  public Set<String> getRelationTypes(final String annSet) {
    return gateDocument.getAnnotations(annSet).getRelations().stream()
        .map(gate.relations.Relation::getType)
        .collect(Collectors.toSet());
  }

  /**
   * Removes the passed relation from the document.
   * @param annSet the annotation set of the related annotations
   * @param relation the relation to be deleted
   */
  public <R extends Relation<R, ?, ?>> void removeRelation(
      final String annSet, final R relation) {
    setDirty();
    final RelationSet relSet = gateDocument.getAnnotations(annSet).getRelations();
    relSet.remove(relation.asGate());
  }

  /**
   * Removes all relations on annotations of the passed annotation set.
   * @param annSet the annotation set of the annotations whose relations are to be deleted.
   * @param type the RelationType of relations to be deleted.
   */
  public <R extends Relation<R, G, Dep>, G extends Annotation<G>, Dep extends Annotation<Dep>> void clear(
      final String annSet, final RelationType<R,G,Dep> type) {
    setDirty();
    final RelationSet relSet = gateDocument.getAnnotations(annSet).getRelations();
    relSet.removeAll(relSet.getRelations(type.getName()));
  }

  /**
   * View the substring of this Document's context between the given bounds
   * Is exactly equivalent to {@code asString().substring((int) start, (int) end)}
   * @param start Inclusive lower character offset
   * @param end Exclusive upper character offset
   * @return String corresponding to the given substring
   */
  public String subString(long start, long end) {
    return asString().substring((int) start, (int) end);
  }

  public Set<String> getAnnotationSets() {
    return gateDocument.getAnnotationSetNames();
  }

  public Set<String> getRelationSets() {
    return getAnnotationSets().stream()
        .filter(as -> !gateDocument.getAnnotations(as).getRelations().isEmpty())
        .collect(Collectors.toSet());
  }

  public void clear(String annotationSet) {
    setDirty();
    gateDocument.removeAnnotationSet(annotationSet);
  }

  public <T extends Annotation<T>> void clear(String annotationSet, AnnotationType<T> type) {
    setDirty();
    final AnnotationSet set = gateDocument.getAnnotations(annotationSet);
    set.removeAll(set.get(type.getName()));
  }

  public <T extends Annotation<T>> void clear(AnnotationType<T> type) {
    setDirty();
    final AnnotationSet set = gateDocument.getAnnotations();
    set.removeAll(set.get(type.getName()));
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    Document<?> document = (Document<?>) o;
    return Objects.equals(string, document.string) &&
        Objects.equals(gateDocument, document.gateDocument) &&
        Objects.equals(features, document.features);
  }

  @Override
  public int hashCode() {
    return Objects.hash(string, gateDocument, features);
  }

  @Override public void close() {
    if (dirty.get()) {
      log.warn("Closing document {} with unsynced changes!", getId());
    }
    for (String asName: gateDocument.getAnnotationSetNames()) {
      final gate.AnnotationSet as = gateDocument.getAnnotations(asName);
      as.getRelations().clear();
      as.clear();
    }
    gateDocument.getNamedAnnotationSets().clear();
    gate.Factory.deleteResource(gateDocument);
  }

  /**
   * Constructs a new Document containing only the content and annotations between the given bounds
   * The content of this document will be the corresponding {@code subString(string, end)}
   * The annotations in this document will be the subset of annotations that exist entirely within the given bounds
   *
   * @param start Inclusive lower character offset
   * @param end   Exclusive upper character offset
   * @return Document bounded by the given offsets
   */
  public Document<D> subDocument(long start, long end) {
    try {
      final gate.Document result = Factory.newDocument(subString(start, end));
      for (String name : gateDocument.getAnnotationSetNames()) {
        result.getAnnotations(name).addAll(gateDocument.getAnnotations(name).get(start, end));
      }
      return new Document<>(result);
    } catch (ResourceInstantiationException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Constructs a new Document containing only the content and annotations between the given bounds
   * The annotations in this document will be the subset of annotations that exist entirely within the given bounds
   *
   * @param start Annotation whose start offset marks the inclusive lower bounds
   * @param end   Annotation whose end offset marks the exclusive upper bounds
   * @return Document bounded by the given offsets
   */
  public <T extends Annotation<T>> Document<D> subDocument(Annotation<T> start, Annotation<T> end) {
    return subDocument(start.get(Annotation.StartOffset), end.get(Annotation.EndOffset));
  }



  /**
   * Returns true if the map contains the given key.
   */
  public <T> boolean has(DocumentAttribute<? super D, T> key) {
    return features.containsKey(key.name);
  }

  /**
   * Returns the value associated with the given key or null if
   * none is provided.
   */
  public <T> T get(DocumentAttribute<? super D, T> key) {
    return key.type.cast(features.get(key.name));
  }

  /**
   * Used to allow Scala's doc(attr) syntax for doc.get(attr)
   * @param key attribute
   * @param <T> type of returned attribute
   * @return attribute cast as type <T>
   */
  public <T>T apply(DocumentAttribute<? super D, T> key) {
    return get(key);
  }

  public <T> T getOrUpdate(DocumentAttribute<? super D, T> key, T value) {
    if (!has(key)) {
      set(key, value);
    }
    return get(key);
  }

  public <T> T getOrElse(DocumentAttribute<? super D, T> key, T value) {
    if (!has(key)) {
      return value;
    }
    return get(key);
  }

  public <T> Optional<T> getOptional(DocumentAttribute<? super D, T> key) {
    return Optional.ofNullable(get(key));
  }

  /**
   * Associates the given value with the given type for future calls
   * to getUnsafeAnnotations.  Returns the value removed or null if no value was present.
   */
  public <T> Document<D> set(DocumentAttribute<? super D, T> key, T value) {
    setDirty();
    cleanFeats = false;
    key.setter.accept(this, value);
    features.put(key.name, value);
    return this;
  }

  /**
   * Flag to tell if the features file should be serialilzed.
   * @return
   */
  public boolean cleanFeats() {
    return cleanFeats;
  }

  /**
   * Used to let Scala use doc(attr) = value syntax for doc.set(attr, value)
   * @param key   attribute to update
   * @param value new value for attribute
   * @param <T> type of value accepted by attribute
   * @return  this
   */
  public <T> Document<D> update(DocumentAttribute<? super D, T> key, T value) {
    return set(key, value);
  }

  /**
   * Removes the given key from the map, returning the value removed.
   */
  public <T> Document<D> remove(DocumentAttribute<? super D, T> key) {
    setDirty();
    features.remove(key);
    return this;
  }

  @Override
  public String getId() {
    return get(BaseDocument.id);
  }

  public int numAttributes() {
    return features.size();
  }

  @Override public String describe() {
    final StringBuilder sb = new StringBuilder();
    sb.append("== Document |").append(this.get(BaseDocument.id)).append("| ==\n");
    sb.append("Content:\n").append(asString()).append('\n');
    sb.append("Features:\n");
    for (Map.Entry<Object, Object> feature : features.entrySet()) {
      sb.append(" • [").append(feature.getKey()).append("] = ").append(feature.getValue()).append('\n');
    }
    sb.append("Annotations:\n");
    for (UnsafeAnnotation uann : get()) {
      sb.append(" • ").append(uann.describe()).append('\n');
    }
    for (String name : getAnnotationSets()) {
      for (UnsafeAnnotation uann : getUnsafeAnnotations(name)) {
        sb.append(" • ").append(name).append(':').append(uann.describe()).append('\n');
      }
    }
    final Set<String> relationSets = getRelationSets();
    if (!relationSets.isEmpty()) {
      sb.append(
          "Relations:\n");//.append(getRelationSets().stream().map(Map::size).reduce((s1, s2) -> s1 + s2).orElse(0)).append("):\n");
      for (String relSet : relationSets) {
        for (UnsafeRelation urel : getUnsafeRelations(relSet)) {
          sb.append(" • ").append(relSet).append(':').append(urel.describe()).append('\n');
        }
      }
    }
    return sb.toString();
  }

  @Override
  public FeatureMap getFeatureMap() {
    return features;
  }
}
