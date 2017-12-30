//package edu.utdallas.hltri.scribe.util;
//
//import com.google.common.base.Throwables;
//import com.google.common.collect.Lists;
//import com.google.common.collect.Sets;
//import edu.utdallas.hltri.io.IOUtils;
//import edu.utdallas.hltri.logging.Logger;
//import edu.utdallas.hltri.scribe.gate.GateUtils;
//import edu.utdallas.hltri.scribe.io.JsonSerializer;
//import edu.utdallas.hltri.scribe.text.BaseDocument;
//import edu.utdallas.hltri.scribe.text.Document;
//
//import java.io.File;
//import java.io.IOException;
//import java.nio.file.Files;
//import java.nio.file.Paths;
//import java.util.List;
//import java.util.Set;
//import java.util.stream.Stream;
//
///**
// * No u Created by rmm120030 on 7/15/15.
// */
//public class Serialization<D extends BaseDocument> {
//  private static final Logger log = Logger.get(Serialization.class);
//
//  private final String textPath;
//  private final String featPath;
//  private final String annPath;
//  private final String writeTextPath;
//  private final String writeFeatPath;
//  private final String writeAnnPath;
//  private final Set<String> annSets;
//  private final Set<String> annSetsOut;
//  private final boolean writeTiered;
//  private final boolean readTiered;
//  private final boolean readAnnotations;
//  private final int section;
//  private final int totalSections;
//
//  protected Serialization(Serialization.Builder<D> builder) {
//    textPath = builder.textPath;
//    annPath = builder.annPath;
//    // document annotation path defaults to annotation path if not explicitly set
//    featPath = (builder.featPath != null) ? builder.featPath : annPath;
//    // if readNoAnnotations is set or if there is no annotation path, don't read annotations
//    readAnnotations = (annPath != null) && !builder.readNoAnnotations;
//
//    // write paths default to read paths if not explicitly set
//    writeTextPath = (builder.writeTextPath != null) ? builder.writeTextPath : textPath;
//    writeAnnPath = (builder.writeAnnPath != null) ? builder.writeAnnPath : annPath;
//    writeFeatPath = (builder.writeFeatPath != null) ? builder.writeFeatPath : featPath;
//
//    annSets = Sets.newHashSet(builder.annSets);
//    // write out annotation sets that were read if none are explicitly given
//    annSetsOut = (builder.annSetsOut == null) ? annSets : Sets.newHashSet(builder.annSetsOut);
//    // remove the read-only annotation sets from annSetsOut
//    if (builder.readOnlyAnnSets != null)  {
//      for (final String annSet : builder.readOnlyAnnSets)
//      annSetsOut.remove(annSet);
//    }
//
//    writeTiered = builder.writeTiered;
//    readTiered = builder.readTiered;
//
//    section = builder.section;
//    totalSections = builder.totalSections;
//  }
//
//  public static class Builder<D extends BaseDocument> {
//    private String textPath;
//    private String featPath;
//    private String annPath;
//    private String writeTextPath;
//    private String writeFeatPath;
//    private String writeAnnPath;
//    private String[] annSets = new String[]{};
//    private String[] readOnlyAnnSets = null;
//    private String[] annSetsOut = null;
//    private boolean writeTiered = false;
//    private boolean readTiered = false;
//    private boolean readNoAnnotations = false;
//    private boolean jsonStore = false;
//    private boolean gateStore = false;
//    private int section = -1;
//    private int totalSections = -1;
//
//    public Builder(final String textPath) {
//      this.textPath = textPath;
//    }
//
//    public Builder<D> annPath(final String annPath) {
//      this.annPath = annPath;
//      return this;
//    }
//
//    public Builder<D> featPath(final String featPath) {
//      this.featPath = featPath;
//      return this;
//    }
//
//    public Builder<D> writeTextPath(final String writeTextPath) {
//      this.writeTextPath = writeTextPath;
//      return this;
//    }
//
//    public Builder<D> writeAnnPath(final String writeAnnPath) {
//      this.writeAnnPath = writeAnnPath;
//      return this;
//    }
//
//    public Builder<D> writeFeatPath(final String writeFeatPath) {
//      this.writeFeatPath = writeFeatPath;
//      return this;
//    }
//
//    public Builder<D> annotationSets(final String... sets) {
//      annSets = sets;
//      return this;
//    }
//
//    public Builder<D> writeAnnotationSets(final String... sets) {
//      annSetsOut = sets;
//      return this;
//    }
//
//    public Builder<D> dontWriteAnnotationSets(final String... sets) {
//      readOnlyAnnSets = sets;
//      return this;
//    }
//
//    public Builder<D> tiered() {
//      writeTiered = true;
//      readTiered = true;
//      return this;
//    }
//
//    public Builder<D> writeTiered() {
//      writeTiered = true;
//      return this;
//    }
//
//    public Builder<D> readTiered() {
//      readTiered = true;
//      return this;
//    }
//
//    public Builder<D> json() {
//      assert !gateStore : "Only one of json and gate can be specified as the type of data store.";
//      jsonStore = true;
//      return this;
//    }
//
//    public Builder<D> gate() {
//      assert !jsonStore : "Only one of json and gate can be specified as the type of data store.";
//      gateStore = true;
//      return this;
//    }
//
//    public Builder<D> readSection(int section, int totalSections) {
//      if ((section < 0) || section > totalSections) {
//        throw new IllegalArgumentException(String.format("Invalid section. Section: %s; Total: %s", section, totalSections));
//      }
//      this.section = section;
//      this.totalSections = totalSections;
//      this.readTiered = true;
//      return this;
//    }
//
//    public Builder<D> readNoAnnotations() {
//      readNoAnnotations = true;
//      return this;
//    }
//
//    public Serialization<D> build() {
//      return new Serialization<>(this);
//    }
//  }
//
//  public Stream<Document<D>> loadLazy() {
//    if (jsonStore) {
//      return loadJsonLazy();
//    }
//    else if (gateStore) {
//      throw new UnsupportedOperationException("lazy gate not ready yet");
//    }
//    else {
//      throw new RuntimeException("No serialization method specified. Call Builder.json(), Builder.gate()...");
//    }
//  }
//
//  private Stream<Document<D>> loadJsonLazy() {
//    GateUtils.init();
//
//    log.info("Opening json serial data store...");
//    log.info("Text from: {}", textPath);
//    if (readAnnotations) {
//      log.info("Annotations from: {}", annPath);
//    }
//    try {
//      return Files.list(Paths.get(textPath))
//          .filter(f -> (section == -1) || Integer.parseInt(f.getFileName().toString(), 16) % totalSections == section - 1)
//          .flatMap(f -> {
//            try {
//              return Files.walk(f);
//            } catch (IOException e) {
//              throw Throwables.propagate(e);
//            }
//          })
//          .filter(f -> f.toString().endsWith(".txt"))
//          .map(f -> readDocument(f.toFile()));
//    }
//    catch (IOException e) {
//      throw new RuntimeException(e);
//    }
//  }
//
//  public List<Document<D>> loadEager() {
//    if (jsonStore) {
//      return loadJsonEager();
//    }
//    else if (gateStore) {
//      throw new UnsupportedOperationException("lazy gate not ready yet");
//    }
//    else {
//      throw new RuntimeException("No serialization method specified. Call Builder.json(), Builder.gate()...");
//    }
//  }
//
//  private List<Document<D>> loadJsonEager() {
//    GateUtils.init();
//
//    log.info("Opening json serial data store...");
//    log.info("Text from: {}", textPath);
//    if (readAnnotations) {
//      log.info("Annotations from: {}", annPath);
//    }
//    final List<Document<D>> docs = Lists.newArrayListWithExpectedSize(1000);
//    for (final File file : IOUtils.lazy.iterateWithSuffix(textPath, ".txt")) {
//      if (section == -1) {
//        docs.add(readDocument(file));
//      }
//      else {
//        final int bucket = (int)(Integer.parseInt(file.getParentFile().getParentFile().getName(), 16) / 256.0 * totalSections);
//        if (bucket == section) {
//          docs.add(readDocument(file));
//        }
//      }
//    }
//
//    log.info("Loaded {} documents.", docs.size());
//    return docs;
//  }
//
//  private Document<D> readDocument(final File file) {
//    Document<D> document;
//    if (jsonStore) {
//      document = JsonSerializer.readText(file);
//      readJsonAnnotations(document);
//    }
//    else {
//      throw new RuntimeException("Cannot read. No serialization format specified");
//    }
//    return document;
//  }
//
//  private void readJsonAnnotations(final Document<D> document) {
//    String docAnn = featPath;
//    String ann = annPath;
//    final String docId = document.get(BaseDocument.id);
//    if (readTiered) {
//      final String hashDirs = TieredHashing.getHashDirsAsString(docId);
//      if (readAnnotations) {
//        docAnn += hashDirs;
//        ann += hashDirs;
//      }
//    }
//
//    if (readAnnotations) {
//      // deserialize document annotations
//      log.trace("Reading document annotations...");
//      JsonSerializer.readDocAnnotations(document, docAnn + "/" + docId);
//
//      // deserialize annotations
//      for (String annSet : annSets) {
//        log.trace("Reading {} annotations...", annSet);
//        JsonSerializer.readAnnotations(document, annSet, ann + "/" + docId);
//      }
//    }
//  }
//
//  @Override
//  public String toString() {
//    return "Serialization{" +
//        "textPath='" + textPath + '\'' +
//        ", featPath='" + featPath + '\'' +
//        ", annPath='" + annPath + '\'' +
//        ", writeTextPath='" + writeTextPath + '\'' +
//        ", writeFeatPath='" + writeFeatPath + '\'' +
//        ", writeAnnPath='" + writeAnnPath + '\'' +
//        ", annSets=" + annSets +
//        ", annSetsOut=" + annSetsOut +
//        ", writeTiered=" + writeTiered +
//        ", readTiered=" + readTiered +
//        ", readAnnotations=" + readAnnotations +
//        ", section=" + section +
//        ", totalSections=" + totalSections +
//        '}';
//  }
//
//  public static void main(String... args) {
//    final Serialization<BaseDocument> corpus = new Builder<>(args[0])
//        .annPath(args[1])
//        .featPath(args[1])
////        .writeAnnotationSets("opennlp", "genia")
////        .writeTiered(true).build();
//        .annotationSets("opennlp", "genia")
//        .tiered()
//        .build();
//
//    final List<Document<BaseDocument>> documents = corpus.loadJsonEager();
////    final GeniaAnnotator<BaseDocument> genia = new GeniaAnnotator<>();
////    genia.annotateAll(documents);
////
////    corpus.save(documents, args[0] + "/tiered", args[1] + "/tiered", args[1] + "/tiered", args[1] + "/tiered");
//    for (final Document<BaseDocument> document : documents) {
//      log.debug("Annotations for doc {}: {}", document.get(BaseDocument.id), document.describe());
//    }
//  }
//}
