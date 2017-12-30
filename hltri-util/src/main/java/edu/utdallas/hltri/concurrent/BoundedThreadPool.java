package edu.utdallas.hltri.concurrent;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

import java.util.concurrent.*;

/**
 * Created by travis on 6/8/15.
 */
public abstract class BoundedThreadPool {
  private BoundedThreadPool() {}

  public static ExecutorService create(final int size) {
    final BlockingQueue<Runnable> linkedBlockingDeque = new ArrayBlockingQueue<>(size);
    final int N = Runtime.getRuntime().availableProcessors();
    return new ThreadPoolExecutor(N, 2 * N, 30,
                                                             TimeUnit.SECONDS, linkedBlockingDeque,
                                                             new BlockPolicy());
  }

  public static ExecutorService create(final int size, final String name) {
    final BlockingQueue<Runnable> linkedBlockingDeque = new ArrayBlockingQueue<>(size);
    final int N = Runtime.getRuntime().availableProcessors();
    return new ThreadPoolExecutor(N, 2 * N, 30,
        TimeUnit.SECONDS, linkedBlockingDeque,
        new ThreadFactoryBuilder().setNameFormat(name + "-%d").build(),
        new BlockPolicy());
  }
}
