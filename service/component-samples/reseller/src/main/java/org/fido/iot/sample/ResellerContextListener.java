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

  @Override
  public void contextInitialized(ServletContextEvent sce) {
    BasicDataSource ds = new BasicDataSource();

    ServletContext sc = sce.getServletContext();
    ds.setUrl(sc.getInitParameter(ResellerAppConstants.DB_URL));
    ds.setDriverClassName(sc.getInitParameter(ResellerAppConstants.DB_DRIVER));
    ds.setUsername(sc.getInitParameter(ResellerAppConstants.DB_USER));
    ds.setPassword(sc.getInitParameter(ResellerAppConstants.DB_PWD));

    sc.log(ds.getUrl());

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

  }

}
