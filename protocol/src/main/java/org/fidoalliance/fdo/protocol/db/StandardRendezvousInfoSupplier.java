package org.fidoalliance.fdo.protocol.db;

import java.io.IOException;
import java.sql.Blob;
import java.sql.SQLException;
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
    Session session = HibernateUtil.getSessionFactory().openSession();
    Transaction trans = null;
    try {
      trans = session.beginTransaction();
      RvData rvData =
          session.get(RvData.class,Long.valueOf(1));

      if (rvData == null) {
        rvData = new RvData();
        rvData.setId(Long.valueOf(1));

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
        String rviString = String.format(defaultRvi, defaultPort,defaultPort);
        RendezvousInfo rvi = Mapper.INSTANCE.readValue(rviString,RendezvousInfo.class);

        Blob blob = session.getLobHelper().createBlob(Mapper.INSTANCE.writeValue(rvi));
        rvData.setData(blob);
        session.persist(rvData);

      }

      byte[] data = HibernateUtil.unwrap(rvData.getData());
      return Mapper.INSTANCE.readValue(data,RendezvousInfo.class);

    } finally {
      if (trans != null) {
        trans.commit();
      }
      session.close();
    }
  }
}
