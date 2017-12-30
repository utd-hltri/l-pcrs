package edu.utdallas.hltri.eeg.io;

import edu.utdallas.hltri.eeg.annotation.CtakesUmlsMention;
import edu.utdallas.hltri.eeg.annotation.UmlsConcept;
import edu.utdallas.hltri.logging.Logger;
import edu.utdallas.hltri.scribe.io.BratCorpus;
import edu.utdallas.hltri.scribe.text.BaseDocument;
import edu.utdallas.hltri.scribe.text.Document;
import edu.utdallas.hltri.scribe.text.annotation.Annotation;
import edu.utdallas.hltri.util.IntIdentifier;

import java.io.*;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Created by rmm120030 on 9/24/15.
 */
public class CtakesBratCorpus<D extends BaseDocument> extends BratCorpus<D> {
  private static final Logger log = Logger.get(CtakesBratCorpus.class);
  private static String annset = "ctakes";

  private CtakesBratCorpus(final File outDir, final String annset) {
    super(outDir);
    this.annset = annset;
  }

  public static <D extends BaseDocument> CtakesBratCorpus<D> at(final String outDir) {
    return new CtakesBratCorpus<D>(new File(outDir), annset);
  }

  public static <D extends BaseDocument> CtakesBratCorpus<D> at(final String outDir, final String annset) {
    return new CtakesBratCorpus<D>(new File(outDir), annset);
  }

//  @Override
//  protected void writeAnnotations(Document<D> document) {
//    log.info("CBC Writing {}", document.get(BaseDocument.id));
//    try (final BufferedWriter writer = new BufferedWriter(new FileWriter(new File(bratDir, document.get(BaseDocument.id) + ".ann")))) {
//      int eventCount = 1;
//
//      for (final CtakesUmlsMention mention : document.get(annset, CtakesUmlsMention.TYPE)) {
//        if (!mention.asString().contains("\n")) {
//          writer.write(String.format("T%d\t%s %d %d\t%s",
//              eventCount++, mention.get(CtakesUmlsMention.type), mention.get(Annotation.StartOffset),
//              mention.get(Annotation.EndOffset), mention.asString()));
//          writer.newLine();
//        }
//      }
//    } catch (IOException e) {
//      throw new RuntimeException(e);
//    }
//  }

  @Override
  protected BiConsumer<BratEntity, Document<D>> annotationCreator() {
    throw new UnsupportedOperationException();
  }

  @Override
  protected Supplier<List<BratEntity>> entitySupplier(Document<D> document, IntIdentifier<BratEntity> entityRegistry,
                                                      IntIdentifier<BratAttribute> attributeRegistry,
                                                      Function<Document<?>, File> textFileFun) {
    return () -> document.get(annset, CtakesUmlsMention.TYPE).stream().map(mention -> {
      final BratEntity be = BratEntity.fromAnnotation(convertTypeName(mention.get(CtakesUmlsMention.type)), mention,
          textFileFun.apply(document), entityRegistry);
      final String concepts = mention.get(CtakesUmlsMention.concepts).stream()
          .map(UmlsConcept::getCui).collect(Collectors.joining(","));
      be.addAttribute(BratAttribute.create("concepts", be.getId(), concepts, attributeRegistry));
      return be;
    }).collect(Collectors.toList());
  }

  private String convertTypeName(String type) {
    switch (type) {
      case "DISEASE_DISORDER": return "PROBLEM";
      case "MEDICATION": return "TREATMENT";
      default: return type;
    }
  }

//  CtakesUmlsMention.type, "ANATOMICAL_SITE");
//}
//          else if (mention.getName().contains("Medication")) {
//              ann.set(CtakesUmlsMention.type, "MEDICATION");
//              }
//              else if (mention.getName().contains("DiseaseDisorder")) {
//              ann.set(CtakesUmlsMention.type, "DISEASE_DISORDER");
//              }
//              else if (mention.getName().contains("SignSymptom")) {
//              ann.set(CtakesUmlsMention.type, "SIGN_SYMPTOM");
//              }
//              else if (mention.getName().contains("Procedure")) {
//              ann.set(CtakesU

  @Override
  protected void readAnnotations(final Document<D> document, final File annotationFile) {
    throw new UnsupportedOperationException();
//    try (BufferedReader reader = new BufferedReader(new FileReader(annotationFile))) {
//      final Map<String, Event> entities = Maps.newHashMap();
//      final Splitter splitter = Splitter.on(CharMatcher.WHITESPACE).omitEmptyStrings();
//      String line;
//      while ((line = reader.readLine()) != null) {
//        final Iterator<String> lineIt = splitter.split(line).iterator();
//        if (line.startsWith("T") ) {
//          if (line.contains("EEG_LOCATION")) {
//            lineIt.next();
//            lineIt.next();
//            EegLocation.TYPE.create(document, annSetName, Long.parseLong(lineIt.next()), Long.parseLong(lineIt.next()));
//          }
//          else if (line.contains("ANATOMICAL_SITE")) {
//            lineIt.next();
//            lineIt.next();
//            AnatomicalSite.TYPE.create(document, annSetName, Long.parseLong(lineIt.next()), Long.parseLong(lineIt.next()));
//          }
//          else {
//            final BratCorpus.BratEntity be = new BratCorpus.BratEntity(lineIt);
//            // default to factual and positive
//            entities.put(be.getId(), Event.TYPE.create(document, annSetName, be.getStartOffset(), be.getEndOffset())
//                .set(Event.type, be.getType())
//                .set(Event.modality, "FACTUAL")
//                .set(Event.polarity, "POS"));
////            log.info("Made {}", entities.get(be.getId()).describe());
//          }
//        }
//        else if (line.startsWith("A")) {
//          final BratCorpus.BratAttribute ba = new BratCorpus.BratAttribute(lineIt);
//          final Event e = entities.get(ba.getGovernorId());
//          assert (e != null) : String.format("Found annotation with no preceding entity. A: %s", line);
//          if (ba.getType().equalsIgnoreCase("MODALITY")) {
//            e.set(Event.modality, ba.getValue());
//          }
//          else if (ba.getType().equalsIgnoreCase("POLARITY")) {
//            e.set(Event.polarity, ba.getValue());
//          }
//        }
//      }
//    } catch (IOException e) {
//      throw new RuntimeException(e);
//    } catch (DuplicateAnnotationException e) {
//      throw new RuntimeException(String.format("%s - Attempting to create annotation at [%s]. Existing annotation: %s",
//          document.get(BaseDocument.id),
//          document.subString(e.old.getStartNode().getOffset(), e.old.getEndNode().getOffset()),
//          e.old));
//    }
  }

}
