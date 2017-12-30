package edu.utdallas.hltri.scribe.io;

import com.google.common.base.CharMatcher;
import com.google.common.base.Splitter;
import com.google.common.base.Throwables;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.attribute.*;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import edu.utdallas.hltri.logging.Logger;
import edu.utdallas.hltri.scribe.gate.GateUtils;
import edu.utdallas.hltri.scribe.text.Attribute;
import edu.utdallas.hltri.scribe.text.BaseDocument;
import edu.utdallas.hltri.scribe.text.Document;
import edu.utdallas.hltri.scribe.text.Text;
import edu.utdallas.hltri.scribe.text.annotation.*;
import edu.utdallas.hltri.scribe.util.TieredHashing;
import edu.utdallas.hltri.util.IntIdentifier;

/**
 * Brat Corpus.
 * Override annotationCreator() and entitySupplier() to use.
 * Override bratify() if the Entities in the [entities] section of your annotation.conf file are not all caps with
 * underscores for delimiters (e.g. EVENT or EEG_ACTIVITY)
 *
 * Created by rmm120030 on 8/26/15.
 */
public abstract class BratCorpus<D extends BaseDocument> extends Corpus<D> {
  private static final Logger log = Logger.get(BratCorpus.class);
  protected final File bratDir;
  protected boolean tiered;

  protected static final BratEntity NULL_ENTITY = new BratEntity();
  protected static final BratAttribute NULL_ATTRIBUTE = new BratAttribute();
  protected static final BratRelation NULL_RELATION = new BratRelation();

  /**
   * This function should take a BratEntity and the containing document and create the correct scribe.Annotation
   * depending on the BratEntity.type of the passed BratEntity.
   */
  protected abstract BiConsumer<BratEntity, Document<D>> annotationCreator();

  /**
   * This function should take a BratRelation and the containing document and create the correct scribe.Relation
   * depending on the BratRelation.type of the passed BratRelation.
   * The default implementation does nothing to handle the common case where there are no relations.
   */
  protected BiConsumer<BratRelation, Document<D>> relationCreator() {
    return (br, doc) -> {};
  }


  /**
   * Creates a list of BratEntity's from the desired subset of the passed Document's annotations.
   * The passed IntIdentifier registries are required by the constructors of BratEntity and BratAttribute.
   * @param document the document
   * @param entityRegistry for numbering BratEntities
   * @param attributeRegistry for numbering BratAttributes
   * @param textFileFun for overwriting text file in the event of an annotation containing a newline
   * @return a list of BratEntity's with optional Brat Attributes attached
   */
  protected abstract Supplier<List<BratEntity>> entitySupplier(Document<D> document,
                                                               IntIdentifier<BratEntity> entityRegistry,
                                                               IntIdentifier<BratAttribute> attributeRegistry,
                                                               Function<Document<?>, File> textFileFun);

  /**
   * Creates a list of BratRelations from the desired subset of the passed Document's relations.
   * The passed IntIdentifier registries are required by the constructors of BratRelation.
   * The default implementation returns an empty list to handle the common case where there are no relations.
   * @param document the document
   * @param relationRegistry for numbering BratRelations
   * @return a list of BratEntity's with optional Brat Attributes attached
   */
  protected Supplier<List<BratRelation>> relationSupplier(Document<D> document,
                                                          IntIdentifier<BratRelation> relationRegistry) {
    return Collections::emptyList;
  }

  protected void readAnnotations(final Document<D> document, File annotationFile) {
    try (BufferedReader reader = new BufferedReader(new FileReader(annotationFile))) {
      final Map<String, BratEntity> entities = Maps.newHashMap();
      final Set<String> emptyEntities = Sets.newHashSet();
      final Set<BratRelation> relations = Sets.newHashSet();
      final Splitter splitter = Splitter.on(CharMatcher.WHITESPACE).omitEmptyStrings();
          log.warn("Empty entity found on doc {}", document.get(BaseDocument.id));
      String line;
      while ((line = reader.readLine()) != null) {
        final Iterator<String> lineIt = splitter.split(line).iterator();
        if (line.startsWith("T") ) {
          final BratEntity be = BratEntity.fromLine(splitter.split(line));
          // default to factual and positive
          if (be.length() > 0) {
            entities.put(be.getId(), be);
          }
          else {
            emptyEntities.add(be.getId());
            log.warn("Empty entity found on doc {}", document.get(BaseDocument.id));
//              throw new RuntimeException("Zero length entity on doc " + document.get(BaseDocument.id) + ": " + be.toString());
          }
        }
        else if (line.startsWith("A")) {
          final BratAttribute ba = new BratAttribute(lineIt);
          if (!emptyEntities.contains(ba.getGovernorId())) {
            entities.get(ba.getGovernorId()).addAttribute(ba);
          }
        }else if(line.startsWith("#")){
          final BratAttribute note = BratAttribute.fromNote(line);
          if (!emptyEntities.contains(note.getGovernorId())){
            entities.get(note.getGovernorId()).addAttribute(note);
          }
        }else if (line.startsWith("R")) {
          relations.add(BratRelation.fromLine(line));
        }//changed from unconditional 'else' since there can be blank lines in brat annotation files.
        else if(!line.isEmpty()) {
          throw new RuntimeException(document.getId() + ": unexpected line in annotation file: " + line);
        }
      }
      for (BratEntity entity : entities.values()) {
        annotationCreator().accept(entity, document);
      }
      for (BratRelation relation : relations) {
        relationCreator().accept(relation, document);
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  protected void writeAnnotations(final Document<D> document, BratRules br){
    final IntIdentifier<BratEntity> entityRegistry = new IntIdentifier<>();
    entityRegistry.add(NULL_ENTITY);
    final IntIdentifier<BratAttribute> attributeRegistry = new IntIdentifier<>();
    attributeRegistry.add(NULL_ATTRIBUTE);
    final IntIdentifier<BratRelation> relationRegistry = new IntIdentifier<>();
    relationRegistry.add(NULL_RELATION);
    final File file = new File(getDir(document), document.get(BaseDocument.id) + ".ann");

    try {
      // deal with file attributes. Annotation files must be a+rwx
      GroupPrincipal group = Files.readAttributes(file.getParentFile().toPath(), PosixFileAttributes.class, LinkOption.NOFOLLOW_LINKS).group();
      if (file.exists()) {
        log.trace("Annotation file {} already exists. Deleting...", file.getAbsolutePath());
        PosixFileAttributes attrs = Files.getFileAttributeView(file.toPath(), PosixFileAttributeView.class).readAttributes();
        group = attrs.group();
        file.delete();
      }
      Files.createFile(file.toPath());
      Files.setPosixFilePermissions(file.toPath(), PosixFilePermissions.fromString("rwxrwxrwx"));
      Files.getFileAttributeView(file.toPath(), PosixFileAttributeView.class, LinkOption.NOFOLLOW_LINKS).setGroup(group);

      try (final BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
        for (final BratEntity entity : entitySupplier(document, entityRegistry, attributeRegistry, this::getTextFile).get()) {
          if(br != null)
            br.processEntity(entity);
          // don't make 0 or negative span annotations
          if (entity.getStartOffset() < entity.getEndOffset()) {
            writer.write(entity.toString());
            writer.newLine();
          } else {
            log.warn("Doc {} contains invalid annotation: {}", document.getId(), entity.toString());
          }
        }

        for (final BratRelation relation : relationSupplier(document, relationRegistry).get()) {
          writer.write(relation.toString());
          writer.newLine();
        }
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  protected void writeAnnotations(final Document<D> document) {
    writeAnnotations(document,null);
  }

  /**
   * Convenience corpus for visualizing token and sentence annotations.
   * @param location the directory in which to store the brat files
   * @param tokenAnnSet the annotation set of the token annotations
   * @param sentenceAnnSet the annotation set of the sentence annotations
   * @param <D> Document type
   * @return corpus for visualizing token and sentence annotations
   */
  public static <D extends BaseDocument> BratCorpus<D> tokenAndSentenceCorpus(final String location, final String tokenAnnSet, final String sentenceAnnSet) {
    return new BratCorpus<D>(new File(location)) {
      @Override
      protected void writeAnnotations(Document<D> document) {
        int eventCount = 0;
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(new File(getDir(document), document.get(BaseDocument.id) + ".ann")))) {
          for (final Sentence sentence : document.get(sentenceAnnSet, Sentence.TYPE)) {
            writer.write(String.format("T%d\tSENTENCE %d %d\t%s",
                eventCount++, sentence.get(Annotation.StartOffset), sentence.get(Annotation.EndOffset), sentence.asString()));
            writer.newLine();
          }
          for (final Token token : document.get(tokenAnnSet, Token.TYPE)) {
            writer.write(String.format("T%d\tTOKEN %d %d\t%s",
                eventCount++, token.get(Annotation.StartOffset), token.get(Annotation.EndOffset), token.asString()));
            writer.newLine();
          }
        } catch (IOException e) {
          throw new RuntimeException(e);
        }
      }

      @Override
      protected BiConsumer<BratEntity, Document<D>> annotationCreator() {
        throw new UnsupportedOperationException("Not implemented");
      }

      @Override
      protected BiConsumer<BratRelation, Document<D>> relationCreator() {
        throw new UnsupportedOperationException("Not implemented");
      }

      @Override
      protected Supplier<List<BratEntity>> entitySupplier(Document<D> document,
                                                          IntIdentifier<BratEntity> entityRegistry,
                                                          IntIdentifier<BratAttribute> attributeRegistry,
                                                          Function<Document<?>, File> textFileFun) {
        throw new UnsupportedOperationException("Not implemented");
      }

      @Override
      protected Supplier<List<BratRelation>> relationSupplier(Document<D> document,
                                                              IntIdentifier<BratRelation> relationRegistry) {
        throw new UnsupportedOperationException("Not implemented");
      }

      @Override
      protected void readAnnotations(Document<D> document, File annotationFile) {
        throw new UnsupportedOperationException("Not implemented");
      }
    };
  }

  protected BratCorpus(final File outDir) {
    GateUtils.init();
    this.bratDir = outDir;
    if (!Files.exists(outDir.toPath())) {
      log.warn("No brat corpus at {}. Creating...", outDir.toString());
      outDir.mkdir();
    }
  }

  public Document<D> attachAnnotations(final Document<D> document) {
    readAnnotations(document, new File(getDir(document), document.get(BaseDocument.id) + ".ann"));
    return document;
  }

  public void tiered() {
    tiered = true;
  }

  protected File getDir(final Document<?> document) {
    return getDir(document.get(BaseDocument.id));
  }

  protected File getDir(final String id) {
    if (tiered) {
      final File dir = new File(bratDir, TieredHashing.getHashDirsAsString(id));
      if (!dir.exists()) {
        dir.mkdirs();
        dir.mkdir();
      }
      return dir;
    }
    return bratDir;
  }

  protected File getTextFile(final Document<?> document) {
    return new File(getDir(document.getId()), document.getId() + ".txt");
  }

  @Override
  public boolean canLoad(String id) {
    return Files.exists(getDir(id).toPath().resolve(id + ".txt"));
  }

  @Override
  public Document<D> loadDocument(String id) {
    try (BufferedReader textReader = new BufferedReader(new FileReader(new File(getDir(id), id + ".txt")))) {
      log.trace("Document: {}", id);
      final StringBuilder sb = new StringBuilder();
      String line;
      for (int i = 0; (line = textReader.readLine()) != null; i++) {
        if (i > 0) {
          sb.append("\n");
        }
        sb.append(line);
      }
      final Document<D> document = Document.fromString(sb.toString());
      document.set(BaseDocument.id, id);
      readAnnotations(document, new File(getDir(id), id + ".ann"));

      return document;
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public void save(Document<D> document) {
    log.info("Writing {}", document.get(BaseDocument.id));
    final File textFile = new File(getDir(document), document.get(BaseDocument.id) + ".txt");
    if (!textFile.exists()) {
      writeText(document.asString(), textFile);
    }
    writeAnnotations(document);
  }

  private static void writeText(final String text, final File textFile) {
    try (BufferedWriter writer = new BufferedWriter(new FileWriter(textFile))) {
      writer.write(text);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public Stream<String> getIdStream() {
    try {
      log.info("Reading ids from {}", bratDir);
      return Files.list(bratDir.toPath())
          .flatMap(f -> {
            try {
              return Files.walk(f);
            } catch (IOException e) {
              throw Throwables.propagate(e);
            }
          })
          .filter(f -> f.toString().endsWith(".txt"))
          .map(f -> f.getFileName().toString().substring(0, f.getFileName().toString().length() - 4));
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  protected static String bratify(final String str) {
    return str.toUpperCase().replaceAll("[- ]", "_");
  }

  public static <A extends Annotation<A>> void setScribeAttr(final Attribute<? super A, String> feature, final BratEntity be,
                                                             final A ann, final String name, String... defaultValue) {
    final BratAttribute ba = be.getAttribute(name);
    if (ba != null) {
      ann.set(feature, ba.getValue());
    }
    else if (defaultValue.length > 0) {
      ann.set(feature, defaultValue[0]);
    }
  }

  // id<tab>type<space>startOffset<space>endOffset<tab>asString
  // T44  concept 432 444 heart attack
  protected static class BratEntity {
    private final int id;
    private final String type;
    private final long startOffset;
    private final long endOffset;
    private final String string;
    private final Map<String, BratAttribute> attributes;

    /**
     * Create a BratEntity from an Annotation
     * @param type the Brat Entity Type. Should be the exact string corresponding to the entry in the [entities] section
     *             of the annotation.conf file.
     * @param ann the annotation
     * @param registry BratEntity registry
     * @param <A> AnnotationType
     * @return BratEntity of the passed Annotation
     */
    public static <A extends Text & Annotation<A>> BratEntity fromAnnotation(final String type, final A ann,
                                                                             File textFile,
                                                                             final IntIdentifier<BratEntity> registry) {
      return new BratEntity(type, ann.asString(), ann.get(Annotation.StartOffset), ann.getDocument(), textFile, registry);
    }

    private BratEntity(final String type, String asString, final long start, final Document<?> document,
                       final File textFile, final IntIdentifier<BratEntity> registry) {
      this.id = registry.add(this);
      this.type = bratify(type);
      this.attributes = Maps.newHashMap();
      this.startOffset = start;

      // if the annotation spans more than one line, remove the interceding newline
      if (asString.contains("\n")) {
        if (asString.trim().contains("\n")) {
          String docString = document.asString();
          while (asString.contains("\n")) {
            docString = docString.replace('\n', ' ');
            asString = asString.replace('\n', ' ');
          }
          writeText(docString, textFile);
        }
        else {
          asString = asString.trim();
        }
      }

      this.string = asString;
      this.endOffset = start + string.length();
    }

    private BratEntity() {
      type = null;
      startOffset = -1;
      endOffset = -1;
      string = null;
      id = -1;
      this.attributes = null;
    }

    public static BratEntity create(final String type, final String asString, final long start, final Document<?> document,
                                    final File textFile, final IntIdentifier<BratEntity> registry) {
      return new BratEntity(type, asString, start, document, textFile, registry);
    }

    // we can just ignore the middle offset in the event of multi-line annotations.
    // e.g. T10 <Type> <Start1> <End1>;<Start2> <End2> <text>
    // ->   T10 <Type> <Start1> <End2> <text>
    public static BratEntity fromLine(final Iterable<String> line) {
      return new BratEntity(StreamSupport.stream(line.spliterator(), false).filter(s -> !s.contains(";"))
          .collect(Collectors.toList()).iterator());
    }

    /**
     * Convenience method for creating BratEntity from a line of a .ann file.
     * @param line Iterator of fields of an event from a Brat .ann file
     */
    private BratEntity(final Iterator<String> line) {
      try {
        this.attributes = Maps.newHashMap();
        this.id = Integer.parseInt(line.next().substring(1));
        this.type = line.next();
        this.startOffset = Long.parseLong(line.next());
        this.endOffset = Long.parseLong(line.next());
        if (startOffset == endOffset) {
          string = null;
        }
        else {
          string = line.next();
        }
      } catch (NoSuchElementException e) {
        final Iterable<String> itb = () -> line;
        System.out.printf("Cannot create entity from line: |%s|\n\n",
            StreamSupport.stream(itb.spliterator(), false).reduce("", (s1, s2) -> s1 + " | " + s2));
        throw new RuntimeException(e);
      }
    }


    public void addAttribute(final BratAttribute attr) {
      attributes.put(attr.getType().toUpperCase(), attr);
    }

    public String getId() {
      return "T" + id;
    }

    public String getType() {
      return type;
    }

    public long getStartOffset() {
      return startOffset;
    }

    public long getEndOffset() {
      return endOffset;
    }

    public long length() {
      return getEndOffset() - getStartOffset();
    }

    public BratAttribute getAttribute(final String type) {
      return attributes.get(type.toUpperCase());
    }

    @Override
    public String toString() {
      final StringBuilder sb = new StringBuilder();
      sb.append(getId()).append('\t').append(type).append(' ').append(startOffset).append(' ').append(endOffset)
          .append('\t').append(string);
      for (final BratAttribute attr : attributes.values()) {
        if (attr.value != null) {
          sb.append("\n").append(attr.toString(getId()));
        }
      }
      return sb.toString();
    }
    //added by stuart
    public String getString(){
      return this.string;
    }
  }

  // id<tab>type<space>entityId<space>value
  // A5  assertion T17 PRESENT
  protected static class BratAttribute {

    private final int id;
    private final String type;
    private final String governorId;
    private String value; //had to make this not final to support setting

    /**
     * Abstraction of a Brat attribute.
     * @param type Attribute type. Should be one of the exact types of the [attributes] section of the annotation.conf
     *             file.
     * @param governorId id of the BratEntity this is an attribute of
     * @param value Attribute value. Should be one of the exact values of the Attribute type from the [attributes]
     *              section of the annotation.conf file.
     * @param registry attribute registry for consistently numbering each attribute.
     */
    public BratAttribute(final String type, final String governorId, final String value, final IntIdentifier<BratAttribute> registry) {
      this.type = !type.equals(" NOTE") ? bratify(type) : type;
      this.governorId = governorId;
      this.value = value;
      if (this.value == null) {
        log.warn("Creating null attribute for entity {}", this.governorId);
      }
      id = registry.add(this);
    }

    public static BratAttribute create(final String type, final String governorId, final String value, final IntIdentifier<BratAttribute> registry) {
      return new BratAttribute(type, governorId, value, registry);
    }

    public static BratAttribute booleanAttribute(final String type, final String governorId, final IntIdentifier<BratAttribute> registry) {
      return new BratAttribute(type, governorId, "BOOLEAN", registry);
    }

    public static BratAttribute fromNote(String line) {
      String[] temp = line.split("\t");
      String text=temp[2];
      String tgovernorId=temp[1].split("\\s+")[1];
      return new BratAttribute(Arrays.asList(new String[]{temp[0]," NOTE",tgovernorId,text}).iterator());
    }

    private BratAttribute() {
      type = null;
      governorId = null;
      value = null;
      id = -1;
    }

    public BratAttribute(final Iterator<String> line) {
      id = Integer.parseInt(line.next().substring(1));
      type = line.next();
      governorId = line.next();
      if (line.hasNext()) {
        value = line.next();
      }
      else {
        value = "BOOLEAN";
      }
    }

    public String getId() {
      return "A" + id;
    }

    public String getType() {
      return type;
    }

    public String getGovernorId() {
      return governorId;
    }

    public String getValue() {
      return value;
    }

    //added by stuart
    public void setValue(final String val){
      this.value=val;
    }

    @Override
    public String toString() {
      if(this.value == null) {
        log.warn("Null attribute for entity {}", governorId);
        return "";
      }
      if(type.equals(" NOTE")) {
        return String.format("#%d\tAnnotatorNotes %s\t%s", this.id, this.governorId, this.value);
      }
      return getId() + "\t" + type + " " + governorId + " " + value;
    }

    public String toString(String gid) {
      if (!this.governorId.equals(gid)) {
        log.warn("Attribute has different governor id({}) than the one passed({})", this.governorId, gid);
      }
      if(this.value == null) {
        log.warn("Null attribute for entity {}", gid);
        return "";
      }
      if(type.equals(" NOTE")) {
        return String.format("#%d\tAnnotatorNotes %s\t%s", this.id, gid, this.value);
      }
      return getId() + "\t" + type + " " + gid + " " + value;
    }
  }

  protected static class BratRelation {
    private final int id;
    private final String type;
    private final String governorId;
    private final String dependentId;

    public static BratRelation equivalenceRelation(final String type, final String governorId, final String dependentId) {
      log.debug("Creating realtion from {} -> {}", governorId, dependentId);
      return new BratRelation(type, governorId, dependentId);
    }

    public static BratRelation of(final String type, final String governorId, final String dependentId, final IntIdentifier<BratRelation> registry) {
      return new BratRelation(type, governorId, dependentId, registry);
    }

    public static BratRelation fromLine(final String line) {
      final Splitter splitter = Splitter.on(CharMatcher.BREAKING_WHITESPACE).omitEmptyStrings();
      final List<String> list = splitter.splitToList(line);
      return new BratRelation(list.get(1), list.get(2).substring(5), list.get(3).substring(5));
    }

    public BratRelation(final String type, final String governorId, final String dependentId, final IntIdentifier<BratRelation> registry) {
      this.id = registry.add(this);
      this.type = type;
      this.governorId = governorId;
      this.dependentId = dependentId;
    }

    private BratRelation(final String type, final String governorId, final String dependentId) {
      this.id = -1;
      this.type = type;
      this.governorId = governorId;
      this.dependentId = dependentId;
    }

    private BratRelation() {
      this.id = -1;
      this.type = null;
      this.governorId = null;
      this.dependentId = null;
    }

    public String getId() {
      return (id == -1) ? "*" : "R" + id;
    }

    public String getType() {
      return type;
    }

    public String getGovernorId() {
      return governorId;
    }

    public String getDependentId() {
      return dependentId;
    }

    @Override
    public String toString() {
      return getId() + "\t" + getType() + " Arg1:" + getGovernorId() + " Arg2:" + getDependentId();
    }
  }

  public static class BratRules {
    String filename;
    LinkedList<Rule> rules;

    public BratRules(String ffilename) throws Exception{
      this.filename=ffilename;
      this.rules=new LinkedList<>();
      this.load(filename);
    }

    public void load(String filename) throws Exception {
      Scanner file = new Scanner(new File(filename));
      this.rules.clear();//make sure old rules are cleared out
      String[] curr = new String[4];
      int dex = 0;
      boolean inrule = false;
      while(file.hasNextLine()){
        String line = file.nextLine().trim();//get the next line
        if(!line.isEmpty() && line.charAt(0) == '#')//if it starts with a # it is a comment so skip
          continue;
        else if(inrule)//if in a rule stanza add the current line
          curr[dex++]=line;
        else if(line.isEmpty())//if not in a rule and line is empty skip it
          continue;
        else if(!inrule && line.startsWith("start")){//if not in a rule and line starts with start. Begin the rule.
          inrule=true;
          curr[dex++]=line;
        }else//If one of these conditions is not met the file is in an odd format.
          throw new Exception("Unexpected rules file format for " + this.filename);
        //if in a rule and 4 lines have been added break out of the rule since it shoud be done.
        if(inrule && dex == 4){
          inrule=false;
          this.rules.add(new Rule(curr));
          curr = new String[4];
          dex=0;
        }
      }
    }

    public String getFilename(){
      return this.filename;
    }

    //goes thru a brat corpus and applies all the rules to all the documents' entities
    public <D extends BaseDocument> void processCorpus(BratCorpus<D> bc, String as){
      bc.forEachDocument(doc -> {
        bc.writeAnnotations(doc,this);
      });
    }
    //processes rules on a brat entity
    public void processEntity(BratEntity be){
      for(Rule r : this.rules)
        r.process(be);
    }
    //class for storing the rule and such
    private class Rule{
      //raw array passed as a parameter. Not currently used
      String[] arr;
      //the line containing "start". May be used to pass args later. Separated by whitespace.
      String[] startstanz;
      String etype="";//Entity Type
      String etext="";//Entity text span
      String[][] conds;//Entity attribute conditions
      String[][] trans;//Entity attribute replacement rules
      public String toString(){
        return String.format("type=%s\n" +
                "text = \"%s\"\n" +
                "conditions = %s\n" +
                "replacements= %s\n",etype,etext,Arrays.deepToString(conds),Arrays.deepToString(trans));
      }
      public Rule(String[] lines){
        try {
          this.arr = lines;

          //any arguments should the feature be implimented
          this.startstanz = lines[0].split("\\s+");

          //entity type + text span
          String[] temp = lines[1].split("\t");
          this.etype = temp[0];
          this.etext = temp.length > 1 ? temp[1] : "";

          //attribute conditionals
          if(!lines[2].isEmpty()) {
            temp = lines[2].split("\\s+");
            assert temp.length % 2 == 0;
            this.conds = new String[temp.length / 2][2];
            for (int i = 0; i < temp.length; i += 2) {
              this.conds[i / 2][0] = temp[i];
              this.conds[i / 2][1] = temp[i + 1];
            }
          }else
            this.conds = new String[0][0];

          //replacement rules
          temp = lines[3].split("\\s+");
          assert temp.length % 2 == 0;
          assert temp.length > 0;
          this.trans = new String[temp.length / 2][2];
          for (int i = 0; i < temp.length; i += 2) {
            this.trans[i / 2][0] = temp[i];
            this.trans[i / 2][1] = temp[i + 1];
          }
        }catch(Error e){
          System.out.println("Failed while loading rule: " + Arrays.toString(lines));
          throw e;
        }
      }
      //returns true if meets criteria for rule to be applied
      //else returns false
      public boolean isValidTarget(BratEntity be){
	    if(!(this.etext.isEmpty() || this.etext.equals(be.getString()) && (this.etype.isEmpty() || this.etype.equals(be.getType()))))
		  return false;
        for(String[] cond : this.conds){
		  String battr = be.getAttribute(cond[0]).getValue();
          if(battr == null || !cond[1].equals(battr)) {
            return false;
          }
		}
        return true;
      }
      //applies rule reguardless of whether or not criteria is met
      public void applyRule(BratEntity be){
        for(String[] x : this.trans) {
          String temp = be.getAttribute(x[0]).getValue();
          be.getAttribute(x[0]).setValue(x[1]);
        }
      }
      //returns true if rule was applied. false otherwise.
      //only applies rule if it meets criteria for the rule
      public boolean process(BratEntity be){
        try{
        if(this.isValidTarget(be)) {
          this.applyRule(be);
          return true;
        }}catch(Exception e){
          System.out.println(be);
          throw e;
        }
        return false;
      }
    }
  }
}
