package edu.utdallas.hltri.scribe.kirk;

import com.google.common.collect.Maps;
import edu.utdallas.hlt.genia_wrapper.GeniaToken;
import edu.utdallas.hlt.text.*;
import edu.utdallas.hltri.logging.Logger;
import edu.utdallas.hltri.scribe.annotators.GeniaAnnotator;
import edu.utdallas.hltri.scribe.text.Attribute;
import edu.utdallas.hltri.scribe.text.BaseDocument;
import edu.utdallas.hltri.scribe.text.Document;
import edu.utdallas.hltri.scribe.text.DuplicateAnnotationException;
import edu.utdallas.hltri.scribe.text.annotation.*;
import edu.utdallas.hltri.scribe.text.annotation.Annotation;
import edu.utdallas.hltri.scribe.text.annotation.Event;
import edu.utdallas.hltri.scribe.text.annotation.Sentence;
import edu.utdallas.hltri.scribe.text.annotation.Token;
import edu.utdallas.hltri.struct.Triple;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Created by rmm120030 on 10/1/15.
 */
public abstract class KirkDocument {
  private static final Logger log = Logger.get(KirkDocument.class);

  private KirkDocument(){}

  public static edu.utdallas.hlt.text.Document asKirk(final Document<? extends BaseDocument> document,
                                                      final String tokenAnnSet, final String sentenceAnnSet,
                                                      final String eventAnnSet) {
    return new ToKirkBuilder<>(document)
        .tokens(s -> s.getContained(tokenAnnSet, Token.TYPE))
        .sentences(d -> d.get(sentenceAnnSet, Sentence.TYPE))
        .events(d -> d.get(eventAnnSet, Event.TYPE).stream()
            .filter(e -> !e.get(Event.type).equals("PATIENT_STATE")).collect(Collectors.toList()))
        .buildWithGeniaFeatures();
  }

  public static edu.utdallas.hlt.text.Document asKirk(final Document<? extends BaseDocument> document,
                                                      final String tokenAnnSet, final String sentenceAnnSet) {
    return new ToKirkBuilder<>(document)
        .tokens(s -> s.getContained(tokenAnnSet, Token.TYPE))
        .sentences(d -> d.get(sentenceAnnSet, Sentence.TYPE))
        .buildWithGeniaFeatures();
  }

  public static class ToKirkBuilder<D extends BaseDocument> {
    private final Document<? extends D> document;
    private Function<Sentence, ? extends Iterable<Token>> tokenProvider;
    private Function<Document<? extends D>, ? extends Iterable<Sentence>> sentenceProvider;
    private Function<Document<? extends D>, ? extends Iterable<Event>> eventProvider;

    public ToKirkBuilder(final Document<? extends D> document) {
      this.document = document;
    }

    public ToKirkBuilder<D> tokens(final Function<Sentence, ? extends Iterable<Token>> fun) {
      tokenProvider = fun;
      return this;
    }

    public ToKirkBuilder<D> sentences(final Function<Document<? extends D>, ? extends Iterable<Sentence>> fun) {
      sentenceProvider = fun;
      return this;
    }

    public ToKirkBuilder<D> events(final Function<Document<? extends D>, ? extends Iterable<Event>> fun) {
      eventProvider = fun;
      return this;
    }

    public edu.utdallas.hlt.text.Document buildWithGeniaFeatures() {
      assert sentenceProvider != null : "No sentence provider. Use Builder.sentences() to provide one.";
      assert tokenProvider != null : "No token provider. Use Builder.tokens() to provide one.";

      final edu.utdallas.hlt.text.Document kirkDoc = new edu.utdallas.hlt.text.Document(document.asString());
      kirkDoc.setDocumentID(document.getId());

      //NEEDS: Token (POS, STEM, GENIA), Sentence, Dependencies, Entity, Concepts, Probpank Predicate
      // TIMEX, TLink, SLink, UMLS
      kirkDoc.addAnnotatedType(edu.utdallas.hlt.text.Token.TYPE);
      kirkDoc.addAnnotatedType(edu.utdallas.hlt.text.Token.POS_TYPE);
      kirkDoc.addAnnotatedType(edu.utdallas.hlt.text.Token.STEM_TYPE);
      kirkDoc.addAnnotatedType(edu.utdallas.hlt.text.Sentence.TYPE);
      kirkDoc.addAnnotatedType(GeniaToken.TYPE);

      assert kirkDoc.getCharLength() == document.length() :
          String.format("Different lengths, kirk: %d, doc: %d", kirkDoc.getCharLength(), document.length());

      edu.utdallas.hlt.text.Token first = null;
      edu.utdallas.hlt.text.Token last = null;
      for (Sentence sentence : sentenceProvider.apply(document)) {
        for (Token token : tokenProvider.apply(sentence)) {
          edu.utdallas.hlt.text.Token kirkToken = new edu.utdallas.hlt.text.Token(kirkDoc,
              token.get(Annotation.StartOffset).intValue(),
              token.get(Annotation.EndOffset).intValue());
          kirkToken.setPOS(token.get(Token.PoS));
          kirkToken.setStem(token.get(Token.Lemma));
          kirkDoc.addToken(kirkToken);
//        log.info("Added a token. Total tokens: {}", kirkDoc.getTokens().size());

          // add genia token
          final GeniaToken genToken = new GeniaToken(kirkToken, token.get(Token.Lemma), token.get(Token.PoS),
              token.get(GeniaAnnotator.phraseIOB), token.get(GeniaAnnotator.entityIOB));
          kirkDoc.addAnnotation(genToken);

          // save the first and last kirkToken of this sentence
          if (first == null) {
            first = kirkToken;
          }
          last = kirkToken;
        }
        // add kirk sentence
        edu.utdallas.hlt.text.Sentence ksent = new edu.utdallas.hlt.text.Sentence(Text.create(first, last));
//      log.info("Adding sentence: {}", ksent);
        kirkDoc.addAnnotation(ksent);
        first = null;
      }

      if (eventProvider != null) {
        final List<edu.utdallas.hlt.text.Token> ktokens = kirkDoc.getTokens();
//    log.info("{} tokens in doc", document.get("genia", Token.TYPE).size());
//    log.info("{} tokens in kirk doc", ktokens.size());
        int eventCount = 1;
        for (final Event event : eventProvider.apply(document)) {
          // kirk events are created from kirk text objects
          // kirk texts are created from kirk tokens
          // so find the kirk token representation of this event
          final edu.utdallas.hlt.text.Token start = kirkDoc.findToken(event.get(Annotation.StartOffset).intValue(),
              edu.utdallas.hlt.text.Document.TokenSearch.RIGHT);
          edu.utdallas.hlt.text.Token end = null;
          for (int i = ktokens.indexOf(start); i < ktokens.size(); i++) {
            final edu.utdallas.hlt.text.Token kt = ktokens.get(i);
            if (kt.getEndCharOffset() >= event.get(Annotation.EndOffset).intValue()) {
              end = kt;
              break;
            }
          }
          assert end != null : String.format("Could not find end event %s in %s from %s", event.describe(), ktokens, kirkDoc.asRawString());
          final edu.utdallas.hlt.text.Text ktext = (start == end) ? edu.utdallas.hlt.text.Text.create(start)
              : edu.utdallas.hlt.text.Text.create(start, end);
          final edu.utdallas.hlt.text.Event kevent = new edu.utdallas.hlt.text.Event(ktext, "" + eventCount++);
          kevent.setProvider("i2b2_gold");
          kevent.addInstance("ei" + event.getId(), event.get(Event.polarity), "NONE", "NONE", "NONE", event.get(Event.modality));
          kevent.setClassType(event.get(Event.type));
          kirkDoc.addAnnotation(kevent);
        }
        kirkDoc.addAnnotatedType(edu.utdallas.hlt.text.Event.TYPE);
      }

      return kirkDoc;
    }

    public edu.utdallas.hlt.text.Document build() {
      final edu.utdallas.hlt.text.Document kdoc = new edu.utdallas.hlt.text.Document(document.asString());
      kdoc.setDocumentID(document.get(BaseDocument.id));
      edu.utdallas.hlt.text.Token first = null;
      edu.utdallas.hlt.text.Token last = null;
      if (sentenceProvider != null) {
        for (Sentence sentence : sentenceProvider.apply(document)) {
          if (tokenProvider != null) {
            for (Token token : tokenProvider.apply(sentence)) {
              // create the kirk token
              edu.utdallas.hlt.text.Token kirkToken = new edu.utdallas.hlt.text.Token(kdoc,
                  token.get(Annotation.StartOffset).intValue(),
                  token.get(Annotation.EndOffset).intValue());
              kirkToken.setPOS(token.get(Token.PoS));
              kirkToken.setStem(token.get(Token.Lemma));
              kdoc.addToken(kirkToken);

              // save the first and last kirkToken of this sentence to create a kirkSentence
              if (first == null) {
                first = kirkToken;
              }
              last = kirkToken;
            }
          }
          // create and add kirk sentence
          kdoc.addAnnotation(new edu.utdallas.hlt.text.Sentence(Text.create(first, last)));
          first = null;
        }
        if (tokenProvider != null) {
          kdoc.addAnnotatedType(edu.utdallas.hlt.text.Token.TYPE);
        }
        kdoc.addAnnotatedType(edu.utdallas.hlt.text.Sentence.TYPE);
      }

      return kdoc;
    }
  }

  public static class FromKirkBuilder<D extends BaseDocument> {
    private String tokenAnnSet = null;
    private String sentenceAnnSet = null;
    private String eventAnnSet = null;
    private Consumer<Triple<String, Annotation<?>, Annotation<?>>> relationHandler;
    private boolean temporalEvents = false;
    private boolean normalEvents = false;

    public FromKirkBuilder<D> tokens(String tokenAnnSet) {
      this.tokenAnnSet = tokenAnnSet;
      return this;
    }

    public FromKirkBuilder<D> sentences(String sentenceAnnSet) {
      this.sentenceAnnSet = sentenceAnnSet;
      return this;
    }

    public FromKirkBuilder<D> events(String eventAnnSet) {
      assert !temporalEvents : "can not do Events and TemporalEvents at the same time";
      this.eventAnnSet = eventAnnSet;
      this.normalEvents = true;
      return this;
    }

    public FromKirkBuilder<D> temporalEvents(String eventAnnSet) {
      assert !normalEvents : "can not do Events and TemporalEvents at the same time";
      this.eventAnnSet = eventAnnSet;
      this.temporalEvents = true;
      return this;
    }

    public FromKirkBuilder<D> relationHandler(Consumer<Triple<String, Annotation<?>, Annotation<?>>> relationHandler) {
      this.relationHandler = relationHandler;
      return this;
    }

    public Document<D> build(final edu.utdallas.hlt.text.Document kdoc) {
      final Document<D> doc = Document.fromString(kdoc.asRawString());
      doc.set(BaseDocument.id, kdoc.getDocumentID());
      // tokens
      if ("genia".equals(tokenAnnSet)) {
        kdoc.getAnnotations(GeniaToken.class).forEach(gentok -> {
          final Token token = Token.TYPE.create(doc, tokenAnnSet, gentok.getStartCharOffset(), gentok.getEndCharOffset());
          set(token, Token.PoS, gentok.getPOS());
          set(token, Token.Lemma, gentok.getStem());
          set(token, GeniaAnnotator.entityIOB, gentok.getEntity());
          set(token, GeniaAnnotator.phraseIOB, gentok.getPhraseChunk());
        });
      }
      else if (tokenAnnSet != null) {
        kdoc.getTokens().forEach(ktok -> {
          final Token token = Token.TYPE.create(doc, tokenAnnSet, ktok.getStartCharOffset(), ktok.getEndCharOffset());
          set(token, Token.PoS, ktok.getPOS());
          set(token, Token.Lemma, ktok.getStem());
        });
      }

      // sentences
      if (sentenceAnnSet != null) {
        kdoc.getAnnotations(edu.utdallas.hlt.text.Sentence.class).forEach(ksent ->
            Sentence.TYPE.create(doc, sentenceAnnSet, ksent.getStartCharOffset(), ksent.getEndCharOffset()));
      }

      // events
      if (eventAnnSet != null) {
        if (temporalEvents) {
          Map<String, TemporalEvent> eventMap = Maps.newHashMap();
          Map<String, Timex3> timexMap = Maps.newHashMap();
          kdoc.getAnnotations(edu.utdallas.hlt.text.Event.class).forEach(kevent -> {
            try {
              final TemporalEvent event = TemporalEvent.TYPE.create(doc, eventAnnSet, kevent.getStartCharOffset(), kevent.getEndCharOffset());
              set(event, TemporalEvent.type, kevent.getClassType());
              if (kevent.getInstances().size() > 0) {
                set(event, TemporalEvent.modality, kevent.getInstances().get(0).getModality());
                set(event, TemporalEvent.polarity, kevent.getInstances().get(0).getPolarity());
                set(event, TemporalEvent.aspect, kevent.getInstances().get(0).getAspect());
                set(event, TemporalEvent.docTimeRel, kevent.getInstances().get(0).getDocTimeRel());
                set(event, TemporalEvent.id, kevent.getInstances().get(0).getID());
              }
              eventMap.put(kevent.getID(), event);
            } catch (DuplicateAnnotationException e) {
              log.warn("Duplicate temporal event annotation at ([], {})", e.old.getStartNode().getOffset(),
                  e.old.getEndNode().getOffset());
            }
          });
          kdoc.getAnnotations(Timex.class).forEach(ktime -> {
            try {
              final Timex3 timex = Timex3.TYPE.create(doc, eventAnnSet, ktime.getStartCharOffset(), ktime.getEndCharOffset());
              set(timex, Timex3.type, ktime.getType());
//              set(timex, Timex3.beginPoint, Integer.parseInt(ktime.getBeginPoint()));
//              set(timex, Timex3.endPoint, Integer.parseInt(ktime.getEndPoint()));
              set(timex, Timex3.mod, ktime.getMod());
              set(timex, Timex3.value, ktime.getValue());
              timexMap.put(ktime.getID(), timex);
            } catch (DuplicateAnnotationException e) {
              log.warn("Duplicate timex annotation at ({}, {})", e.old.getStartNode().getOffset(),
                  e.old.getEndNode().getOffset());
            }
          });

          kdoc.getAnnotations(TLink.class).forEach(ktl -> {
            final Text kgov = ktl.getFirstArgument();
            final Text kdep = ktl.getSecondArgument();
            if (kgov instanceof edu.utdallas.hlt.text.Event) {
              if (kdep instanceof edu.utdallas.hlt.text.Event) {
                relationHandler.accept(Triple.of(ktl.getType(), eventMap.get(((edu.utdallas.hlt.text.Event) kgov).getID()),
                    eventMap.get(((edu.utdallas.hlt.text.Event) kdep).getID())));
              }
              else if (kdep instanceof Timex) {
                relationHandler.accept(Triple.of(ktl.getType(), eventMap.get(((edu.utdallas.hlt.text.Event) kgov).getID()),
                    timexMap.get(((Timex) kdep).getID())));
              }
            }
            else if (kgov instanceof Timex) {
              if (kdep instanceof edu.utdallas.hlt.text.Event) {
                relationHandler.accept(Triple.of(ktl.getType(), timexMap.get(((Timex) kgov).getID()),
                    eventMap.get(((edu.utdallas.hlt.text.Event) kdep).getID())));
              }
              else if (kdep instanceof Timex) {
                relationHandler.accept(Triple.of(ktl.getType(), timexMap.get(((Timex) kgov).getID()),
                    timexMap.get(((Timex) kdep).getID())));
              }
            }
          });
        }
        else {
          kdoc.getAnnotations(edu.utdallas.hlt.text.Event.class).forEach(kevent -> {
            final Event event = Event.TYPE.create(doc, eventAnnSet, kevent.getStartCharOffset(), kevent.getEndCharOffset());
            set(event, Event.type, kevent.getClassType());
            if (kevent.getInstances().size() > 0) {
              set(event, Event.modality, kevent.getInstances().get(0).getModality());
              set(event, Event.polarity, kevent.getInstances().get(0).getPolarity());
            }
          });
        }
      }
      return doc;
    }
  }

  private static <A extends Annotation<A>, V> void set(A ann, Attribute<A, V> attr, V val) {
    if (val != null) {
      ann.set(attr, val);
    }
  }
}
