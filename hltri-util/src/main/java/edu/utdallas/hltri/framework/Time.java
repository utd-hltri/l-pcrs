package edu.utdallas.hltri.framework;

import org.joda.time.Period;
import org.joda.time.format.PeriodFormatter;
import org.joda.time.format.PeriodFormatterBuilder;

/**
 * Created by travis on 6/8/15.
 */
public abstract class Time {

  public static final PeriodFormatter formatter = new PeriodFormatterBuilder()
      .appendDays().appendSuffix("d")
      .appendSeparator(" ")
      .appendHours().appendSuffix("h")
      .appendSeparator(" ")
      .appendMinutes().appendSuffix("m")
      .appendSeparator(" ")
      .appendSeconds().appendSuffix("s")
      .appendSeparator(" ")
      .printZeroAlways().appendMillis().appendSuffix("ms").toFormatter();

  private Time() {
  }

  /**
   * @return a human-readable formatted string for the given amount of nanos
   */
  public static String formatElapsedNanos(long nanos) {
    return formatElapsed(nanos / 1000000);
  }

  public static String formatElapsed(long millis) {
    return formatter.print(new Period(millis).normalizedStandard());
  }
}
