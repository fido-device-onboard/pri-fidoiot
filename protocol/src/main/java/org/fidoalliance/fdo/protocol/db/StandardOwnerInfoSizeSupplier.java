// Copyright 2022 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fidoalliance.fdo.protocol.db;

import java.io.IOException;
import org.fidoalliance.fdo.protocol.dispatch.OwnerInfoSizeSupplier;
import org.fidoalliance.fdo.protocol.entity.OnboardingConfig;

public class StandardOwnerInfoSizeSupplier implements OwnerInfoSizeSupplier {

  @Override
  public Integer get() throws IOException {

    OnboardingConfig config = new OnboardConfigSupplier().get();
    return config.getMaxServiceInfoSize();
  }
}
