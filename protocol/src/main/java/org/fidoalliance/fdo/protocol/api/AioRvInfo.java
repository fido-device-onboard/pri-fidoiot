package org.fidoalliance.fdo.protocol.api;

import jakarta.servlet.http.HttpServletResponse;
import org.fidoalliance.fdo.protocol.Config;
import org.fidoalliance.fdo.protocol.HttpServer;
import org.fidoalliance.fdo.protocol.LoggerService;
import org.fidoalliance.fdo.protocol.Mapper;
import org.fidoalliance.fdo.protocol.entity.RvData;
import org.fidoalliance.fdo.protocol.message.RendezvousInfo;

import java.sql.Blob;

/***
 *  AioRvInfo REST endpoint enables users to set value for RVINFO_BLOB in RV_DATA table
 *  with IP address and Protocol_type.
 *
 *  Accepted URL pattern: POST /api/v1/aio/rvinfo?ip=<ip-address>&rvprot=http/https
 *
 *  RestApi Class provides a wrapper over the HttpServletRequest methods.
 *
*/

public class AioRvInfo extends RestApi {

  @Override
  public void doPost() throws Exception {

    // Create Session object and begin Hibernate transaction.
    getTransaction();

    LoggerService logger = new LoggerService(AioRvInfo.class);

    try {
      // Constructing the yaml structure of RvInfo Object.
      StringBuilder rvi = new StringBuilder("- - - 5\n");
      String ip = getParamByValue("ip");
      if (ip != null) {
        rvi = rvi.append(String.format("    - \"%s\"\n  - - 2\n    - \"%s\"\n", ip, ip));
      } else {
        rvi = rvi.append("    - \"localhost\"\n  - - 2\n    - \"127.0.0.1\"\n");
      }

      String rvProt = getParamByValue("rvprot");
      if (rvProt != null && rvProt.equals("https")) {
        rvi = rvi.append("  - - 12\n    - 2\n");
      } else {
        // if invalid protocol specified, defaults to http.
        rvi = rvi.append("  - - 12\n    - 1\n");
      }

      String port = Config.getWorker(HttpServer.class).getPort();
      rvi = rvi.append(String.format("  - - 3\n    - %s\n  - - 4\n    - %s", port, port));

      // Creating RendezvousInfo object from yaml structure.
      RendezvousInfo rviObject = Mapper.INSTANCE.readValue(rvi.toString(), RendezvousInfo.class);

      // Querying DB for RVINFO_BLOB with id=1
      RvData rviData = getSession().get(RvData.class, Long.valueOf(1));

      if (rviData == null) {
        // if data doesn't exist in DB, create new row and insert into RV_DATA table.
        rviData = new RvData();
        rviData.setData(Mapper.INSTANCE.writeValue(rviObject));
        getSession().save(rviData);
      } else {
        // if data exist in DB, update RV_INFO table with new RVInfo_blob.
        rviData.setData(Mapper.INSTANCE.writeValue(rviObject));
        getSession().update(rviData);
      }
    } catch (Exception e) {
      logger.error("Unable to update RVInfo");
      getResponse().setStatus(HttpServletResponse.SC_BAD_REQUEST);
    }

  }
}
