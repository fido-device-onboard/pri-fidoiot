// Copyright 2020 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fidoalliance.fdo.sample;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URI;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.sql.DataSource;
import org.apache.commons.dbcp2.BasicDataSource;
import org.fidoalliance.fdo.certutils.PemLoader;
import org.fidoalliance.fdo.loggingutils.LoggerService;
import org.fidoalliance.fdo.protocol.CloseableKey;
import org.fidoalliance.fdo.protocol.Composite;
import org.fidoalliance.fdo.protocol.Const;
import org.fidoalliance.fdo.protocol.CryptoService;
import org.fidoalliance.fdo.protocol.DiServerService;
import org.fidoalliance.fdo.protocol.DiServerStorage;
import org.fidoalliance.fdo.protocol.DispatchException;
import org.fidoalliance.fdo.protocol.InvalidMessageException;
import org.fidoalliance.fdo.protocol.KeyResolver;
import org.fidoalliance.fdo.protocol.MessageDispatcher;
import org.fidoalliance.fdo.protocol.MessagingService;
import org.fidoalliance.fdo.protocol.RendezvousBlobDecoder;
import org.fidoalliance.fdo.protocol.RendezvousInfoDecoder;
import org.fidoalliance.fdo.protocol.To0ServerService;
import org.fidoalliance.fdo.protocol.To0ServerStorage;
import org.fidoalliance.fdo.protocol.To1ServerService;
import org.fidoalliance.fdo.protocol.To1ServerStorage;
import org.fidoalliance.fdo.protocol.To2ServerService;
import org.fidoalliance.fdo.protocol.To2ServerStorage;
import org.fidoalliance.fdo.protocol.VoucherExtensionService;
import org.fidoalliance.fdo.protocol.epid.EpidUtils;
import org.fidoalliance.fdo.protocol.ondie.OnDieCache;
import org.fidoalliance.fdo.protocol.ondie.OnDieService;
import org.fidoalliance.fdo.storage.CertificateResolver;
import org.fidoalliance.fdo.storage.DiDbManager;
import org.fidoalliance.fdo.storage.DiDbStorage;
import org.fidoalliance.fdo.storage.OwnerDbManager;
import org.fidoalliance.fdo.storage.OwnerDbStorage;
import org.fidoalliance.fdo.storage.OwnerDbTo0Util;
import org.fidoalliance.fdo.storage.RvsDbManager;
import org.fidoalliance.fdo.storage.To0DbStorage;
import org.fidoalliance.fdo.storage.To1DbStorage;


public class AioContextListener implements ServletContextListener {

  private KeyResolver ownResolver;
  private CertificateResolver certResolver;
  private String newDevicePath;
  private boolean autoInjectBlob;
  private ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
  private static final LoggerService logger = new LoggerService(AioContextListener.class);

  @Override
  public void contextInitialized(ServletContextEvent sce) {
    BasicDataSource ds = new BasicDataSource();

    ServletContext sc = sce.getServletContext();

    ds.setUrl(sc.getInitParameter(AioAppSettings.DB_URL));
    ds.setDriverClassName(sc.getInitParameter(AioAppSettings.DB_DRIVER));
    ds.setUsername(sc.getInitParameter(AioAppSettings.DB_USER));
    ds.setPassword(sc.getInitParameter(AioAppSettings.DB_PWD));

    logger.info(ds.getUrl());

    ds.setMinIdle(5);
    ds.setMaxIdle(10);
    ds.setMaxOpenPreparedStatements(100);

    CryptoService cs = new CryptoService();
    String epidTestMode = sc.getInitParameter(AioAppSettings.EPID_TEST_MODE);
    if (null != epidTestMode && Boolean.valueOf(epidTestMode)) {
      cs.setEpidTestMode();
      logger.warn("*** WARNING ***");
      logger.warn("EPID Test mode enabled. This should NOT be enabled in production deployment.");
    }
    String epidUrl = sc.getInitParameter(AioAppSettings.EPID_URL);
    if (null != epidUrl) {
      EpidUtils.setEpidOnlineUrl(epidUrl);
    } else {
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
    if (sc.getInitParameter(AioAppSettings.ONDIE_CACHEDIR) != null
        && !sc.getInitParameter(AioAppSettings.ONDIE_CACHEDIR).isEmpty()) {

      try {
        OnDieCache odc = new OnDieCache(
            URI.create(sc.getInitParameter(AioAppSettings.ONDIE_CACHEDIR)),
            sc.getInitParameter(AioAppSettings.ONDIE_AUTOUPDATE).toLowerCase().equals("true"),
            sc.getInitParameter(AioAppSettings.ONDIE_ZIP_ARTIFACT),
            null);

        odc.initializeCache();

        initialOds = new OnDieService(odc,
            sc.getInitParameter(AioAppSettings.ONDIE_CHECK_REVOCATIONS)
                .toLowerCase().equals("true"));

      } catch (Exception ex) {
        throw new RuntimeException("OnDie initialization error: " + ex.getMessage());
      }

    }
    final OnDieService ods = initialOds;

    ownResolver = new AioOwnerKeyResolver(
        sc.getInitParameter(AioAppSettings.OWNER_KEYSTORE),
        sc.getInitParameter(AioAppSettings.OWNER_KEYSTORE_TYPE),
        sc.getInitParameter(AioAppSettings.OWNER_KEYSTORE_PWD));

    certResolver = new AioCertificateResolver(cs,
        sc.getInitParameter(AioAppSettings.MANUFACTURER_KEYSTORE),
        sc.getInitParameter(AioAppSettings.MANUFACTURER_KEYSTORE_TYPE),
        sc.getInitParameter(AioAppSettings.MANUFACTURER_KEYSTORE_PWD)
    );

    newDevicePath = sc.getInitParameter(AioAppSettings.DB_NEW_DEVICE_SQL);
    autoInjectBlob = Boolean.parseBoolean(sc.getInitParameter(AioAppSettings.AUTO_INJECT_BLOB));

    MessageDispatcher dispatcher = new MessageDispatcher() {
      @Override
      protected MessagingService getMessagingService(Composite request) {
        int msgId = request.getAsNumber(Const.SECOND_KEY).intValue();
        switch (msgId) {
          case Const.TO2_HELLO_DEVICE:
          case Const.TO2_GET_OVNEXT_ENTRY:
          case Const.TO2_PROVE_DEVICE:
          case Const.TO2_DEVICE_SERVICE_INFO_READY:
          case Const.TO2_DEVICE_SERVICE_INFO:
          case Const.TO2_DONE:
            return createTo2Service(cs, ds, ods);
          case Const.TO0_HELLO:
          case Const.TO0_OWNER_SIGN:
            return createTo0Service(cs, ds);
          case Const.TO1_HELLO_RV:
          case Const.TO1_PROVE_TO_RV:
            return createTo1Service(cs, ds, ods);
          case Const.DI_APP_START:
          case Const.DI_SET_HMAC:
            return createDiService(cs, ds, ods);
          case Const.ERROR:
            msgId = request.getAsNumber(Const.SM_MSG_ID).intValue();
            switch (msgId) {
              case Const.TO2_HELLO_DEVICE:
              case Const.TO2_GET_OVNEXT_ENTRY:
              case Const.TO2_PROVE_DEVICE:
              case Const.TO2_DEVICE_SERVICE_INFO_READY:
              case Const.TO2_DEVICE_SERVICE_INFO:
              case Const.TO2_DONE:
                return createTo2Service(cs, ds, ods);
              case Const.TO0_HELLO:
              case Const.TO0_OWNER_SIGN:
                return createTo0Service(cs, ds);
              case Const.TO1_HELLO_RV:
              case Const.TO1_PROVE_TO_RV:
                return createTo1Service(cs, ds, ods);
              case Const.DI_APP_START:
              case Const.DI_SET_HMAC:
                return createDiService(cs, ds, ods);
              default:
                throw new RuntimeException(new InvalidMessageException());
            }
          default:
            throw new RuntimeException(new InvalidMessageException());
        }
      }

      @Override
      protected void replied(Composite reply) {
        String msgId = reply.getAsNumber(Const.SM_MSG_ID).toString();
        logger.debug("msg/" + msgId + ": " + reply.toString());
      }

      @Override
      protected void dispatched(Composite request, Composite reply) {
        if (reply.getAsNumber(Const.SM_MSG_ID).intValue() == Const.DI_DONE) {
          String sessionID = request.getAsComposite(Const.SM_PROTOCOL_INFO)
              .getAsString(Const.PI_TOKEN);
          if (autoInjectBlob) {
            newDevice(sessionID, ds, cs, certResolver);
          }
        }
      }

      @Override
      protected void dispatching(Composite request) {
        String msgId = request.getAsNumber(Const.SM_MSG_ID).toString();
        logger.debug("msg/" + msgId + ": " + request.toString());
      }
    };

    sc.setAttribute(Const.DISPATCHER_ATTRIBUTE, dispatcher);
    // create tables
    OwnerDbManager ownManager = new OwnerDbManager();
    ownManager.createTables(ds);

    ownManager.loadTo2Settings(ds);
    RvsDbManager rvsManager = new RvsDbManager();
    rvsManager.createTables(ds);
    DiDbManager disManager = new DiDbManager();

    disManager.createTables(ds);
    AioDbManager aioDbManager = new AioDbManager();
    aioDbManager.createTables(ds);
    aioDbManager.loadInitScript(ds, sc.getInitParameter(AioAppSettings.DB_INIT_SQL));
    aioDbManager.updateTo0RvBlob(ds, sc.getInitParameter(AioAppSettings.TO0_RV_BLOB));

    Consumer<String> injector = a -> newDevice(a, ds, cs, certResolver);
    sc.setAttribute(AioRegisterBlobServlet.BLOB_INJECTOR, injector);

    // schedule session cleanup scheduler
    scheduler.scheduleWithFixedDelay(new Runnable() {
      @Override
      public void run() {
        try {
          new AioDbManager().removeSessions(ds);
        } catch (Exception e) {
          logger.warn("Failed to setup AIO. Reason: " + e.getMessage());
        }
      }
    }, 5, Integer.parseInt(
        sc.getInitParameter(AioAppSettings.DB_SESSION_CHECK_INTERVAL)), TimeUnit.SECONDS);
  }


  @Override
  public void contextDestroyed(ServletContextEvent sce) {
  }

  private To0ServerService createTo0Service(CryptoService cs, DataSource ds) {
    return new To0ServerService() {
      private To0ServerStorage storage;

      @Override
      public To0ServerStorage getStorage() {
        if (storage == null) {
          storage = new To0DbStorage(cs, ds);
        }
        return storage;
      }

      @Override
      public CryptoService getCryptoService() {
        return cs;
      }
    };
  }


  private DiServerService createDiService(CryptoService cs, DataSource ds, OnDieService ods) {
    return new DiServerService() {
      private DiServerStorage storage;

      @Override
      public DiServerStorage getStorage() {
        if (storage == null) {
          storage = new DiDbStorage(cs, ds, certResolver, ods);
        }
        return storage;
      }

      @Override
      public CryptoService getCryptoService() {
        return cs;
      }
    };
  }

  private To1ServerService createTo1Service(CryptoService cs, DataSource ds, OnDieService ods) {
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


  private To2ServerService createTo2Service(CryptoService cs,
      DataSource ds,
      OnDieService ods) {
    return new To2ServerService() {
      private To2ServerStorage storage;

      @Override
      public To2ServerStorage getStorage() {
        if (storage == null) {
          storage = new OwnerDbStorage(cs, ds, ownResolver, ods);
        }
        return storage;
      }

      @Override
      public CryptoService getCryptoService() {
        return cs;
      }
    };
  }


  private void newDevice(String sessionId, DataSource ds,
      CryptoService cs, CertificateResolver resolver) {

    Composite voucher = Const.EMPTY_MESSAGE;
    String ownerKeys = "";
    String guid = "";
    String sql = "SELECT MT_DEVICES.VOUCHER, MT_CUSTOMERS.KEYS , MT_DEVICES.GUID "
        + "FROM MT_DEVICES "
        + "LEFT JOIN MT_CUSTOMERS "
        + "ON MT_CUSTOMERS.CUSTOMER_ID=MT_DEVICES.CUSTOMER_ID "
        + "WHERE MT_DEVICES.GUID = (SELECT GUID FROM "
        + "DI_SESSIONS WHERE SESSION_ID = ?);";

    try (Connection conn = ds.getConnection();
        PreparedStatement pstmt = conn.prepareStatement(sql)) {
      pstmt.setString(1, sessionId);
      try (ResultSet rs = pstmt.executeQuery()) {
        while (rs.next()) {
          voucher = Composite.fromObject(rs.getBytes(1));
          ownerKeys = rs.getString(2);
          guid = rs.getString(3);
        }
      }
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }

    //now extend the voucher
    List<PublicKey> certs =
        PemLoader.loadPublicKeys(ownerKeys);

    Composite ovh = voucher.getAsComposite(Const.OV_HEADER);
    Composite mfgPub = ovh.getAsComposite(Const.OVH_PUB_KEY);

    int keyType = mfgPub.getAsNumber(Const.PK_TYPE).intValue();
    Certificate[] issuerChain =
        resolver.getCertChain(keyType);

    PublicKey ownerPub = null;
    for (PublicKey pub : certs) {
      int ownerType = cs.getPublicKeyType(pub);
      if (ownerType == keyType) {
        ownerPub = pub;
        break;
      }
    }

    VoucherExtensionService vse = new VoucherExtensionService(voucher, cs);
    try (CloseableKey signer = resolver.getPrivateKey(issuerChain[0])) {
      vse.add(signer.get(), ownerPub);
      OwnerDbManager ownManager = new OwnerDbManager();
      ownManager.importVoucher(ds, voucher);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

    //create a rv blob
    Composite encodedKey = cs.getOwnerPublicKey(voucher);
    PublicKey pubKey = cs.decode(encodedKey);
    String fingerPrint = cs.getFingerPrint(pubKey);
    pubKey = cs.getDevicePublicKey(voucher);
    Composite deviceX509 = Composite.newArray();
    if (pubKey != null) {
      deviceX509 = cs.encode(pubKey, Const.PK_ENC_X509);
    }

    byte[] singedBlob = generateSignedBlob(ds, cs, voucher);

    AioDbManager aioDbManager = new AioDbManager();

    aioDbManager.storeRedirectBlob(guid, singedBlob, fingerPrint, deviceX509.toBytes(), ds);
    aioDbManager.loadNewDeviceScript(ds, newDevicePath, guid);

    logger.info("TO0 completed for GUID: " + guid);
  }

  private byte[] generateSignedBlob(DataSource ds, CryptoService cs, Composite voucher) {

    Composite ownerBlob = RendezvousBlobDecoder.decode(new AioDbManager().loadRvBlob(ds));
    Composite to01Payload = Composite.newArray()
        .set(Const.TO1D_RV, ownerBlob); //unsigned blob

    //to0d does not matter for direct injection
    //we will just generate the hash to make it parse
    Composite to0d = Composite.newArray()
        .set(Const.TO0D_VOUCHER, voucher)
        .set(Const.TO0D_WAIT_SECONDS, Integer.MAX_VALUE)
        .set(Const.TO0D_NONCETO0SIGN, cs.getRandomBytes(Const.NONCE16_SIZE));

    Composite ovh = voucher.getAsComposite(Const.OV_HEADER);
    PublicKey mfgPublic = cs.decode(ovh.getAsComposite(Const.OVH_PUB_KEY));
    int hashType = cs.getCompatibleHashType(mfgPublic);
    Composite hash = cs.hash(hashType, to0d.toBytes());

    //hash does not matter for direct injection
    to01Payload.set(Const.TO1D_TO0D_HASH, hash);

    Composite pubEncKey = cs.getOwnerPublicKey(voucher);
    PublicKey ownerPublicKey = cs.decode(pubEncKey);
    Composite singedBlob = null;
    try (CloseableKey key = new CloseableKey(
        ownResolver.getKey(ownerPublicKey))) {
      singedBlob = cs.sign(
          key.get(), to01Payload.toBytes(), cs.getCoseAlgorithm(ownerPublicKey));
    } catch (IOException e) {
      throw new DispatchException(e);
    }

    return singedBlob.toBytes();
  }

}
