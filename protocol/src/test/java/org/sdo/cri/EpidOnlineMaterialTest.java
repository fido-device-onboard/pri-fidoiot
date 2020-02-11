// Copyright 2020 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.sdo.cri;

import java.net.http.HttpClient;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

class EpidOnlineMaterialTest {

  final byte[] gid = {
      0x00,
      0x00,
      0x00,
      0x0D,
      (byte) 0xDD,
      (byte) 0xDD,
      (byte) 0xCC,
      (byte) 0xCC,
      0x00,
      0x00,
      0x00,
      0x00,
      (byte) 0xEE,
      (byte) 0xEE,
      (byte) 0xEE,
      0x05
  };

  @Test
  @Disabled
  void test() throws Exception {
    HttpClient c = HttpClient.newBuilder().build();
    EpidOnlineMaterial e = new EpidOnlineMaterial(EpidConstants.sandboxEpidUrlDefault.toURI(), c);
    byte[] b = e.readEpidRestService(gid, EpidLib.EpidVersion.EPID_2_0, EpidLib.MaterialId.SIGRL);
    Assertions.assertArrayEquals(new byte[]{}, b);
  }
}