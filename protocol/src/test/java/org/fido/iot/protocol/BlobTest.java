// Copyright 2020 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fido.iot.protocol;

import org.junit.jupiter.api.Test;

public class BlobTest {

  @Test
  void Test() throws Exception {
    Composite blob = RendezvousBlobDecoder.decode("http://localhost:8042?ipaddress=127.0.0.1");

  }
}
