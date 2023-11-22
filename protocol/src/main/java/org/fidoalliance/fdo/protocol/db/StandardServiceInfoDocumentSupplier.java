// Copyright 2023 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fidoalliance.fdo.protocol.db;

import java.io.IOException;
import java.sql.SQLException;
import org.fidoalliance.fdo.protocol.InternalServerErrorException;
import org.fidoalliance.fdo.protocol.LoggerService;
import org.fidoalliance.fdo.protocol.dispatch.ServiceInfoDocumentSupplier;
import org.fidoalliance.fdo.protocol.entity.SystemPackage;
import org.fidoalliance.fdo.protocol.message.ServiceInfoDocument;

import org.hibernate.Session;
import org.hibernate.Transaction;

public class StandardServiceInfoDocumentSupplier implements ServiceInfoDocumentSupplier {


  private final LoggerService logger = new LoggerService(StandardServiceInfoDocumentSupplier.class);

  @Override
  public ServiceInfoDocument get() throws IOException {
    ServiceInfoDocument document = new ServiceInfoDocument();
    final Session session = HibernateUtil.getSessionFactory().openSession();
    try {
      Transaction trans = session.beginTransaction();
      SystemPackage systemPackage =
          session.find(SystemPackage.class, Long.valueOf(1));

      if (systemPackage != null) {
        String body = systemPackage.getData().getSubString(1,
            Long.valueOf(systemPackage.getData().length()).intValue());

        document.setInstructions(body);

      }
      trans.commit();
    } catch (SQLException e) {
      logger.error("SQL Exception" + e.getMessage());
      throw new InternalServerErrorException(e);
    } finally {
      session.close();
    }
    return document;
  }
}
