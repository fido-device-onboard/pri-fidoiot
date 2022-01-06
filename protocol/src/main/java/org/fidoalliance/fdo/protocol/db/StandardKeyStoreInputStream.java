package org.fidoalliance.fdo.protocol.db;


import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Blob;
import java.sql.SQLException;
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
      final String storeName = new File(s).getName();
      CertificateData certStore = session.get(CertificateData.class,storeName);
      if (certStore == null) {
        certStore = new CertificateData();
        certStore.setId(storeName);

        session.persist(certStore);
        return null;
      }


      Blob blob = certStore.getData();
      if (blob != null) {
        try {
          byte[] data = blob.getBytes(Long.valueOf(1),
              Long.valueOf(blob.length()).intValue());
          return new ByteArrayInputStream(data);
        } catch (SQLException e) {
          throw new IOException(e);
        }
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
