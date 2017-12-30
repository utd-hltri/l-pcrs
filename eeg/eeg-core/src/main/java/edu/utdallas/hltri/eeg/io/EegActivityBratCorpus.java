package edu.utdallas.hltri.eeg.io;

import com.google.common.collect.Lists;
import edu.utdallas.hltri.eeg.EegNote;
import edu.utdallas.hltri.eeg.annotation.EegActivity;
import edu.utdallas.hltri.logging.Logger;
import edu.utdallas.hltri.scribe.io.BratCorpus;
import edu.utdallas.hltri.scribe.text.BaseDocument;
import edu.utdallas.hltri.scribe.text.Document;
import edu.utdallas.hltri.scribe.text.annotation.AbstractAnnotation;
import edu.utdallas.hltri.util.IntIdentifier;

import java.io.File;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

/**
 * Created by stuart on 5/13/16.
 */
public class EegActivityBratCorpus extends BratCorpus<EegNote> {
  private static final Logger log = Logger.get(EegActivityBratCorpus.class);

  private final String annset;

  public EegActivityBratCorpus(final File bratDir, final String annset) {
    super(bratDir);
    this.annset = annset;
  }


  @Override
  protected BiConsumer<BratEntity, Document<EegNote>> annotationCreator() {
    return (entity, doc) -> activityCreator(annset).accept(entity, doc);
  }

  @Override
  protected Supplier<List<BratEntity>> entitySupplier(Document<EegNote> document, IntIdentifier<BratEntity> entityRegistry,
                                                      IntIdentifier<BratAttribute> attributeRegistry,
                                                      Function<Document<?>, File> textFileFun) {
    return activitySaver(document, entityRegistry, attributeRegistry, annset, textFileFun);
  }

  public static BiConsumer<BratEntity, Document<? extends BaseDocument>> activityCreator(String annset) {
    return (entity, doc) -> {
      if (entity.getType().equals("EEG_ACTIVITY")) {
        createScribeActivity(doc, entity, annset);
      }
    };
  }

  public static Supplier<List<BratEntity>> activitySaver(Document<? extends BaseDocument> document, IntIdentifier<BratEntity> entityRegistry,
                                                         IntIdentifier<BratAttribute> attributeRegistry, String annset,
                                                         Function<Document<?>, File> textFileFun) {
    return activitySaver(document, entityRegistry, attributeRegistry, annset, textFileFun, null);
  }

  public static Supplier<List<BratEntity>> activitySaver(Document<? extends BaseDocument> document, IntIdentifier<BratEntity> entityRegistry,
                                                         IntIdentifier<BratAttribute> attributeRegistry, String annset,
                                                         Function<Document<?>, File> textFileFun,
                                                         Map<AbstractAnnotation<?>, String> ann2id) {
    return () -> {
      final List<BratEntity> list = Lists.newArrayList();
      for (final EegActivity activity : document.get(annset, EegActivity.TYPE)) {
        final BratEntity entity = BratEntity.fromAnnotation("EEG_ACTIVITY", activity, textFileFun.apply(document), entityRegistry);
        final String eid = entity.getId();
        if (!Objects.isNull(ann2id)) {
          ann2id.put(activity, eid);
        }
        entity.addAttribute(new BratAttribute("FREQUENCY_BAND", eid, activity.get(EegActivity.band), attributeRegistry));
        final String pol = activity.get(EegActivity.polarity);
        entity.addAttribute(new BratAttribute("POLARITY", eid, (pol == null) ? "POS" : pol, attributeRegistry));
        final String mod = activity.get(EegActivity.modality);
        entity.addAttribute(new BratAttribute("MODALITY", eid, (mod == null) ? "FACTUAL" : mod, attributeRegistry));
        entity.addAttribute(new BratAttribute("MORPHOLOGY", eid, activity.get(EegActivity.morphology), attributeRegistry));
        entity.addAttribute(new BratAttribute("HEMISPHERE", eid, activity.get(EegActivity.hemisphere), attributeRegistry));
        entity.addAttribute(new BratAttribute("DISPERSAL", eid, activity.get(EegActivity.dispersal), attributeRegistry));
        entity.addAttribute(new BratAttribute("RECURRENCE", eid, activity.get(EegActivity.recurrence), attributeRegistry));
        entity.addAttribute(new BratAttribute("MAGNITUDE", eid, activity.get(EegActivity.magnitude), attributeRegistry));
        entity.addAttribute(new BratAttribute("IN_BACKGROUND", eid, activity.get(EegActivity.in_background), attributeRegistry));
        //if()
        entity.addAttribute(new BratAttribute(" NOTE", eid, activity.get(EegActivity.note), attributeRegistry));
        // boolean location attributes
        activity.getLocations().stream().map(EegActivity.Location::toString).forEach(loc ->
            entity.addAttribute(BratAttribute.booleanAttribute(loc, eid, attributeRegistry)));

        list.add(entity);
      }

      return list;
    };
  }

  public static EegActivity createScribeActivity(Document<? extends BaseDocument> doc, BratEntity entity, String annset) {
    final EegActivity activity = EegActivity.TYPE.create(doc, annset, entity.getStartOffset(), entity.getEndOffset());
    setScribeAttr(EegActivity.band, entity, activity, "FREQUENCY_BAND", "NA");
    setScribeAttr(EegActivity.polarity, entity, activity, "POLARITY", "POS");
    setScribeAttr(EegActivity.modality, entity, activity, "MODALITY", "FACTUAL");
    setScribeAttr(EegActivity.morphology, entity, activity, "MORPHOLOGY");
    setScribeAttr(EegActivity.hemisphere, entity, activity, "HEMISPHERE", "NA");
    setScribeAttr(EegActivity.dispersal, entity, activity, "DISPERSAL", "NA");
    setScribeAttr(EegActivity.recurrence, entity, activity, "RECURRENCE", "NONE");
    setScribeAttr(EegActivity.magnitude, entity, activity, "MAGNITUDE", "NORMAL");
    setScribeAttr(EegActivity.in_background, entity, activity, "IN_BACKGROUND", "NO");
    setScribeAttr(EegActivity.note, entity, activity, " NOTE");
    // locations are multivalued, so they are represented as a string of <location>[,<location>]
    final StringBuilder sb = new StringBuilder();
    for (EegActivity.Location location : EegActivity.Location.values()) {
      if (entity.getAttribute(location.toString()) != null) {
        sb.append(location.toString()).append(",");
      }
    }
    if (sb.length() > 0) {
      sb.deleteCharAt(sb.length() - 1);
      activity.set(EegActivity.location, sb.toString());
    }
    return activity;
  }
}
