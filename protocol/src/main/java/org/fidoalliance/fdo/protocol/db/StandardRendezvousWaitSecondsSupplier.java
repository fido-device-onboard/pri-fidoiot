// Copyright 2022 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fidoalliance.fdo.protocol.db;

import java.io.IOException;
import java.time.Duration;
import org.fidoalliance.fdo.protocol.dispatch.RendezvousWaitSecondsSupplier;

public class StandardRendezvousWaitSecondsSupplier implements RendezvousWaitSecondsSupplier {

  @Override
  public Long get() throws IOException {
    return Duration.ofDays(1).toSeconds();
  }
}
