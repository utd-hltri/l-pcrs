package edu.utdallas.hltri.scribe.io;

import com.fasterxml.jackson.core.*;
import com.google.common.base.Throwables;
import com.google.common.collect.Sets;

import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorOutputStream;
import org.apache.commons.compress.compressors.xz.XZCompressorInputStream;
import org.apache.commons.compress.compressors.xz.XZCompressorOutputStream;

import edu.utdallas.hltri.logging.Logger;
import edu.utdallas.hltri.scribe.text.BaseDocument;
import edu.utdallas.hltri.scribe.text.Document;
import edu.utdallas.hltri.scribe.text.HasFeatureMap;
import edu.utdallas.hltri.scribe.text.annotation.Annotation;
import edu.utdallas.hltri.scribe.text.annotation.UnsafeAnnotation;
import edu.utdallas.hltri.scribe.text.relation.UnsafeRelation;
import edu.utdallas.hltri.scribe.util.TieredHashing;
import edu.utdallas.hltri.util.Unsafe;

import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.*;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Stream;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Created by rmm120030 on 6/26/15.
 */
public class JsonCorpus<D extends BaseDocument> extends Corpus<D> implements Serializable {
  private final long serialVersionUID = 1l;

  private static final Logger log = Logger.get(JsonCorpus.class);
  private static final JsonFactory jf = new JsonFactory();

  private final String textPath;
  private final String featPath;
  private final String annPath;
  private final Set<String> annSets;
  private final boolean tiered;
  private final boolean allAnnSets;
  private final int shard;
  private final int totalShards;
  transient private final FileAttribute<Set<PosixFilePermission>> DEFAULT_FILE_ATTRS = PosixFilePermissions.asFileAttribute(
      EnumSet.of(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE,
          PosixFilePermission.GROUP_READ, PosixFilePermission.GROUP_WRITE)
  );
  transient private final Set<PosixFilePermission> DEFAULT_TEXT_FILE_ATTRS =
      EnumSet.<PosixFilePermission>of(PosixFilePermission.OWNER_READ, PosixFilePermission.GROUP_READ);

  private boolean noAnnotationsFlag = false;
//  private int noAnnotationsCount = 0;

  private final Compression compression;

//  private final Progress

  public static enum Compression {
    BZ2(Unsafe.function(BZip2CompressorOutputStream::new),
        Unsafe.function(BZip2CompressorInputStream::new),
        ".bz2"),
    GZ(Unsafe.function(GZIPOutputStream::new),
       Unsafe.function(GZIPInputStream::new),
       ".gz"),
    XZ(Unsafe.function(XZCompressorOutputStream::new),
        Unsafe.function(XZCompressorInputStream::new),
        ".xz"),
    NONE(Function.identity(),
         Function.identity(),
         "");

    private Compression(final Function<OutputStream, ? extends OutputStream> outputWrapper, final Function<InputStream, ? extends InputStream> inputWrapper, final String ext) {
      this.outputWrapper = outputWrapper;
      this.inputWrapper = inputWrapper;
      this.ext = ext;
    }

    final private String ext;
    final private Function<OutputStream, ? extends OutputStream> outputWrapper;
    final private Function<InputStream, ? extends InputStream> inputWrapper;
  }

  public static <D extends BaseDocument> Builder<D> builder(final String textPath) {
    return new Builder<>(textPath);
  }

  public static <D extends BaseDocument> Builder<D> at(final File path) {
    return at(path.getAbsolutePath());
  }

  public static <D extends BaseDocument> Builder<D> at(final Path path) {
    return at(path.toAbsolutePath().toString());
  }

  public static <D extends BaseDocument> Builder<D> at(final String path) {
    return new Builder<D>(new File(path, "text").getAbsolutePath())
        .annPath(new File(path, "anns").getAbsolutePath())
        .featPath(new File(path, "feats").getAbsolutePath());
  }

  protected JsonCorpus(final JsonCorpus.Builder<D> builder) {
    textPath = builder.textPath;
    annPath = builder.annPath;
    // document annotation path defaults to annotation path if not explicitly set
    featPath = (builder.featPath != null) ? builder.featPath : textPath;

    allAnnSets = builder.readAllAnnSets;
    annSets = (allAnnSets) ? Sets.newHashSet() : Sets.newHashSet(builder.annSets);

    tiered = builder.tiered;

    shard = builder.shard;
    totalShards = builder.totalShards;

    compression = builder.compression;

    log.info("Opening JSON corpus with text at {}", textPath);
    log.info("• annotations at {}", annPath);
    log.info("• features at {}", featPath);
    log.info("• annotations sets: {}", (allAnnSets) ? "all" : annSets);
  }

  public static class Builder<D extends BaseDocument> {
    private String textPath;
    private String featPath;
    private String annPath;
    private String[] annSets = new String[]{};
    private boolean readAllAnnSets = true;
    private boolean tiered = false;
    private int shard = -1;
    private int totalShards = -1;
    private Compression compression = Compression.NONE;

    private Builder(final String textPath) {
      this.textPath = textPath;
    }

    public Builder<D> annPath(final String annPath) {
      this.annPath = annPath;
      return this;
    }

    public Builder<D> featPath(final String featPath) {
      this.featPath = featPath;
      return this;
    }

    /**
     * Set the annotation sets you want to read/write. If empty, every existing annotation set will be read/written.
     * To read/write no annotation sets, pass no annotation sets to this method.
     * @param sets varargs of annotation sets to read/serialize
     * @return this
     */
    public Builder<D> annotationSets(final String... sets) {
      readAllAnnSets = false;
      annSets = sets;
      return this;
    }

    public Builder<D> annotationSets(final String[] sets1, final String... sets2) {
      readAllAnnSets = false;
      final Set<String> annsetSet = Sets.newHashSet(sets1);
      annsetSet.addAll(Arrays.asList(sets2));
      annSets = annsetSet.toArray(new String[annsetSet.size()]);
      return this;
    }

    public Builder<D> tiered() {
      tiered = true;
      return this;
    }

    public Builder<D> readShard(int shard, int totalSections) {
      if ((shard < 0) || shard > totalSections) {
        throw new IllegalArgumentException(String.format("Invalid shard. Section: %s; Total: %s", shard, totalSections));
      }
      this.shard = shard;
      this.totalShards = totalSections;
      this.tiered = true;
      return this;
    }

    public Builder<D> compressWith(final Compression compression) {
      this.compression = compression;
      return this;
    }

    public JsonCorpus<D> build() {
      return new JsonCorpus<>(this);
    }
  }

  public String getAnnPath() {
    return annPath;
  }

  public String getFeatPath() {
    return featPath;
  }

  public String getTextPath() {
    return textPath;
  }

  public int getShard() {
    return shard;
  }

  public int getTotalShards() {
    return totalShards;
  }

  public Compression getCompression() {
    return compression;
  }

  public boolean isTiered() {
    return tiered;
  }


  public String getTextPath(String id) {
    String text = textPath;
    if (tiered) {
      text += File.separator + TieredHashing.getHashDirsAsString(id);
    }
    return Paths.get(text, id + ".txt" + compression.ext).toAbsolutePath().toString();
  }

  @Override
  public boolean canLoad(String id) {
    return Files.exists(Paths.get(getTextPath(id)));
  }

  /**
   * Returns true if the document with the passed id has annotations of the passed annotation set.
   * Returns false otherwise.
   * @param annotationSet
   * @param id
   * @return
   */
  public boolean canLoad(String annotationSet, String id) {
    Path annSetPath = Paths.get(getAnnPath(), annotationSet);
    if (Files.exists(annSetPath)) {
      if (tiered) {
        annSetPath = annSetPath.resolve(TieredHashing.getHashDirsAsString(id));
      }
      return Files.exists(annSetPath.resolve(id + ".json" + compression.ext));
    } else {
      return false;
    }
  }

  public Document<D> loadFeaturesOnly(String id) {
    String feat = featPath;
    String hashDirs = null;
    if (tiered) {
      hashDirs = File.separator + TieredHashing.getHashDirsAsString(id);
      feat += hashDirs;
    }

    final Document<D> document = Document.empty();
    // deserialize document annotations
    log.trace("Reading document features...");
    readDocFeats(document, feat + File.separator + id);

    document.clearDirty();
    return document;
  }

  /**
   * Loads a document from its json serialized state.
   * Loads the text from textPath.
   * Loads the document features from featPath.
   * Loads the annotations from annPath.
   *
   * By default, all annotation sets that exist under annPath will be deserialized.
   * It is possible to only read specific annotation sets using the Builder.annotationSets(String... ) method.
   * To read no annotation sets in, use Builder.annotationSets(), providing no arguments.
   * @param id the BaseDocument.id of the document to be read
   * @return a Document
   */
  @Override
  protected Document<D> loadDocument(final String id) {
    String text = textPath;
    String feat = featPath;
    String hashDirs = null;
    if (tiered) {
      hashDirs = File.separator + TieredHashing.getHashDirsAsString(id);
      text += hashDirs;
      feat += hashDirs;
    }
    final Document<D> document = readText(new File(text, id + ".txt"));
    // deserialize document annotations
    log.trace("Reading document annotations...");
    readDocFeats(document, feat + File.separator + id);

    // deserialize annotations
    if (allAnnSets) {
      final File[] files = new File(annPath).listFiles();
      if (files != null && files.length > 0) {
        for (final File annSet : files) {
          log.trace("Reading {} annotations...", annSet.getName());
          if (tiered) {
            readAnnotations(document, annSet.getName(), annSet.getAbsolutePath() + hashDirs);
          } else {
            readAnnotations(document, annSet.getName(), annSet.getAbsolutePath());
          }
        }
      }
    }
    else {
      for (String annSet : annSets) {
        log.trace("Reading {} annotations...", annSet);
        if (tiered) {
          readAnnotations(document, annSet, annPath + File.separator + annSet + hashDirs);
        }
        else {
          readAnnotations(document, annSet, annPath + File.separator + annSet);
        }
      }
    }
    document.clearDirty();
    return document;
  }

  /**
   * Serializes a document to json.
   * Writes the text to <textPath>/<tiering>/<id>.txt
   * Writes the document features to <featPath>/<tiering>/<id>.json
   * Writes the annotations to <annPath>/<annSet>/<tiering>/<id>.json
   *
   * By default, all annotations attached to the passed document will be serialized.
   * It is possible to only serialize specific annotaion sets using the Builder.annotationSets(String... ) method.
   * To serialize no annotation sets, use Builder.annotationSets(), providing no arguments.
   *
   * To serialize different annotation sets than were read in using this corpus, create a new corpus and use
   * Document.setCorpus(<newCorpus>) and Document.sync().
   * @param document the document to be serialized
   */
  @Override
  public void save(final Document<D> document) {
    String text = textPath;
    String feat = featPath;
    String hashDirs = null;
    final String docId = document.get(BaseDocument.id);
    if (tiered) {
      hashDirs = File.separator + TieredHashing.getHashDirsAsString(docId);
      text += hashDirs;
      feat += hashDirs;
    }

    // save text
    writeText(document, new File(text));

    // save document annotations
    writeDocumentFeatures(document, new File(feat));

    // save annotations
    Set<String> writeAnnSets = (allAnnSets) ? document.getAnnotationSets() : annSets;
    for (final String annSet : writeAnnSets) {
      if (tiered) {
        writeAnnotations(document, new File(annPath + File.separator + annSet + hashDirs), annSet);
      }
      else {
        writeAnnotations(document, new File(annPath, annSet), annSet);
      }
    }
    document.clearDirty();
  }

  public Stream<String> getIdStream(String annotationSet) {
    if (!Files.exists(Paths.get(annPath, annotationSet))) {
      return Stream.empty();
    }
    try {
      return Files.list(Paths.get(annPath, annotationSet))
          .filter(f -> (shard == -1) || Integer.parseInt(f.getFileName().toString(), 16) % totalShards == shard - 1)
          .flatMap(f -> {
            try {
              return Files.walk(f);
            } catch (IOException e) {
              throw new RuntimeException(e);
            }
          })
          .filter(f -> f.toString().endsWith(".json"))
          .map(f -> f.getFileName().toString().substring(0, f.getFileName().toString().length() - 5));
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public Stream<String> getIdStream() {
    try {
      return Files.list(Paths.get(textPath))
          .filter(f -> (shard == -1) || Integer.parseInt(f.getFileName().toString(), 16) % totalShards == shard - 1)
          .flatMap(f -> {
            try {
              return Files.walk(f);
            } catch (IOException e) {
              throw new RuntimeException(e);
            }
          })
          .filter(f -> f.toString().endsWith(".txt"))
          .map(f -> f.getFileName().toString().substring(0, f.getFileName().toString().length() - 4));
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public Set<String> getAnnSets() {
    return annSets;
  }

  /**
   * Creates a Document from a text file, setting:
   * -BaseDocument.id
   * -BaseDocument.path
   * @param text the document raw text file
   * @return Document
   */
  private  Document<D> readText(File text) {
    try (BufferedReader reader = new BufferedReader(new FileReader(text))) {
      // id is filename without the .txt extension
      final String id = text.getName().substring(0, text.getName().length() - 4);
      log.trace("Document: {}", id);
      log.trace("Reading text from: {}", text.getAbsolutePath());
      final StringBuilder sb = new StringBuilder();
      String line;
      for (int i = 0; (line = reader.readLine()) != null; i++) {
        if (i > 0) {
          sb.append("\n");
        }
        sb.append(line);
      }
      if (sb.length() < 1) {
        log.warn("Empty text file for doc {} at {}", id, text.getAbsolutePath());
      }
      final Document<D> document = Document.fromString(sb.toString());
      document.set(BaseDocument.id, id);
      document.set(BaseDocument.path, text.getAbsolutePath());
      return document;
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  // read from <featPath>/<tiering>/<docId>.json
  //TODO: compress this poor fella here
  private void readDocFeats(final Document<D> doc, final String path) {
    final File file = new File(path + ".json");
    if (!file.exists()) {
      log.warn("Features file {} does not exist, ignoring features...", file.getAbsolutePath());
    }
    else {
      try (JsonParser jp = jf.createParser(compression.inputWrapper.apply(Files.newInputStream(file.toPath())))) {
        while (jp.nextToken() != JsonToken.END_OBJECT) {
          // until the end of this json object
          if ("features".equals(jp.getCurrentName())) {
            jp.nextToken();
            readFeaturemap(jp, doc);
          }
        }
      } catch (IOException | ClassNotFoundException e) {
        throw new RuntimeException(e);
      }
    }
  }

  // read from <annPath>/<annset>/<tiering>/<docId>.json
  private void readAnnotations(final Document<D> doc, final String annSet, final String path) {
    final File file = new File(path + File.separator + doc.get(BaseDocument.id) + ".json");
//    log.info("Reading annotations from {}", file.getPath());
    if (!file.exists() || file.length() == 0) {
      /* TODO: this doesn't work -- if you have multiple streams or processes sharing a json corpus the counts get crazy
       * Also its super annoying so I commented it out...
       * :'(
       */
//      noAnnotationsCount++;
//      if (noAnnotationsCount % 100 == 1) {
//        log.debug("{} document(s) do not have {} annotations", noAnnotationsCount, annSet);
//      }
    }
    else {
      try (JsonParser jp = jf.createParser(compression.inputWrapper.apply(Files.newInputStream(file.toPath())))) {
        // until the end of this json object
        while (jp.nextToken() != JsonToken.END_OBJECT) {
          // skip types
          if ("types".equals(jp.getCurrentName())) {
            while (jp.nextToken() != JsonToken.END_OBJECT) {
              ; // do nothing: we just want to increment the parser's position
            }
          }

          JsonToken jt;
          // annotation map
          if ("annotations".equals(jp.getCurrentName())) {
            String type;
            jp.nextToken();
            while ((jt = jp.nextToken()) != JsonToken.END_OBJECT) {
              if (jt == JsonToken.START_OBJECT) {
                type = jp.getCurrentName();
                while(jp.nextToken() != JsonToken.END_OBJECT) {
                  readAnnotation(jp, doc, annSet, type);
                }
              }
            }
          }
          //relation map
          if ("relations".equals(jp.getCurrentName())) {
            String type;
            jp.nextToken();
            while ((jt = jp.nextToken()) != JsonToken.END_OBJECT) {
              if (jt == JsonToken.START_OBJECT) {
                type = jp.getCurrentName();
                while(jp.nextToken() != JsonToken.END_OBJECT) {
                  readRelation(jp, doc, annSet, type);
                }
              }
            }
          }
        }
      } catch (IOException | ClassNotFoundException e) {
        throw new RuntimeException(e);
      }
    }
  }

  private  void readAnnotation(final JsonParser jp, final Document<D> doc, final String annSet, final String type) throws IOException, ClassNotFoundException {
    long start = -1, end = -1;
    int id = -1;
    JsonToken jt;
    while ((jt = jp.nextToken()) != JsonToken.END_OBJECT) {
      if (jt == JsonToken.START_OBJECT && id == -1) {
        id = Integer.parseInt(jp.getCurrentName());
      }
      final String currentFieldName = jp.getCurrentName();
      if ("start".equals(currentFieldName)) {
        jp.nextToken();
        log.trace("start: {}", jp.getLongValue());
        start = jp.getLongValue();
      } else if ("end".equals(currentFieldName)) {
        jp.nextToken();
        log.trace("end: {}", jp.getLongValue());
        end = jp.getLongValue();
      } else if ("feature-map".equals(currentFieldName)) {
        log.trace("Deserializing annotation at {start={}, end={}, type={}}", start, end, type);
        final UnsafeAnnotation ua = UnsafeAnnotation.at(doc, annSet, type, start, end, id);
        jp.nextToken();
        assert ua != null : String.format("Annotation never created. Start: %d. end: %d, type: %s", start, end,
                                          type);
        readFeaturemap(jp, ua);
        start = -1;
        end = -1;
        id = -1;
      }
    }
  }

  private  void readRelation(final JsonParser jp, Document<D> doc, final String annSet, final String type) throws IOException, ClassNotFoundException {
    int govId = 0, depId = 0, id = -1;
    boolean govInit = false, depInit = false;
    JsonToken jt;
    while ((jt = jp.nextToken()) != JsonToken.END_OBJECT) {
      if (jt == JsonToken.START_OBJECT && id == -1) {
        id = Integer.parseInt(jp.getCurrentName());
      }
      final String currentFieldName = jp.getCurrentName();
      if ("governor-id".equals(currentFieldName)) {
        jp.nextToken();
        govId = jp.getIntValue();
        govInit = true;
        log.trace("govId: {}", govId);
      }
      else if ("dependant-id".equals(currentFieldName)) {
        jp.nextToken();
        depId = jp.getIntValue();
        depInit = true;
        log.trace("depId: {}", depId);
      }
      else if ("feature-map".equals(currentFieldName)) {
        final UnsafeRelation ur = (govInit && depInit) ?  UnsafeRelation.create(doc, type, govId, depId, annSet)
            : null;
        assert ur != null : String.format(
            "Cannot create unsafe relation. GovId = %d, type = %s, DepId = %d. Ids of 0 indicate they were unassigned.",
            govId, type, depId);
        log.trace("Created relation: {}", ur);
        jp.nextToken();
        readFeaturemap(jp, ur);
        govId = 0;
      }
    }
  }

  private void readFeaturemap(final JsonParser jp, final HasFeatureMap mapHaver) throws IOException, ClassNotFoundException {
    JsonToken jt;
    while ((jt = jp.nextToken()) != JsonToken.END_OBJECT) {
      if (jt == JsonToken.START_OBJECT) {
        String key = jp.getCurrentName();
        String clazz = null;
        while ((jt = jp.nextToken()) != JsonToken.END_OBJECT) {
          if (jt != JsonToken.FIELD_NAME) {
            final String tokenName = jp.getCurrentName();
            if ("class".equals(tokenName)) {
              clazz = jp.getText();
            }
            else if ("value".equals(tokenName)){
//              log.info("Setting {} to {}/{}.", key, clazz, jp.getText());
              if (String.class.getSimpleName().equals(clazz)) {
                mapHaver.set(key, jp.getText());
              }
              else if (Integer.class.getSimpleName().equals(clazz)) {
                mapHaver.set(key, jp.getIntValue());
              }
              else if (Double.class.getSimpleName().equals(clazz)) {
                mapHaver.set(key, jp.getDoubleValue());
              }
              else if (Long.class.getSimpleName().equals(clazz)) {
                mapHaver.set(key, jp.getLongValue());
              }
              else if (Float.class.getSimpleName().equals(clazz)) {
                mapHaver.set(key, jp.getFloatValue());
              }
              else if (Boolean.class.getSimpleName().equals(clazz)) {
                mapHaver.set(key, jp.getBooleanValue());
              }
              else {
                log.trace("Object found of class: {}", clazz);
                mapHaver.set(key, readBinaryString(jp.getText()));
              }
            }
          }
        }
      }
    }
  }

  /**
   * Creates a read-only file at path named id.txt and populates it with the String text if it doesn't already exist.
   * @param path file object of the location of the resultant file
   * @param doc  document to be written
   */
  private void writeText(final Document<D> doc, final File path) {
    try {
      final File file = new File(path, doc.get(BaseDocument.id) + ".txt" + compression.ext);
      if (!file.exists()) {
        file.getParentFile().mkdirs();
        final Path instantiatedFile = Files.createFile(file.toPath());
        log.info("Created new text file at {}", instantiatedFile.toString());
        final OutputStream out = compression.outputWrapper.apply(Files.newOutputStream(instantiatedFile));
        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(out))) {
          writer.write(doc.asString());
        } catch (IOException e) {
          throw new RuntimeException(e);
        }
        Files.setPosixFilePermissions(instantiatedFile, DEFAULT_TEXT_FILE_ATTRS);
        final PosixFileAttributes attrs = Files.getFileAttributeView(path.toPath(), PosixFileAttributeView.class).readAttributes();
        final GroupPrincipal group = attrs.group();
        Files.getFileAttributeView(instantiatedFile, PosixFileAttributeView.class, LinkOption.NOFOLLOW_LINKS).setGroup(group);
      } else {
        log.trace("Text Document at  already exists, skipping...", file.getAbsolutePath(), doc.get(BaseDocument.id));
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Writes the annotations of the document itself (title, id, etc.) to a file called document.json in the parent
   * file passed
   */
  private void writeDocumentFeatures(final Document<D> doc, final File parent) {
    if (!doc.cleanFeats()) {
      try {
        if (!parent.exists()) {
          log.debug("Directory at {} does not exist, creating...", parent);
          makeDirs(parent.toPath());
        }
        final File file = new File(parent, doc.get(BaseDocument.id) + ".json" + compression.ext);
        final Path temp = tempFile(file, "feat", doc.getId());
        final OutputStream out = compression.outputWrapper.apply(Files.newOutputStream(temp));
        try (JsonGenerator jg = jf.createGenerator(out, JsonEncoding.UTF8).useDefaultPrettyPrinter()) {
          jg.writeStartObject();
          jg.writeObjectFieldStart("features");
          {
            for (Map.Entry<Object, Object> entry : doc.features.entrySet()) {
              writeFeaturemapEntry((String) entry.getKey(), entry.getValue(), jg);
            }
          }
          jg.writeEndObject();
          jg.writeEndObject();
        }
        moveFile(temp, file.toPath());
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    } else {
      log.trace("No need to update features for document {}.", doc.getId());
    }
  }

  private Path tempFile(final File oldFile, final String fileType, final String docId) throws IOException {
    FileAttribute<Set<PosixFilePermission>> permissions = DEFAULT_FILE_ATTRS;
    assert oldFile.getParentFile().exists() : "No parent file of " + oldFile.getAbsolutePath();
    GroupPrincipal group = Files.readAttributes(oldFile.getParentFile().toPath(), PosixFileAttributes.class, LinkOption.NOFOLLOW_LINKS).group();
    if (oldFile.exists()) {
      log.trace("Feature file {} already exists. Deleting...", oldFile.getAbsolutePath());
      PosixFileAttributes attrs = Files.getFileAttributeView(oldFile.toPath(), PosixFileAttributeView.class).readAttributes();
      group = attrs.group();
      permissions = PosixFilePermissions.asFileAttribute(attrs.permissions());
      oldFile.delete();
    }
    final Path temp = Files.createTempFile("scribe-json-#" + docId, fileType + ".tmp", permissions);
    Files.getFileAttributeView(temp, PosixFileAttributeView.class, LinkOption.NOFOLLOW_LINKS).setGroup(group);

    return temp;
  }

  private void moveFile(final Path temporaryFile, final Path destination) throws IOException {
    Files.copy(temporaryFile, destination, StandardCopyOption.COPY_ATTRIBUTES);
    Files.delete(temporaryFile);
  }

  /**
   * Writes the annotations and relations attached to the document (tokens, sentences, etc.) of a particular
   * annotation set to a file at <parent>/<id>.json
   */
  private void writeAnnotations(final Document<D> doc, final File parent, final String annSet) {
    final List<UnsafeAnnotation> unsafeAnnotations = doc.getUnsafeAnnotations(annSet);
    if (!unsafeAnnotations.isEmpty()) {
      try {
        if (!parent.exists()) {
          log.trace("Directory at {} does not exist, creating...", parent);
          makeDirs(parent.toPath());
        }
        final File file = new File(parent, doc.get(BaseDocument.id) + ".json");
        final Path temp = tempFile(file, "ann", doc.getId());

        final OutputStream out = compression.outputWrapper.apply(Files.newOutputStream(temp));
        try (JsonGenerator jg = jf.createGenerator(out, JsonEncoding.UTF8).useDefaultPrettyPrinter()) {
          jg.writeStartObject();
          {
            // Types
            jg.writeObjectFieldStart("types");
            {
              jg.writeArrayFieldStart("Annotations");
              for (String type : doc.getAnnotationTypes(annSet)) {
                jg.writeString(type);
              }
              jg.writeEndArray();
              jg.writeArrayFieldStart("Relations");
              for (String type : doc.getRelationTypes(annSet)) {
                jg.writeString(type);
              }
              jg.writeEndArray();
            }
            jg.writeEndObject();

            // Annotations
            jg.writeObjectFieldStart("annotations");
            {
              for (final String type : doc.getAnnotationTypes(annSet)) {
                jg.writeObjectFieldStart(type);
                for (final UnsafeAnnotation ann : doc.getUnsafeAnnotations(annSet)) {
                  if (ann.getType().equals(type)) {
                    writeAnnotation(ann, jg);
                  }
                }
                jg.writeEndObject();
              }
            }
            jg.writeEndObject();

            // Relations
            final Set<UnsafeRelation> unsafeRelations = doc.getUnsafeRelations(annSet);
            if (!unsafeRelations.isEmpty()) {
              jg.writeObjectFieldStart("relations");
              {
                for (final String type : doc.getRelationTypes(annSet)) {
                  jg.writeObjectFieldStart(type);
                  for (final UnsafeRelation rel : unsafeRelations) {
                    if (rel.getType().equals(type)) {
                      writeRelation(rel, jg);
                    }
                  }
                  jg.writeEndObject();
                }
              }
              jg.writeEndObject();
            }
          }
          jg.writeEndObject();
        }
        moveFile(temp, file.toPath());
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }
  }

  private synchronized void makeDirs(final Path dir) throws IOException {
    final Path parent = dir.getParent();
    if (parent.toFile().exists()) {
      PosixFileAttributes attrs = Files.getFileAttributeView(parent, PosixFileAttributeView.class).readAttributes();
      FileAttribute<Set<PosixFilePermission>> permissions = PosixFilePermissions.asFileAttribute(attrs.permissions());
      Files.createDirectory(dir, permissions);
      Files.getFileAttributeView(dir, PosixFileAttributeView.class, LinkOption.NOFOLLOW_LINKS).setGroup(attrs.group());
    } else {
      makeDirs(parent);
      makeDirs(dir);
    }
  }

  private void writeAnnotation(final UnsafeAnnotation ua, final JsonGenerator jg) throws IOException {
    jg.writeObjectFieldStart(Integer.toString(ua.getGateId()));
    {
      // Start offset
      jg.writeNumberField("start", ua.get(Annotation.StartOffset));
      // End offset
      jg.writeNumberField("end", ua.get(Annotation.EndOffset));
      // Feature Map
      jg.writeObjectFieldStart("feature-map");
      {
        for (Map.Entry<String, Object> entry : ua) {
          writeFeaturemapEntry(entry.getKey(), entry.getValue(), jg);
        }
      }
      jg.writeEndObject();
    }
    jg.writeEndObject();
  }

  private void writeRelation(final UnsafeRelation ur, final JsonGenerator jg) throws IOException {
    jg.writeObjectFieldStart(Integer.toString(ur.getId()));
    {
      // Governor Id
      jg.writeNumberField("governor-id", ur.getGovernorId());
      // Dependant Id
      jg.writeNumberField("dependant-id", ur.getDependantId());
      // Feature Map
      jg.writeObjectFieldStart("feature-map");
      {
        for (Map.Entry<String, Object> entry : ur) {
          writeFeaturemapEntry(entry.getKey(), entry.getValue(), jg);
        }
      }
      jg.writeEndObject();
    }
    jg.writeEndObject();
  }

  private void writeFeaturemapEntry(final String key, final Object value, final JsonGenerator jg) throws IOException {
    jg.writeObjectFieldStart(key);
    jg.writeStringField("class", value.getClass().getSimpleName());
    switch (value.getClass().getSimpleName()) {
      case "String": jg.writeStringField("value", (String) value);
        break;
      case "Integer": jg.writeNumberField("value", (Integer) value);
        break;
      case "Double": jg.writeNumberField("value", (Double) value);
        break;
      case "Long": jg.writeNumberField("value", (Long) value);
        break;
      case "Float": jg.writeNumberField("value", (Float) value);
        break;
      case "Boolean": jg.writeBooleanField("value", (Boolean) value);
        break;
      default: jg.writeStringField("value", writeBinaryString(value));
    }
    jg.writeEndObject();
  }

  public String writeBinaryString(Object o) throws IOException {
    assert o instanceof Serializable : String.format("Object %s does not implement Serializable", o);
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    ObjectOutputStream oos = new ObjectOutputStream( baos );
    oos.writeObject( o );
    oos.close();
    return Base64.getEncoder().encodeToString(baos.toByteArray());
  }

  public Object readBinaryString( String s ) throws IOException, ClassNotFoundException {
//    log.info("Recovering binary string: {}", s);
    try {
      byte[] data = Base64.getDecoder().decode(s);
      ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(data));
      Object o = ois.readObject();
      ois.close();
      return o;
    } catch (InvalidClassException e) {
      log.error("Invalid class", e);
      return null;
    }
  }

  @Override
  public String toString() {
    return "JsonCorpus with text at: " + textPath
        + ", anns at: " + annPath
        + ", and feats at " + featPath;
  }
}
