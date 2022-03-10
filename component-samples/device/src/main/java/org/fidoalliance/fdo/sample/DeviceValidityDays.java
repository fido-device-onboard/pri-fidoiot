// Copyright 2022 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fidoalliance.fdo.sample;

import java.io.IOException;
import org.fidoalliance.fdo.protocol.dispatch.ValidityDaysSupplier;

public class DeviceValidityDays implements ValidityDaysSupplier {

  @Override
  public Integer get() throws IOException {
    return 360 * 10;//the validity days for the device cert request
  }
}
