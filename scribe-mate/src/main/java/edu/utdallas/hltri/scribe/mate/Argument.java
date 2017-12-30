package edu.utdallas.hltri.scribe.mate;

import edu.utdallas.hlt.medbase.pred.Role;
import edu.utdallas.hltri.logging.Logger;
import edu.utdallas.hltri.scribe.text.Attribute;
import edu.utdallas.hltri.scribe.text.BaseDocument;
import edu.utdallas.hltri.scribe.text.Document;
import edu.utdallas.hltri.scribe.text.annotation.*;

import java.util.Optional;

/**
 * Created with IntelliJ IDEA.
 * User: Ramon
 * Date: 7/17/14
 * Time: 7:17 PM
 */
public class Argument extends AbstractAnnotation<Argument> {
  private static final long serialVersionUID = 4L;
  private static final Logger log = Logger.get(Argument.class);

  public static transient final Attribute<Argument, Role> Role = Attribute.typed("name", Role.class);
  public static transient final Attribute<Argument, Integer> Token_id = Attribute.typed("head-token", Integer.class);
  private transient Token head = null;

  private Argument(Document parent, gate.Annotation ann) {
    super(parent, ann);
  }

  public static final AnnotationType<Argument> TYPE = new DuplicatableAbstractAnnotationType<Argument>("Argument") {
    @Override public Argument wrap(Document parent, gate.Annotation ann) {
      return new Argument(parent, ann);
    }
  };

  public Role getRole() {
    return this.get(Role);
  }

  public Token getHead() {
    if (head == null) {
      final Optional<Token> opt = getDocument().getAnnotationById(this.get(Token_id), Token.TYPE);
      assert (opt.isPresent()) : String.format("Null argument head token in document %s, tokenId %s", getDocument().getLocalId(),
          get(Token_id));
      head = opt.get();
    }
    return head;
  }
}
