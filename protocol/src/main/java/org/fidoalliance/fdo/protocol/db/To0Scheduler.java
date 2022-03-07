package org.fidoalliance.fdo.protocol.db;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.IOException;
import java.time.Duration;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import org.fidoalliance.fdo.protocol.Config;
import org.fidoalliance.fdo.protocol.LoggerService;
import org.fidoalliance.fdo.protocol.Mapper;
import org.fidoalliance.fdo.protocol.ResourceNotFoundException;
import org.fidoalliance.fdo.protocol.SelfSignedHttpClientSupplier;
import org.fidoalliance.fdo.protocol.StandardTo0Client;
import org.fidoalliance.fdo.protocol.dispatch.VoucherQueryFunction;
import org.fidoalliance.fdo.protocol.entity.OnboardingConfig;
import org.fidoalliance.fdo.protocol.entity.OnboardingVoucher;
import org.fidoalliance.fdo.protocol.message.OwnershipVoucher;
import org.fidoalliance.fdo.protocol.message.To0OwnerSign;
import org.fidoalliance.fdo.protocol.message.To0d;
import org.fidoalliance.fdo.protocol.message.To2AddressEntries;
import org.hibernate.Session;
import org.hibernate.Transaction;

public class To0Scheduler {

   private final ThreadPoolExecutor executor;
  private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

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
      return Long.parseLong(Config.resolve(interval));
    }

    public int getThreadCount() {
      int threadCountValue;
      try {
        threadCountValue = Integer.parseInt(Config.resolve(threadCount));
        if(threadCountValue <= 0) {
          logger.error("Received threadCount less than 1. Defaulting the thread-count to 1.");
          threadCountValue = 1;
        }
      } catch (NumberFormatException e) {
        threadCountValue = 5;
        logger.error("Invalid threadCount. Defaulting the thread-count to 5.");
      }
      return threadCountValue;
    }
  }

  static LoggerService logger = new LoggerService(To0Scheduler.class);

  private static To0SchedulerConfig config = Config.getConfig(RootConfig.class).config.scheduler;

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
      OnboardingConfig onboardConfig = new OnboardConfigSupplier().get();
      for (OnboardingVoucher onboardingVoucher : list) {
        if (onboardingVoucher.getTo0Expiry() == null ||
            now.after(onboardingVoucher.getTo0Expiry())) {

          OwnershipVoucher voucher = Mapper.INSTANCE.readValue(onboardingVoucher.getData(),
              OwnershipVoucher.class);

          StandardTo0Client to0Client = new StandardTo0Client();
          To0OwnerSign ownerSign = new To0OwnerSign();
          To0d to0d = new To0d();
          to0d.setVoucher(voucher);

          To2AddressEntries addressEntries =
              Mapper.INSTANCE.readValue(onboardConfig.getRvBlob(), To2AddressEntries.class);
          to0Client.setAddressEntries(addressEntries);

          to0d.setWaitSeconds(onboardConfig.getWaitSeconds());
          to0Client.setTo0d(to0d);

          executor.submit(to0Client);
        }
      }


    } catch (IOException e) {

    } finally {
      session.close();
    }

    executor.submit(new StandardTo0Client());
  }

  public To0Scheduler() {


    executor =
        (ThreadPoolExecutor) Executors.newFixedThreadPool(config.getThreadCount());

    Long interval = config.getInterval();
    scheduler.scheduleWithFixedDelay(new Runnable() {
      @Override
      public void run() {
        onInterval();
      }
    }, interval, interval,TimeUnit.SECONDS);

    logger.info("To0Scheduler will run every " + interval + " seconds");
  }
}
