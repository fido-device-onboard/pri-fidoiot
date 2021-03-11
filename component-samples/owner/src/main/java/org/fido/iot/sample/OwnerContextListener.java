// Copyright 2020 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fido.iot.sample;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URI;
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
import org.fido.iot.protocol.Composite;
import org.fido.iot.protocol.Const;
import org.fido.iot.protocol.CryptoService;
import org.fido.iot.protocol.KeyResolver;
import org.fido.iot.protocol.MessageDispatcher;
import org.fido.iot.protocol.MessagingService;
import org.fido.iot.protocol.To2ServerService;
import org.fido.iot.protocol.To2ServerStorage;
import org.fido.iot.protocol.epid.EpidUtils;
import org.fido.iot.protocol.ondie.OnDieCache;
import org.fido.iot.protocol.ondie.OnDieService;
import org.fido.iot.storage.OwnerDbManager;
import org.fido.iot.storage.OwnerDbStorage;
import org.fido.iot.storage.OwnerDbTo0Util;

/**
 * TO2 Servlet Context Listener.
 */
public class OwnerContextListener implements ServletContextListener {

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

    sc.log(ds.getUrl());

    ds.setMinIdle(5);
    ds.setMaxIdle(10);
    ds.setMaxOpenPreparedStatements(100);

    CryptoService cs = new CryptoService();
    String epidTestMode = sc.getInitParameter(OwnerAppSettings.EPID_TEST_MODE);
    if (null != epidTestMode && Boolean.valueOf(epidTestMode)) {
      cs.setEpidTestMode();
      sc.log("EPID Test mode enabled.");
    }
    String epidUrl = sc.getInitParameter(OwnerAppSettings.EPID_URL);
    if (null != epidUrl) {
      EpidUtils.setEpidOnlineUrl(epidUrl);
    } else {
      sc.log("EPID URL not set. Default URL will be used: "
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
    OwnerDbManager manager = new OwnerDbManager();
    manager.createTables(ds);
    manager.loadSampleServiceInfo(ds,
        Paths.get(sc.getInitParameter(OwnerAppSettings.SAMPLE_VALUES_PATH)));
    manager.loadSampleDeviceTypeSviStringMapping(
        ds, Paths.get(sc.getInitParameter(OwnerAppSettings.SAMPLE_SVI_PATH)));
    manager.loadTo2Settings(ds);

    // schedule devices for TO0 only if the flag is set
    if (Boolean.valueOf(sc.getInitParameter(OwnerAppSettings.TO0_SCHEDULING_ENABLED))) {
      scheduler.scheduleWithFixedDelay(new Runnable() {
        @Override
        public void run() {
          try {
            OwnerDbTo0Util to0Util = new OwnerDbTo0Util();
            List<UUID> uuids = to0Util.fetchDevicesForTo0(ds);
            sc.log("Scheduling UUIDs for TO0: " + uuids.toString());
            for (UUID guid : uuids) {
              CompletableFuture.runAsync(() -> scheduleTo0(sc, ds, guid, to0Util),
                  to0ExecutorService);
            }
          } catch (Exception e) {
            sc.log(e.getMessage());
          }
        }
      }, 5, Integer.parseInt(
          sc.getInitParameter(OwnerAppSettings.TO0_SCHEDULING_INTREVAL)), TimeUnit.SECONDS);
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
    } catch (IOException | NoSuchAlgorithmException | InterruptedException e) {
      sc.log("Error during TO0 for GUID: " + guid.toString());
      throw new RuntimeException(e);
    }
  }
}
