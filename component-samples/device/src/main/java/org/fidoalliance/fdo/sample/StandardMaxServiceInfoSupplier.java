package org.fidoalliance.fdo.sample;

import java.io.IOException;
import org.fidoalliance.fdo.protocol.Config;
import org.fidoalliance.fdo.protocol.dispatch.MaxServiceInfoSupplier;


public class StandardMaxServiceInfoSupplier implements MaxServiceInfoSupplier {

  @Override
  public Integer get() throws IOException {
    DeviceConfig config = Config.getConfig(RootConfig.class).getRoot();
    return config.getSviMtu();
  }
}
