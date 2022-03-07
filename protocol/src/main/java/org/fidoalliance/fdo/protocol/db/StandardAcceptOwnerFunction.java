package org.fidoalliance.fdo.protocol.db;

import java.io.IOException;
import java.time.Duration;
import java.util.Date;
import org.fidoalliance.fdo.protocol.dispatch.AcceptOwnerFunction;
import org.fidoalliance.fdo.protocol.entity.OnboardingVoucher;
import org.hibernate.Session;
import org.hibernate.Transaction;

public class StandardAcceptOwnerFunction implements AcceptOwnerFunction {


  @Override
  public Date apply(String guid, Long ws) throws IOException {
    Session session = HibernateUtil.getSessionFactory().openSession();
    Date expiry = new Date(System.currentTimeMillis() +
        Duration.ofSeconds(ws).toMillis());
    try {

      OnboardingVoucher onboardingVoucher = session.find(OnboardingVoucher.class, guid);
      if (onboardingVoucher != null) {
        final Transaction trans = session.beginTransaction();
        onboardingVoucher.setTo0Expiry(expiry);
        session.update(onboardingVoucher);
        trans.commit();
      }
    } finally {
      session.close();
    }
    return expiry;
  }
}
