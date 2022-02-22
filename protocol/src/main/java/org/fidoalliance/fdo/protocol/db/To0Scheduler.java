package org.fidoalliance.fdo.protocol.db;

import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

public class To0Scheduler {


  public To0Scheduler() {

    ThreadPoolExecutor executor =
        (ThreadPoolExecutor) Executors.newFixedThreadPool(2);

    //executor.submit()
    //executor.submit(() -> {
    //  Thread.sleep(1000);
    //  return null;
    //});
    //executor.submit(() -> {
    //  Thread.sleep(1000);
    //  return null;
    //});
    //executor.submit(() -> {
    //   Thread.sleep(1000);
    //   return null;
    // });
  }
}
