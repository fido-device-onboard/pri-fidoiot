// Copyright 2022 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fidoalliance.fdo.protocol.db;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import java.io.Closeable;
import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import org.fidoalliance.fdo.protocol.Config;
import org.fidoalliance.fdo.protocol.LoggerService;
import org.fidoalliance.fdo.protocol.Mapper;
import org.fidoalliance.fdo.protocol.StandardTo0Client;
import org.fidoalliance.fdo.protocol.entity.OnboardingConfig;
import org.fidoalliance.fdo.protocol.entity.OnboardingVoucher;
import org.fidoalliance.fdo.protocol.message.OwnershipVoucher;
import org.fidoalliance.fdo.protocol.message.To0d;
import org.fidoalliance.fdo.protocol.message.To2AddressEntries;
import org.hibernate.Session;

public class To0Scheduler implements Closeable {

  private static final LoggerService logger = new LoggerService(To0Scheduler.class);

  private final ThreadPoolExecutor executor;
  private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

  @Override
  public void close() throws IOException {
    if (executor != null) {
      executor.shutdown();
      scheduler.shutdown();
    }
  }

  private static class RootConfig {

    @JsonProperty("owner")
    private To0SchedulerRoot config;

  }

  private static class To0SchedulerRoot {

    @JsonProperty("to0-scheduler")
    private To0SchedulerConfig scheduler;


  }

  private static class To0SchedulerConfig {

    @JsonProperty("thread-count")
    private String threadCount;
    @JsonProperty("interval")
    private String interval;

    public long getInterval() {
      Long intervalValue;
      try {
        intervalValue = Long.parseLong(Config.resolve(interval));
        if (intervalValue <= 60) {
          logger.error("Received intervalValue less than 60. Defaulting intervalValue to 60.");
          intervalValue = Long.valueOf(60);
        }
      } catch (NumberFormatException e) {
        intervalValue = Long.valueOf(120);
        logger.error("Invalid intervalValue. Defaulting intervalValue to 120.");
      }
      return intervalValue;
    }

    public int getThreadCount() {
      int threadCountValue;
      try {
        threadCountValue = Integer.parseInt(Config.resolve(threadCount));
        if (threadCountValue < 5) {
          logger.error("Received threadCount less than 5. Defaulting the thread-count to 5.");
          threadCountValue = 5;
        }
      } catch (NumberFormatException e) {
        threadCountValue = 5;
        logger.error("Invalid threadCount. Defaulting the thread-count to 5.");
      }
      return threadCountValue;
    }
  }


  private static final To0SchedulerConfig config = Config.getConfig(
      RootConfig.class).config.scheduler;

  private void onInterval() {

    final Session session = HibernateUtil.getSessionFactory().openSession();
    try {

      CriteriaBuilder cb = session.getCriteriaBuilder();
      CriteriaQuery<OnboardingVoucher> cq = cb.createQuery(OnboardingVoucher.class);
      Root<OnboardingVoucher> rootEntry = cq.from(OnboardingVoucher.class);
      CriteriaQuery<OnboardingVoucher> all = cq.select(rootEntry);

      TypedQuery<OnboardingVoucher> allQuery = session.createQuery(all);
      List<OnboardingVoucher> list = allQuery.getResultList();
      Date now = new Date(System.currentTimeMillis());
      To2AddressEntries addressEntries = new To2BlobSupplier().get();
      OnboardingConfig onboardConfig = new OnboardConfigSupplier().get();
      for (OnboardingVoucher onboardingVoucher : list) {
        if ((onboardingVoucher.getTo0Expiry() == null
            || now.after(onboardingVoucher.getTo0Expiry()))
                && onboardingVoucher.getTo2CompletedOn() == null) {

          OwnershipVoucher voucher = Mapper.INSTANCE.readValue(onboardingVoucher.getData(),
              OwnershipVoucher.class);

          StandardTo0Client to0Client = new StandardTo0Client();

          To0d to0d = new To0d();
          to0d.setVoucher(voucher);

          to0Client.setAddressEntries(addressEntries);

          to0d.setWaitSeconds(onboardConfig.getWaitSeconds());
          to0Client.setTo0d(to0d);

          executor.submit(to0Client);
        }
      }

    } catch (IOException e) {
      logger.error("T0 scheduler failure due to " + e.getMessage());
    } finally {
      session.close();
    }

    executor.submit(new StandardTo0Client());
  }

  /**
   * Worker Constructor.
   */
  public To0Scheduler() {

    executor =
        (ThreadPoolExecutor) Executors.newFixedThreadPool(config.getThreadCount());

    Long interval = config.getInterval();
    scheduler.scheduleWithFixedDelay(new Runnable() {
      @Override
      public void run() {
        onInterval();
      }
    }, interval, interval, TimeUnit.SECONDS);

    logger.info("To0Scheduler will run every " + interval + " seconds");
  }
}
