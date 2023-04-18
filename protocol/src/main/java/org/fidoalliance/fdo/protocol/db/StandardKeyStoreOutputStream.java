// Copyright 2022 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fidoalliance.fdo.protocol.db;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import org.fidoalliance.fdo.protocol.LoggerService;
import org.fidoalliance.fdo.protocol.dispatch.KeyStoreOutputStreamFunction;
import org.fidoalliance.fdo.protocol.entity.CertificateData;
import org.hibernate.Session;
import org.hibernate.Transaction;

public class StandardKeyStoreOutputStream implements KeyStoreOutputStreamFunction {
  private static final LoggerService logger = new LoggerService(StandardKeyStoreOutputStream.class);

  private static class StoreOutputStream extends ByteArrayOutputStream {

    private final CertificateData certStore;
    private final Session session;
    private final Transaction trans;

    public StoreOutputStream(Session session, Transaction trans, CertificateData certStore) {
      this.session = session;
      this.certStore = certStore;
      this.trans = trans;

    }

    @Override
    public void close() throws IOException {

      try {
        certStore.setData(toByteArray());
        //Blob blob = session.getLobHelper().createBlob(toByteArray());
        //certStore.setData(data);
        //blob.
        session.persist(certStore);
        trans.commit();
      } finally {
        session.close();
      }

    }
  }


  @Override
  public OutputStream apply(String s) throws IOException {
    Session session = HibernateUtil.getSessionFactory().openSession();
    Transaction trans = null;
    try {

      final String name = new File(s).getName();
      trans = session.beginTransaction();
      CertificateData certStore = session.find(CertificateData.class, name);
      if (certStore == null) {
        certStore = new CertificateData();
        certStore.setName(name);
      }
      OutputStream out = new StoreOutputStream(session, trans, certStore);
      session = null;
      trans = null;
      return out;
    } finally {
      if (trans != null) {
        logger.debug("Committing transaction");
        trans.commit();
      }
      if (session != null) {
        logger.debug("Closing session");
        session.close();
      }
    }
  }
}
