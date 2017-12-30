package edu.utdallas.hltri.eeg.relation;

/**
 * Created by rmm120030 on 9/25/17.
 */

import edu.utdallas.hltri.eeg.EegNote;
import edu.utdallas.hltri.eeg.annotation.AnatomicalSite;
import edu.utdallas.hltri.eeg.annotation.EegActivity;
import edu.utdallas.hltri.eeg.annotation.EegLocation;
import edu.utdallas.hltri.eeg.io.EegActivityBratCorpus;
import edu.utdallas.hltri.logging.Logger;
import edu.utdallas.hltri.scribe.io.BratCorpus;
import edu.utdallas.hltri.scribe.text.Document;
import edu.utdallas.hltri.scribe.text.annotation.AbstractAnnotation;
import edu.utdallas.hltri.scribe.text.annotation.Annotation;
import edu.utdallas.hltri.scribe.text.annotation.Event;
import edu.utdallas.hltri.util.IntIdentifier;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Created by ramon on 9/8/15.
 */
public class RelationBratCorpus extends BratCorpus<EegNote> {
  private static final Logger log = Logger.get(edu.utdallas.hltri.eeg.io.EegEventBratCorpus.class);

  private final String annSetName;
  private final Map<String, Annotation<?>> id2ann;
  private final Map<AbstractAnnotation<?>, String> ann2id;

  private RelationBratCorpus(final File outDir, final String annSetName) {
    super(outDir);
    this.annSetName = annSetName;
    this.id2ann = new HashMap<>();
    this.ann2id = new HashMap<>();
  }

  public static RelationBratCorpus at(final String outDir, final String annSetName) {
    return new RelationBratCorpus(new File(outDir), annSetName);
  }

  public static RelationBratCorpus at(final String outDir) {
    return new RelationBratCorpus(new File(outDir), "brat");
  }

  public static RelationBratCorpus at(final File outDir, final String annSetName) {
    return new RelationBratCorpus(outDir, annSetName);
  }

  public static RelationBratCorpus at(final File outDir) {
    return new RelationBratCorpus(outDir, "brat");
  }

  @Override
  protected BiConsumer<BratRelation, Document<EegNote>> relationCreator() {
    return (bratRelation, document) -> {
      final Annotation<?> gov = id2ann.get(bratRelation.getGovernorId());
      final Annotation<?> dep = id2ann.get(bratRelation.getDependentId());
      if (gov.getType().equals("Event")) {
        if (dep.getType().equals("Activity")) {
          final EAEegRelation rel = EAEegRelation.TYPE.create(annSetName, (Event) gov, (EegActivity) dep);
          rel.set(EegRelation.type, bratRelation.getType());
        } else if (dep.getType().equals("Event")) {
          final EEEegRelation rel = EEEegRelation.TYPE.create(annSetName, (Event) gov, (Event) dep);
          rel.set(EegRelation.type, bratRelation.getType());
        } else {
          throw new RuntimeException("Unrecognized dependant type: " + dep.getType());
        }
      } else if (gov.getType().equals("Activity")) {
        if (dep.getType().equals("Event")) {
          final AEEegRelation rel = AEEegRelation.TYPE.create(annSetName, (EegActivity) gov, (Event) dep);
          rel.set(EegRelation.type, bratRelation.getType());
        } else if (dep.getType().equals("Activity")) {
          log.warn("Ignoring A -> A relation with type: {}", bratRelation.getType());
        } else {
          throw new RuntimeException("Unrecognized dependant type: " + dep.getType());
        }
      }
    };
  }

  @Override
  protected Supplier<List<BratRelation>> relationSupplier(Document<EegNote> document, IntIdentifier<BratRelation> relationRegistry) {
    return Collections::emptyList;
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
          final EegActivity activity = EegActivityBratCorpus.createScribeActivity(doc, be, annSetName);
          id2ann.put(be.getId(), activity);
          break;
        default:
          final Event event = Event.TYPE.create(doc, annSetName, be.getStartOffset(), be.getEndOffset())
              .set(Event.type, be.getType());
          setScribeAttr(Event.modality, be, event, "MODALITY", "FACTUAL");
          setScribeAttr(Event.polarity, be, event, "POLARITY", "POS");
          id2ann.put(be.getId(), event);
      }
    };
  }

  @Override
  protected Supplier<List<BratEntity>> entitySupplier(Document<EegNote> document, IntIdentifier<BratEntity> entityRegistry,
                                                      IntIdentifier<BratAttribute> attributeRegistry,
                                                      Function<Document<?>, File> textFileFun) {
    return () -> {
      final List<BratEntity> entities = EegActivityBratCorpus.activitySaver(document, entityRegistry, attributeRegistry,
          annSetName, textFileFun, ann2id).get();
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
        ann2id.put(event, entity.getId());
      }
      for (AnatomicalSite site : document.get(annSetName, AnatomicalSite.TYPE)) {
        BratEntity.fromAnnotation("ANATOMICAL_SITE", site, textFile, entityRegistry);
      }
      return entities;
    };
  }
}

