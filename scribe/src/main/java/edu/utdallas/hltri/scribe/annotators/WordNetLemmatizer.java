package edu.utdallas.hltri.scribe.annotators;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.List;

import edu.mit.jwi.Dictionary;
import edu.mit.jwi.IDictionary;
import edu.mit.jwi.item.IIndexWord;
import edu.mit.jwi.item.POS;
import edu.mit.jwi.morph.WordnetStemmer;
import edu.utdallas.hltri.conf.Config;
import edu.utdallas.hltri.logging.Logger;

/**
 *
 * @author travis
 */
@SuppressWarnings("unused")
public class WordNetLemmatizer implements Lemmatizer, Closeable {

  private static final Logger log = Logger.get(WordNetLemmatizer.class);
  private static IDictionary    wordnet;
  private static WordnetStemmer stemmer;


  public WordNetLemmatizer() {
    if (wordnet == null) {
      final Config conf = Config.load("wordnet");
      wordnet = new Dictionary(new File(conf.getString("path")));
    }

    if (!wordnet.isOpen()) {
      try {
        wordnet.open();
      } catch (IOException ex) {
        throw new RuntimeException(ex);
      }
    }

    if (stemmer == null) {
      stemmer = new WordnetStemmer(wordnet);
    }
  }

  @Override
  public String getName() {
    return "WordNet";
  }

  @Override
  public String lemmatize(CharSequence phrase) {
    List<String> stems;
    for (POS pos : new POS[]{POS.NOUN, POS.VERB, POS.ADJECTIVE}) {
      String term = phrase.toString();
      StringBuilder prefix = new StringBuilder();
      while (true) {
        try {
          stems = stemmer.findStems(term, pos);

          for (String stem : stems) {
            IIndexWord indexWord = wordnet.getIndexWord(stem, pos);
            if (indexWord != null) {
              return prefix + indexWord.getLemma().replaceAll("_", " ");
            }
          }

          int space = term.indexOf(' ');
          if (space < 0) {
            break;
          }

          prefix.append(term.substring(0, space)).append(" ");
          term = term.substring(space + 1);
        } catch (IllegalArgumentException ex) {
          log.warn("Error lemmatizing {}: ", phrase, ex);
          return phrase.toString();
        }
      }
    }

    return phrase.toString();
  }

  @Override public void close() {
    wordnet.close();
  }
}
