// Copyright 2020 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fido.iot.sample;

import java.io.PrintWriter;
import java.io.StringWriter;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.sql.DataSource;
import org.apache.commons.dbcp2.BasicDataSource;
import org.fido.iot.protocol.Composite;
import org.fido.iot.protocol.Const;
import org.fido.iot.protocol.CryptoService;
import org.fido.iot.protocol.InvalidMessageException;
import org.fido.iot.protocol.MessageDispatcher;
import org.fido.iot.protocol.MessagingService;
import org.fido.iot.protocol.To0ServerService;
import org.fido.iot.protocol.To0ServerStorage;
import org.fido.iot.protocol.To1ServerService;
import org.fido.iot.protocol.To1ServerStorage;
import org.fido.iot.protocol.epid.EpidUtils;
import org.fido.iot.storage.RvsDbManager;
import org.fido.iot.storage.To0AllowListDenyListDbStorage;
import org.fido.iot.storage.To1DbStorage;

/** Rendezvous servlet Context Listener. */
public class RvContextListener implements ServletContextListener {

  @Override
  public void contextInitialized(ServletContextEvent sce) {
    BasicDataSource ds = new BasicDataSource();

    ServletContext sc = sce.getServletContext();
    ds.setUrl(sc.getInitParameter(RvAppSettings.DB_URL));
    ds.setDriverClassName(RvAppSettings.H2_DRIVER);
    ds.setUsername(sc.getInitParameter(RvAppSettings.DB_USER));
    ds.setPassword(sc.getInitParameter(RvAppSettings.DB_PWD));

    System.out.println(ds.getUrl());

    ds.setMinIdle(5);
    ds.setMaxIdle(10);
    ds.setMaxOpenPreparedStatements(100);

    final CryptoService cs = new CryptoService();
    String epidTestMode = sc.getInitParameter(RvAppSettings.EPID_TEST_MODE);
    if (null != epidTestMode && Boolean.valueOf(epidTestMode)) {
      cs.setEpidTestMode();
      sc.log("EPID Test mode enabled.");
    }
    String epidUrl = sc.getInitParameter(RvAppSettings.EPID_URL);
    if (null != epidUrl) {
      EpidUtils.setEpidOnlineUrl(epidUrl);
    } else {
      sc.log("EPID URL not set. Default URL will be used: "
              + EpidUtils.getEpidOnlineUrl().toString());
    }

    sc.setAttribute("datasource", ds);
    sc.setAttribute("cryptoservice", cs);

    MessageDispatcher dispatcher =
        new MessageDispatcher() {
          @Override
          protected MessagingService getMessagingService(Composite request) {
            switch (request.getAsNumber(Const.SM_MSG_ID).intValue()) {
              case Const.TO0_HELLO:
              case Const.TO0_OWNER_SIGN:
                return createTo0Service(cs, ds);
              case Const.TO1_HELLO_RV:
              case Const.TO1_PROVE_TO_RV:
                return createTo1Service(cs, ds);
              default:
                throw new InvalidMessageException();
            }
          }

          @Override
          protected void replied(Composite reply) {
            sc.log("replied with: " + reply.toString());
          }

          @Override
          protected void dispatching(Composite request) {
            sc.log("dispatching: " + request.toString());
          }

          @Override
          protected void failed(Exception e) {
            StringWriter writer = new StringWriter();
            try (PrintWriter pw = new PrintWriter(writer)) {
              sc.log("Failed to write data: " + e.getMessage());
            }
            sc.log(writer.toString());
          }
        };
    sc.setAttribute(Const.DISPATCHER_ATTRIBUTE, dispatcher);

    // create tables
    RvsDbManager manager = new RvsDbManager();
    manager.createTables(ds);
    manager.createAllowListDenyListTables(ds);
    manager.importAllowListKeyHash(ds);
    manager.importDenyListKeyHash(ds);
    manager.importGuidFromDenyList(ds);
  }

  @Override
  public void contextDestroyed(ServletContextEvent sce) {}

  private To0ServerService createTo0Service(CryptoService cs, DataSource ds) {
    return new To0ServerService() {
      private To0ServerStorage storage;

      @Override
      public To0ServerStorage getStorage() {
        if (storage == null) {
          storage = new To0AllowListDenyListDbStorage(cs, ds);
        }
        return storage;
      }

      @Override
      public CryptoService getCryptoService() {
        return cs;
      }
    };
  }

  private To1ServerService createTo1Service(CryptoService cs, DataSource ds) {
    return new To1ServerService() {
      private To1ServerStorage storage;

      @Override
      public To1ServerStorage getStorage() {
        if (storage == null) {
          storage = new To1DbStorage(cs, ds);
        }
        return storage;
      }

      @Override
      public CryptoService getCryptoService() {
        return cs;
      }
    };
  }
}
