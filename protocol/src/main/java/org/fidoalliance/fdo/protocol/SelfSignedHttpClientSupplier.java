// Copyright 2022 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fidoalliance.fdo.protocol;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.Enumeration;
import java.util.Map;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContextBuilder;

public class SelfSignedHttpClientSupplier implements HttpClientSupplier {

  private static final LoggerService logger = new LoggerService(SelfSignedHttpClientSupplier.class);
  private static final SSLConnectionSocketFactory socketFactory = buildFactory();

  static SSLConnectionSocketFactory buildFactory() {

    try {
      logger.warn("Using SSL self-signed certificate trust strategy for Http Clients");

      logger.info("Env vars:");
      Map<String, String> envMap = System.getenv();
      for (Map.Entry<String, String> entry : envMap.entrySet()) {
        logger.info(entry.getKey() + ":" + entry.getValue());
      }
      logger.info("System vars:");
      Enumeration names = System.getProperties().propertyNames();
      while (names.hasMoreElements()) {
        String key = names.nextElement().toString();
        logger.info(key + ": " + System.getProperties().getProperty(key));
      }

      SSLContextBuilder builder = new SSLContextBuilder();
      builder.loadTrustMaterial(null, new TrustSelfSignedStrategy());
      return new SSLConnectionSocketFactory(
          builder.build());
      //print env and propertye

    } catch (NoSuchAlgorithmException e) {
      throw new RuntimeException(e);
    } catch (KeyStoreException | KeyManagementException e) {
      throw new RuntimeException(e);
    }


  }

  @Override
  public CloseableHttpClient get() throws IOException {

    CredentialsProvider provider = new BasicCredentialsProvider();
    String user = "apiUser";
    try {
      user = Config.resolve("$(api_user)");
    } catch (Exception err) {
      //nothing to do
    }

    String apiPassword = null;
    try {
      apiPassword = Config.resolve("$(api_password)");
    } catch (Exception err) {
      //nothing to do
    }

    if (apiPassword != null) {
      UsernamePasswordCredentials credentials
          = new UsernamePasswordCredentials(user, apiPassword);
      provider.setCredentials(AuthScope.ANY, credentials);
    }

    HttpClientBuilder builder = HttpClients.custom()
        .setSSLSocketFactory(
            socketFactory).useSystemProperties();

    if (provider != null) {
      builder.setDefaultCredentialsProvider(provider);
    }
    return builder.build();
  }
}
