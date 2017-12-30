package edu.utdallas.hltri.framework;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;

/**
 * User: bryan
 * Date: 5/20/13
 * Time: 2:59 PM
 * Created with IntelliJ IDEA.
 */
@Deprecated
public class ProgressReporter {
  private long lastPrint = 0;
  private PrintStream output;
  private long updateInterval;
  private TimeUnit updateUnit;

  public ProgressReporter(PrintStream output, long updateInterval, TimeUnit updateUnit) {
    this.output = output;
    this.updateInterval = updateInterval;
    this.updateUnit = updateUnit;
  }

  public void updateStatus(double current, double max, String status) {
    if (System.currentTimeMillis() - lastPrint > updateUnit.toMillis(updateInterval)) {
      double percent = current / max;
      output.printf("Progress: %.1f%% %s\n", 100*percent, status);
      lastPrint = System.currentTimeMillis();
    }
  }

  static Map<Object, ProgressReporter> reporters = new HashMap<>();
  public static ProgressReporter get(Object obj) {
    if (reporters.containsKey(obj)) { return reporters.get(obj); }
    ProgressReporter reporter = new ProgressReporter(System.err, 1, TimeUnit.SECONDS);
    reporters.put(obj, reporter);
    return reporter;
  }
}
