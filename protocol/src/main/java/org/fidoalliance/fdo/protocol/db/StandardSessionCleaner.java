package org.fidoalliance.fdo.protocol.db;

import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import org.fidoalliance.fdo.protocol.InvalidJwtTokenException;
import org.fidoalliance.fdo.protocol.Mapper;
import org.fidoalliance.fdo.protocol.entity.ProtocolSession;
import org.fidoalliance.fdo.protocol.message.SimpleStorage;
import org.hibernate.Session;
import org.hibernate.Transaction;

public class StandardSessionCleaner {

  private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

  private final  Duration interval = Duration.ofHours(2);

  public StandardSessionCleaner() {

    scheduler.scheduleWithFixedDelay(new Runnable() {
      @Override
      public void run() {
        onClean();
      }
    },interval.toSeconds(),interval.toSeconds(), TimeUnit.SECONDS);

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
      long intervalSeconds = interval.toSeconds();

      for (ProtocolSession protocolSession : allQuery.getResultList()) {
        Date created = protocolSession.getCreatedOn();
        long dur = Duration.between(created.toInstant(), now.toInstant()).toSeconds();
        if (dur > intervalSeconds) {
          session.delete(protocolSession);
        }

      }
      trans.commit();
    } finally {
      session.close();
    }
  }
}
