package edu.utdallas.hltri.eeg.annotation;

import com.google.common.collect.Sets;
import edu.utdallas.hltri.eeg.annotation.label.ModalityLabel;
import edu.utdallas.hltri.eeg.annotation.label.PolarityLabel;
import edu.utdallas.hltri.ml.label.EnumLabel;
import edu.utdallas.hltri.ml.label.Label;
import edu.utdallas.hltri.scribe.text.Attribute;
import edu.utdallas.hltri.scribe.text.Document;
import edu.utdallas.hltri.scribe.text.annotation.AbstractAnnotation;
import edu.utdallas.hltri.scribe.text.annotation.AbstractAnnotationType;
import edu.utdallas.hltri.scribe.text.annotation.Annotation;
import edu.utdallas.hltri.scribe.text.annotation.AnnotationType;
import edu.utdallas.hltri.scribe.text.annotation.attributes.HasModality;
import edu.utdallas.hltri.scribe.text.annotation.attributes.HasPolarity;

import java.util.HashSet;
import java.util.Set;

/**
 * Created by rmm120030 on 2/29/16.
 */
public class EegActivity extends AbstractAnnotation<EegActivity> implements HasPolarity, HasModality {
  private final static long serialVersionUID = 1L;
  public static final Attribute<EegActivity, String> morphology = Attribute.typed("morphology", String.class);
  public static final Attribute<EegActivity, String> band = Attribute.typed("band", String.class);
  public static final Attribute<EegActivity, String> hemisphere = Attribute.typed("hemisphere", String.class);
  public static final Attribute<EegActivity, String> location = Attribute.typed("location", String.class);
  public static final Attribute<EegActivity, String> dispersal = Attribute.typed("dispersal", String.class);
  public static final Attribute<EegActivity, String> recurrence = Attribute.typed("recurrence", String.class);
  public static final Attribute<EegActivity, String> magnitude = Attribute.typed("magnitude", String.class);
  public static final Attribute<EegActivity, String> in_background = Attribute.typed("in_background", String.class);
  public static final Attribute<EegActivity, String> note = Attribute.typed("note", String.class);

  protected EegActivity(Document<?> parent, gate.Annotation ann) {
    super(parent, ann);
  }

  public static final AnnotationType<EegActivity> TYPE = new AbstractAnnotationType<EegActivity>("Activity") {
    @Override public EegActivity wrap(Document<?> parent, gate.Annotation ann) {
      return new EegActivity(parent, ann);
    }
  };

  public Set<Location> getLocations() {
    final HashSet<Location> set = Sets.newHashSet();
    final String locationString = get(location);
    if (locationString != null) {
      if (locationString.contains(",")) {
        for (String loc : locationString.split(",")) {
          set.add(Location.valueOf(loc));
        }
      }
      else {
        set.add(Location.valueOf(locationString));
      }
    }
    return set;
  }

  public void addLocation(String loc) {
    String locs = get(location);
    if (locs == null) {
      set(location, loc);
    } else {
      set(location, locs + "," + loc);
    }
  }

  public static EegActivity duplicateActivity(EegActivity a, String newAnnset, Document<?> doc) {
    final EegActivity anew = EegActivity.TYPE.create(doc, newAnnset, a.get(Annotation.StartOffset), a.get(Annotation.EndOffset));
    copyIfNotNull(EegActivity.band, a, anew);
    copyIfNotNull(EegActivity.dispersal, a, anew);
    copyIfNotNull(EegActivity.recurrence, a, anew);
    copyIfNotNull(EegActivity.hemisphere, a, anew);
    copyIfNotNull(EegActivity.in_background, a, anew);
    copyIfNotNull(EegActivity.location, a, anew);
    copyIfNotNull(EegActivity.magnitude, a, anew);
    copyIfNotNull(EegActivity.morphology, a, anew);
    copyIfNotNull(EegActivity.modality, a, anew);
    copyIfNotNull(EegActivity.polarity, a, anew);
    return anew;
  }

  public static EegActivity duplicateActivity(EegActivity a, String newAnnset) {
    return duplicateActivity(a, newAnnset, a.getDocument());
  }

  private static <A extends Annotation<A>, B> void copyIfNotNull(Attribute<? super A,B> attr, A old, A new_) {
    final B val = old.get(attr);
    if (val != null) {
      new_.set(attr, val);
    }
  }

  public enum Morphology implements EnumLabel {
    RHYTHM,
    SUPPRESSION,
    SLOWING,
    BETS,
    PHOTIC_DRIVING,
    PLEDS,
    GPEDS,
    EPILEPTIFORM_DISCHARGE,
    K_COMPLEX,
    SLEEP_SPINDLE,
    SPIKE_AND_SHARP_WAVE,
    SPIKE_AND_SLOW_WAVE,
    SHARP_AND_SLOW_WAVE,
    TRIPHASIC_WAVE,
    POLYSPIKE,
    POLYSPIKE_AND_WAVE,
    VERTEX_WAVE,
    WICKET,
    SPIKE,
    SHARP_WAVE,
    SLOW_WAVE,
    LAMBDA_WAVE,
    ABNORMAL,
    AMPLITUDE_GRADIENT,
    DISORGANIZATION,
    UNSPECIFIED,
    BREACH_RHYTHM
  }

  public enum Band implements EnumLabel {
    NA,
    ALPHA,
    BETA,
    DELTA,
    THETA,
    GAMMA,
    MU
  }

  public enum Hemisphere implements EnumLabel {
    NA,
    RIGHT,
    LEFT,
    BOTH
  }

  public enum Location implements EnumLabel {
    PARIETO_OCCIPITAL,
    FRONTAL,
    OCCIPITAL,
    TEMPORAL,
    FRONTOTEMPORAL,
    FRONTOCENTRAL,
    CENTRAL,
    PARIETAL,
    CENTROPARIETAL
  }

  public enum Dispersal implements EnumLabel {
    NA,
    LOCALIZED,
    GENERALIZED
  }

  public enum Recurrence implements EnumLabel {
    NONE,
    CONTINUOUS,
    REPEATED
  }

  public enum Magnitude implements EnumLabel {
    NORMAL,
    HIGH,
    LOW
  }

  public enum In_Background implements EnumLabel {
    NO,
    YES;

    public static In_Background nullableValueOf(String s) {
      if (s.equals("NONE")) {
        return NO;
      }
      else {
        return valueOf(s);
      }
    }
  }

  public static Attribute<? super EegActivity, String> getScribeAttributeByName(String attrName) {
    switch (attrName.toUpperCase()) {
      case "MORPHOLOGY": return EegActivity.morphology;
      case "BACKGROUND": return EegActivity.in_background;
      case "DISPERSAL": return EegActivity.dispersal;
      case "FREQUENCY_BAND": return EegActivity.band;
      case "HEMISPHERE": return EegActivity.hemisphere;
      case "MAGNITUDE": return EegActivity.magnitude;
      case "MODALITY": return EegActivity.modality;
      case "POLARITY": return EegActivity.polarity;
      case "RECURRENCE": return EegActivity.recurrence;
      case "LOCATION": return EegActivity.location;
      default: throw new IllegalArgumentException("No EegActivity attribute named: " + attrName);
    }
  }

  public static Label[] getAttributeLabelByName(String attrName) {
    switch (attrName.toUpperCase()) {
      case "MORPHOLOGY": return Morphology.values();
      case "BACKGROUND": return In_Background.values();
      case "DISPERSAL": return Dispersal.values();
      case "FREQUENCY_BAND": return Band.values();
      case "HEMISPHERE": return Hemisphere.values();
      case "MAGNITUDE": return Magnitude.values();
      case "MODALITY": return ModalityLabel.values();
      case "POLARITY": return PolarityLabel.values();
      case "RECURRENCE": return Recurrence.values();
      case "LOCATION": return Location.values();
      default: throw new IllegalArgumentException("No EegActivity attribute named: " + attrName);
    }
  }
}
