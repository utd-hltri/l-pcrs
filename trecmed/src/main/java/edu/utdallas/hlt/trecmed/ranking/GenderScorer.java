package edu.utdallas.hlt.trecmed.ranking;

import com.google.common.collect.EnumMultiset;
import com.google.common.collect.Multiset;

import edu.utdallas.hlt.text.Gender;
import edu.utdallas.hlt.trecmed.Report;
import edu.utdallas.hlt.trecmed.Topic;
import edu.utdallas.hlt.trecmed.Visit;
import edu.utdallas.hlt.trecmed.VisitScorer;
import edu.utdallas.hltri.inquire.ie.GenderExtractor;
import edu.utdallas.hltri.inquire.lucene.LuceneResult;
import edu.utdallas.hltri.logging.Logger;

/**
*
* @author travis
*/
public class GenderScorer extends VisitScorer {
  private static final Logger log = Logger.get(GenderScorer.class);

  private final Multiset<Gender.Type> counts = EnumMultiset.create(Gender.Type.class);

  private Gender.Type gender, opposite;
  private boolean skip;

  private static Gender.Type gender2Kirk(GenderExtractor.Gender gender) {
    switch(gender) {
      case MALE: return Gender.Type.MALE;
      case FEMALE: return Gender.Type.FEMALE;
      default: return Gender.Type.NEUTRAL;
    }
  }


  @Override
  public void setTopic(final Topic topic) {
    super.setTopic(topic);
    skip = !topic.hasGenderRequirement();

    if (!skip) {
      gender = gender2Kirk(topic.getGender());
      opposite = gender2Kirk(topic.getGender().getOpposite());
    }
  }

  @Override
  public void setVisit(LuceneResult<Visit> visit) {
    super.setVisit(visit);
    counts.clear();
  }

  @Override
  public void setReport(final Report report) {
    if (!skip) {
      for (Gender genderAnn : report.getDocument().getSub(Gender.class)) {
        counts.add(genderAnn.getType());
      }
    }
  }

  @Override
  public double getRank() {
    int matching = counts.count(gender);
    int unmatching = counts.count(opposite);
    if (unmatching > matching) {
      log.debug("Demoted visit {} because it had {} more {} ({}) than {} ({}) mentions.",
                visit.getValue().getId(),
                unmatching - matching,
                opposite,
                unmatching,
                gender,
                matching);
      return -50;
    }
    return 0;
  }
}
