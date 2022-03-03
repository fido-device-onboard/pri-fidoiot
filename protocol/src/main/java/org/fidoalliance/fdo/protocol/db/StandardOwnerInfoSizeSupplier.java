package org.fidoalliance.fdo.protocol.db;

import java.io.IOException;
import org.fidoalliance.fdo.protocol.BufferUtils;
import org.fidoalliance.fdo.protocol.dispatch.OwnerInfoSizeSupplier;
import org.fidoalliance.fdo.protocol.entity.OnboardingConfig;

public class StandardOwnerInfoSizeSupplier implements OwnerInfoSizeSupplier {

  @Override
  public Integer get() throws IOException {

    OnboardingConfig config = new OnboardConfigSupplier().get();
    return config.getMaxServiceInfoSize();
  }
}
