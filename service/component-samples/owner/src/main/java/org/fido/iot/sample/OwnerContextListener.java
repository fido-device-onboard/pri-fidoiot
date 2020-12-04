// Copyright 2020 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fido.iot.sample;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
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
import org.fido.iot.storage.OwnerDbManager;
import org.fido.iot.storage.OwnerDbStorage;
import org.fido.iot.storage.OwnerDbTo0Util;

/**
 * TO2 Servlet Context Listener.
 */
public class OwnerContextListener implements ServletContextListener {

  private static final String VOUCHER = ""
      + "8486186450f0956089c0df4c349c61f460457e87eb81858205696c6f63616c686f73748203191f68820c01820"
      + "2447f0000018204191f686a44656d6f446576696365830d0258402c02709032b3fc1696ab55b1ecf8e44795b9"
      + "2cb21b6a681265e54d525c8533fb74b0c0310166ef11b0f32aef76e135f86acdd65633267de932b31df43e50c"
      + "62582085820744c9e7af744e7408e288d27017d0904605eed5bc07e7c1771404e569bdfe3e6820558205cd7dd"
      + "9fd13c8f681dc42208f4770ccbe10b6beb178cb595bacd53bc3f61e3b18259017a308201763082011d0209008"
      + "da355b7e71c51f5300a06082a8648ce3d040302300d310b300906035504030c0243413020170d313931313232"
      + "3135353430315a180f32303534313131333135353430315a3078310b3009060355040613025553310f300d060"
      + "35504080c064f7265676f6e3112301006035504070c0948696c6c73626f726f310e300c060355040a0c05496e"
      + "74656c311d301b060355040b0c14446576696365204d616e75666163747572696e673115301306035504030c0"
      + "c44656d6f44657669636532373059301306072a8648ce3d020106082a8648ce3d03010703420004a582f072ec"
      + "6a4746d8e7c974558a6c4ec694ce91420a978dddb995d201e9e712c7330bc1151c8eb656313745dac7c7040ec"
      + "7ef22e549621632b5b3863e467c98300a06082a8648ce3d040302034700304402204386077f39aee794f7e48e"
      + "af04ff4c18822a8c306994ad4ad75ccab5aef7478c022073ce183429452662c29d4c4d1b750f63167e85c9cb0"
      + "ef7b2581a986ec9282bf1590126308201223081c9a003020102020900a4d303ae980f53f1300a06082a8648ce"
      + "3d040302300d310b300906035504030c0243413020170d3139303432343134343634375a180f3230353430343"
      + "1353134343634375a300d310b300906035504030c0243413059301306072a8648ce3d020106082a8648ce3d03"
      + "0107034200042c02709032b3fc1696ab55b1ecf8e44795b92cb21b6a681265e54d525c8533fb74b0c0310166e"
      + "f11b0f32aef76e135f86acdd65633267de932b31df43e50c625a310300e300c0603551d13040530030101ff30"
      + "0a06082a8648ce3d0403020348003045022100a5419b823613d24eb701e440b4f3368be5675ba72461a272bc5"
      + "2eeb96c3e414002204e70d27b631cb6efc26aa0c027e1e53eaef1ec5074203683d1ecbb9de129c6928184a101"
      + "2640588e838208582099713d28d33bb5fa29f77f0da8ff182e5a076670a4ec23244ed504ec6f10fd0f8208582"
      + "082d4659e9dbbc7fac58ad015faf42ac0947ee511d752ab37edc42eb0d969df28830d025840595504d86d062f"
      + "2f2c72600ec90ca1701885fdf4947778bf3a0ed70d286225bd88b1b099491aadd5e935e486de088e73ec11de6"
      + "b61991a068aeb77320f5e603458473045022022acd405ca7c95e8104093becea5d5ddfb25adb55012a1cc7169"
      + "ccd114977ff50221009e9cdd0815358d35d543bae8362f02ddced995ab1ff96115d423c76313ccea2c";

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
    
    resolver = new OwnerKeyResolver(sc.getInitParameter(OwnerAppSettings.OWNER_KEYSTORE),
        sc.getInitParameter(OwnerAppSettings.OWNER_KEYSTORE_PWD));
    
    MessageDispatcher dispatcher = new MessageDispatcher() {
      @Override
      protected MessagingService getMessagingService(Composite request) {
        return createTo2Service(cs, ds);
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
    manager.importVoucher(ds, Composite.fromObject(VOUCHER));
    // do a clean-up to avoid exception due to stale data.
    manager.removeSviFromDevice(ds,
        Composite.fromObject(VOUCHER).getAsComposite(Const.OV_HEADER).getAsUuid(Const.OVH_GUID));
    manager.loadSampleServiceInfo(ds,
        Composite.fromObject(VOUCHER).getAsComposite(Const.OV_HEADER).getAsUuid(Const.OVH_GUID),
        Paths.get(sc.getInitParameter(OwnerAppSettings.SAMPLE_VALUES_PATH)),
        Paths.get(sc.getInitParameter(OwnerAppSettings.SAMPLE_SVI_PATH)));

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

  private To2ServerService createTo2Service(CryptoService cs, DataSource ds) {
    return new To2ServerService() {
      private To2ServerStorage storage;

      @Override
      public To2ServerStorage getStorage() {
        if (storage == null) {
          storage = new OwnerDbStorage(cs, ds, resolver);
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
