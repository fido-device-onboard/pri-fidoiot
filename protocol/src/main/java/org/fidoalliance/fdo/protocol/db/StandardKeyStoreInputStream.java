// Copyright 2022 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fidoalliance.fdo.protocol.db;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import org.fidoalliance.fdo.protocol.dispatch.KeyStoreInputStreamFunction;
import org.fidoalliance.fdo.protocol.entity.CertificateData;
import org.hibernate.Session;
import org.hibernate.Transaction;

public class StandardKeyStoreInputStream implements KeyStoreInputStreamFunction {

  @Override
  public InputStream apply(String s) throws IOException {
    final Session session = HibernateUtil.getSessionFactory().openSession();
    Transaction trans = null;
    try {
      trans = session.beginTransaction();

      final String name = new File(s).getName();
      CertificateData certStore = session.find(CertificateData.class, name);
      if (certStore == null) {
        certStore = new CertificateData();

        certStore.setName(name);
        session.persist(certStore);

        return null;
      }

      byte[] data = certStore.getData();
      if (data != null) {
        return new ByteArrayInputStream(data);
      }
      return null;

    } finally {
      if (trans != null) {
        trans.commit();
      }
      session.close();
    }
  }
}
