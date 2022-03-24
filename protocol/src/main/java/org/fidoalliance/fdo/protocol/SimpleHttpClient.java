package org.fidoalliance.fdo.protocol;

import java.io.IOException;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

public class SimpleHttpClient implements HttpClientSupplier {

  private static final LoggerService logger = new LoggerService(SimpleHttpClient.class);

  public SimpleHttpClient() {
    logger.info("Using Default HTTP Client");
  }

  @Override
  public CloseableHttpClient get() throws IOException {
    return HttpClients.createDefault();
  }
}
