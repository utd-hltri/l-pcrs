package edu.utdallas.hltri.eeg.io;

import edu.utdallas.hltri.eeg.EegNote;
import edu.utdallas.hltri.eeg.annotation.AnatomicalSite;
import edu.utdallas.hltri.eeg.annotation.EegLocation;
import edu.utdallas.hltri.logging.Logger;
import edu.utdallas.hltri.scribe.io.BratCorpus;
import edu.utdallas.hltri.scribe.text.Document;
import edu.utdallas.hltri.scribe.text.annotation.Event;
import edu.utdallas.hltri.util.IntIdentifier;

import java.io.File;
import java.util.function.Function;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

/**
 * Created by ramon on 9/8/15.
 */
public class EegEventBratCorpus extends BratCorpus<EegNote> {
  private static final Logger log = Logger.get(EegEventBratCorpus.class);

  private final String annSetName;

  private EegEventBratCorpus(final File outDir, final String annSetName) {
    super(outDir);
    this.annSetName = annSetName;
  }

  public static EegEventBratCorpus at(final String outDir, final String annSetName) {
    return new EegEventBratCorpus(new File(outDir), annSetName);
  }

  public static EegEventBratCorpus at(final String outDir) {
    return new EegEventBratCorpus(new File(outDir), "brat");
  }

  public static EegEventBratCorpus at(final File outDir, final String annSetName) {
    return new EegEventBratCorpus(outDir, annSetName);
  }

  public static EegEventBratCorpus at(final File outDir) {
    return new EegEventBratCorpus(outDir, "brat");
  }

  @Override
  protected BiConsumer<BratEntity, Document<EegNote>> annotationCreator() {
    return (be, doc) -> {
      switch(be.getType()) {
        case "EEG_LOCATION":
          EegLocation.TYPE.create(doc, annSetName, be.getStartOffset(), be.getEndOffset());
          break;
        case "ANATOMICAL_SITE":
          AnatomicalSite.TYPE.create(doc, annSetName, be.getStartOffset(), be.getEndOffset());
          break;
        case "EEG_ACTIVITY":
          EegActivityBratCorpus.activityCreator(annSetName).accept(be, doc);
          break;
        default:
          Event event = Event.TYPE.create(doc, annSetName, be.getStartOffset(), be.getEndOffset())
              .set(Event.type, be.getType());
          setScribeAttr(Event.modality, be, event, "MODALITY", "FACTUAL");
          setScribeAttr(Event.polarity, be, event, "POLARITY", "POS");
      }
    };
  }

  @Override
  protected Supplier<List<BratEntity>> entitySupplier(Document<EegNote> document, IntIdentifier<BratEntity> entityRegistry,
                                                      IntIdentifier<BratAttribute> attributeRegistry,
                                                      Function<Document<?>, File> textFileFun) {
    return () -> {
      final List<BratEntity> entities = EegActivityBratCorpus.activitySaver(document, entityRegistry, attributeRegistry,
          annSetName, textFileFun).get();
      final File textFile = textFileFun.apply(document);
      for (Event event : document.get(annSetName, Event.TYPE)) {
        final BratEntity entity = BratEntity.fromAnnotation(event.get(Event.type), event, textFile, entityRegistry);
        if (event.get(Event.polarity) != null) {
          entity.addAttribute(new BratAttribute("POLARITY", entity.getId(), event.get(Event.polarity), attributeRegistry));
        }
        if (event.get(Event.modality) != null) {
          entity.addAttribute(new BratAttribute("MODALITY", entity.getId(), event.get(Event.modality), attributeRegistry));
        }
        entities.add(entity);
      }
      for (AnatomicalSite site : document.get(annSetName, AnatomicalSite.TYPE)) {
        BratEntity.fromAnnotation("ANATOMICAL_SITE", site, textFile, entityRegistry);
      }
      return entities;
    };
  }
}
