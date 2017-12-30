package edu.utdallas.hltri.scribe.annotators;

import edu.utdallas.hltri.scribe.text.BaseDocument;

import org.apache.ctakes.dictionary.lookup2.dictionary.JdbcRareWordDictionary;

/**
 * Created by rmm120030 on 9/28/15.
 */
public class CtakesUmlsAnnotator<D extends BaseDocument> implements Annotator<D> {
  private final JdbcRareWordDictionary dictionary;

  public CtakesUmlsAnnotator() {
    dictionary = new JdbcRareWordDictionary();
  }
}