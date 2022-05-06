package org.fidoalliance.fdo.protocol.db;

import java.io.IOException;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

public class StandardServiceInfoClientSupplier implements ServiceInfoHttpClientSupplier {

  @Override
  public CloseableHttpClient get() throws IOException {
    return HttpClients.createSystem();
  }
}
