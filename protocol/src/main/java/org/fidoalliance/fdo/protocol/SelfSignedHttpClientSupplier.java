package org.fidoalliance.fdo.protocol;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContextBuilder;

public class SelfSignedHttpClientSupplier implements HttpClientSupplier {

  static LoggerService logger = new LoggerService(SelfSignedHttpClientSupplier.class);
  static private SSLConnectionSocketFactory socketFactory = buildFactory();

  static SSLConnectionSocketFactory buildFactory() {

    try {
      logger.warn("Using SSL self-signed certificate trust strategy for Http Clients");
      SSLContextBuilder builder = new SSLContextBuilder();
      builder.loadTrustMaterial(null, new TrustSelfSignedStrategy());
      return new SSLConnectionSocketFactory(
          builder.build());
    } catch (NoSuchAlgorithmException e) {
      throw new RuntimeException(e);
    } catch (KeyStoreException | KeyManagementException e) {
      throw new RuntimeException(e);
    }


  }
  @Override
  public CloseableHttpClient get() throws IOException {
    return HttpClients.custom().setSSLSocketFactory(
        socketFactory).useSystemProperties().build();
  }
}
