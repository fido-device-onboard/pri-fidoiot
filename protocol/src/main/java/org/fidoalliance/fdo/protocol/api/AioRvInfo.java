// Copyright 2022 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fidoalliance.fdo.protocol.api;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.fidoalliance.fdo.protocol.Config;
import org.fidoalliance.fdo.protocol.HttpServer;
import org.fidoalliance.fdo.protocol.LoggerService;
import org.fidoalliance.fdo.protocol.Mapper;
import org.fidoalliance.fdo.protocol.entity.RvData;
import org.fidoalliance.fdo.protocol.message.RendezvousInfo;


/**
 * AioRvInfo REST endpoint enables users to set value for RVINFO_BLOB in RV_DATA table with IP
 * address and Protocol_type.
 *
 * <p>Accepted URL pattern: POST /api/v1/aio/rvinfo?ip=&lt; ip-address&gt&rvprot=http/https
 *
 * <p>RestApi Class provides a wrapper over the HttpServletRequest methods.
 */
public class AioRvInfo extends RestApi {

  @Override
  public void doPost() throws Exception {

    // Create Session object and begin Hibernate transaction.
    getTransaction();

    LoggerService logger = new LoggerService(AioRvInfo.class);

    try {
      // Constructing the yaml structure of RvInfo Object.
      final String defaultRvi = "[[[5, \"%s\"], [3,%s], [12, %s], [2, \"%s\"], [4, %s]]]";

      String port = Config.getWorker(HttpServer.class).getHttpPort();
      String securePort = Config.getWorker(HttpServer.class).getHttpsPort();

      String theDns = "localhost";
      String theIp = "127.0.0.1";
      String thePort = port;
      String theProto = "1";
      String rvprot = getParamByValue("rvprot");
      if (rvprot != null) {
        if (rvprot.equals("https")) {
          thePort = securePort;
          theProto = "2";
        }
      }

      String ip = getParamByValue("ip");
      if (ip != null) {
        theDns = ip;
        theIp = ip;
      }

      String rvi = String.format(defaultRvi, theDns, thePort, theProto, theIp, thePort);
      logger.info("Rendezvous Info " + rvi);

      // Creating RendezvousInfo object from yaml structure.
      RendezvousInfo rviObject = Mapper.INSTANCE.readValue(rvi, RendezvousInfo.class);
      if (rviObject.isEmpty()) {
        throw new IOException();
      }

      // Querying DB for RVINFO_BLOB with id=1
      RvData rviData = getSession().get(RvData.class, Long.valueOf(1));

      if (rviData == null) {
        // if data doesn't exist in DB, create new row and insert into RV_DATA table.
        logger.info("Inserting data into RV_DATA");
        rviData = new RvData();
        rviData.setData(getSession().getLobHelper().createClob(rvi));
        getSession().save(rviData);
      } else {
        // if data exist in DB, update RV_INFO table with new RVInfo_blob.
        rviData.setData(getSession().getLobHelper().createClob(rvi));
        getSession().update(rviData);
      }
    } catch (Exception e) {
      logger.error("Unable to update RVInfo");
      getResponse().setStatus(HttpServletResponse.SC_BAD_REQUEST);
    }

  }
}
