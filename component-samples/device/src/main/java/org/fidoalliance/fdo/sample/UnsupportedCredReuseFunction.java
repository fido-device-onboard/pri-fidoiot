// Copyright 2022 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fidoalliance.fdo.sample;

import java.io.IOException;
import org.fidoalliance.fdo.protocol.dispatch.CredReuseFunction;

public class UnsupportedCredReuseFunction implements CredReuseFunction {

  @Override
  public Boolean apply(Boolean reuseFlag) throws IOException {
    if (reuseFlag) {
      return false;
    }
    return true;
  }
}
