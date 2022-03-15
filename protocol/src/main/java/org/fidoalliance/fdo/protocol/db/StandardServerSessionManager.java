// Copyright 2022 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fidoalliance.fdo.protocol.db;

import java.io.IOException;
import java.time.Instant;
import java.util.Date;
import org.fidoalliance.fdo.protocol.InvalidJwtTokenException;
import org.fidoalliance.fdo.protocol.Mapper;
import org.fidoalliance.fdo.protocol.dispatch.SessionManager;
import org.fidoalliance.fdo.protocol.entity.ProtocolSession;
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
      ProtocolSession protocolSession = session.find(ProtocolSession.class, name);
      if (protocolSession == null) {
        throw new InvalidJwtTokenException(name);
      }

      return Mapper.INSTANCE.readValue(
          HibernateUtil.unwrap(protocolSession.getData()),
          SimpleStorage.class);


    } finally {
      if (trans != null) {
        trans.commit();
      }
      session.close();
    }

  }

  @Override
  public void saveSession(String name, SimpleStorage storage) throws IOException {

    final Session session = HibernateUtil.getSessionFactory().openSession();
    try {
      final Transaction trans = session.beginTransaction();
      final ProtocolSession protocolSession = new ProtocolSession();
      protocolSession.setName(name);
      protocolSession.setCreatedOn(Date.from(Instant.now()));
      protocolSession.setData(session.getLobHelper().createBlob(
          Mapper.INSTANCE.writeValue(storage)));
      session.save(protocolSession);
      trans.commit();
    } finally {
      session.close();
    }
  }

  @Override
  public void updateSession(String name, SimpleStorage storage) throws IOException {
    final Session session = HibernateUtil.getSessionFactory().openSession();
    try {
      final Transaction trans = session.beginTransaction();
      final ProtocolSession protocolSession = new ProtocolSession();
      protocolSession.setName(name);
      protocolSession.setCreatedOn(Date.from(Instant.now()));
      protocolSession.setData(session.getLobHelper().createBlob(
          Mapper.INSTANCE.writeValue(storage)));
      session.update(protocolSession);
      trans.commit();
    } finally {
      session.close();
    }
  }


  @Override
  public void expireSession(String name) {
    Session session = HibernateUtil.getSessionFactory().openSession();
    try {
      Transaction trans = session.beginTransaction();
      ProtocolSession protocolSession = session.get(ProtocolSession.class, name);
      if (protocolSession != null) {

        session.delete(protocolSession);
      }
      trans.commit();

    } finally {
      session.close();
    }
  }
}
