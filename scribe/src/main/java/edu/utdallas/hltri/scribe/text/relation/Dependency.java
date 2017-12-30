package edu.utdallas.hltri.scribe.text.relation;

import edu.utdallas.hltri.scribe.text.Attribute;
import edu.utdallas.hltri.scribe.text.Document;
import edu.utdallas.hltri.scribe.text.annotation.AnnotationType;
import edu.utdallas.hltri.scribe.text.annotation.Token;

public class Dependency extends AbstractRelation<Dependency, Token, Token> {

  public static final Attribute<Dependency, String> Label = Attribute.typed("label", String.class);

  public Dependency(final Document<?> document, final gate.relations.Relation relation, final String annset) {
    super(document, relation, annset);
  }

  @Override
  public RelationType<Dependency, Token, Token> relationType() {
    return TYPE;
  }

  public static RelationType<Dependency, Token, Token> TYPE = new AbstractRelationType<Dependency, Token, Token>("Dependency") {
    @Override
    public AnnotationType<Token> governorType() {
      return Token.TYPE;
    }

    @Override
    public AnnotationType<Token> dependantType() {
      return Token.TYPE;
    }

    @Override
    public Dependency wrap(Document<?> parent, gate.relations.Relation relation, String annSet) {
      return new Dependency(parent, relation, annSet);
    }
  };
}
