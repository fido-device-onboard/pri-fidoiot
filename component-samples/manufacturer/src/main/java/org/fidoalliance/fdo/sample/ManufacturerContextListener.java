// Copyright 2020 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fidoalliance.fdo.sample;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.util.Arrays;
import java.util.Iterator;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.sql.DataSource;
import org.apache.commons.dbcp2.BasicDataSource;
import org.fidoalliance.fdo.loggingutils.LoggerService;
import org.fidoalliance.fdo.protocol.CloseableKey;
import org.fidoalliance.fdo.protocol.Composite;
import org.fidoalliance.fdo.protocol.Const;
import org.fidoalliance.fdo.protocol.CryptoService;
import org.fidoalliance.fdo.protocol.DiServerService;
import org.fidoalliance.fdo.protocol.DiServerStorage;
import org.fidoalliance.fdo.protocol.MessageDispatcher;
import org.fidoalliance.fdo.protocol.MessagingService;
import org.fidoalliance.fdo.protocol.ondie.OnDieCache;
import org.fidoalliance.fdo.protocol.ondie.OnDieService;
import org.fidoalliance.fdo.storage.CertificateResolver;
import org.fidoalliance.fdo.storage.DiDbManager;
import org.fidoalliance.fdo.storage.DiDbStorage;

/**
 * Device Initialization servlet Context Listener.
 */
public class ManufacturerContextListener implements ServletContextListener {

  CertificateResolver keyResolver;
  private static KeyStore mfgKeyStore;
  private static LoggerService logger = new LoggerService(ManufacturerContextListener.class);

  @Override
  public void contextInitialized(ServletContextEvent sce) {
    BasicDataSource ds = new BasicDataSource();

    ServletContext sc = sce.getServletContext();
    ds.setUrl(sc.getInitParameter(ManufacturerAppSettings.DB_URL));
    ds.setDriverClassName(ManufacturerAppSettings.H2_DRIVER);
    ds.setUsername(sc.getInitParameter(ManufacturerAppSettings.DB_USER));
    ds.setPassword(sc.getInitParameter(ManufacturerAppSettings.DB_PWD));

    logger.info(ds.getUrl());

    ds.setMinIdle(5);
    ds.setMaxIdle(10);
    ds.setMaxOpenPreparedStatements(100);

    CryptoService cs = new CryptoService();

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
    if (sc.getInitParameter(ManufacturerAppSettings.ONDIE_CACHEDIR) != null
            && !sc.getInitParameter(ManufacturerAppSettings.ONDIE_CACHEDIR).isEmpty()) {

      try {
        OnDieCache odc = new OnDieCache(
                URI.create(sc.getInitParameter(ManufacturerAppSettings.ONDIE_CACHEDIR)),
                sc.getInitParameter(ManufacturerAppSettings.ONDIE_AUTOUPDATE)
                        .toLowerCase().equals("true"),
                sc.getInitParameter(ManufacturerAppSettings.ONDIE_SOURCE_URLS),
                null);

        odc.initializeCache();

        initialOds = new OnDieService(odc,
                sc.getInitParameter(ManufacturerAppSettings.ONDIE_CHECK_REVOCATIONS)
                        .toLowerCase().equals("true"));

      } catch (Exception ex) {
        throw new RuntimeException("OnDie initialization error: " + ex.getMessage());
      }
    }
    final OnDieService ods = initialOds;

    initManufacturerKeystore(
        sc.getInitParameter(ManufacturerAppSettings.MFG_KEYSTORE),
        sc.getInitParameter(ManufacturerAppSettings.MFG_KEYSTORE_PWD));
    keyResolver = new CertificateResolver() {
      @Override
      public CloseableKey getPrivateKey(Certificate cert) {

        if (null != mfgKeyStore && null != cert) {
          try {
            Iterator<String> aliases = mfgKeyStore.aliases().asIterator();
            while (aliases.hasNext()) {
              String alias = aliases.next();
              Certificate certificate = mfgKeyStore.getCertificate(alias);
              if (null == certificate) {
                continue;
              }
              if (Arrays.equals(certificate.getEncoded(), cert.getEncoded())) {
                return new CloseableKey((PrivateKey) mfgKeyStore.getKey(alias,
                    sc.getInitParameter(ManufacturerAppSettings.MFG_KEYSTORE_PWD).toCharArray()));
              }
            }
          } catch (KeyStoreException | UnrecoverableKeyException | NoSuchAlgorithmException
              | CertificateEncodingException e) {
            logger.error("Unable to retrieve Private Key. " + e.getMessage());
          }
        }
        throw new RuntimeException();
      }

      @Override
      public Certificate[] getCertChain(int publicKeyType) {
        String algName;
        switch (publicKeyType) {
          case Const.PK_RSA2048RESTR:
          case Const.PK_RSA3072:
            algName = Const.RSA_ALG_NAME;
            break;
          case Const.PK_SECP256R1:
          case Const.PK_SECP384R1:
            algName = Const.EC_ALG_NAME;
            break;
          default:
            throw new RuntimeException();
        }
        if (null != mfgKeyStore) {
          try {
            Iterator<String> aliases = mfgKeyStore.aliases().asIterator();
            while (aliases.hasNext()) {
              String alias = aliases.next();
              Certificate[] certificateChain = mfgKeyStore.getCertificateChain(alias);
              if (certificateChain != null && certificateChain.length > 0
                  && certificateChain[0].getPublicKey().getAlgorithm().equals(algName)
                  && publicKeyType == cs.getPublicKeyType(certificateChain[0].getPublicKey())) {
                return certificateChain;
              }
            }
          } catch (KeyStoreException e) {
            logger.error("Unable to retrieve Certificate chain. " + e.getMessage());
          }
        }
        throw new RuntimeException();
      }
    };

    MessageDispatcher dispatcher = new MessageDispatcher() {
      @Override
      protected MessagingService getMessagingService(Composite request) {
        return createDiService(cs, ds, ods);
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

      @Override
      protected void failed(Exception e) {
        StringWriter writer = new StringWriter();
        try (PrintWriter pw = new PrintWriter(writer)) {
          logger.warn(e.getMessage());
        }
        logger.warn(writer.toString());
      }
    };
    sc.setAttribute(Const.DISPATCHER_ATTRIBUTE, dispatcher);
    sc.setAttribute("resolver", keyResolver);

    //create tables
    DiDbStorage db = new DiDbStorage(cs, ds, keyResolver, ods);
    DiDbManager manager = new DiDbManager();
    manager.createTables(ds);
    try {
      final String ownerKeysPem = Files.readString(Paths.get(
              sc.getInitParameter(ManufacturerAppSettings.OWNER_PUB_KEY_PATH)));
      manager.addCustomer(ds, 1, "owner", ownerKeysPem);
      manager.setAutoEnroll(ds, 1);
      logger.info("Registered public keys for customer 'owner'");
    } catch (IOException e) {
      logger.info("No default public keys found for customer 'owner'");
    }
    try {
      final String resellerKeysPem = Files.readString(Paths.get(
              sc.getInitParameter(ManufacturerAppSettings.RESELLER_PUB_KEY_PATH)));
      manager.addCustomer(ds, 2, "reseller", resellerKeysPem);
      logger.info("Registered public keys for customer 'reseller'");
    } catch (IOException e) {
      logger.info("No default public keys found for customer 'reseller'");
    }

  }

  @Override
  public void contextDestroyed(ServletContextEvent sce) {
  }

  private DiServerService createDiService(CryptoService cs, DataSource ds, OnDieService ods) {
    return new DiServerService() {
      private DiServerStorage storage;

      @Override
      public DiServerStorage getStorage() {
        if (storage == null) {
          storage = new DiDbStorage(cs, ds, keyResolver, ods);
        }
        return storage;
      }

      @Override
      public CryptoService getCryptoService() {
        return cs;
      }
    };
  }

  // load manufacturer keystore
  private void initManufacturerKeystore(String mfgKeyStorePath, String mfgKeyStorePin) {
    try {
      if (null == mfgKeyStorePath || null == mfgKeyStorePin) {
        throw new IOException();
      }
      mfgKeyStore = KeyStore.getInstance(ManufacturerAppSettings.MFG_KEYSTORE_TYPE);
      Path keystorePath = Path.of(mfgKeyStorePath);
      if (!keystorePath.toAbsolutePath().toFile().exists()) {
        throw new IOException();
      }
      mfgKeyStore.load(new FileInputStream(keystorePath.toAbsolutePath().toFile()),
          mfgKeyStorePin.toCharArray());
    } catch (NoSuchAlgorithmException | CertificateException | IOException | KeyStoreException e) {
      logger.error("Error in loading keystore. " + e.getMessage());
    }
  }
}
