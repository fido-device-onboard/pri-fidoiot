package org.fidoalliance.fdo.sample;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Blob;
import java.sql.SQLException;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.fidoalliance.fdo.protocol.Config;
import org.fidoalliance.fdo.protocol.DatabaseServer;
import org.fidoalliance.fdo.protocol.db.HibernateUtil;
import org.fidoalliance.fdo.protocol.HttpServer;
import org.fidoalliance.fdo.protocol.Mapper;
import org.fidoalliance.fdo.protocol.entity.CertificateValidity;
import org.fidoalliance.fdo.protocol.entity.SystemResource;
import org.fidoalliance.fdo.protocol.message.RendezvousInfo;
import org.hibernate.Session;
import org.hibernate.Transaction;

public class AioApp {


  private static void test() {


    final Session session = HibernateUtil.getSessionFactory().openSession();
    try {
      final Transaction trans = session.beginTransaction();

      SystemResource sr = session.get(SystemResource.class, "text.txt");
      if (sr == null) {
        sr = new SystemResource();
        sr.setName("text.txt");
        Blob blob =
            session.getLobHelper().createBlob("hello word".getBytes(StandardCharsets.UTF_8));

        sr.setData(blob);

        session.persist(sr);
      } else {

        Blob blob = sr.getData();
        InputStream is =  blob.getBinaryStream();
        is.readAllBytes();

      }
      trans.commit();


    } catch (SQLException | IOException throwables) {
      throwables.printStackTrace();
    } finally {
      session.close();
    }

  }

  public static void main(String args[]) {
    HibernateUtil.getSessionFactory();

    test();
    Config.getWorker(HttpServer.class).run();
  }
}
