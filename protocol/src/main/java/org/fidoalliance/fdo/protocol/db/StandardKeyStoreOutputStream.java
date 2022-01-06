package org.fidoalliance.fdo.protocol.db;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.sql.Blob;
import org.fidoalliance.fdo.protocol.dispatch.KeyStoreOutputStreamFunction;
import org.fidoalliance.fdo.protocol.entity.CertificateData;
import org.hibernate.Session;
import org.hibernate.Transaction;

public class StandardKeyStoreOutputStream implements KeyStoreOutputStreamFunction {


  private class StoreOutputStream extends ByteArrayOutputStream {

    private final CertificateData certStore;
    private final Session session;

    public StoreOutputStream(Session session, CertificateData certStore) {
      this.session = session;
      this.certStore = certStore;

    }

    @Override
    public void close() throws IOException {

      try {
        Transaction trans = session.beginTransaction();
        Blob blob = session.getLobHelper().createBlob(toByteArray());
        certStore.setData(blob);
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
    try {

      final String storeName = new File(s).getName();
      CertificateData certStore = session.get(CertificateData.class, storeName);
      if (certStore == null) {
        certStore = new CertificateData();
        certStore.setId(storeName);
      }
      OutputStream out = new StoreOutputStream(session, certStore);
      session = null;
      return out;
    } finally {
      if (session != null) {
        session.close();
      }
    }
  }
}
