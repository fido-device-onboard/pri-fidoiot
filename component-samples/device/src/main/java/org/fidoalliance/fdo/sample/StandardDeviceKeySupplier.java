package org.fidoalliance.fdo.sample;

import java.io.IOException;
import org.fidoalliance.fdo.protocol.Config;
import org.fidoalliance.fdo.protocol.KeyResolver;
import org.fidoalliance.fdo.protocol.dispatch.DeviceKeySupplier;

public class StandardDeviceKeySupplier implements DeviceKeySupplier {


  @Override
  public KeyResolver get() throws IOException {
    DeviceConfig config = Config.getConfig(RootConfig.class).getRoot();
    //the key resolver will only have the selected key
    KeyResolver keyResolver = new KeyResolver();
    keyResolver.load(config.getKeyStoreConfig());
    return keyResolver;
  }
}
