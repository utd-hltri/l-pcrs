/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.utdallas.hlt.trecmed.analysis;

import edu.utdallas.hlt.text.NegationSpan;
import edu.utdallas.hlt.trecmed.Keyword;
import edu.utdallas.hltri.logging.Logger;

/**
 *
 * @author travis
 */
class NegationAnalyzer {
  private static final Logger log = Logger.get(NegationAnalyzer.class);
  static void analyze(Iterable<Keyword> keywords) {
    for (Keyword keyword : keywords) {
      if (keyword.getText().hasIntersecting(NegationSpan.class)) {
        keyword.setNegated();
        log.info("Negated {}", keyword);
      }
      analyze(keyword.getSubKeywords());
    }
  }
}
