package edu.utdallas.hltri.scribe.mate;

import edu.utdallas.hltri.logging.Logger;
import edu.utdallas.hlt.medbase.pred.Role;
import edu.utdallas.hlt.medbase.pred.RoleSet;
import edu.utdallas.hltri.scribe.annotators.MateAnnotator;
import edu.utdallas.hltri.scribe.text.Attribute;
import edu.utdallas.hltri.scribe.text.BaseDocument;
import edu.utdallas.hltri.scribe.text.Document;
import edu.utdallas.hltri.scribe.text.annotation.*;

import java.io.Serializable;
import java.util.Optional;
import java.util.Set;

/**
* Created with IntelliJ IDEA.
* User: Ramon
* Date: 7/17/14
* Time: 4:47 PM
*/
public class Predicate extends AbstractAnnotation<Predicate> {
  private static transient final Logger log = Logger.get(Predicate.class);

  public static transient final Attribute<Predicate, RoleSet> RoleSet = Attribute.typed("roleset", RoleSet.class);
  public static transient final Attribute<Predicate, String> Name = Attribute.typed("name", String.class);
  public static transient final Attribute<Predicate, String> Source = Attribute.typed("source", String.class);
  public static transient final Attribute<Predicate, Integer> Token_id = Attribute.typed("token", Integer.class);

  private transient Token token = null;

  protected Predicate(Document parent, gate.Annotation ann) {
    super(parent, ann);
  }

  public static final AnnotationType<Predicate> TYPE = new DuplicatableAbstractAnnotationType<Predicate>("Predicate") {
    @Override public Predicate wrap(Document parent, gate.Annotation ann) {
      return new Predicate(parent, ann);
    }
  };

  public Token getToken() {
    if (token == null) {
      final Optional<Token> opt = getDocument().getAnnotationById(this.get(Token_id), Token.TYPE);
      assert (opt.isPresent()) : String.format("Null predicate head token in document %s, tokenId %s", getDocument().getLocalId(),
          get(Token_id));
      token = opt.get();
    }
    return token;
  }

  public Role getRoleForArgument(final String argNum) {
    final RoleSet roleset = this.get(Predicate.RoleSet);
    if (roleset != edu.utdallas.hlt.medbase.pred.RoleSet.NULL_ROLESET) {
      if (roleset.getRole(argNum.substring(1)) != null) {
        return roleset.getRole(argNum.substring(1));
      }
      else if ((argNum.startsWith("R-A") || argNum.startsWith("C-A")) && roleset.getRole(argNum.substring(3)) != null) {
        return roleset.getRole(argNum.substring(3));
      }
      else if (argNum.length() > 3) {
        return new Role(argNum.substring(3), getArgmName(argNum));
      }
      else {
        return new Role(argNum, argNum);
      }

    }
    //else make a new Role using argNum
    else {
      return new Role(argNum, argNum);
    }
  }

  private String getArgmName(String argm) {
    switch (argm) {
      case "AM-DIR" : return "directional";
      case "AM-LOC" : return "locative";
      case "AM-MNR" : return "manner";
      case "AM-TMP" : return "temporal";
      case "AM-EXT" : return "extent";
      case "AM-REC" : return "reciprocal";
      case "AM-PRD" : return "secondary predication";
      case "AM-PNC" : return "purpose";
      case "AM-CAU" : return "cause";
      case "AM-DIS" : return "discourse";
      case "AM-ADV" : return "adverbial";
      case "AM-MOD" : return "modal";
      case "AM-NEG" : return "negation";
      case "AM-STR" : return "stranded";
      case "R-AM-TMP" : return "temporal";
      case "R-AM-LOC" : return "locative";
      case "R-AM-MNR" : return "manner";
      default: log.warn("Unrecognized argm: {}", argm);
        MateAnnotator.addUnrecognizedArg(argm);
        return argm;
    }
  }
}