// Copyright 2022 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fidoalliance.fdo.protocol.db;

import java.io.IOException;
import org.fidoalliance.fdo.protocol.Config;
import org.fidoalliance.fdo.protocol.HttpServer;
import org.fidoalliance.fdo.protocol.Mapper;
import org.fidoalliance.fdo.protocol.dispatch.RendezvousInfoSupplier;
import org.fidoalliance.fdo.protocol.entity.RvData;
import org.fidoalliance.fdo.protocol.message.RendezvousInfo;
import org.hibernate.Session;
import org.hibernate.Transaction;

public class StandardRendezvousInfoSupplier implements RendezvousInfoSupplier {

  @Override
  public RendezvousInfo get() throws IOException {
    final Session session = HibernateUtil.getSessionFactory().openSession();
    try {
      final Transaction trans = session.beginTransaction();
      RvData rvData =
          session.find(RvData.class, Long.valueOf(1));

      if (rvData == null) {
        rvData = new RvData();

        final String defaultRvi = "- - - 5\n"
            + "    - \"localhost\"\n"
            + "  - - 3\n"
            + "    - %s\n"
            + "  - - 12\n"
            + "    - 1\n"
            + "  - - 2\n"
            + "    - \"127.0.0.1\"\n"
            + "  - - 4\n"
            + "    - %s";

        String defaultPort = Config.getWorker(HttpServer.class).getPort();
        String rviString = String.format(defaultRvi, defaultPort, defaultPort);
        RendezvousInfo rvi = Mapper.INSTANCE.readValue(rviString, RendezvousInfo.class);

        rvData.setData(Mapper.INSTANCE.writeValue(rvi));

        session.persist(rvData);
      }
      trans.commit();

      return Mapper.INSTANCE.readValue(rvData.getData(), RendezvousInfo.class);

    } finally {
      session.close();
    }
  }
}
