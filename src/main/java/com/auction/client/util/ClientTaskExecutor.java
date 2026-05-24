package com.auction.client.util;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class ClientTaskExecutor {
  private static final ExecutorService IO_EXECUTOR = Executors.newFixedThreadPool(4, runnable -> {
    Thread thread = new Thread(runnable, "client-io");
    thread.setDaemon(true);
    return thread;
  });

  private ClientTaskExecutor() {
  }

  public static void execute(Runnable task) {
    if (task == null) {
      return;
    }
    IO_EXECUTOR.execute(task);
  }
}
