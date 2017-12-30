package edu.utdallas.hlt.trecmed.analysis;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import edu.utdallas.hlt.text.Conjunction;
import edu.utdallas.hlt.text.Text;
import edu.utdallas.hlt.trecmed.Keyword;
import edu.utdallas.hlt.trecmed.Topic;
import edu.utdallas.hltri.logging.Logger;

/**
 *
 * @author travis
 */
class DisjunctionAnalyzer {

  static void filterOrGroups(Topic topic) {
    topic.getText().getDocument().annotate(Conjunction.TYPE);

    for (Conjunction conj : topic.getText().getSub(Conjunction.class)) {
      if (conj.getType() == Conjunction.Type.OR) {
        Set<Keyword> disjunctionGroup = new TreeSet<>();
        for (Text text : conj.getItems()) {
          for (Keyword keyword : topic) {
            if (text.asString().contains(keyword.asString())) {
              disjunctionGroup.add(keyword);
            }
          }
        }
        final List<Keyword> keywords = new ArrayList<>(disjunctionGroup);
        for (Keyword keyword : disjunctionGroup)  {
          keyword.setDisjunctionGroup(keywords);
        }
      }
    }
  }
}
