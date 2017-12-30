package edu.utdallas.hltri.logging;

/**
 * Created by travis on 2/9/15.
 */

import java.util.Map;

import edu.utdallas.hltri.io.AC;


public class MDC {

  public static void put(Object key, Object val) {
    org.slf4j.MDC.put(key.toString(), val.toString());
  }

  public static void clear() {
    org.slf4j.MDC.clear();
  }

  public static AC with(Object key, Object value) {
    put(key, value);
    return new AC() {
      @Override
      public void close() {
        remove(key);
      }
    };
  }

  public static Map<String, String> getCopyOfContextMap() {
    return org.slf4j.MDC.getCopyOfContextMap();
  }

  public static String get(Object key) {
    return org.slf4j.MDC.get(key.toString());
  }

  public static void remove(Object key) {
    org.slf4j.MDC.remove(key.toString());
  }

  public static void setContextMap(Map<String, String> contextMap) {
    org.slf4j.MDC.setContextMap(contextMap);
  }

}
