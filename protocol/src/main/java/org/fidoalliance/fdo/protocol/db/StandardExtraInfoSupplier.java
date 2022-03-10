// Copyright 2022 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fidoalliance.fdo.protocol.db;

import java.io.IOException;
import org.fidoalliance.fdo.protocol.dispatch.ExtraInfoSupplier;

public class StandardExtraInfoSupplier implements ExtraInfoSupplier {

  @Override
  public byte[] get() throws IOException {
    return null;
  }
}
