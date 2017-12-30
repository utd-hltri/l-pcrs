package edu.utdallas.hltri.scribe.text.annotation;

import edu.utdallas.hltri.scribe.text.Attribute;
import edu.utdallas.hltri.scribe.text.Document;

/**
 * Created by travis on 8/13/14.
 */
public class CorefMention extends AbstractAnnotation<CorefMention> {
  private static final long serialVersionUID = 1L;

  public static final Attribute<CorefMention, Integer>
      ClusterId = Attribute.typed("cluster_id", Integer.class);
  public static final Attribute<CorefMention, Integer>
      ClusterHeadId = Attribute.typed("cluster_head_id", Integer.class);

  public CorefMention(Document<?> document, gate.Annotation gateAnnotation) {
    super(document, gateAnnotation);
  }

  public CorefMention setClusterId(int clusterId) {
    features.put("cluster_id", clusterId);
    return this;
  }

  public CorefMention setHead(Token head) {
    features.put("cluster_head", head.getGateId());
    return this;
  }

  public Token getHead() {
    return getDocument().getAnnotationById(this.get(ClusterHeadId), Token.TYPE).get();
  }

  public int getClusterId() {
    return (int) features.get("cluster_id");
  }

  public static final AnnotationType<CorefMention> TYPE = AbstractAnnotationType.register("Coref_Mention", CorefMention::new);
}
