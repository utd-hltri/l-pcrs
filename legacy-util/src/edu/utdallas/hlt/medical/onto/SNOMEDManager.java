package edu.utdallas.hlt.medical.onto;


import com.google.common.collect.HashMultimap;
import com.google.common.collect.SetMultimap;
import edu.utdallas.hlt.util.Strings;
import gnu.trove.iterator.TLongIterator;
import gnu.trove.map.hash.TLongObjectHashMap;
import java.io.*;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SNOMEDManager {

  private static final Logger LOGGER = LoggerFactory.getLogger(SNOMEDManager.class);
  private static final long serialVersionUID = 4L;


  public static enum SNOMEDRelationshipType {
    HAS_SPECIMEN(116686009),
    RECIPIENT_CATEGORY(370131001),
    PROCEDURE_SITE(363704007),
    ROUTE_OF_ADMINISTRATION(410675002),
    PART_OF(123005000),
    METHOD(260686004),
    WAS_A(159083000),
    SPECIMEN_SUBSTANCE(370133003),
    ASSOCIATED_PROCEDURE(363589002),
    HAS_INTERPRETATION(363713009),
    ASSOCIATED_MORPHOLOGY(116676008),
    IS_A(116680003),
    HAS_DEFINITIONAL_MANIFESTATION(363705008),
    SEVERITY(246112005),
    DUE_TO(42752001),
    CAUSATIVE_AGENT(246075003),
    PROCEDURE_CONTEXT(408730004),
    DIRECT_MORPHOLOGY(363700003),
    FINDING_INFORMER(419066007),
    USING_DEVICE(424226004),
    HAS_DOSE_FORM(411116001),
    AFTER(255234002),
    FINDING_SITE(363698007),
    PROCEDURE_DEVICE(405815000),
    USING_ACCESS_DEVICE(425391005),
    SPECIMEN_SOURCE_TOPOGRAPHY(118169006),
    SPECIMEN_SOURCE_MORPHOLOGY(118168003),
    PROCEDURE_SITE_DIRECT(405813007),
    MEASUREMENT_METHOD(370129005),
    HAS_FOCUS(363702006),
    EPISODICITY(246456000),
    SAME_AS(168666000),
    LATERALITY(272741003),
    SPECIMEN_SOURCE_IDENTITY(118170007),
    USING_SUBSTANCE(424361007),
    SUBJECT_RELATIONSHIP_CONTEXT(408732007),
    OCCURRENCE(246454002),
    SCALE_TYPE(370132008),
    HAS_INTENT(363703001),
    ASSOCIATED_WITH(47429007),
    PRIORITY(260870009),
    INTERPRETS(363714003),
    INDIRECT_DEVICE(363710007),
    DIRECT_DEVICE(363699004),
    MOVED_TO(370125004),
    INDIRECT_MORPHOLOGY(363709002),
    TEMPORAL_CONTEXT(408731000),
    SPECIMEN_PROCEDURE(118171006),
    FINDING_METHOD(418775008),
    REPLACED_BY(370124000),
    SURGICAL_APPROACH(424876005),
    MAY_BE_A(149016008),
    COMPONENT(246093002),
    CLINICAL_COURSE(263502005),
    PROPERTY(370130000),
    PROCEDURE_MORPHOLOGY(405816004),
    ASSOCIATED_FINDING(246090004),
    USING_ENERGY(424244007),
    DIRECT_SUBSTANCE(363701004),
    REVISION_STATUS(246513007),
    ACCESS(260507000),
    HAS_ACTIVE_INGREDIENT(127489000),
    PROCEDURE_SITE_INDIRECT(405814001),
    FINDING_CONTEXT(408729009),
    PATHOLOGICAL_PROCESS(370135005);
    private long value;
    public static Map<Long,SNOMEDRelationshipType> lookup;


    private SNOMEDRelationshipType(long value) {
      this.value = value;
      add(value, this);
    }

    private void add(long value, SNOMEDRelationshipType type) { // ghetto ass shit, don't even
      if (lookup == null) {
        lookup = new HashMap<>();
      }
      lookup.put(value, type);
    }

    public static SNOMEDRelationshipType forValue(long value) {
      return lookup.get(value);
    }

    public long getValue() {
      return value;
    }
  }

  public static class SNOMEDDescription {
    public long descriptionID;
    public int descriptionStatus;
    public long conceptID;
    public String term;
    public int initialCapitalStatus;
    public int descriptionType;
    public String languageCode;

    public SNOMEDDescription(String line) {
      List<String> fields = Strings.split(line, "\t");
      assert fields.size() == 7 : "Expected 6 fields: " + line;
      this.descriptionID = Long.parseLong(fields.get(0));
      this.descriptionStatus = Integer.parseInt(fields.get(1));
      this.conceptID = Long.parseLong(fields.get(2));
      this.term= fields.get(3);
      this.initialCapitalStatus = Integer.parseInt(fields.get(4));
      this.descriptionType = Integer.parseInt(fields.get(5));
      this.languageCode = fields.get(6);
    }
  }

  /* Paths for serialized output */
  public final File CACHED_CONCEPT_PATH;
  public final File CACHED_RELATION_PATH;


  // Snomed paths, everything is relative to the snomed root directory
  private final File snomedRoot;
  private static final String CORE_PATH = "Terminology/Content/";
  private static final String CORE_CONCEPTS_PATH = CORE_PATH + "sct1_Concepts_Core_INT_20100131.txt";
  private static final String CORE_DESCRIPTIONS_PATH = CORE_PATH + "sct1_Descriptions_en_INT_20100131.txt";
  private static final String CORE_RELATIONSHIPS_PATH = CORE_PATH + "sct1_Relationships_Core_INT_20100131.txt";
  private static final String DRUG_PATH = "USDrugExtension/Terminology/Content/";
  private static final String DRUG_CONCEPTS_PATH = DRUG_PATH + "sct1_Concepts_USDrug_INT1000002_20100131.txt";
  private static final String DRUG_DESCRIPTIONS_PATH = DRUG_PATH + "sct1_Descriptions_USDrug-en-US_INT1000002_20100131.txt";
  private static final String DRUG_RELATIONSHIPS_PATH = DRUG_PATH + "sct1_Relationships_USDrug_INT1000002_20100131.txt";

  /* Internal maps */
  public SetMultimap<String, Long> nameToConceptIds;
  public TLongObjectHashMap<String> conceptIdToName;
  public TLongObjectHashMap<List<SNOMEDRelation>> conceptIdToRelations;
  public TLongObjectHashMap<List<SNOMEDDescription>> conceptIdToDescriptions;
  public Set<String> cache_ids;
  public Set<Long> cache_names;
  public Set<Long> cache_relations;
  public SetMultimap<String, Long> cache_nameToConceptIds;
  public TLongObjectHashMap<String> cache_conceptIdToName;
  public TLongObjectHashMap<List<SNOMEDRelation>> cache_conceptIdToRelations;

  private boolean enableCache = false;


  public SNOMEDManager(File snomedRoot, File cacheRoot) {
    this.snomedRoot = snomedRoot;
    // Initialize maps
    nameToConceptIds = HashMultimap.create();
    conceptIdToName = new TLongObjectHashMap<>();
    conceptIdToRelations = new TLongObjectHashMap<>();
    conceptIdToDescriptions = new TLongObjectHashMap<>();

    cache_ids = new HashSet<>();
    cache_names = new HashSet<>();
    cache_relations = new HashSet<>();
    cache_nameToConceptIds = HashMultimap.create();
    cache_conceptIdToName = new TLongObjectHashMap<>();
    cache_conceptIdToRelations = new TLongObjectHashMap<>();


      // Initialize serialized output file

      // Read cached data from disk
    CACHED_CONCEPT_PATH = new File(cacheRoot, "data/cache/snomed/snomed_concepts_cache.ser");
    CACHED_RELATION_PATH = new File(cacheRoot, "data/cache/snomed/snomed_relations_cache.ser");

    if (enableCache) {
      try {
        ObjectInputStream in = new ObjectInputStream(new BufferedInputStream(new FileInputStream(CACHED_CONCEPT_PATH)));
        LOGGER.debug("Restoring cached concept data from {}.", CACHED_CONCEPT_PATH);

        cache_nameToConceptIds = (HashMultimap<String, Long>) in.readObject();
        cache_conceptIdToName = (TLongObjectHashMap<String>) in.readObject();
        cache_ids = (HashSet<String>) in.readObject();
        cache_names = (HashSet<Long>) in.readObject();
      } catch (ClassCastException | ClassNotFoundException e) {
        LOGGER.error("Cached concept data is corrupt!", e);
      }catch (IOException e) {
       LOGGER.warn("Unable to restore cached concept data!", e);
     }
      try {
        ObjectInputStream in = new ObjectInputStream(new BufferedInputStream(new FileInputStream(CACHED_RELATION_PATH)));
        LOGGER.debug("Restoring cached relationship data from {}.", CACHED_RELATION_PATH);

        cache_conceptIdToRelations = (TLongObjectHashMap<List<SNOMEDRelation>>) in.readObject();
        cache_relations = (HashSet<Long>) in.readObject();
      } catch (ClassCastException | ClassNotFoundException e) {
        LOGGER.error("Cached relationship data is corrupt!", e);
      }catch (IOException e) {
       LOGGER.warn("Unable to restore cached relationship data!", e);
     }
    }
  }


  private void loadConcepts() {
    try {
      LOGGER.info("Initializing SNOMED core concepts.");
      parseConcepts(CORE_CONCEPTS_PATH, nameToConceptIds, conceptIdToName);
      LOGGER.info("Initializing SNOMED drug concepts.");
      parseConcepts(DRUG_CONCEPTS_PATH, nameToConceptIds, conceptIdToName);
    } catch (IOException ex) {
      throw new RuntimeException(ex);
    }
  }

  private void loadRelations() {
    try {
      LOGGER.info("Initializing SNOMED core relations.");
      parseRelations(CORE_RELATIONSHIPS_PATH, conceptIdToRelations);
      LOGGER.info("Initializing SNOMED drug relations.");
      parseRelations(DRUG_RELATIONSHIPS_PATH, conceptIdToRelations);
    } catch (IOException ex) {
      throw new RuntimeException(ex);
    }
  }

  private void loadDescriptions() {
    try {
      LOGGER.info("Initializing SNOMED core descriptions.");
      parseDescriptions(CORE_DESCRIPTIONS_PATH, conceptIdToDescriptions);
      LOGGER.info("Initializing SNOMED drug descriptions.");
      parseDescriptions(DRUG_DESCRIPTIONS_PATH, conceptIdToDescriptions);
    } catch (IOException ex) {
      throw new RuntimeException(ex);
    }
  }

  boolean closed = false;
  public void close() {
    try {
      LOGGER.info("Saving SNOMED cache to {} & {}.", CACHED_CONCEPT_PATH, CACHED_RELATION_PATH);

      CACHED_CONCEPT_PATH.getParentFile().mkdirs();
      ObjectOutputStream out = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(CACHED_CONCEPT_PATH)));

      out.writeObject(cache_nameToConceptIds);
      out.writeObject(cache_conceptIdToName);
      out.writeObject(cache_ids);
      out.writeObject(cache_names);
      out.close();

      CACHED_RELATION_PATH.getParentFile().mkdirs();
      out = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(CACHED_RELATION_PATH)));

      out.writeObject(cache_conceptIdToRelations);
      out.writeObject(cache_relations);
      out.close();

      closed = true;
    } catch (IOException e) {
      LOGGER.error("Error serializing cached SNOMED data!", e);
    }
  }

  @Override
  protected void finalize() throws Throwable {
    if (!closed) {
      try {
        LOGGER.warn("SNOMEDManager was not closed properly.");
        close();
      } finally {
        super.finalize();
      }
    }
  }

  public static String getParsedName(String name) {
    // Strip off ending " (description)" text
    int delim = name.lastIndexOf('(');
    String result = name;
    try {
      if (delim > 0) {
        result = name.substring(0, delim - 1);
      }
    } catch (StringIndexOutOfBoundsException e) {
      LOGGER.error("StringIndexOutOfBoundsException", e);
    }
    return result.toLowerCase();
  }

  private void parseConcepts(String path, SetMultimap<String, Long> nameToId, TLongObjectHashMap<String> idToName) throws FileNotFoundException, IOException {
    BufferedReader reader = new BufferedReader(new FileReader(new File(snomedRoot, path)));

    // Skip the first line
    String line = reader.readLine();
    int lineNo = 0;
    while ((line = reader.readLine()) != null) {
      String[] columns = line.split("\t");

      // Assure we have the correct number of fields
      if (columns.length == 6) {
        try {
          long conceptId = Long.parseLong(columns[0]);
          String conceptStatus = columns[1];
          String fullySpecifiedName = columns[2];
          String ctv3Id = columns[3];
          String snomedId = columns[4];
          String isPrimitive = columns[5];

          // Parse the name and store it in both maps
          String parsedName = getParsedName(fullySpecifiedName);

          nameToId.put(parsedName, conceptId);
          idToName.put(conceptId, parsedName);
        } catch (NumberFormatException e) {
          long max = Long.MAX_VALUE;
          long number = Long.parseLong(columns[0]);
          LOGGER.warn("Found illegal int value {} (max: {}) on line {}.", new Object[]{number, max, lineNo, e});
        } finally {
          lineNo++;
        }
      } else {
        // Something went wrong.
        LOGGER.warn("Found {} columns. Excepted 6.", columns.length);
      }
    }
  }

  private void addRelation(TLongObjectHashMap<List<SNOMEDRelation>> idToRelations, long id, SNOMEDRelation relation) {
    List<SNOMEDRelation> list = idToRelations.get(id);
    if (list == null) {
      list = new LinkedList<>();
    }

    list.add(relation);
    idToRelations.put(id, list);
  }

  private void parseRelations(String path, TLongObjectHashMap<List<SNOMEDRelation>> idToRelations) throws FileNotFoundException, IOException {
    BufferedReader reader = new BufferedReader(new FileReader(new File(snomedRoot, path)));

    // Skip the first line
    String line = reader.readLine();
    int lineNo = 0;
    while ((line = reader.readLine()) != null) {
      String[] columns = line.split("\t");

      // Assure we have the correct number of fields
      if (columns.length == 7) {
        try {
          long relationshipId = Long.parseLong(columns[0]);
          long conceptId1 = Long.parseLong(columns[1]);
          long relationshipType = Long.parseLong(columns[2]);
          long conceptId2 = Long.parseLong(columns[3]);
          String characteristicType = columns[4];
          String refinability = columns[5];
          String relationshipGroup = columns[6];

          // Generate SNOMEDRelation object
          SNOMEDRelation relation = new SNOMEDRelation(
                  conceptId1,
                  relationshipType,
                  conceptId2);

          //  Store the relation under both concepts
          addRelation(idToRelations, conceptId1, relation);
          addRelation(idToRelations, conceptId2, relation);
        } catch (NumberFormatException e) {
          long max = Long.MAX_VALUE;
          long number = Long.parseLong(columns[0]);
          LOGGER.warn("Found illegal int value {} (max: {}) on line {}.", new Object[]{number, max, lineNo, e});
        } finally {
          lineNo++;
        }
      } else {
        // Something went wrong.
        LOGGER.warn("Found {} columns. Expected 7.", columns.length);
      }
    }
  }

  private void parseDescriptions(String path, TLongObjectHashMap<List<SNOMEDDescription>> idToDescriptions) throws FileNotFoundException, IOException {
    BufferedReader reader = new BufferedReader(new FileReader(new File(snomedRoot, path)));

    // Skip the first line
    String line = reader.readLine();
    int lineNo = 0;
    while ((line = reader.readLine()) != null) {
      SNOMEDDescription descrip = new SNOMEDDescription(line);
      if ( ! idToDescriptions.containsKey(descrip.conceptID)) {
        idToDescriptions.put(descrip.conceptID, new ArrayList<SNOMEDDescription>());
      }
      idToDescriptions.get(descrip.conceptID).add(descrip);
    }
  }

  public static enum SNOMEDRelationshipDirection {
    SOURCE,
    TARGET,
    BOTH,
  };

  private Set<Long> getConceptId(String name) {
    Set<Long> ids = cache_nameToConceptIds.get(name);

    if (cache_ids.contains(name)) {
      return ids;
    }

    LOGGER.debug("Name \"{}\" not found in SNOMED concept cache.", name);
    if (nameToConceptIds.isEmpty()) {
      loadConcepts();
    }

    ids = nameToConceptIds.get(name);
    if (ids == null) {
      ids = new HashSet<>();
    }

    cache_nameToConceptIds.putAll(name, ids);
    cache_ids.add(name);

    return ids;
  }

  private String getName(long id) {
    String name = cache_conceptIdToName.get(id);

    if (cache_names.contains(id)) {
      return name;
    }

    LOGGER.debug("ID {} not found in SNOMED concept cache.", id);
    if (conceptIdToName.isEmpty()) {
      loadConcepts();
    }

    name = conceptIdToName.get(id);
    if (name == null) {
      name = "";
    }
    cache_conceptIdToName.put(id, name);
    cache_names.add(id);

    return name;
  }

  private List<SNOMEDRelation> getRelations(long id) {
    List<SNOMEDRelation> relations = cache_conceptIdToRelations.get(id);

    if (cache_relations.contains(id)) {
      return relations;
    }

    LOGGER.debug("ID {} not found in SNOMED relation cache.", id);
    if (conceptIdToRelations.isEmpty()) {
      loadRelations();
    }

    relations = conceptIdToRelations.get(id);
    if (relations == null) {
      relations = new LinkedList<>();
    }
    cache_conceptIdToRelations.put(id, relations);
    cache_relations.add(id);

    return relations;
  }

  public List<SNOMEDDescription> getDescriptions(long id) {
    if (conceptIdToDescriptions.isEmpty()) {
      loadDescriptions();
    }
    List<SNOMEDDescription> descriptions = conceptIdToDescriptions.get(id);
    if (descriptions == null) {
      descriptions = Collections.EMPTY_LIST;
    }
    return descriptions;
  }

  public List<String> getSynonyms(long id) {
    List<SNOMEDDescription> descriptions = getDescriptions(id);
    List<String> syns = new ArrayList<>();
    for (SNOMEDDescription descrip : descriptions) {
      String term = descrip.term.toLowerCase();
      if (descrip.initialCapitalStatus != 0) {
        String newTerm = String.valueOf(Character.toUpperCase(term.charAt(0)));
        if (term.length() > 1) {
          newTerm = newTerm + term.substring(1);
        }
        term = newTerm;
      }
      syns.add(term);
    }
    return syns;
  }

  public Set<String> getRelatedConcepts(String name, SNOMEDRelationshipType relationshipType, int levels, SNOMEDRelationshipDirection direction) {
    // Return nothing if we are asked for nonsense
    if (levels < 0) {
      return new HashSet<>();
    }

    Set<Long> ids = getConceptId(getParsedName(name));
    Set<String> results = new HashSet<>();

    if (ids != null) {
      for (long id : ids) {
        results.addAll(getRelatedConcepts(id, relationshipType, levels, direction));
      }
    }

    return results;
  }

  private Set<String> getRelatedConcepts(long conceptID, SNOMEDRelationshipType relationshipType, int levels, SNOMEDRelationshipDirection direction) {
    return getNames(getRelatedConceptIDs(conceptID, relationshipType, levels, direction));
  }

  public Set<Long> getRelatedConceptIDs(long conceptID, SNOMEDRelationshipType relationshipType, int levels, SNOMEDRelationshipDirection direction) {
    Set<Long> matching = new HashSet<>();
    List<SNOMEDRelation> all = getRelations(conceptID);

    if (all != null) {
      for (SNOMEDRelation relation : all) {
        long source = 0L;
        long target = 0L;
        //System.err.println("Relation: " + SNOMEDRelationshipType.forValue(relation.relationshipType) + " " + getName(relation.conceptId2));
        switch (direction) {
          case SOURCE:
            source = relation.conceptId2;
            target = relation.conceptId1;
            break;
          case TARGET:
            source = relation.conceptId1;
            target = relation.conceptId2;
            break;
          case BOTH:
            matching.addAll(getRelatedConceptIDs(conceptID, relationshipType, levels, SNOMEDRelationshipDirection.SOURCE));
            matching.addAll(getRelatedConceptIDs(conceptID, relationshipType, levels, SNOMEDRelationshipDirection.TARGET));
            return matching;
        }

        if (relation.relationshipType == relationshipType.getValue() && source == conceptID) {
          matching.add(target);
          if (levels > 1) {
            matching.addAll(getRelatedConceptIDs(target, relationshipType, levels - 1, direction));
          }
        }
      }
    }

    return matching;
  }

  public Set<String> getNames(Iterable<Long> ids) {
    Set<String> names = new HashSet<>();
    for (Long id : ids) {
      names.add(getName(id));
    }
    return names;
  }

  /** Removes any items which are a substring of another item */
  public Set<String> filterExpandedConcepts(Set<String> list) {
    Set<String> concepts = (Set) ((HashSet) list).clone();
    Set<String> results = new HashSet<>();

    for (Iterator<String> it = concepts.iterator(); it.hasNext();) {
      String concept = it.next();

      boolean isPrefix = true;
      for (Iterator<String> jt = concepts.iterator(); jt.hasNext();) {
        String other = jt.next();

        // Compare against all other elements
        if (!concept.equals(other) && concept.startsWith(other)) {
          isPrefix = false;
          break;
        }
      }

      if (isPrefix) {
        results.add(concept);
      } else {
        it.remove();
      }
    }

    return results;
  }

  public Set<String> getFilteredConcepts(String name, SNOMEDRelationshipType relationshipType, int levels, SNOMEDRelationshipDirection direction) {
    Set<String> concepts = getRelatedConcepts(name, relationshipType, levels, direction);
    String parsedName = getParsedName(name);
    concepts.add(parsedName);
    LOGGER.debug("CONCEPTS = {}", concepts);
    concepts = filterExpandedConcepts(concepts);
    concepts.remove(parsedName);

    LOGGER.debug("FILTERED = {}", concepts);

    return concepts;
  }

  public Set<Long> conceptIDS() {
    if (conceptIdToName.isEmpty()) { loadConcepts(); }
    Set<Long> ids = new HashSet<>();
    TLongIterator it = conceptIdToName.keySet().iterator();
    while (it.hasNext()) {
      ids.add(it.next());
    }
    return ids;
  }

  public static class SNOMEDRelation implements java.io.Serializable {

    public long conceptId1;
    public long conceptId2;
    public long relationshipType;

    public SNOMEDRelation(long a, long type, long b) {
      conceptId1 = a;
      conceptId2 = b;
      relationshipType = type;
    }
  }

  public static void main(String[] args) {
    File snomedRoot= new File(args[0]);
    File cacheRoot= new File(args[1]);
    SNOMEDManager manager = new SNOMEDManager(snomedRoot, cacheRoot);


    String name = null;
    Scanner sc = new Scanner(System.in);

    System.out.printf("Look up: ");
    while ((name = sc.nextLine()) != null && !name.equals("exit")) {
      int delim = name.indexOf('@');
      int levels = 2;
      if (delim > 0) {
        levels = Integer.parseInt(name.substring(delim + 1).trim());
        name = name.substring(0, delim).trim();
      }

      System.out.printf("--- Parent Concepts :: %d Levels ---\n", levels);
      for (String concept : manager.getFilteredConcepts(name, SNOMEDRelationshipType.IS_A, levels, SNOMEDRelationshipDirection.TARGET)) {
        System.out.println(concept);
      }

      System.out.printf("--- Children Concepts :: %d Levels ---\n", levels);
      for (String concept : manager.getFilteredConcepts(name, SNOMEDRelationshipType.IS_A, levels, SNOMEDRelationshipDirection.SOURCE)) {
        System.out.println(concept);
      }

      System.out.printf("--- Finding sites :: %d Levels ---\n", levels);
      for (String concept : manager.getFilteredConcepts(name, SNOMEDRelationshipType.FINDING_SITE, levels, SNOMEDRelationshipDirection.TARGET)) {
        System.out.println(concept);
      }

      System.out.printf("--- Part of :: %d Levels ---\n", levels);
      for (String concept : manager.getFilteredConcepts(name, SNOMEDRelationshipType.PART_OF, levels, SNOMEDRelationshipDirection.TARGET)) {
        System.out.println(concept);
      }

      System.out.printf("--- Synonyms :: %d Levels ---\n", 1);
      for (Long id : manager.getConceptId(name)) {
        for (String syn : manager.getSynonyms(id)) {
          System.out.printf(syn + "  ");
        }
        System.out.println();
      }

      System.out.printf("\nLook up: ");
    }
  }
}
