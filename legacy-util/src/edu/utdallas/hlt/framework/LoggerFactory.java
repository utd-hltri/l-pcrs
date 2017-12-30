package edu.utdallas.hlt.framework;

/**
 *
 * @author travis
 */
public class LoggerFactory {

  private LoggerFactory() {
  }

  public static Logger getLogger() {
    return getLogger(new Exception().getStackTrace()[1].getClassName());
  }

  public static Logger getLogger(String name) {
    return new Logger(org.slf4j.LoggerFactory.getLogger(name));
  }

  public static Logger getLogger(Class<?> clazz) {
    return new Logger(org.slf4j.LoggerFactory.getLogger(clazz));
  }
}