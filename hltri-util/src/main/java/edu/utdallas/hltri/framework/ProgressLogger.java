package edu.utdallas.hltri.framework;


import java.text.DecimalFormat;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import edu.utdallas.hltri.io.AC;
import edu.utdallas.hltri.logging.Logger;

/**
 * User: travis
 * Created with IntelliJ IDEA.
 */
public class ProgressLogger implements AC {
  private final long startTime;
  private final long max;
  private final String description;
  private final long updateInterval;
  ReentrantLock lock = new ReentrantLock();
  private long last = 0l;
  private long lastPrint = 0l;
  private final Logger log;
  private final DecimalFormat df = new DecimalFormat("#,##0.0#");

  /**
   * Gets the name of the class that called the method that called this method
   * Taken from: http://stackoverflow.com/questions/11306811/how-to-get-the-caller-class-in-java
   * @return Class name
   */
  public static String getCallerClassName() {
    StackTraceElement[] stElements = Thread.currentThread().getStackTrace();
    for (int i = 1; i < stElements.length; i++) {
      StackTraceElement ste = stElements[i];
      if (!ste.getClassName().equals(ProgressLogger.class.getName()) && ste.getClassName().indexOf("java.lang.Thread") != 0) {
        return ste.getClassName();
      }
    }
    return null;
  }

  protected ProgressLogger(String description, long max, long updateInterval, TimeUnit updateUnit) {
    this.updateInterval = updateUnit.toMillis(updateInterval);
    this.startTime = System.currentTimeMillis();
    this.max = max;
    this.description = description;
    String clazz = getCallerClassName();
    if (clazz == null) {
      log = Logger.get(ProgressLogger.class);
    } else {
      log = Logger.get(clazz);
    }
  }

  public static ProgressLogger fixedSize(String description, long max, long updateInterval, TimeUnit updateUnit) {
    return new ProgressLogger(description, max, updateInterval, updateUnit);
  }

  public static ProgressLogger indeterminateSize(String description, long updateInterval, TimeUnit updateUnit) {
    return new ProgressLogger(description, -1, updateInterval, updateUnit);
  }

  public enum Level {
    TRACE, DEBUG, INFO, WARN, ERROR;
  }

  private void updateStatus(long current, Level level, String status, Object... args) {
    lock.lock();
    try {
      final long currentTime = System.currentTimeMillis();
      if (currentTime - lastPrint > updateInterval) {
        lastPrint = System.currentTimeMillis();
        final StringBuilder sb = new StringBuilder();
        sb.append('[').append(description).append(']').append(' ').append(status);
        sb.append(String.format(" [%,d", current));
        if (max > 0) {
          sb.append(String.format(" %05.2f%%", current * 100.0 / max));
        }
        if (current > 0) {
          final long totalTime = lastPrint - startTime;
          final double avgTime = totalTime / (double) current;
//          final long totSq = totalTime * totalTime;
          sb.append("][TOT: ")
              .append(Time.formatElapsed(totalTime))
              .append("][AVG: ")
              .append(Time.formatElapsed((long) avgTime))
              .append("][RATE: ")
              .append(df.format(1000.0 / avgTime)).append("/s");
          if (max > 0 && current > 1) {
            sb.append("][ETA: ")
                .append(Time.formatElapsed((long) avgTime * (max - current)));
          }
        }
        sb.append(']');
        switch(level) {
          case TRACE:
            log.trace(sb.toString(), args);
            break;
          case DEBUG:
            log.debug(sb.toString(), args);
            break;
          case INFO:
            log.info(sb.toString(), args);
            break;
          case WARN:
            log.warn(sb.toString(), args);
            break;
          case ERROR:
            log.error(sb.toString(), args);
            break;
        }
      }
      last = current;
    } finally {
      lock.unlock();
    }
  }

  public void debug(String status, Object... args) {
    updateStatus(last, Level.DEBUG, status, args);
  }

  public void error(String status, Object... args) {
    updateStatus(last, Level.ERROR, status, args);
  }

  public void info(String status, Object... args) {
    updateStatus(last, Level.INFO, status, args);
  }

  public void trace(String status, Object... args) {
    updateStatus(last, Level.TRACE, status, args);
  }

  public void warn(String status, Object... args) {
    updateStatus(last, Level.WARN, status, args);
  }

  public void update(String status, Object... args) {
    update(last + 1, status, args);
  }

  public void update(long current, String status, Object... args) {
    updateStatus(current, Level.INFO, status, args);
  }

  public void error(long current, String status, Object... args) {
    updateStatus(current, Level.ERROR, status, args);
  }

  public void info(long current, String status, Object... args) {
    updateStatus(current, Level.INFO, status, args);
  }

  public void trace(long current, String status, Object... args) {
    updateStatus(current, Level.TRACE, status, args);
  }

  public void warn(long current, String status, Object... args) {
    updateStatus(current, Level.WARN, status, args);
  }

  public void close() {
    final String elapsed = Time.formatElapsed(System.currentTimeMillis() - startTime);
    lock.lock();
    try {
      if (last > 0) {
        if (last == max || max < 0l) {
          log.info("[{}] completed after {} iterations [{}]",
              description, last, elapsed);
        } else if (max > 0l) {
          log.warn("[{}] failed after {} iterations (of {}) [{}]", description,
              last, max, elapsed);
        } else {
          log.warn("[{}] failed after {} iterations [{}]", description,
              last, max, elapsed);
        }
      }
    } finally {
      lock.unlock();
    }
  }
}
