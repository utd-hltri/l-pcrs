package edu.utdallas.hltri.scribe;

import java.util.Map;
import java.util.Set;

import edu.utdallas.hltri.Describable;
import edu.utdallas.hltri.util.SchmidtIntervalTree;


/**
 * Created by trg19 on 8/14/2016.
 */
public class AnnotationBearer {
  final Map<String, SchmidtIntervalTree<? extends Annotation>> annotations;


  String appendAnnotationList(StringBuilder sb, int spaces, char bullet) {
    for (String name : annotations.keySet() {
      for (Annotation ann : annotations.get(name).) {
        sb.append(" • ").append(name).append(':').append(uann.describe()).append('\n');
      }
    }
  }

  public String describeAnnotations() {
    final StringBuilder sb = new StringBuilder();
    sb.append("Annotations:\n");
    for (UnsafeAnnotation uann : get()) {
      sb.append(" • ").append(uann.describe()).append('\n');
    }
    for (String name : getAnnotationSets()) {
      for (UnsafeAnnotation uann : getUnsafeAnnotations(name)) {
        sb.append(" • ").append(name).append(':').append(uann.describe()).append('\n');
      }
    }
    final Set<String> relationSets = getRelationSets();
    if (!relationSets.isEmpty()) {
      sb.append(
          "Relations:\n");//.append(getRelationSets().stream().map(Map::size).reduce((s1, s2) -> s1 + s2).orElse(0)).append("):\n");
      for (String relSet : relationSets) {
        for (UnsafeRelation urel : getUnsafeRelations(relSet)) {
          sb.append(" • ").append(relSet).append(':').append(urel.describe()).append('\n');
        }
      }
    }
    return sb.toString();
  }
}
