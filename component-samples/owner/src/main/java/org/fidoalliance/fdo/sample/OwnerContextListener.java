// Copyright 2020 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fidoalliance.fdo.sample;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.sql.DataSource;

import org.apache.commons.dbcp2.BasicDataSource;
import org.fidoalliance.fdo.loggingutils.LoggerService;
import org.fidoalliance.fdo.protocol.Composite;
import org.fidoalliance.fdo.protocol.Const;
import org.fidoalliance.fdo.protocol.CryptoService;
import org.fidoalliance.fdo.protocol.KeyResolver;
import org.fidoalliance.fdo.protocol.MessageDispatcher;
import org.fidoalliance.fdo.protocol.MessagingService;
import org.fidoalliance.fdo.protocol.To2ServerService;
import org.fidoalliance.fdo.protocol.To2ServerStorage;
import org.fidoalliance.fdo.protocol.epid.EpidUtils;
import org.fidoalliance.fdo.protocol.ondie.OnDieCache;
import org.fidoalliance.fdo.protocol.ondie.OnDieService;
import org.fidoalliance.fdo.storage.OwnerDbManager;
import org.fidoalliance.fdo.storage.OwnerDbStorage;
import org.fidoalliance.fdo.storage.OwnerDbTo0Util;

/**
 * TO2 Servlet Context Listener.
 */
public class OwnerContextListener implements ServletContextListener {

  private static final LoggerService logger = new LoggerService(OwnerContextListener.class);

  private KeyResolver resolver;
  private ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
  private ExecutorService to0ExecutorService = Executors.newCachedThreadPool();

  @Override
  public void contextInitialized(ServletContextEvent sce) {
    BasicDataSource ds = new BasicDataSource();

    ServletContext sc = sce.getServletContext();
    ds.setUrl(sc.getInitParameter(OwnerAppSettings.DB_URL));
    ds.setDriverClassName(OwnerAppSettings.H2_DRIVER);
    ds.setUsername(sc.getInitParameter(OwnerAppSettings.DB_USER));
    ds.setPassword(sc.getInitParameter(OwnerAppSettings.DB_PWD));

    logger.info(ds.getUrl());

    ds.setMinIdle(5);
    ds.setMaxIdle(10);
    ds.setMaxOpenPreparedStatements(100);

    CryptoService cs = new CryptoService();
    String epidTestMode = sc.getInitParameter(OwnerAppSettings.EPID_TEST_MODE);
    if (null != epidTestMode && Boolean.valueOf(epidTestMode)) {
      cs.setEpidTestMode();
      logger.warn("*** WARNING ***");
      logger.warn("EPID Test mode enabled. This should NOT be enabled in production deployment.");
    }
    try {
      String epidUrl = sc.getInitParameter(OwnerAppSettings.EPID_URL);
      if (null != epidUrl) {
        EpidUtils.setEpidOnlineUrl(epidUrl);
      }
    } catch (IllegalArgumentException e) {
      logger.info("EPID URL not set. Default URL will be used: "
              + EpidUtils.getEpidOnlineUrl().toString());
    }

    sc.setAttribute("datasource", ds);
    sc.setAttribute("cryptoservice", cs);

    // To maintain backwards compatibility with installation without
    // any OnDie settings or installations that do not wish to use
    // OnDie we will check if the one required setting is present.
    // If not then the ods object is set to null and operation should
    // proceed without error. If an OnDie operation is attempted then
    // an error will occur at that time and the user will need to
    // correct their configuration.
    OnDieService initialOds = null;
    if (sc.getInitParameter(OwnerAppSettings.ONDIE_CACHEDIR) != null
            && !sc.getInitParameter(OwnerAppSettings.ONDIE_CACHEDIR).isEmpty()) {

      try {
        OnDieCache odc = new OnDieCache(
                URI.create(sc.getInitParameter(OwnerAppSettings.ONDIE_CACHEDIR)),
                sc.getInitParameter(OwnerAppSettings.ONDIE_AUTOUPDATE).toLowerCase().equals("true"),
                sc.getInitParameter(OwnerAppSettings.ONDIE_ZIP_ARTIFACT),
                null);

        odc.initializeCache();

        initialOds = new OnDieService(odc,
                sc.getInitParameter(OwnerAppSettings.ONDIE_CHECK_REVOCATIONS)
                        .toLowerCase().equals("true"));

      } catch (Exception ex) {
        throw new RuntimeException("OnDie initialization error: " + ex.getMessage());
      }

    }
    final OnDieService ods = initialOds;

    resolver = new OwnerKeyResolver(sc.getInitParameter(OwnerAppSettings.OWNER_KEYSTORE),
        sc.getInitParameter(OwnerAppSettings.OWNER_KEYSTORE_PWD));

    MessageDispatcher dispatcher = new MessageDispatcher() {
      @Override
      protected MessagingService getMessagingService(Composite request) {
        return createTo2Service(cs, ds, ods);
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
    // create tables
    OwnerDbManager manager = new OwnerDbManager();
    manager.createTables(ds);
    try {
      final String ownerKeysPem = Files.readString(Paths.get(
              sc.getInitParameter(OwnerAppSettings.OWNER_PUB_KEY_PATH)));
      manager.addCustomer(ds, 1, "owner", ownerKeysPem);
    } catch (IOException e) {
      logger.warn("No default public keys found for customer 'owner'");
    }
    try {
      final String owner2KeysPem = Files.readString(Paths.get(
              sc.getInitParameter(OwnerAppSettings.OWNER2_PUB_KEY_PATH)));
      manager.addCustomer(ds, 2, "owner2", owner2KeysPem);
    } catch (IOException e) {
      logger.warn("No default public keys found for customer 'owner2'");
    }
    manager.loadTo2Settings(ds);

    try {
      // schedule devices for TO0 only if the flag is set
      if (Boolean.valueOf(sc.getInitParameter(OwnerAppSettings.TO0_SCHEDULING_ENABLED))) {
        scheduler.scheduleWithFixedDelay(new Runnable() {
          @Override
          public void run() {
            try {
              OwnerDbTo0Util to0Util = new OwnerDbTo0Util();
              List<UUID> uuids = to0Util.fetchDevicesForTo0(ds);
              logger.info("Scheduling UUIDs for TO0: " + uuids.toString());
              for (UUID guid : uuids) {
                CompletableFuture.runAsync(() -> scheduleTo0(sc, ds, guid, to0Util),
                        to0ExecutorService);
              }
            } catch (Exception e) {
              logger.warn("TO0 scheduler failed. Reason: " + e.getMessage());
            }
          }
        }, 5, Integer.parseInt(
                sc.getInitParameter(OwnerAppSettings.TO0_SCHEDULING_INTREVAL)), TimeUnit.SECONDS);
      }
    } catch (Exception e) {
      logger.error("Error while scheduling TO0.");
    }
  }

  @Override
  public void contextDestroyed(ServletContextEvent sce) {
  }

  private To2ServerService createTo2Service(CryptoService cs,
                                            DataSource ds,
                                            OnDieService ods) {
    return new To2ServerService() {
      private To2ServerStorage storage;

      @Override
      public To2ServerStorage getStorage() {
        if (storage == null) {
          storage = new OwnerDbStorage(cs, ds, resolver, ods);
        }
        return storage;
      }

      @Override
      public CryptoService getCryptoService() {
        return cs;
      }
    };
  }

  private void scheduleTo0(ServletContext sc, DataSource ds, UUID guid, OwnerDbTo0Util to0Util) {
    try {
      OwnerTo0Client to0Client = new OwnerTo0Client(new CryptoService(), ds,
          new OwnerKeyResolver(sc.getInitParameter(OwnerAppSettings.OWNER_KEYSTORE),
              sc.getInitParameter(OwnerAppSettings.OWNER_KEYSTORE_PWD)),
              guid, to0Util);
      to0Client.setRvBlob(sc.getInitParameter(OwnerAppSettings.TO0_RV_BLOB));
      to0Client.run();
    } catch (Exception e) {
      logger.error("Error during TO0 for GUID: " + guid.toString());
      throw new RuntimeException(e);
    }
  }
}
