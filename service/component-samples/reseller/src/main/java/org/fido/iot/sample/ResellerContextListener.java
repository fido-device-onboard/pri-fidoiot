// Copyright 2020 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fido.iot.sample;

import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.Provider;
import java.security.Security;
import java.security.cert.CertificateException;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import org.apache.commons.dbcp2.BasicDataSource;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.fido.iot.protocol.CryptoService;
import org.fido.iot.protocol.KeyStoreResolver;

public class ResellerContextListener implements ServletContextListener {

  private final String ownerKeysPem = "-----BEGIN PUBLIC KEY-----\n"
      + "MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEWVUE2G0GLy8scmAOyQyhcBiF/fSU\n"
      + "d3i/Og7XDShiJb2IsbCZSRqt1ek15IbeCI5z7BHea2GZGgaK63cyD15gNA==\n"
      + "-----END PUBLIC KEY-----\n"
      + "-----BEGIN PUBLIC KEY-----\n"
      + "MHYwEAYHKoZIzj0CAQYFK4EEACIDYgAE4RFfGVQdojLIODXnUT6NqB6KpmmPV2Rl\n"
      + "aVWXzdDef83f/JT+/XLPcpAZVoS++pwZpDoCkRU+E2FqKFdKDDD4g7obfqWd87z1\n"
      + "EtjdVaI1qiagqaSlkul2oQPBAujpIaHZ\n"
      + "-----END PUBLIC KEY-----\n"
      + "-----BEGIN PUBLIC KEY-----\n"
      + "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAwTWjO2WTkQJSRuf1sIlx\n"
      + "365VxOxdIAnDZu/GYNMg8oKDapg0uvi/DguFkrxbs3AtRHGWdONYXbGd1ZsGcVY9\n"
      + "DsCDR5R5+NCx8EEYfYSbz88dvncJMEq7iJiQXNdaj9dCHuZqaj5LGChBcLLldynX\n"
      + "mx3ZDE780aKPGomjeXEqcWgpeb0L4O+vGxkvz42C1XtvlsjBNPGKAjMM6xRPkorL\n"
      + "SfC1P0XyER3kqVYc4/cM9FyO7/vHLwH9byPCV4WbUpkti/bEtPs9xLnEtYP0oV30\n"
      + "PcdFVOg8hcuaEy6GoseU1EhlpgWJeBsbHMTlOB20JJa0kfFzREaJENyH6nHW3bSU\n"
      + "AwIDAQAB\n"
      + "-----END PUBLIC KEY-----";

  @Override
  public void contextInitialized(ServletContextEvent sce) {
    BasicDataSource ds = new BasicDataSource();

    ServletContext sc = sce.getServletContext();
    ds.setUrl(sc.getInitParameter(ResellerAppConstants.DB_URL));
    ds.setDriverClassName(sc.getInitParameter(ResellerAppConstants.DB_DRIVER));
    ds.setUsername(sc.getInitParameter(ResellerAppConstants.DB_USER));
    ds.setPassword(sc.getInitParameter(ResellerAppConstants.DB_PWD));

    System.out.println(ds.getUrl());

    ds.setMinIdle(5);
    ds.setMaxIdle(10);
    ds.setMaxOpenPreparedStatements(100);

    CryptoService cs = new CryptoService();

    sc.setAttribute("datasource", ds);
    sc.setAttribute("cryptoservice", cs);
    KeyStoreResolver resolver = new KeyStoreResolver() {
      @Override
      protected String getPassword() {
        return ResellerConfigLoader.loadConfig(ResellerAppConstants.KEYSTORE_PWD);
      }

      @Override
      protected String getKeyStoreType() {
        return ResellerConfigLoader.loadConfig(ResellerAppConstants.KEYSTORE_TYPE);
      }

      @Override
      protected String getKeyStorePath() {
        return ResellerConfigLoader.loadConfig(ResellerAppConstants.KEYSTORE_PATH);
      }
    };

    sc.setAttribute("keyresolver", resolver);
    sc.setAttribute("keystore", resolver.getKeyStore());
    sc.setAttribute("keystore_password",
        ResellerConfigLoader.loadConfig(ResellerAppConstants.KEYSTORE_PWD));

    ResellerDbManager dbManager = new ResellerDbManager();
    dbManager.createTables(ds);
    dbManager.defineKeySet(ds, ownerKeysPem, "owner", 1);
  }

}
