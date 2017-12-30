/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.utdallas.hlt.trecmed.evaluation;

import edu.utdallas.hlt.trecmed.Keyword;
import edu.utdallas.hlt.trecmed.Topic;
import edu.utdallas.hlt.util.Triple;
import edu.utdallas.hltri.struct.Weighted;
import edu.utdallas.hltri.util.Expansion;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import org.apache.lucene.document.Field;
import org.apache.lucene.search.highlight.Encoder;
import org.apache.lucene.search.vectorhighlight.BaseFragmentsBuilder;
import org.apache.lucene.search.vectorhighlight.FieldFragList.WeightedFragInfo;
import org.apache.lucene.search.vectorhighlight.FieldFragList.WeightedFragInfo.SubInfo;
import org.apache.lucene.search.vectorhighlight.FieldPhraseList.WeightedPhraseInfo.Toffs;

/**
 *
 * @author travis
 */
public class ExpansionHTMLFragmentBuilder extends BaseFragmentsBuilder {

  private final Map<String, Triple<String, String, Double>> sources = new HashMap<>();

	ExpansionHTMLFragmentBuilder(final Topic topic) {
    for (final Keyword keyword : topic) {
      final Collection<Expansion<Weighted<String>>> expansions = new HashSet<>(keyword.getExpansions());
      for (Keyword subkeyword : keyword.getSubKeywords()) {
        expansions.addAll(subkeyword.getExpansions());
      }
      for (final Expansion<Weighted<String>> expansion : expansions) {
        for (final Weighted<String> term : expansion) {
          if (!sources.containsKey(term.value) || sources.get(term.value).getThird() < term.weight) {
            sources.put(term.value.toLowerCase(), Triple.of(expansion.getName(), keyword.asString(), term.weight));
          }
        }
      }
    }
	}

  @Override
  protected String makeFragment(StringBuilder buffer,
                                int[] index,
                                Field[] values,
                                WeightedFragInfo fragInfo,
                                String[] preTags,
                                String[] postTags,
                                Encoder encoder) {

    final StringBuilder fragment = new StringBuilder();
    final int s = fragInfo.getStartOffset();
    int[] modifiedStartOffset = { s };
    final String src = getFragmentSourceMSO( buffer, index, values, s, fragInfo.getEndOffset(), modifiedStartOffset);
    int srcIndex = 0;
    for( SubInfo subInfo : fragInfo.getSubInfos() ){
      for( Toffs to : subInfo.getTermsOffsets() ){
        final String originalText = encoder.encodeText(src.substring(to.getStartOffset() - modifiedStartOffset[0], to.getEndOffset() - modifiedStartOffset[0]));
        final Triple<String, String, Double> triple = sources.get(originalText.toLowerCase());
        fragment.append(encoder.encodeText(src.substring(srcIndex, to.getStartOffset() - modifiedStartOffset[0])));
        fragment.append("<span class=\"match");
        String title = "N/A";
        if (triple != null) {
          fragment.append(" ").append(triple.getFirst());
          title = triple.getFirst() + " - " + triple.getSecond();
        }
        fragment.append("\" title=\"");
        fragment.append(title);
        fragment.append("\">^^");
        fragment.append(originalText);
        fragment.append("</span>");
        srcIndex = to.getEndOffset() - modifiedStartOffset[0];
      }
    }
    fragment.append(encoder.encodeText(src.substring(srcIndex)));
    return fragment.toString();
  }

  @Override
  public List<WeightedFragInfo> getWeightedFragInfoList(List<WeightedFragInfo> src) {
    return src;
  }
}
