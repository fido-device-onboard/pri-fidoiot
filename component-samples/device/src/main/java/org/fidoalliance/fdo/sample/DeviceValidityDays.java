// Copyright 2022 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fidoalliance.fdo.sample;

import java.io.IOException;
import org.fidoalliance.fdo.protocol.LoggerService;
import org.fidoalliance.fdo.protocol.dispatch.ValidityDaysSupplier;

public class DeviceValidityDays implements ValidityDaysSupplier {
  LoggerService logger = new LoggerService(DeviceValidityDays.class);

  @Override
  public Integer get() throws IOException {
    logger.debug("Validity days for device cert request is 365 * 10");
    return 360 * 10;//the validity days for the device cert request
  }
}
