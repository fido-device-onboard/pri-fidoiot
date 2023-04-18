package org.fidoalliance.fdo.protocol.db;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
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
import org.fidoalliance.fdo.protocol.Config;
import org.fidoalliance.fdo.protocol.LoggerService;
import org.fidoalliance.fdo.protocol.SelfSignedHttpClientSupplier;

public class BasicServiceInfoClientSupplier implements ServiceInfoHttpClientSupplier {

  private static final LoggerService logger = new LoggerService(SelfSignedHttpClientSupplier.class);
  private static final SSLConnectionSocketFactory socketFactory = buildFactory();

  static SSLConnectionSocketFactory buildFactory() {

    try {
      logger.warn("Using SSL self-signed certificate trust strategy for service info Http Clients");
      SSLContextBuilder builder = new SSLContextBuilder();
      builder.loadTrustMaterial(null, new TrustSelfSignedStrategy());
      return new SSLConnectionSocketFactory(
          builder.build());
    } catch (NoSuchAlgorithmException e) {
      logger.error(e.getMessage());
      throw new RuntimeException(e);
    } catch (KeyStoreException | KeyManagementException e) {
      logger.error("Error in Key management or storage");
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
      logger.warn("Using Basic Auth for service info Http Clients");

      builder.setDefaultCredentialsProvider(provider);
    }
    return builder.build();
  }
}
