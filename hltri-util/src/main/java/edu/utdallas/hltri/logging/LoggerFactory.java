package edu.utdallas.hltri.logging;

/**
 *
 * @author travis
 * @deprecated Use Logger.get() instead
 */

@Deprecated
public class LoggerFactory {

  private LoggerFactory() {
  }

  public static Logger getLogger() {
    return Logger.get();
  }

  public static Logger getLogger(String name) {
    return Logger.get(name);
  }

  public static Logger getLogger(Class<?> clazz) {
    return Logger.get(clazz);
  }
}
