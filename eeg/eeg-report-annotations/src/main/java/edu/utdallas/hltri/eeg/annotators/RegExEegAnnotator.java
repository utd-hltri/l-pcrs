package edu.utdallas.hltri.eeg.annotators;

import com.google.common.base.CharMatcher;
import com.google.common.base.Splitter;
import com.google.common.collect.Sets;

import edu.utdallas.hltri.eeg.EegNote;
import edu.utdallas.hltri.eeg.annotation.AnatomicalSite;
import edu.utdallas.hltri.eeg.annotation.EegLocation;
import edu.utdallas.hltri.scribe.text.annotation.Section;
import edu.utdallas.hltri.logging.Logger;
import edu.utdallas.hltri.scribe.annotators.Annotator;
import edu.utdallas.hltri.scribe.text.BaseDocument;
import edu.utdallas.hltri.scribe.text.Document;
import edu.utdallas.hltri.scribe.text.DuplicateAnnotationException;
import edu.utdallas.hltri.scribe.text.Text;
import edu.utdallas.hltri.scribe.text.annotation.*;

import java.util.Set;
import java.util.function.Function;
import java.util.regex.Pattern;

/**
 * Created by rmm120030 on 9/4/15.
 */
public class RegExEegAnnotator implements Annotator<EegNote> {
  private static final Logger log = Logger.get(RegExEegAnnotator.class);
  public static final String ANNOTATION_SET_NAME = "regex-eeg";

  private final boolean annotateSections;
  private final boolean annotateEegTechnique;
  private final boolean annotateMedications;
  private final boolean annotateInterpretation;
  private final Function<Document<? extends EegNote>, ? extends Iterable<Sentence>> sentenceProvider;
  private final Function<? extends Text, ? extends Iterable<Token>> tokenProvider;
  private final Function<Document<? extends EegNote>, ? extends Iterable<Event>> eventProvider;
  private final boolean clear;
  private final Set<String> ignoredTitles;
//  private final Set<String> techniques = Sets.newHashSet("10-20 electrode placement system",
//      "additional anterior temporal", "single lead EKG");

  private RegExEegAnnotator(final Builder builder) {
    this.annotateSections = builder.annotateSections;
    this.annotateEegTechnique = builder.annotateEegTechnique;
    this.annotateMedications = builder.annotateMedications;
    this.annotateInterpretation = builder.annotateInterpretation;
    this.sentenceProvider = builder.sentenceProvider;
    this.tokenProvider = builder.tokenProvider;
    this.eventProvider = builder.eventProvider;
    this.clear = builder.clear;
    this.ignoredTitles = builder.ignoredTitles;
  }

  public static RegExEegAnnotator sectionAnnotator(final String sentenceAnnSet) {
    return new Builder().annotateSections().withSentences(
        d -> d.get(sentenceAnnSet, Sentence.TYPE)).clear().build();
  }

  public static RegExEegAnnotator impressionAnnotator(final String eventAnnSet) {
    return new Builder().annotateImpression().withEvents(d -> d.get(eventAnnSet, Event.TYPE)).clear().build();
  }

  public static class Builder extends Annotator.Builder<EegNote,Builder> {
    private boolean annotateSections = false;
    private boolean annotateEegTechnique = false;
    private boolean annotateMedications = false;
    private boolean annotateInterpretation = false;
    private Function<Document<? extends EegNote>, ? extends Iterable<Sentence>> sentenceProvider = null;
    private Function<Sentence, ? extends Iterable<Token>> tokenProvider = null;
    private Function<Document<? extends EegNote>, ? extends Iterable<Event>> eventProvider = null;
    private boolean clear = false;
    private Set<String> ignoredTitles = Sets.newHashSet("A", "I");

    @Override
    protected Builder self() {
      return this;
    }

    public Builder clear() {
      this.clear = true;
      return self();
    }

    public Builder ignoreTitles(String... titles) {
      for (String title : titles) {
        ignoredTitles.add(title);
      }
      return self();
    }

    public Builder annotateSections() {
      annotateSections = true;
      return self();
    }

    public Builder annotateEegTechnique() {
      annotateEegTechnique = true;
      return self();
    }

    public Builder annotateMedications() {
      annotateMedications = true;
      return self();
    }

    public Builder annotateImpression() {
      annotateInterpretation = true;
      return self();
    }

    public Builder withSentences(Function<Document<? extends EegNote>, ? extends Iterable<Sentence>> sentenceProvider) {
      this.sentenceProvider = sentenceProvider;
      return self();
    }

    public Builder withTokens(Function<Sentence, ? extends Iterable<Token>> tokenProvider) {
      this.tokenProvider = tokenProvider;
      return self();
    }

    public Builder withEvents(Function<Document<? extends EegNote>, ? extends Iterable<Event>> eventProvider) {
      this.eventProvider = eventProvider;
      return self();
    }

    @Override
    public RegExEegAnnotator build() {
      if (eventProvider == null && annotateInterpretation) {
        throw new RuntimeException("Interpretation annotation requires event annotations. Use Builder.withEvents()");
      }
      return new RegExEegAnnotator(self());
    }
  }

  /*
   * Annotates any occurrence of the passed event in the same manner.
   */
  public static <B extends D, D extends BaseDocument> void annotateSimilar(final Document<B> document, final Event event) {
    final String docString = document.asString().toLowerCase();
    final String eventString = event.asString().toLowerCase();
    int index = docString.indexOf(eventString);
    while (index != -1) {
      try {
        if (event.get(Event.type).equalsIgnoreCase("event")) {
          throw new RuntimeException(String.format("Bad event %s", event.describe()));
        }
        Event.TYPE.create(document, ANNOTATION_SET_NAME, index, index + eventString.length())
            .set(Event.type, event.get(Event.type))
            .set(Event.modality, event.get(Event.modality))
            .set(Event.polarity, event.get(Event.polarity));
      } catch (DuplicateAnnotationException e) {
        e.old.getFeatures().put("type", event.get(Event.type));
        e.old.getFeatures().put("modality", event.get(Event.modality));
        e.old.getFeatures().put("polarity", event.get(Event.polarity));
      }
      index = docString.indexOf(eventString, index + eventString.length());
    }
  }

  public static <B extends D, D extends BaseDocument> void annotateSimilar(final Document<B> document, final AnatomicalSite site) {
    final String docString = document.asString();
    final String siteString = site.asString();
    int index = docString.indexOf(siteString);
    while (index != -1) {
      try {
        AnatomicalSite.TYPE.create(document, ANNOTATION_SET_NAME, index, index + siteString.length());
      } catch (DuplicateAnnotationException e) {
        log.info("Found duplicate of {} in {} at {}", site.asString(), document.get(BaseDocument.id), index);
      }
      index = docString.indexOf(siteString, index + siteString.length());
    }
  }

  public static <B extends D, D extends BaseDocument> void annotateSimilar(final Document<B> document, final EegLocation loc) {
    final String docString = document.asString();
    final String locString = loc.asString();
    int index = docString.indexOf(locString);
    while (index != -1) {
      try {
        EegLocation.TYPE.create(document, ANNOTATION_SET_NAME, index, index + locString.length());
      } catch (DuplicateAnnotationException e) {
        log.info("Found duplicate of {} in {} at {}", loc.asString(), document.get(BaseDocument.id), index);
      }
      index = docString.indexOf(locString, index + locString.length());
    }
  }

  @Override
  public <B extends EegNote> void annotate(final Document<B> document) {
    if (clear) {
      document.clear(ANNOTATION_SET_NAME);
    }

    if (annotateSections) {
      // section title annotation
      // annotate the start of a sentence ending in ':' as a section title if it contains no lower case letters
      long currentOffset = 0;
      String prevSectionTitle = null, currentSectionTitle = null;
      final Splitter splitter = Splitter.on("\n");
      long start = 0;
      final StringBuilder sb = new StringBuilder();
      for (final String line : splitter.split(document.asString())) {
        start = document.asString().indexOf(line, (int) start);
        sb.setLength(0);
        boolean startNewSection = false;
        boolean isSectionTitle = false;
        for (int i = 0; i < line.length(); i++) {
          char c = line.charAt(i);
//          char d = (i > 0) ? line.charAt(i - 1) : '_';
          if (Character.isLetter(c)) {
            if (Character.isUpperCase(c)) {
              sb.append(c);
              isSectionTitle = true;
            } else {
              if (isSectionTitle) {
                int delim = sb.length();
                for (; delim > 0 && !Character.isWhitespace(sb.charAt(delim - 1)); delim--) {
                  ;
                }
                sb.setLength(delim);
                if (sb.length() > 1) {
                  startNewSection = true;
                  currentSectionTitle = sb.toString();
                }
              }
              break;
            }
          } else if (i > 0) {
            if ((Character.isWhitespace(c) || '.' == c || ',' == c) && isSectionTitle) {
              sb.append(c);
            } else if (c == ':' && isSectionTitle) {
              startNewSection = true;
              currentSectionTitle = sb.toString();
              break;
            }
          } else if (!Character.isWhitespace(c)) {
            break;
          }
        }
        if (startNewSection) {
          // if new section
            // save the last section if it exists
          if (!ignoredTitles.contains(currentSectionTitle)) {
            if (prevSectionTitle != null) {
              createSection(document, currentOffset, start - 1, prevSectionTitle);
            }
            prevSectionTitle = currentSectionTitle;
            currentOffset = start;
          }
        }
      }
      if (currentSectionTitle != null && !ignoredTitles.contains(currentSectionTitle)) {
        createSection(document, currentOffset, document.length(), currentSectionTitle);
      }
    }

    if (annotateEegTechnique) {
      for (final Section section : document.get(ANNOTATION_SET_NAME, Section.TYPE)) {
        if (section.get(Section.title).equalsIgnoreCase("introduction")) {
          if (section.asString().contains("using")) {
            long start = section.get(Annotation.StartOffset) + section.asString().indexOf("using");
            long end = document.asString().indexOf(".", (int)start);

            try {
              Event.TYPE.create(document, ANNOTATION_SET_NAME, start, end)
                  .set(Event.type, "EEG_TECHNIQUE")
                  .set(Event.modality, "FACTUAL")
                  .set(Event.polarity, "POS");
            } catch (DuplicateAnnotationException e) {
              e.old.getFeatures().put("type", "EEG_TECHNIQUE");
              e.old.getFeatures().put("modality", "FACTUAL");
              e.old.getFeatures().put("polarity", "POS");
            }
          }
        }
      }
    }

    if (annotateMedications) {
      final Splitter splitter = Splitter.on(Pattern.compile(",|\\.\\s|\\sand\\s|\\n")).omitEmptyStrings();
      for (final Section section : document.get(ANNOTATION_SET_NAME, Section.TYPE)) {
        if (section.get(Section.title).equalsIgnoreCase("MEDICATIONS")) {
          final String sectionString = section.asString();
          for (String med : splitter.split(sectionString.substring(sectionString.indexOf(':') + 1))) {
            med = med.trim();
            // sometimes strings like _________ make it in the medications list
            if (med.length() > 0 && !med.contains("___")) {
              try {
                final long start = section.get(Annotation.StartOffset) + sectionString.indexOf(med);
                Event.TYPE.create(document, ANNOTATION_SET_NAME, start, start + med.length())
                    .set(Event.type, "TREATMENT")
                    .set(Event.modality, "FACTUAL")
                    .set(Event.polarity, "POS");
              } catch (DuplicateAnnotationException e) {
                e.old.getFeatures().put("type", "TREATMENT");
                e.old.getFeatures().put("modality", "FACTUAL");
                e.old.getFeatures().put("polarity", "POS");
              }
            }
          }
        }
      }
    }

    if (annotateInterpretation) {
      document.set(EegNote.interpretation, "NORMAL");
      for (final Event event : eventProvider.apply(document)) {
        final String type = event.get(Event.type).toUpperCase();
        if (type.contains("IMPRESSION") || type.contains("INTERPRETATION") || type.contains("INTERPTRETATION")) {
          if (type.contains("INTERPTRETATION")) {
            event.set(Event.type, type.replace("INTERPTRETATION", "INTERPRETATION"));
          }
          if (event.asString().toUpperCase().contains("ABNORMAL")) {
            document.set(EegNote.interpretation, "ABNORMAL");
            break;
          }
        }
      }
    }
  }

  private void createSection(final Document<? extends EegNote> document, final long start, final long end, final String title) {
    if (start < end && !title.isEmpty()) {
      try {
        final Section sect = Section.TYPE.create(document, ANNOTATION_SET_NAME, start, end)
            .set(Section.title, title);
//        log.info(sect.describe());
      } catch (DuplicateAnnotationException e) {
//        log.warn("Doc {}: Duplicate section: ({},{}) {}", document.get(BaseDocument.id), start, end, e.old.toString());
      }
    }
  }

//  public static void fixTechniques(final String annFile) {
//    final StringBuilder sb = new StringBuilder();
//    final Splitter splitter = Splitter.on(CharMatcher.WHITESPACE).omitEmptyStrings();
//    try (BufferedReader reader = new BufferedReader(new FileReader(annFile))) {
//      String line;
//      while ((line = reader.readLine()) != null) {
//        if (line.contains("EEG_TECHNIQUE")) {
//          if (line.contains("using standard 10-20 system of electrode placement with 1 channel of ekg 27")) {
//
//          }
//        }
//        else {
//          sb.append(line).append("\n");
//        }
//      }
//    } catch (IOException e) {
//      throw new RuntimeException(e);
//    }
//    try {
//      Files.write()
//    } catch (IOException e) {
//      throw new RuntimeException(e);
//    }
//  }

  private boolean isAllCaps(final String str) {
    return CharMatcher.JAVA_LOWER_CASE.matchesNoneOf(str);
  }
}
