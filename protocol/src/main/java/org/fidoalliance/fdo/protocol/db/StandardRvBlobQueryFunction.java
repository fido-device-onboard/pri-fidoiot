// Copyright 2022 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fidoalliance.fdo.protocol.db;

import java.io.IOException;
import java.sql.SQLException;
import org.fidoalliance.fdo.protocol.InternalServerErrorException;
import org.fidoalliance.fdo.protocol.Mapper;
import org.fidoalliance.fdo.protocol.ResourceNotFoundException;
import org.fidoalliance.fdo.protocol.dispatch.RvBlobQueryFunction;
import org.fidoalliance.fdo.protocol.entity.RvRedirect;
import org.fidoalliance.fdo.protocol.message.To2RedirectEntry;
import org.hibernate.Session;
import org.hibernate.Transaction;

public class StandardRvBlobQueryFunction implements RvBlobQueryFunction {

  @Override
  public To2RedirectEntry apply(String s) throws IOException {
    final Session session = HibernateUtil.getSessionFactory().openSession();
    try {
      final Transaction trans = session.beginTransaction();
      RvRedirect rvBlob =
          session.find(RvRedirect.class, s);

      trans.commit();
      if (rvBlob == null) {
        throw new ResourceNotFoundException("guid: " + s);
      }

      return Mapper.INSTANCE.readValue(rvBlob.getData(),To2RedirectEntry.class);

    } finally {
      session.close();
    }
  }
}
