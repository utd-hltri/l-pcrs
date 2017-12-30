package edu.utdallas.hltri.concurrent;

import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * A handler for rejected tasks that will have the caller block until space is
 * available.
 */
public class BlockPolicy implements RejectedExecutionHandler {

  /**
   * Puts the Runnable to the blocking queue, effectively blocking the
   * delegating thread until space is available.
   *
   * @param r the runnable task requested to be executed
   * @param e the executor attempting to execute this task
   */
  @Override
  public void rejectedExecution(Runnable r, ThreadPoolExecutor e) {
    try {
      e.getQueue().put(r);
    } catch (InterruptedException e1) {
      throw new RuntimeException(e1);
    }
  }
}
