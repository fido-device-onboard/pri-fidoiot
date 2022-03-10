// Copyright 2022 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fidoalliance.fdo.protocol.db;

import java.io.IOException;
import org.fidoalliance.fdo.protocol.dispatch.ValidityDaysSupplier;
import org.fidoalliance.fdo.protocol.entity.CertificateValidity;
import org.hibernate.Session;
import org.hibernate.Transaction;

public class StandardValidityDaysSupplier implements ValidityDaysSupplier {

  @Override
  public Integer get() throws IOException {

    final Session session = HibernateUtil.getSessionFactory().openSession();
    try {
      final Transaction trans = session.beginTransaction();

      CertificateValidity validityDays = session.get(CertificateValidity.class, Long.valueOf(1));
      if (validityDays == null) {
        validityDays = new CertificateValidity();
        validityDays.setDays(360 * 30);

        session.persist(validityDays);
      }
      trans.commit();
      return validityDays.getDays();

    } finally {
      session.close();
    }
  }
}