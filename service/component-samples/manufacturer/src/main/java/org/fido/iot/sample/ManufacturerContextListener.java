// Copyright 2020 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fido.iot.sample;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
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
import org.fido.iot.protocol.CloseableKey;
import org.fido.iot.protocol.Composite;
import org.fido.iot.protocol.Const;
import org.fido.iot.protocol.CryptoService;
import org.fido.iot.protocol.DiServerService;
import org.fido.iot.protocol.DiServerStorage;
import org.fido.iot.protocol.MessageDispatcher;
import org.fido.iot.protocol.MessagingService;
import org.fido.iot.storage.CertificateResolver;
import org.fido.iot.storage.DiDbManager;
import org.fido.iot.storage.DiDbStorage;

/**
 * Device Initialization servlet Context Listener.
 */
public class ManufacturerContextListener implements ServletContextListener {

  private final String ownerKeysPem = "-----BEGIN PUBLIC KEY-----\n"
      + "MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEWVUE2G0GLy8scmAOyQyhcBiF/fSU\n"
      + "d3i/Og7XDShiJb2IsbCZSRqt1ek15IbeCI5z7BHea2GZGgaK63cyD15gNA==\n"
      + "-----END PUBLIC KEY-----\n"
      + "-----BEGIN PUBLIC KEY-----\n"
      + "MHYwEAYHKoZIzj0CAQYFK4EEACIDYgAEkhES/RfsfzMFJP1N4xjlCoWTOtr/ay8b\n"
      + "fFHlXftScQIzlK4/fR/eKYInMEoB5UsIkP6Yo+oJ1AEc4MzFN83N/1U7IYMkkzxy\n"
      + "VeJJtKP1OA4NFliXFc6fC8ey6A+vthWJ\n"
      + "-----END PUBLIC KEY-----\n"
      + "-----BEGIN PUBLIC KEY-----\n"
      + "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAu5Cyn1+lblNm9zXmKQPV\n"
      + "Bhnl4K3lVYT7DJsedoqduk5tCZbinlG9/XYXvHGKFageh352xTck89LWXOTfCKUh\n"
      + "xiSE6zugN1zKnEritPm/DiP38LNmiffttVApYBidwBw9Hgq3fdFdMYAF+aLMWCZe\n"
      + "t0gLMoyrEBsg2k6Uaq5ES13G/Gvk8V8cDjFSIdidZe6P4xvHyVgkePqRfeHtJdEx\n"
      + "hlyPOt2Jqf3+h3kGDJoz6WA7xycX2hbvZ1ZA6oTvKosw8PMbDb48CteXn6Cl0BKI\n"
      + "5EC6Gjr5VtHdo0r8QCZvOw6kQPjkzqWZFDElm4aCIiixbjTH+NN8ncc9DaHCo27H\n"
      + "4wIDAQAB\n"
      + "-----END PUBLIC KEY-----\n";

  CertificateResolver keyResolver;
  private static KeyStore mfgKeyStore;

  @Override
  public void contextInitialized(ServletContextEvent sce) {
    BasicDataSource ds = new BasicDataSource();

    ServletContext sc = sce.getServletContext();
    ds.setUrl(sc.getInitParameter(ManufacturerAppSettings.DB_URL));
    ds.setDriverClassName(ManufacturerAppSettings.H2_DRIVER);
    ds.setUsername(sc.getInitParameter(ManufacturerAppSettings.DB_USER));
    ds.setPassword(sc.getInitParameter(ManufacturerAppSettings.DB_PWD));

    System.out.println(ds.getUrl());

    ds.setMinIdle(5);
    ds.setMaxIdle(10);
    ds.setMaxOpenPreparedStatements(100);

    CryptoService cs = new CryptoService();

    sc.setAttribute("datasource", ds);
    sc.setAttribute("cryptoservice", cs);

    initManufacturerKeystore(sc.getInitParameter(ManufacturerAppSettings.MFG_KEYSTORE_PWD));
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
            System.out.println("Unable to retrieve Private Key. " + e.getMessage());
          }
        }
        throw new RuntimeException();
      }

      @Override
      public Certificate[] getCertChain(int publicKeyType) {
        String algName;
        switch (publicKeyType) {
          case 1:
          case 4:
            algName = Const.RSA_ALG_NAME;
            break;
          case 13:
          case 14:
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
                  && certificateChain[0].getPublicKey().getAlgorithm().equals(algName)) {
                return certificateChain;
              }
            }
          } catch (KeyStoreException e) {
            System.out.println("Unable to retrieve Certificate chain. " + e.getMessage());
          }
        }
        throw new RuntimeException();
      }
    };

    MessageDispatcher dispatcher = new MessageDispatcher() {
      @Override
      protected MessagingService getMessagingService(Composite request) {
        return createDiService(cs, ds);
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
          sc.log(e.getMessage());
        }
        sc.log(writer.toString());
      }
    };
    sc.setAttribute(Const.DISPATCHER_ATTRIBUTE, dispatcher);
    sc.setAttribute("resolver", keyResolver);

    //create tables
    DiDbStorage db = new DiDbStorage(cs, ds, keyResolver);
    DiDbManager manager = new DiDbManager();
    manager.createTables(ds);
    manager.addCustomer(ds, 1, "owner", ownerKeysPem);
    manager.setAutoEnroll(ds, 1);

  }

  @Override
  public void contextDestroyed(ServletContextEvent sce) {
  }

  private DiServerService createDiService(CryptoService cs, DataSource ds) {
    return new DiServerService() {
      private DiServerStorage storage;

      @Override
      public DiServerStorage getStorage() {
        if (storage == null) {
          storage = new DiDbStorage(cs, ds, keyResolver);
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
  private void initManufacturerKeystore(String mfgKeyStorePin) {
    try {
      if (null == mfgKeyStorePin) {
        throw new IOException();
      }
      if (null == mfgKeyStore) {
        mfgKeyStore = KeyStore.getInstance(ManufacturerAppSettings.MFG_KEYSTORE_TYPE);
        mfgKeyStore.load(null, mfgKeyStorePin.toCharArray());
      }
    } catch (NoSuchAlgorithmException | CertificateException | IOException | KeyStoreException e) {
      System.out.println("Error in loading keystore. " + e.getMessage());
    }
  }
}
