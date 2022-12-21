// Copyright 2022 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fidoalliance.fdo.protocol;

import org.fidoalliance.fdo.protocol.dispatch.CredReuseFunction;

import java.io.IOException;

public class StandardCredReuseFunction implements CredReuseFunction {

  @Override
  public Boolean apply(Boolean reuseFlag) throws IOException {
    return true;
  }
}
