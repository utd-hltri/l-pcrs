package edu.utdallas.hltri.scribe.text.relation;

import edu.utdallas.hltri.scribe.text.Attribute;
import edu.utdallas.hltri.scribe.text.Document;
import edu.utdallas.hltri.scribe.text.annotation.AnnotationType;
import edu.utdallas.hltri.scribe.text.annotation.Chunk;

import java.util.List;

/**
 * Abstracts a Stanford OpenIE relation triple.
 * The governor and dependent are both stanford chunks and the third unit of the triple, the 'relation', is stored as
 * a a list of Stanford Token ids. This is done because there are cases where the phrase representing the relation is
 * disjoint and cannot be represented as a chunk.
 */
public class OpenRelation extends AbstractRelation<OpenRelation, Chunk, Chunk> {

  public static final Attribute<OpenRelation, String> relation = Attribute.typed("relation", String.class);
  public static final Attribute<OpenRelation, List<Integer>> relationTokenIds = Attribute.inferred("relation-token-ids");

  public OpenRelation(final Document<?> document, final gate.relations.Relation relation, final String annset) {
    super(document, relation, annset);
  }

  @Override
  public RelationType<OpenRelation, Chunk, Chunk> relationType() {
    return TYPE;
  }

  public static RelationType<OpenRelation, Chunk, Chunk> TYPE = new AbstractRelationType<OpenRelation, Chunk, Chunk>("OpenRelation") {
    @Override
    public AnnotationType<Chunk> governorType() {
      return Chunk.TYPE;
    }

    @Override
    public AnnotationType<Chunk> dependantType() {
      return Chunk.TYPE;
    }

    @Override
    public OpenRelation wrap(Document<?> parent, gate.relations.Relation relation, String annSet) {
      return new OpenRelation(parent, relation, annSet);
    }
  };
}
