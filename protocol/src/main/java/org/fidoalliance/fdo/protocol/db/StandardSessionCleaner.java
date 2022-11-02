// Copyright 2022 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fidoalliance.fdo.protocol.db;

import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import java.io.Closeable;
import java.io.IOException;
import java.time.Duration;
import java.util.Date;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.fidoalliance.fdo.protocol.LoggerService;
import org.fidoalliance.fdo.protocol.entity.ProtocolSession;
import org.hibernate.Session;
import org.hibernate.Transaction;

public class StandardSessionCleaner implements Closeable {

  private static final LoggerService logger = new LoggerService(StandardSessionCleaner.class);


  private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

  //private final Duration interval = Duration.ofHours(2);
  private final Duration interval = Duration.ofMinutes(60);
  private final Duration expires = Duration.ofHours(2);

  /**
   * Worker Constructor.
   */
  public StandardSessionCleaner() {

    scheduler.scheduleWithFixedDelay(new Runnable() {
      @Override
      public void run() {
        onClean();
      }
    }, interval.toSeconds(), interval.toSeconds(), TimeUnit.SECONDS);

    logger.info("Session cleaner will run every " + interval.toSeconds() + " seconds");

  }

  private void onClean() {

    Session session = HibernateUtil.getSessionFactory().openSession();
    try {
      Transaction trans = session.beginTransaction();
      CriteriaBuilder cb = session.getCriteriaBuilder();
      CriteriaQuery<ProtocolSession> cq = cb.createQuery(ProtocolSession.class);
      Root<ProtocolSession> rootEntry = cq.from(ProtocolSession.class);
      CriteriaQuery<ProtocolSession> all = cq.select(rootEntry);

      TypedQuery<ProtocolSession> allQuery = session.createQuery(all);
      Date now = new Date(System.currentTimeMillis());
      long expiresSeconds = expires.toSeconds();

      boolean reported = false;
      for (ProtocolSession protocolSession : allQuery.getResultList()) {
        Date created = protocolSession.getCreatedOn();
        long dur = Duration.between(created.toInstant(), now.toInstant()).toSeconds();
        if (dur > expiresSeconds) {
          session.delete(protocolSession);
          if (!reported) {
            logger.info("expired session removed");
            reported = true;
          }
        }

      }
      trans.commit();
    } finally {
      session.close();
    }
  }

  @Override
  public void close() throws IOException {
    scheduler.shutdown();
  }
}
