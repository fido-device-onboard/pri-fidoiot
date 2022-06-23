package org.fidoalliance.fdo.protocol.db;

import java.io.IOException;
import org.apache.commons.lang3.function.FailableSupplier;
import org.apache.http.impl.client.CloseableHttpClient;

public interface ServiceInfoHttpClientSupplier extends
    FailableSupplier<CloseableHttpClient, IOException> {

}
