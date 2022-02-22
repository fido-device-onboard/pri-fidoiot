package org.fidoalliance.fdo.protocol;

import java.io.IOException;
import org.apache.commons.lang3.function.FailableSupplier;
import org.apache.http.impl.client.CloseableHttpClient;

public interface HttpClientSupplier extends FailableSupplier<CloseableHttpClient, IOException> {

}
