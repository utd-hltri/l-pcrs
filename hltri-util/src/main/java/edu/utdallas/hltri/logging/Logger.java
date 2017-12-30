package edu.utdallas.hltri.logging;

import org.slf4j.Marker;

import java.io.Serializable;

/**
 *
 * @author travis
 */
public class Logger implements org.slf4j.Logger, Serializable {

  private final String name;

  private final org.slf4j.Logger self;

  Logger(org.slf4j.Logger logger) {
    this.name = logger.getName();
    this.self = logger;
  }

  public String getName() {
    return this.name;
  }

  /**
   * Is the logger instance enabled for the TRACE level?
   *
   * @return True if this Logger is enabled for the TRACE level,
   *         false otherwise.
   * @since 1.4
   */
  @Override public boolean isTraceEnabled() {
    return self.isTraceEnabled();
  }

  /**
   * Log a message at the TRACE level.
   *
   * @param msg the message string to be logged
   * @since 1.4
   */
  @Override public void trace(String msg) {
    self.trace(msg);
  }

  /**
   * Log a message at the TRACE level according to the specified format
   * and argument.
   * <p/>
   * <p>This form avoids superfluous object creation when the logger
   * is disabled for the TRACE level. </p>
   *
   * @param format the format string
   * @param arg    the argument
   * @since 1.4
   */
  @Override public void trace(String format, Object arg) {
    self.trace(format, arg);
  }

  /**
   * Log a message at the TRACE level according to the specified format
   * and arguments.
   * <p/>
   * <p>This form avoids superfluous object creation when the logger
   * is disabled for the TRACE level. </p>
   *
   * @param format the format string
   * @param arg1   the first argument
   * @param arg2   the second argument
   * @since 1.4
   */
  @Override public void trace(String format, Object arg1, Object arg2) {
    self.trace(format, arg1, arg2);
  }

  /**
   * Log a message at the TRACE level according to the specified format
   * and arguments.
   * <p/>
   * <p>This form avoids superfluous string concatenation when the logger
   * is disabled for the TRACE level. However, this variant incurs the hidden
   * (and relatively small) cost of creating an <code>Object[]</code> before invoking the method,
   * even if this logger is disabled for TRACE. The variants taking {@link #trace(String, Object) one} and
   * {@link #trace(String, Object, Object) two} arguments exist solely in order to avoid this hidden cost.</p>
   *
   * @param format    the format string
   * @param arguments a list of 3 or more arguments
   * @since 1.4
   */
  @Override public void trace(String format, Object... arguments) {
    self.trace(format, arguments);
  }

  /**
   * Log an exception (throwable) at the TRACE level with an
   * accompanying message.
   *
   * @param msg the message accompanying the exception
   * @param t   the exception (throwable) to log
   * @since 1.4
   */
  @Override public void trace(String msg, Throwable t) {
    self.trace(msg, t);
  }

  /**
   * Similar to {@link #isTraceEnabled()} method except that the
   * marker data is also taken into account.
   *
   * @param marker The marker data to take into consideration
   * @return True if this Logger is enabled for the TRACE level,
   *         false otherwise.
   *
   * @since 1.4
   */
  @Override public boolean isTraceEnabled(Marker marker) {
    return self.isTraceEnabled(marker);
  }

  /**
   * Log a message with the specific Marker at the TRACE level.
   *
   * @param marker the marker data specific to this log statement
   * @param msg    the message string to be logged
   * @since 1.4
   */
  @Override public void trace(Marker marker, String msg) {
    self.trace(marker, msg);
  }

  /**
   * This method is similar to {@link #trace(String, Object)} method except that the
   * marker data is also taken into consideration.
   *
   * @param marker the marker data specific to this log statement
   * @param format the format string
   * @param arg    the argument
   * @since 1.4
   */
  @Override public void trace(Marker marker, String format, Object arg) {
    self.trace(marker, format, arg);
  }

  /**
   * This method is similar to {@link #trace(String, Object, Object)}
   * method except that the marker data is also taken into
   * consideration.
   *
   * @param marker the marker data specific to this log statement
   * @param format the format string
   * @param arg1   the first argument
   * @param arg2   the second argument
   * @since 1.4
   */
  @Override public void trace(Marker marker, String format, Object arg1, Object arg2) {
    self.trace(marker, format, arg1, arg2);
  }

  /**
   * This method is similar to {@link #trace(String, Object...)}
   * method except that the marker data is also taken into
   * consideration.
   *
   * @param marker   the marker data specific to this log statement
   * @param format   the format string
   * @param argArray an array of arguments
   * @since 1.4
   */
  @Override public void trace(Marker marker, String format, Object... argArray) {
    self.trace(marker, format, argArray);
  }

  /**
   * This method is similar to {@link #trace(String, Throwable)} method except that the
   * marker data is also taken into consideration.
   *
   * @param marker the marker data specific to this log statement
   * @param msg    the message accompanying the exception
   * @param t      the exception (throwable) to log
   * @since 1.4
   */
  @Override public void trace(Marker marker, String msg, Throwable t) {
    self.trace(marker, msg, t);
  }

  /**
   * Is the logger instance enabled for the DEBUG level?
   *
   * @return True if this Logger is enabled for the DEBUG level,
   *         false otherwise.
   */
  @Override public boolean isDebugEnabled() {
    return self.isDebugEnabled();
  }

  /**
   * Log a message at the DEBUG level.
   *
   * @param msg the message string to be logged
   */
  @Override public void debug(String msg) {
    self.debug(msg);
  }

  /**
   * Log a message at the DEBUG level according to the specified format
   * and argument.
   * <p/>
   * <p>This form avoids superfluous object creation when the logger
   * is disabled for the DEBUG level. </p>
   *  @param format the format string
   * @param arg    the argument
   */
  @Override public void debug(String format, Object arg) {
    self.debug(format, arg);
  }

  /**
   * Log a message at the DEBUG level according to the specified format
   * and arguments.
   * <p/>
   * <p>This form avoids superfluous object creation when the logger
   * is disabled for the DEBUG level. </p>
   *  @param format the format string
   * @param arg1   the first argument
   * @param arg2   the second argument
   */
  @Override public void debug(String format, Object arg1, Object arg2) {
    self.debug(format, arg1, arg2);
  }

  /**
   * Log a message at the DEBUG level according to the specified format
   * and arguments.
   * <p/>
   * <p>This form avoids superfluous string concatenation when the logger
   * is disabled for the DEBUG level. However, this variant incurs the hidden
   * (and relatively small) cost of creating an <code>Object[]</code> before invoking the method,
   * even if this logger is disabled for DEBUG. The variants taking
   * {@link #debug(String, Object) one} and {@link #debug(String, Object, Object) two}
   * arguments exist solely in order to avoid this hidden cost.</p>
   *  @param format    the format string
   * @param arguments a list of 3 or more arguments
   */
  @Override public void debug(String format, Object... arguments) {
    self.debug(format, arguments);
  }

  /**
   * Log an exception (throwable) at the DEBUG level with an
   * accompanying message.
   *  @param msg the message accompanying the exception
   * @param t   the exception (throwable) to log
   */
  @Override public void debug(String msg, Throwable t) {
    self.debug(msg, t);
  }

  /**
   * Similar to {@link #isDebugEnabled()} method except that the
   * marker data is also taken into account.
   *
   * @param marker The marker data to take into consideration
   * @return True if this Logger is enabled for the DEBUG level,
   *         false otherwise.
   */
  @Override public boolean isDebugEnabled(Marker marker) {
    return self.isDebugEnabled(marker);
  }

  /**
   * Log a message with the specific Marker at the DEBUG level.
   *  @param marker the marker data specific to this log statement
   * @param msg    the message string to be logged
   */
  @Override public void debug(Marker marker, String msg) {
    self.debug(marker, msg);
  }

  /**
   * This method is similar to {@link #debug(String, Object)} method except that the
   * marker data is also taken into consideration.
   *  @param marker the marker data specific to this log statement
   * @param format the format string
   * @param arg    the argument
   */
  @Override public void debug(Marker marker, String format, Object arg) {
    self.debug(marker, format, arg);
  }

  /**
   * This method is similar to {@link #debug(String, Object, Object)}
   * method except that the marker data is also taken into
   * consideration.
   *  @param marker the marker data specific to this log statement
   * @param format the format string
   * @param arg1   the first argument
   * @param arg2   the second argument
   */
  @Override public void debug(Marker marker, String format, Object arg1, Object arg2) {
    self.debug(marker, format, arg1, arg2);
  }

  /**
   * This method is similar to {@link #debug(String, Object...)}
   * method except that the marker data is also taken into
   * consideration.
   *  @param marker    the marker data specific to this log statement
   * @param format    the format string
   * @param arguments a list of 3 or more arguments
   */
  @Override public void debug(Marker marker, String format, Object... arguments) {
    self.debug(marker, format, arguments);
  }

  /**
   * This method is similar to {@link #debug(String, Throwable)} method except that the
   * marker data is also taken into consideration.
   *  @param marker the marker data specific to this log statement
   * @param msg    the message accompanying the exception
   * @param t      the exception (throwable) to log
   */
  @Override public void debug(Marker marker, String msg, Throwable t) {
    self.debug(marker, msg, t);
  }

  /**
   * Is the logger instance enabled for the INFO level?
   *
   * @return True if this Logger is enabled for the INFO level,
   *         false otherwise.
   */
  @Override public boolean isInfoEnabled() {
    return self.isInfoEnabled();
  }

  /**
   * Log a message at the INFO level.
   *
   * @param msg the message string to be logged
   */
  @Override public void info(String msg) {
    self.info(msg);
  }

  /**
   * Log a message at the INFO level according to the specified format
   * and argument.
   * <p/>
   * <p>This form avoids superfluous object creation when the logger
   * is disabled for the INFO level. </p>
   *  @param format the format string
   * @param arg    the argument
   */
  @Override public void info(String format, Object arg) {
    self.info(format, arg);
  }

  /**
   * Log a message at the INFO level according to the specified format
   * and arguments.
   * <p/>
   * <p>This form avoids superfluous object creation when the logger
   * is disabled for the INFO level. </p>
   *  @param format the format string
   * @param arg1   the first argument
   * @param arg2   the second argument
   */
  @Override public void info(String format, Object arg1, Object arg2) {
    self.info(format, arg1, arg2);
  }

  /**
   * Log a message at the INFO level according to the specified format
   * and arguments.
   * <p/>
   * <p>This form avoids superfluous string concatenation when the logger
   * is disabled for the INFO level. However, this variant incurs the hidden
   * (and relatively small) cost of creating an <code>Object[]</code> before invoking the method,
   * even if this logger is disabled for INFO. The variants taking
   * {@link #info(String, Object) one} and {@link #info(String, Object, Object) two}
   * arguments exist solely in order to avoid this hidden cost.</p>
   *  @param format    the format string
   * @param arguments a list of 3 or more arguments
   */
  @Override public void info(String format, Object... arguments) {
    self.info(format, arguments);
  }

  /**
   * Log an exception (throwable) at the INFO level with an
   * accompanying message.
   *  @param msg the message accompanying the exception
   * @param t   the exception (throwable) to log
   */
  @Override public void info(String msg, Throwable t) {
    self.info(msg, t);
  }

  /**
   * Similar to {@link #isInfoEnabled()} method except that the marker
   * data is also taken into consideration.
   *
   * @param marker The marker data to take into consideration
   * @return true if this logger is warn enabled, false otherwise
   */
  @Override public boolean isInfoEnabled(Marker marker) {
    return self.isInfoEnabled(marker);
  }

  /**
   * Log a message with the specific Marker at the INFO level.
   *  @param marker The marker specific to this log statement
   * @param msg    the message string to be logged
   */
  @Override public void info(Marker marker, String msg) {
    self.info(marker, msg);
  }

  /**
   * This method is similar to {@link #info(String, Object)} method except that the
   * marker data is also taken into consideration.
   *  @param marker the marker data specific to this log statement
   * @param format the format string
   * @param arg    the argument
   */
  @Override public void info(Marker marker, String format, Object arg) {
    self.info(marker, format, arg);
  }

  /**
   * This method is similar to {@link #info(String, Object, Object)}
   * method except that the marker data is also taken into
   * consideration.
   *  @param marker the marker data specific to this log statement
   * @param format the format string
   * @param arg1   the first argument
   * @param arg2   the second argument
   */
  @Override public void info(Marker marker, String format, Object arg1, Object arg2) {
    self.info(marker, format, arg1, arg2);
  }

  /**
   * This method is similar to {@link #info(String, Object...)}
   * method except that the marker data is also taken into
   * consideration.
   *  @param marker    the marker data specific to this log statement
   * @param format    the format string
   * @param arguments a list of 3 or more arguments
   */
  @Override public void info(Marker marker, String format, Object... arguments) {
    self.info(marker, format, arguments);
  }

  /**
   * This method is similar to {@link #info(String, Throwable)} method
   * except that the marker data is also taken into consideration.
   *  @param marker the marker data for this log statement
   * @param msg    the message accompanying the exception
   * @param t      the exception (throwable) to log
   */
  @Override public void info(Marker marker, String msg, Throwable t) {
    self.info(marker, msg, t);
  }

  /**
   * Is the logger instance enabled for the WARN level?
   *
   * @return True if this Logger is enabled for the WARN level,
   *         false otherwise.
   */
  @Override public boolean isWarnEnabled() {
    return self.isWarnEnabled();
  }

  /**
   * Log a message at the WARN level.
   *
   * @param msg the message string to be logged
   */
  @Override public void warn(String msg) {
    self.warn(msg);
  }

  /**
   * Log a message at the WARN level according to the specified format
   * and argument.
   * <p/>
   * <p>This form avoids superfluous object creation when the logger
   * is disabled for the WARN level. </p>
   *  @param format the format string
   * @param arg    the argument
   */
  @Override public void warn(String format, Object arg) {
    self.warn(format, arg);
  }

  /**
   * Log a message at the WARN level according to the specified format
   * and arguments.
   * <p/>
   * <p>This form avoids superfluous string concatenation when the logger
   * is disabled for the WARN level. However, this variant incurs the hidden
   * (and relatively small) cost of creating an <code>Object[]</code> before invoking the method,
   * even if this logger is disabled for WARN. The variants taking
   * {@link #warn(String, Object) one} and {@link #warn(String, Object, Object) two}
   * arguments exist solely in order to avoid this hidden cost.</p>
   *  @param format    the format string
   * @param arguments a list of 3 or more arguments
   */
  @Override public void warn(String format, Object... arguments) {
    self.warn(format, arguments);
  }

  /**
   * Log a message at the WARN level according to the specified format
   * and arguments.
   * <p/>
   * <p>This form avoids superfluous object creation when the logger
   * is disabled for the WARN level. </p>
   *  @param format the format string
   * @param arg1   the first argument
   * @param arg2   the second argument
   */
  @Override public void warn(String format, Object arg1, Object arg2) {
    self.warn(format, arg1, arg2);
  }

  /**
   * Log an exception (throwable) at the WARN level with an
   * accompanying message.
   *  @param msg the message accompanying the exception
   * @param t   the exception (throwable) to log
   */
  @Override public void warn(String msg, Throwable t) {
    self.warn(msg, t);
  }

  /**
   * Similar to {@link #isWarnEnabled()} method except that the marker
   * data is also taken into consideration.
   *
   * @param marker The marker data to take into consideration
   * @return True if this Logger is enabled for the WARN level,
   *         false otherwise.
   */
  @Override public boolean isWarnEnabled(Marker marker) {
    return self.isWarnEnabled(marker);
  }

  /**
   * Log a message with the specific Marker at the WARN level.
   *  @param marker The marker specific to this log statement
   * @param msg    the message string to be logged
   */
  @Override public void warn(Marker marker, String msg) {
    self.warn(marker, msg);
  }

  /**
   * This method is similar to {@link #warn(String, Object)} method except that the
   * marker data is also taken into consideration.
   *  @param marker the marker data specific to this log statement
   * @param format the format string
   * @param arg    the argument
   */
  @Override public void warn(Marker marker, String format, Object arg) {
    self.warn(marker, format, arg);
  }

  /**
   * This method is similar to {@link #warn(String, Object, Object)}
   * method except that the marker data is also taken into
   * consideration.
   *  @param marker the marker data specific to this log statement
   * @param format the format string
   * @param arg1   the first argument
   * @param arg2   the second argument
   */
  @Override public void warn(Marker marker, String format, Object arg1, Object arg2) {
    self.warn(marker, format, arg1, arg2);
  }

  /**
   * This method is similar to {@link #warn(String, Object...)}
   * method except that the marker data is also taken into
   * consideration.
   *  @param marker    the marker data specific to this log statement
   * @param format    the format string
   * @param arguments a list of 3 or more arguments
   */
  @Override public void warn(Marker marker, String format, Object... arguments) {
    self.warn(marker, format, arguments);
  }

  /**
   * This method is similar to {@link #warn(String, Throwable)} method
   * except that the marker data is also taken into consideration.
   *  @param marker the marker data for this log statement
   * @param msg    the message accompanying the exception
   * @param t      the exception (throwable) to log
   */
  @Override public void warn(Marker marker, String msg, Throwable t) {
    self.warn(marker, msg, t);
  }

  /**
   * Is the logger instance enabled for the ERROR level?
   *
   * @return True if this Logger is enabled for the ERROR level,
   *         false otherwise.
   */
  @Override public boolean isErrorEnabled() {
    return self.isErrorEnabled();
  }

  /**
   * Log a message at the ERROR level.
   *
   * @param msg the message string to be logged
   */
  @Override public void error(String msg) {
    self.error(msg);
  }

  /**
   * Log a message at the ERROR level according to the specified format
   * and argument.
   * <p/>
   * <p>This form avoids superfluous object creation when the logger
   * is disabled for the ERROR level. </p>
   *  @param format the format string
   * @param arg    the argument
   */
  @Override public void error(String format, Object arg) {
    self.error(format, arg);
  }

  /**
   * Log a message at the ERROR level according to the specified format
   * and arguments.
   * <p/>
   * <p>This form avoids superfluous object creation when the logger
   * is disabled for the ERROR level. </p>
   *  @param format the format string
   * @param arg1   the first argument
   * @param arg2   the second argument
   */
  @Override public void error(String format, Object arg1, Object arg2) {
    self.error(format, arg1, arg2);
  }

  /**
   * Log a message at the ERROR level according to the specified format
   * and arguments.
   * <p/>
   * <p>This form avoids superfluous string concatenation when the logger
   * is disabled for the ERROR level. However, this variant incurs the hidden
   * (and relatively small) cost of creating an <code>Object[]</code> before invoking the method,
   * even if this logger is disabled for ERROR. The variants taking
   * {@link #error(String, Object) one} and {@link #error(String, Object, Object) two}
   * arguments exist solely in order to avoid this hidden cost.</p>
   *  @param format    the format string
   * @param arguments a list of 3 or more arguments
   */
  @Override public void error(String format, Object... arguments) {
    self.error(format, arguments);
  }

  /**
   * Log an exception (throwable) at the ERROR level with an
   * accompanying message.
   *  @param msg the message accompanying the exception
   * @param t   the exception (throwable) to log
   */
  @Override public void error(String msg, Throwable t) {
    self.error(msg, t);
  }

  /**
   * Similar to {@link #isErrorEnabled()} method except that the
   * marker data is also taken into consideration.
   *
   * @param marker The marker data to take into consideration
   * @return True if this Logger is enabled for the ERROR level,
   *         false otherwise.
   */
  @Override public boolean isErrorEnabled(Marker marker) {
    return self.isErrorEnabled(marker);
  }

  /**
   * Log a message with the specific Marker at the ERROR level.
   *  @param marker The marker specific to this log statement
   * @param msg    the message string to be logged
   */
  @Override public void error(Marker marker, String msg) {
    self.error(marker, msg);
  }

  /**
   * This method is similar to {@link #error(String, Object)} method except that the
   * marker data is also taken into consideration.
   *  @param marker the marker data specific to this log statement
   * @param format the format string
   * @param arg    the argument
   */
  @Override public void error(Marker marker, String format, Object arg) {
    self.error(marker, format, arg);
  }

  /**
   * This method is similar to {@link #error(String, Object, Object)}
   * method except that the marker data is also taken into
   * consideration.
   *  @param marker the marker data specific to this log statement
   * @param format the format string
   * @param arg1   the first argument
   * @param arg2   the second argument
   */
  @Override public void error(Marker marker, String format, Object arg1, Object arg2) {
    self.error(marker, format, arg1, arg2);
  }

  /**
   * This method is similar to {@link #error(String, Object...)}
   * method except that the marker data is also taken into
   * consideration.
   *  @param marker    the marker data specific to this log statement
   * @param format    the format string
   * @param arguments a list of 3 or more arguments
   */
  @Override public void error(Marker marker, String format, Object... arguments) {
    self.error(marker, format, arguments);
  }

  /**
   * This method is similar to {@link #error(String, Throwable)}
   * method except that the marker data is also taken into
   * consideration.
   *  @param marker the marker data specific to this log statement
   * @param msg    the message accompanying the exception
   * @param t      the exception (throwable) to log
   */
  @Override public void error(Marker marker, String msg, Throwable t) {
    self.error(marker, msg, t);
  }

  /* Taken & modified from guava Preconditions.java under ASLv2
   * Also borrowed the MessageFormat support from java.util logging
   */
  @Deprecated
  public static String format(String template, Object... args) {
    template = String.valueOf(template); // null -> "null"
    if (args == null) {
      return template;
    }
    if (template.contains("{0}") || template.contains("{1}") ||
        template.contains("{2}") || template.contains("{3}")) {
        return java.text.MessageFormat.format(template, args);
    }
    StringBuilder builder = new StringBuilder(template.length() + 16 * args.length);
    int templateStart = 0;
    int i = 0;
    while (i < args.length) {
      int placeholderStart = template.indexOf("{}", templateStart);
      if (placeholderStart == -1) {
        break;
      }
      builder.append(template.substring(templateStart, placeholderStart));
      builder.append(args[i++]);
      templateStart = placeholderStart + 2;
    }
    builder.append(template.substring(templateStart));
    if (i < args.length) {
      builder.append(" [");
      builder.append(args[i++]);
      while (i < args.length) {
        builder.append(", ");
        builder.append(args[i++]);
      }
      builder.append(']');
    }
    return builder.toString();
  }

  @Deprecated
  public static Logger getLogger() {
    return get(new Exception().getStackTrace()[1].getClassName());
  }

  @Deprecated
  public static Logger getLogger(String name) {
    return new Logger(org.slf4j.LoggerFactory.getLogger(name));
  }

  @Deprecated
  public static Logger getLogger(Class<?> clazz) {
    return new Logger(org.slf4j.LoggerFactory.getLogger(clazz));
  }

  public static Logger get() {
    return get(new Exception().getStackTrace()[1].getClassName());
  }

  public static Logger get(String name) {
    return new Logger(org.slf4j.LoggerFactory.getLogger(name));
  }

  public static Logger get(Class<?> clazz) {
    return new Logger(org.slf4j.LoggerFactory.getLogger(clazz));
  }
}
