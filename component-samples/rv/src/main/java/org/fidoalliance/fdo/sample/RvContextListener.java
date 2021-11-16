// Copyright 2020 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fidoalliance.fdo.sample;

import java.io.PrintWriter;
import java.io.StringWriter;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.sql.DataSource;
import org.apache.commons.dbcp2.BasicDataSource;
import org.fidoalliance.fdo.loggingutils.LoggerService;
import org.fidoalliance.fdo.protocol.Composite;
import org.fidoalliance.fdo.protocol.Const;
import org.fidoalliance.fdo.protocol.CryptoService;
import org.fidoalliance.fdo.protocol.InvalidMessageException;
import org.fidoalliance.fdo.protocol.MessageDispatcher;
import org.fidoalliance.fdo.protocol.MessagingService;
import org.fidoalliance.fdo.protocol.To0ServerService;
import org.fidoalliance.fdo.protocol.To0ServerStorage;
import org.fidoalliance.fdo.protocol.To1ServerService;
import org.fidoalliance.fdo.protocol.To1ServerStorage;
import org.fidoalliance.fdo.protocol.epid.EpidUtils;
import org.fidoalliance.fdo.protocol.ondie.OnDieCache;
import org.fidoalliance.fdo.protocol.ondie.OnDieService;
import org.fidoalliance.fdo.storage.RvsDbManager;
import org.fidoalliance.fdo.storage.To0AllowListDenyListDbStorage;
import org.fidoalliance.fdo.storage.To1DbStorage;

/** Rendezvous servlet Context Listener. */
public class RvContextListener implements ServletContextListener {

  private static final LoggerService logger = new LoggerService(RvContextListener.class);

  @Override
  public void contextInitialized(ServletContextEvent sce) {
    BasicDataSource ds = new BasicDataSource();

    ServletContext sc = sce.getServletContext();
    ds.setUrl(sc.getInitParameter(RvAppSettings.DB_URL));
    ds.setDriverClassName(RvAppSettings.H2_DRIVER);
    ds.setUsername(sc.getInitParameter(RvAppSettings.DB_USER));
    ds.setPassword(sc.getInitParameter(RvAppSettings.DB_PWD));

    logger.info(ds.getUrl());

    ds.setMinIdle(5);
    ds.setMaxIdle(10);
    ds.setMaxOpenPreparedStatements(100);

    final CryptoService cs = new CryptoService();
    String epidTestMode = sc.getInitParameter(RvAppSettings.EPID_TEST_MODE);
    if (null != epidTestMode && Boolean.valueOf(epidTestMode)) {
      cs.setEpidTestMode();
      logger.warn("*** WARNING ***");
      logger.warn("EPID Test mode enabled. This should NOT be enabled in production deployment.");
    }
    try {
      String epidUrl = sc.getInitParameter(RvAppSettings.EPID_URL);
      if (null != epidUrl) {
        EpidUtils.setEpidOnlineUrl(epidUrl);
      }
    } catch (IllegalArgumentException e) {
      logger.info("EPID URL not set. Default URL will be used: "
              + EpidUtils.getEpidOnlineUrl().toString());
    }

    sc.setAttribute("datasource", ds);
    sc.setAttribute("cryptoservice", cs);

    // OnDieService is only used to validate signatures and not revocations
    // so initialization does not require onDieCache.
    final OnDieService ods = new OnDieService(null, false);

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
                return createTo1Service(cs, ds, ods);
              default:
                throw new InvalidMessageException();
            }
          }

          @Override
          protected void replied(Composite reply) {
            String msgId = reply.getAsNumber(Const.SM_MSG_ID).toString();
            logger.debug("msg/" + msgId + ": " + reply.toString());
          }

          @Override
          protected void dispatching(Composite request) {
            String msgId = request.getAsNumber(Const.SM_MSG_ID).toString();
            logger.debug("msg/" + msgId + ": " + request.toString());
          }
        };
    sc.setAttribute(Const.DISPATCHER_ATTRIBUTE, dispatcher);

    final String configFilePath = "config.properties";
    // create tables
    RvsDbManager manager = new RvsDbManager();
    manager.createTables(ds);
    manager.createAllowListDenyListTables(ds);
    manager.importAllowListKeyHash(ds, configFilePath);
    manager.importDenyListKeyHash(ds, configFilePath);
    manager.importGuidFromDenyList(ds, configFilePath);
  }

  @Override
  public void contextDestroyed(ServletContextEvent sce) {}

  private To0ServerService createTo0Service(CryptoService cs,
                                            DataSource ds) {
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

  private To1ServerService createTo1Service(
          CryptoService cs,
          DataSource ds,
          OnDieService ods) {
    return new To1ServerService() {
      private To1ServerStorage storage;

      @Override
      public To1ServerStorage getStorage() {
        if (storage == null) {
          storage = new To1DbStorage(cs, ds, ods);
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
