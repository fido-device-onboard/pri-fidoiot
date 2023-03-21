package org.fidoalliance.fdo.sample;

import java.io.IOException;
import org.fidoalliance.fdo.protocol.Config;
import org.fidoalliance.fdo.protocol.LoggerService;
import org.fidoalliance.fdo.protocol.dispatch.MaxServiceInfoSupplier;


public class StandardMaxServiceInfoSupplier implements MaxServiceInfoSupplier {
  private static final LoggerService logger =
          new LoggerService(StandardMaxServiceInfoSupplier.class);

  @Override
  public Integer get() throws IOException {
    DeviceConfig config = Config.getConfig(RootConfig.class).getRoot();
    logger.info("SVI MTU value: " + config.getSviMtu());
    return config.getSviMtu();
  }
}
