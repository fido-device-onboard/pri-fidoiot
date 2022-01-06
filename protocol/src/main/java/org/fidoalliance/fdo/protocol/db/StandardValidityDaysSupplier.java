package org.fidoalliance.fdo.protocol.db;

import java.io.IOException;
import org.fidoalliance.fdo.protocol.entity.CertificateValidity;
import org.fidoalliance.fdo.protocol.dispatch.ValidityDaysSupplier;
import org.hibernate.Session;
import org.hibernate.Transaction;

public class StandardValidityDaysSupplier implements ValidityDaysSupplier {

  @Override
  public Integer get() throws IOException {

    Session session = HibernateUtil.getSessionFactory().openSession();
    Transaction trans = null;
    try {
      trans = session.beginTransaction();

      CertificateValidity validityDays = session.get(CertificateValidity.class, Long.valueOf(1));
      if (validityDays == null) {
        validityDays = new CertificateValidity();
        validityDays.setId(Long.valueOf(1));
        validityDays.setDays(360*30);

        session.persist(validityDays);
      }
      return validityDays.getDays();

    } finally {
      if(trans != null) {
        trans.commit();
      }
      session.close();
    }
  }
}