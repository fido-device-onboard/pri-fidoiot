package org.fidoalliance.fdo.protocol.dispatch;

import java.io.IOException;
import org.apache.commons.lang3.function.FailableSupplier;
import org.fidoalliance.fdo.protocol.KeyResolver;

public interface DeviceKeySupplier extends FailableSupplier<KeyResolver, IOException> {

}
