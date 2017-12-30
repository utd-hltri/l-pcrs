package edu.utdallas.hltri.logging;

import edu.utdallas.hltri.io.AC;

import org.slf4j.MDC;

/**
 * Created by travis on 2/9/15.
 */
public class NDC implements AC {
  private final int size;
  static {
    org.slf4j.MDC.put("NDC", "");
  }

  /**
   * Push all the contexts given and pop them when auto-closed
   */
  public static NDC push(String... context) {
    return new NDC(context);
  }

  /**
   * Construct an {@link AutoCloseable} {@link NDC} with the given contexts
   */
  private NDC(String... context) {
    this.size = MDC.get("NDC").length();
    for (String c : context) {
      MDC.put("NDC", MDC.get("NDC") + "[" + c + "] ");
    }
  }

  @Override
  public void close() {
    MDC.put("NDC", MDC.get("NDC").substring(0, size));
  }
}
