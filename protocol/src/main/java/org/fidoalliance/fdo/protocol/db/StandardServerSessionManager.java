package org.fidoalliance.fdo.protocol.db;

import java.io.IOException;
import java.sql.Blob;
import java.time.Instant;
import java.util.Date;
import org.fidoalliance.fdo.protocol.InvalidJwtTokenException;
import org.fidoalliance.fdo.protocol.Mapper;
import org.fidoalliance.fdo.protocol.dispatch.SessionManager;
import org.fidoalliance.fdo.protocol.entity.ProtocolSession;
import org.fidoalliance.fdo.protocol.message.AnyType;
import org.fidoalliance.fdo.protocol.message.SimpleStorage;
import org.hibernate.Session;
import org.hibernate.Transaction;

public class StandardServerSessionManager implements SessionManager {

  @Override
  public SimpleStorage getSession(String name) throws IOException {

    Session session = HibernateUtil.getSessionFactory().openSession();
    Transaction trans = null;
    try {
      trans = session.beginTransaction();
      ProtocolSession protocolSession = session.get(ProtocolSession.class, name);
      if (protocolSession == null) {
        throw new InvalidJwtTokenException(name);
      }
      Blob blob = protocolSession.getData();
      byte[] data = HibernateUtil.unwrap(blob);

      return Mapper.INSTANCE.readValue(data,SimpleStorage.class);


    } finally {
      if (trans != null) {
        trans.commit();
      }
      session.close();
    }

  }

  @Override
  public void saveSession(String name, SimpleStorage storage) throws IOException {

    Session session = HibernateUtil.getSessionFactory().openSession();
    try {
      ProtocolSession protocolSession = new ProtocolSession();
      protocolSession.setId(name);
      protocolSession.setCreatedOn(Date.from(Instant.now()));

      Blob blob = session.getLobHelper().createBlob(
          Mapper.INSTANCE.writeValue(storage)
      );
      protocolSession.setData(blob);
      Transaction trans = session.beginTransaction();
      session.persist(protocolSession);
      trans.commit();;

    } finally {
      session.close();
    }
  }


  @Override
  public void expireSession(String name) {
    Session session = HibernateUtil.getSessionFactory().openSession();
    try {
      ProtocolSession protocolSession = session.get(ProtocolSession.class, name);

      if (protocolSession != null) {
        session.delete(protocolSession);
      }


    } finally {
      session.close();
    }
  }
}
