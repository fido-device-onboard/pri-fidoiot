// Copyright 2022 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fidoalliance.fdo.protocol;

import org.fidoalliance.fdo.protocol.dispatch.CredReuseFunction;

import java.io.IOException;

public class UnsupportedCredReuseFunction implements CredReuseFunction {

  @Override
  public Boolean apply(Boolean reuseFlag) throws IOException {
    if (reuseFlag) {
      throw new CredentialReuseException();
    }
    return true;
  }
}
