//package edu.utdallas.hltri.eeg.annotators;
//
//import com.google.common.collect.Lists;
//import edu.utdallas.hltri.conf.Config;
//import edu.utdallas.hltri.eeg.relation.EventCoreference;
//import edu.utdallas.hltri.logging.Logger;
//import edu.utdallas.hltri.scribe.annotators.Annotator;
//import edu.utdallas.hltri.scribe.text.BaseDocument;
//import edu.utdallas.hltri.scribe.text.Document;
//import edu.utdallas.hltri.scribe.text.annotation.Event;
//import edu.utdallas.hltri.scribe.text.annotation.Token;
//import edu.utdallas.hltri.scribe.text.relation.Relation;
//import edu.utdallas.hltri.scribe.util.SimpleStopwords;
//
//import java.util.List;
//import java.util.function.BiPredicate;
//
///**
// * Created by ramon on 2/6/16.
// */
//public class EventCoreferenceAnnotator<D extends BaseDocument> implements Annotator<D> {
//  private static final Logger log = Logger.get(EventCoreferenceAnnotator.class);
//
//  private final String annset;
//  private final String tokenAnnSet;
//  private final boolean clear;
//  private List<BiPredicate<Event, Event>> constraints = null;
//  private SimpleStopwords stopwords = null;
//
//  public static <D extends BaseDocument> EventCoreferenceAnnotator<D> withEvents(final String annset) {
//    return new Builder<D>(annset).clear().build();
//  }
//
//  private  EventCoreferenceAnnotator(final Builder<D> builder) {
//    this.annset = builder.annset;
//    this.clear = builder.clear;
//    this.tokenAnnSet = builder.tokenAnnSet;
//  }
//
//  public static class Builder<D extends BaseDocument> extends Annotator.Builder<D,Builder<D>> {
//    private final String annset;
//    private String tokenAnnSet = null;
//    private boolean clear = false;
//
//    public Builder(final String annset) {
//      this.annset = annset;
//    }
//
//    public Builder<D> tokenAnnSet(final String tas) {
//      this.tokenAnnSet = tas;
//      return self();
//    }
//
//    public Builder<D> clear() {
//      clear = true;
//      return self();
//    }
//
//    @Override
//    protected Builder<D> self() {
//      return this;
//    }
//
//    @Override
//    public EventCoreferenceAnnotator<D> build() {
//      log.info("Making event coref annotator on events [{}] and with tokens [{}]", annset, tokenAnnSet);
//      if (self().tokenAnnSet == null) {
//        log.warn("EventCorefAnnotator: no token annotation set provided. Defaulting to genia...");
//        self().tokenAnnSet = "genia";
//      }
//      return new EventCoreferenceAnnotator<>(this);
//    }
//  }
//
//  private List<BiPredicate<Event, Event>> constraints() {
//    if (constraints == null) {
//      constraints = Lists.newArrayList(
//          // don't link an event to itself
//          (e1, e2) -> !e1.equals(e2),
//
//          // ignore evidential events
//          (e1, e2) -> !e1.get(Event.type).equalsIgnoreCase("evidential"),
//
//          // only link events of the same type
//          (e1, e2) -> e1.get(Event.type).equals(e2.get(Event.type)),
//
//          // must have the same polarity or one must have null polarity
//          (e1, e2) -> e1.get(Event.polarity).equals(e2.get(Event.polarity))
//              || (e1.get(Event.polarity) == null) || (e2.get(Event.polarity) == null),
//
//          // must have the same modality or one must have null modality
//          (e1, e2) -> e1.get(Event.modality).equals(e2.get(Event.modality))
//              || (e1.get(Event.modality) == null) || (e2.get(Event.modality) == null),
//
//          // must share at least one non-stopword token
//          (e1, e2) -> {
////            log.info("Comparing [{}] to [{}]", e1.asString(), e2.asString());
//            if (e1.getDocument().get(tokenAnnSet, Token.TYPE).isEmpty()) {
//              System.err.println("No " + tokenAnnSet + " tokens on doc " + e1.getDocument().get(BaseDocument.id));
//            }
//            if (stopwords == null)  {
//              final Config config = Config.loadJson("eeg.annotator.event-coref");
//              stopwords = new SimpleStopwords(config.getString("stopword-path"));
//            }
//            boolean sharesToken = false;
//            for (final Token token1 : e1.getContained(tokenAnnSet, Token.TYPE)) {
//              if (sharesToken) break;
//              if (stopwords.test(token1.asString().toLowerCase())) {
//                for (final Token token2 : e2.getContained(tokenAnnSet, Token.TYPE)) {
//                  if (token1.get(Token.Lemma).equalsIgnoreCase(token2.get(Token.Lemma))) {
//                    sharesToken = true;
////                    log.info("[{}] from [{}] matches [{}] from [{}]", token1.asString(), e1.asString(), token2.asString(), e2.asString());
//                    break;
//                  }
//                }
//              }
//            }
//            return sharesToken;
//          }
//      );
//    }
//    return constraints;
//  }
//
//  @Override
//  public <B extends D> void annotate(final Document<B> document) {
//    if (clear) {
//      document.clear(annset, EventCoreference.TYPE);
//    }
//
//    int count = 0;
//    final List<Event> events = document.get(annset, Event.TYPE);
//    for (int i = 0; i < events.size() - 1; i++) {
//      final Event event = events.get(i);
//      for (int j = i + 1; j < events.size(); j++) {
//        final Event otherEvent = events.get(j);
//        boolean candidateCoreference = true;
//        for (final BiPredicate<Event, Event> constraint : constraints()) {
//          // must satisfy every constraint
//          if (!constraint.test(event, otherEvent)) {
//            candidateCoreference = false;
//            break;
//          }
//        }
//        if (candidateCoreference) {
//          count++;
////          final Relation<EventCoreference, Event, Event> relation =
//              Relation.create(EventCoreference.TYPE, event, otherEvent, annset);
////          log.info(relation.describe());
//        }
//      }
//    }
//    log.info("Found {} coref links in doc {}", count, document.get(BaseDocument.id));
//  }
//
//  @Override
//  public void close() {
//
//  }
//}
