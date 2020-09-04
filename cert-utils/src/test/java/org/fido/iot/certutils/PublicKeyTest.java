// Copyright 2020 Intel Corporation
// SPDX-License-Identifier: Apache 2.0
package org.fido.iot.certutils;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.security.PublicKey;
import java.util.List;
import org.junit.jupiter.api.Test;

public class PublicKeyTest {

  String keysPem = "-----BEGIN PUBLIC KEY-----\n"
      + "MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEWVUE2G0GLy8scmAOyQyhcBiF/fSU\n"
      + "d3i/Og7XDShiJb2IsbCZSRqt1ek15IbeCI5z7BHea2GZGgaK63cyD15gNA==\n"
      + "-----END PUBLIC KEY-----\n";

  @Test
  void Test() throws Exception {

    List<PublicKey> keys = PemLoader.loadPublicKeys(keysPem);
    assertTrue(keys.size() > 0);

  }
}
