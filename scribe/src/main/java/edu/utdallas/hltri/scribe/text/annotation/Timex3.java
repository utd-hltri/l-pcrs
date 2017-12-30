package edu.utdallas.hltri.scribe.text.annotation;

import edu.utdallas.hltri.scribe.text.Attribute;
import edu.utdallas.hltri.scribe.text.Document;

import java.util.Calendar;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Created by ramon on 4/12/16.
 */
public class Timex3 extends AbstractAnnotation<Timex3> {
  private final static long serialVersionUID = 1L;

  public static final Attribute<Timex3, String> tid = Attribute.typed("tid", String.class);
  public static final Attribute<Timex3, String> type = Attribute.typed("type", String.class);
  public static final Attribute<Timex3, Integer> beginPoint = Attribute.typed("begin", Integer.class);
  public static final Attribute<Timex3, Integer> endPoint = Attribute.typed("end", Integer.class);
  public static final Attribute<Timex3, String> quant = Attribute.typed("quant", String.class);
  public static final Attribute<Timex3, String> freq = Attribute.typed("freq", String.class);
  public static final Attribute<Timex3, String> mod = Attribute.typed("mod", String.class);
  public static final Attribute<Timex3, String> value = Attribute.typed("value", String.class);
  public static final Attribute<Timex3, String> altvalue = Attribute.typed("alt", String.class);

  protected Timex3(Document<?> parent, gate.Annotation ann) {
    super(parent, ann);
  }

  public static final AnnotationType<Timex3> TYPE = new AbstractAnnotationType<Timex3>("Timex") {
    @Override public Timex3 wrap(Document<?> parent, gate.Annotation ann) {
      return new Timex3(parent, ann);
    }
  };

  public Optional<Calendar> getDate() {
    final String val = get(value);
    if (val == null) {
      return Optional.empty();
    }
    else {
      if (Pattern.matches("\\d\\d\\d\\d-\\d\\d-\\d\\d", val)) {
        int year = Integer.parseInt(val.substring(0, 4));
        int month = Integer.parseInt(val.substring(5, 7));
        int day = Integer.parseInt(val.substring(8, 10));
        return Optional.of(makeCalendar(year, month, day));
      } else if (Pattern.matches("\\d\\d\\d\\d\\d\\d\\d\\d", val)) {
        int year = Integer.parseInt(val.substring(0, 4));
        int month = Integer.parseInt(val.substring(4, 6));
        int day = Integer.parseInt(val.substring(6, 8));
        return Optional.of(makeCalendar(year, month, day));
      }
      throw new UnsupportedOperationException(String.format("%s is not a fully specified date", this));
    }
  }

  private static Calendar makeCalendar(int year, int month, int day) {
    Calendar date = Calendar.getInstance();
    date.clear();
    date.set(year, month - 1, day, 0, 0, 0);
    return date;
  }
}
