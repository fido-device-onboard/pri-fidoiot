// Copyright 2020 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.sdo.cri;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.ByteBuffer;
import java.security.SecureRandom;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class EcdhKeyExchangeTest {

  private static SecureRandom secureRandom;

  @BeforeAll
  static void beforeAll() throws Exception {
    secureRandom = SecureRandom.getInstance("SHA1PRNG");
  }

  @Test
  void p256_generateSharedSecret_secretsMatch() throws Exception {

    KeyExchange kxa = new EcdhKeyExchange.P256.Owner(secureRandom);
    KeyExchange kxb = new EcdhKeyExchange.P256.Device(secureRandom);

    ByteBuffer shSeA = kxa.generateSharedSecret(kxb.getMessage());
    ByteBuffer shSeB = kxb.generateSharedSecret(kxa.getMessage());
    assertEquals(shSeA, shSeB);
  }

  @Test
  void p384_generateSharedSecret_secretsMatch() throws Exception {

    KeyExchange kxa = new EcdhKeyExchange.P384.Owner(secureRandom);
    KeyExchange kxb = new EcdhKeyExchange.P384.Device(secureRandom);

    ByteBuffer shSeA = kxa.generateSharedSecret(kxb.getMessage());
    ByteBuffer shSeB = kxb.generateSharedSecret(kxa.getMessage());
    assertEquals(shSeA, shSeB);
  }
}
