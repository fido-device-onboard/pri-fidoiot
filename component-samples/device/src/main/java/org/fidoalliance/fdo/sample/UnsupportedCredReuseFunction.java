// Copyright 2022 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fidoalliance.fdo.sample;

import java.io.IOException;
import org.fidoalliance.fdo.protocol.CredentialReuseException;
import org.fidoalliance.fdo.protocol.LoggerService;
import org.fidoalliance.fdo.protocol.dispatch.CredReuseFunction;

public class UnsupportedCredReuseFunction implements CredReuseFunction {
  private static final LoggerService logger = new LoggerService(UnsupportedCredReuseFunction.class);

  @Override
  public Boolean apply(Boolean reuseFlag) throws IOException {
    if (reuseFlag) {
      logger.warn("Device doesn't support credential reuse");
      throw new CredentialReuseException();
    }
    return true;
  }
}
