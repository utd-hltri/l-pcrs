package edu.utdallas.hltri.scribe.mate;

import edu.utdallas.hltri.scribe.text.annotation.AnnotationType;
import edu.utdallas.hltri.scribe.text.relation.RelationType;

/**
 * Created by rmm120030 on 7/14/15.
 */
public enum PredArgRelation implements RelationType<Predicate, Argument> {
  TYPE;

  @Override
  public AnnotationType<Predicate> getGovernorType() {
    return Predicate.TYPE;
  }

  @Override
  public AnnotationType<Argument> getDependantType() {
    return Argument.TYPE;
  }

  @Override
  public String getName() {
    return "Pred-Arg-Relation";
  }
}

//public class PredArgRelation extends AbstractRelation<PredArgRelation, Predicate, Argument> {
//
//  private PredArgRelation(final Document document, final String type, final Predicate gov, final Argument dep) {
//    super(document, type, gov, dep);
//  }
//
//  public static final RelationType<PredArgRelation,Predicate,Argument> TYPE = new AbstractRelationType<PredArgRelation,Predicate,Argument>("Pred-Arg-Relation") {
//    @Override
//    public PredArgRelation create(final Document document, final Predicate gov, final Argument dep) {
//      return new PredArgRelation(document, this.getName(), gov, dep);
//    }
//
//    @Override
//    public AnnotationType<Predicate> governorType() {
//      return Predicate.TYPE;
//    }
//
//    @Override
//    public AnnotationType<Argument> dependantType() {
//      return Argument.TYPE;
//    }
//  };
//
//  public Predicate getPredicate() {
//    return this.getGovernor();
//  }
//
//  public Argument getArgument() {
//    return this.getDependant();
//  }
//}