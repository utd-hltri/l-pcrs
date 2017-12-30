package edu.utdallas.hlt.framework;

/**
 *
 * @author travis
 */
public class Logger {
  private final org.slf4j.Logger logger;

  protected Logger(org.slf4j.Logger logger) {
    this.logger = logger;
  }

  public boolean isTraceEnabled() {
    return logger.isTraceEnabled();
  }

  public void trace(String msg, Object... params) {
    logger.trace(format(msg, params));
  }

  public void trace(Throwable cause, String msg, Object... params) {
    if (isTraceEnabled()) {
      logger.trace(format(msg, params), cause);
    }
  }

  public boolean isDebugEnabled() {
    return logger.isDebugEnabled();
  }

  public void debug(String msg, Object... params) {
    logger.debug(format(msg, params));
  }

  public void debug(Throwable cause, String msg, Object... params) {
    if (isDebugEnabled()) {
      logger.debug(format(msg, params), cause);
    }
  }

  public boolean isInfoEnabled() {
    return logger.isInfoEnabled();
  }

  public void info(String msg, Object... params) {
    logger.info(format(msg, params));
  }

  public void info(Throwable cause, String msg, Object... params) {
    if (isInfoEnabled()) {
      logger.info(format(msg, params), cause);
    }
  }

  public boolean isWarnEnabled() {
    return logger.isWarnEnabled();
  }

  public void warn(String msg, Object... params) {
    logger.warn(msg, params);
  }

  public void warn(Throwable cause, String msg, Object... params) {
    if (isWarnEnabled()) {
      logger.warn(format(msg, params), cause);
    }
  }

  public boolean isErrorEnabled() {
    return logger.isErrorEnabled();
  }

  public void error(String msg, Object... params) {
    logger.error(format(msg, params));
  }

  public void error(Throwable cause, String msg, Object... params) {
    if (isErrorEnabled()) {
      logger.error(format(msg, params), cause);
    }
  }

  /* Taken & modified from guava Preconditions.java under ASLv2
   * Also borrowed the MessageFormat support from java.util logging
   */
  private static String format(String template, Object... args) {
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
}
